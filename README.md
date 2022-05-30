# undoredomanager
Lightweight library implementing an undo-redo system for Kotlin
-----
The library consists of three clases:

1. **UndoRedoable**:  derived classes must implement **#undo**, **#redo** methods to define invertible actions
2. **UndoRedoTransaction**: defines a undoredoable **transaction** *(multiple actions treated atomically)*. Main method **#add** for adding single actions or another transactions to this one
3. **UndoRedoManager**: stack with all added undoredoable actions and transactions. Main method **#addItem** for adding new single actions or transactions

-----


**Example with StringBuilder**

In this test example, a **StringBuilder** is modified and changes are registered in the undo-redo manger using three derived classes from **UndoRedoable** ;
1. *UndoRedoableAppendText*
2. *UndoRedoableInsertText*
3. *UndoRedoableClearText*

~~~ kotlin
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
            override fun getDescription() = "Clear and append $txt"
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
        println( "charBuffer: \"$charBuffer\"" )
    }

    @AfterAll
    fun afterAll()
    {
        urm.clearAll()
    }
}

class UndoRedoableAppendText( private val buffer: StringBuilder, private val txt: String ) : UndoRedoable()
{
    override fun getDescription() = "append \"$txt\""

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
    override fun getDescription() = "insert in $idx the text \"$txt\""

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
~~~

> **Output:**</br>
charBuffer: "Lorem ipsum"</br>
charBuffer: "Lorem ipsum, consectetur adipiscing elit."</br>
charBuffer: "Lorem ipsum, consectetur adipiscing elit."</br>
charBuffer: "Lorem ipsum, consectetur adipiscing elit."</br>
charBuffer: "Hi World!!"</br>
charBuffer: "Lorem ipsum"</br>
charBuffer: "Hi World!!"</br>

        


* * *




**Source code (included in jar)**


~~~ kotlin
///////////////////////////////////////////////////////////////////////////
// UndoRedoManager is a small library to implement an undo&redo system
// Copyright (C) 2022  Miguel Alejandro Moreno Barrientos
//
// UndoRedoManager is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// UndoRedoManager is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
///////////////////////////////////////////////////////////////////////////

package undoredomanager


typealias SubscriberAction = (UndoRedoable?) -> Unit

/**
 * Base class for any UndoRedoable action
 */
abstract class UndoRedoable
{
    val subscribers = mutableListOf<SubscriberAction>()

    abstract fun undo()

    abstract fun redo()

    open fun canUndo() = true

    open fun canRedo() = true

    open fun undoDescription() = "Undo ${getDescription()}"

    open fun redoDescription() = "Redo ${getDescription()}"

    abstract fun getDescription(): String

    fun notifySubscribers() = subscribers.forEach { it( this ) }

    override fun toString() = "UndoRedoable(undoDescription=${undoDescription()}," +
                              "redoDescription=${redoDescription()}," +
                              "canUndo=${canUndo()}," +
                              "canRedo=${canRedo()})"
}

/**
 * Transaction for grouping several UndoRedoables.
 */
open class UndoRedoTransaction private constructor( private val list: MutableList<UndoRedoable> )
    : MutableList<UndoRedoable> by list, UndoRedoable()
{
    constructor(): this( mutableListOf() )

    private var undone = false

    final override fun undo()
    {
        if ( canUndo() )
        {
            for ( undoRedoable in reversed() )
                undoRedoable.undo()
            undone = true
        }
        else
            throw IllegalStateException( "Can't undo" )
    }

    final override fun redo()
    {
        if ( canRedo() )
        {
            for ( undoRedoable in this )
                undoRedoable.redo()
            undone = false
        }
        else
            throw IllegalStateException( "Can't redo" )
    }

    final override fun canUndo() = isNotEmpty() && !undone

    final override fun canRedo() = isNotEmpty() && undone

    override fun undoDescription() = if ( isNotEmpty() ) last().undoDescription() else "Empty transaction"

    override fun redoDescription() = if ( isNotEmpty() ) last().redoDescription() else "Empty transaction"

    override fun getDescription() = if ( isNotEmpty() ) last().getDescription() else "Empty transaction"

    override fun toString()
            = "UndoRedoTransaction(undoDescription=${undoDescription()}, " +
            "redoDescription=${redoDescription()}, " +
            "list=$list, " +
            "canUndo=${canUndo()}, " +
            "canRedo=${canRedo()}, " +
            "subscribers=$subscribers)"

}  // Class UndoRedoTransaction


/**
 * Main class to manage the undo-redo system. ***Note: use implemented methods to modify the manager, to use list methods
 * will cause unexpected behaviour***
 * @param limit maximum number of UndoRedoable actions.
 *                 If this number is exceeded, first actions are removed (limited queue)
 */
open class UndoRedoManager private constructor( private val list: MutableList<UndoRedoable>,
                                                private var limit: Int )
    : MutableList<UndoRedoable> by list, UndoRedoable()
{
    constructor( limit: Int = Int.MAX_VALUE ): this( mutableListOf(), limit )

    private var index = list.size - 1

    final override fun undo()
    {
        if ( canUndo() )
            this[index--].undo()
        else
            throw IllegalStateException( "Can't undo" )

        notifySubscribers()
    }

    final override fun redo()
    {
        if ( canRedo() )
            this[++index].redo()
        else
            throw IllegalStateException( "Can't redo" )

        notifySubscribers()
    }

    final override fun canUndo() = isNotEmpty() && index >= 0 && this[index].canUndo()

    final override fun canRedo() = isNotEmpty() && index < size-1 && this[index+1].canRedo()

    override fun undoDescription()
            = if ( canUndo() ) this[index].undoDescription() else "Can´t undo"

    override fun redoDescription()
            = if ( canRedo() ) this[index+1].redoDescription() else "Can't redo"

    override fun getDescription() = if ( isNotEmpty() && index >= 0 ) this[index].getDescription()
                                    else "Empty or rewound manager"

    fun addItem( undoRedo: UndoRedoable)
    {
        subList( index + 1, size ).clear()

        if ( size >= limit )
            subList( 0, size - limit + 1 ).clear()

        add( undoRedo )
        index = size - 1

        notifySubscribers()
    }

    fun clearAll()
    {
        clear()
        index = size - 1

        notifySubscribers()
    }

    override fun toString() = "UndoRedoManager(list=$list, limit=$limit, canUndo=${canUndo()}, canRedo=${canRedo()})"

}  // class UndoRedoManager
~~~ 
