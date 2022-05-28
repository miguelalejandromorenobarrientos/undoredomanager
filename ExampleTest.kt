import org.junit.jupiter.api.*
import undoredomanager.UndoRedoManager
import undoredomanager.UndoRedoTransaction
import undoredomanager.UndoRedoable
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val urm = UndoRedoManager()
private val charBuffer = StringBuilder()

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestUndoRedoManager
{
    @Test
    @Order(1)
    fun append1()
    {
        assertTrue { charBuffer.isEmpty() }

        ////////////////// UndoRedoable Action ///////////////////
        "Lorem ipsum".also {
            charBuffer.append( it )
            urm.addItem( UndoRedoableAppendText( charBuffer, it ) )
        }
        //////////////////////////////////////////////////////////

        assertTrue( urm.getDescription().also { urm.undo() } ) { charBuffer.isEmpty() }
        urm.redoDescription().also {
            urm.redo()
            assertEquals( "Lorem ipsum", charBuffer.toString(), it )
        }

    }

    @Test
    @Order(2)
    fun append2()
    {
        assertTrue { charBuffer.isNotEmpty() }

        ////////////////// UndoRedoable Action ///////////////////
        ", consectetur adipiscing elit.".also {
            charBuffer.append( it )
            urm.addItem( UndoRedoableAppendText( charBuffer, it ) )
        }
        //////////////////////////////////////////////////////////

        urm.undoDescription().also {
            urm.undo()
            assertEquals( "Lorem ipsum", charBuffer.toString(), it )
        }
        urm.redoDescription().also {
            urm.redo()
            assertEquals( "Lorem ipsum, consectetur adipiscing elit.", charBuffer.toString(), it )
        }
    }

    @Test
    @Order(3)
    fun clear1()
    {
        assertTrue { charBuffer.isNotEmpty() }

        ////////////////// UndoRedoable Action ///////////////////
        urm.addItem( UndoRedoableClearText( charBuffer ) )  // must be before clear buffer
        charBuffer.clear()
        //////////////////////////////////////////////////////////

        assertTrue( urm.getDescription() ) { charBuffer.isEmpty() }
        urm.undoDescription().also {
            urm.undo()
            assertEquals( "Lorem ipsum, consectetur adipiscing elit.", charBuffer.toString(), it )
        }
        assertTrue( urm.redoDescription().also { urm.redo() } ) { charBuffer.isEmpty() }
        urm.undoDescription().also {
            urm.undo()
            assertEquals( "Lorem ipsum, consectetur adipiscing elit.", charBuffer.toString(), it )
        }
    }

    @Test
    @Order(4)
    fun insert1()
    {
        assertTrue { charBuffer.isNotEmpty() }

        ////////////////// UndoRedoable Action ///////////////////
        "FOO BAR".also {
            charBuffer.insert( 12, it )
            urm.addItem( UndoRedoableInsertText( charBuffer, 12, it ) )
        }
        //////////////////////////////////////////////////////////

        urm.undoDescription().also {
            urm.undo()
            assertEquals( "Lorem ipsum, consectetur adipiscing elit.", charBuffer.toString(), it )
        }
        urm.redoDescription().also {
            urm.redo()
            assertEquals( "Lorem ipsum,FOO BAR consectetur adipiscing elit.", charBuffer.toString(), it )
        }
        urm.undoDescription().also {
            urm.undo()
            assertEquals( "Lorem ipsum, consectetur adipiscing elit.", charBuffer.toString(), it )
        }
    }

    @Test
    @Order(5)
    fun transaction1()
    {
        assertTrue { charBuffer.isNotEmpty() }

        val txt = "Hi World!!"

        ////////////////// UndoRedoable Transaction ///////////////////
        val transaction = object: UndoRedoTransaction() {
            override fun getDescription() = """Clear and append "$txt""""
            override fun redoDescription() = "Redo ${getDescription()}"
            override fun undoDescription() = "Undo ${getDescription()}"
        }
        transaction.add( UndoRedoableClearText( charBuffer ) )  // must be before clear buffer
        charBuffer.clear()
        charBuffer.append( txt )
        transaction.add( UndoRedoableAppendText( charBuffer, txt ) )
        urm.addItem( transaction )
        //////////////////////////////////////////////////////////////

        assertEquals( txt, charBuffer.toString(), urm.getDescription() )

        urm.undoDescription().also {
            urm.undo()
            assertEquals( "Lorem ipsum, consectetur adipiscing elit.", charBuffer.toString(), it )
        }
        urm.redoDescription().also {
            urm.redo()
            assertEquals( txt, charBuffer.toString(), it )
        }
    }

    @Test
    @Order(6)
    fun twoUndos1()
    {
        assertTrue { charBuffer.isNotEmpty() }

        repeat(2) { urm.undo() }

        assertEquals( "Lorem ipsum", charBuffer.toString() )
    }

    @Test
    @Order(7)
    fun twoRedos1()
    {
        assertTrue { charBuffer.isNotEmpty() }

        repeat(2) { urm.redo() }

        assertEquals( "Hi World!!", charBuffer.toString() )
    }

    @AfterEach
    fun afterEach()
    {
        println( """charBuffer: "$charBuffer"""" )
    }

    @AfterAll
    fun afterAll()
    {
        urm.clearAll()
    }
}

class UndoRedoableAppendText( private val buffer: StringBuilder, private val txt: String ) : UndoRedoable()
{
    override fun getDescription() = """append "$txt""""

    override fun redo()
    {
        buffer.append( txt )
    }

    override fun undo()
    {
        buffer.delete( buffer.length - txt.length, buffer.length )
    }
}

class UndoRedoableInsertText( private val buffer: StringBuilder, private val idx: Int = 0, private val txt: String )
    : UndoRedoable()
{
    override fun getDescription() = """insert in $idx the text "$txt""""

    override fun redo()
    {
        buffer.insert( idx, txt )
    }

    override fun undo()
    {
        buffer.delete( idx, idx + txt.length )
    }
}

class UndoRedoableClearText( private val buffer: StringBuilder ) : UndoRedoable()
{
    private val oldBuffer = StringBuilder( buffer )

    override fun getDescription() = "clear buffer"

    override fun redo()
    {
        buffer.clear()
    }

    override fun undo()
    {
        buffer.append( oldBuffer )
    }
}
