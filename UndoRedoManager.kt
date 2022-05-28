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
