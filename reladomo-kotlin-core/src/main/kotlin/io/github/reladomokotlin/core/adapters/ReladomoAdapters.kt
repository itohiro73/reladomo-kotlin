package io.github.reladomokotlin.core.adapters

import com.gs.fw.common.mithra.MithraObject
import com.gs.fw.common.mithra.MithraTransactionalObject
import com.gs.fw.common.mithra.MithraDatedTransactionalObject
import com.gs.fw.common.mithra.finder.RelatedFinder
import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.ReladomoObject
import io.github.reladomokotlin.core.ReladomoFinder

/**
 * Adapter to convert MithraObject to ReladomoObject interface.
 */
class MithraObjectAdapter(private val mithraObject: MithraObject) : ReladomoObject {
    override fun insert() {
        when (mithraObject) {
            is MithraTransactionalObject -> mithraObject.insert()
            is MithraDatedTransactionalObject -> mithraObject.insert()
            else -> throw UnsupportedOperationException("Insert not supported for read-only objects")
        }
    }
    
    override fun update() {
        // MithraObject doesn't have a direct update method
        // Updates happen automatically when you modify attributes
    }
    
    override fun delete() {
        when (mithraObject) {
            is MithraTransactionalObject -> mithraObject.delete()
            else -> throw UnsupportedOperationException("Delete not supported for dated objects - use terminate()")
        }
    }
    
    override fun terminate() {
        when (mithraObject) {
            is MithraDatedTransactionalObject -> mithraObject.terminate()
            else -> throw UnsupportedOperationException("Terminate only supported for dated objects")
        }
    }
}

/**
 * Adapter to convert RelatedFinder to ReladomoFinder interface.
 */
class RelatedFinderAdapter<T : MithraObject>(
    private val finder: RelatedFinder<T>
) : ReladomoFinder<T> {
    
    override fun findOne(operation: Operation): T? {
        return finder.findOne(operation)
    }
    
    override fun findMany(operation: Operation): List<T> {
        return finder.findMany(operation)
    }
    
    override fun all(): Operation {
        return finder.all()
    }
}

/**
 * Extension function to convert MithraObject to ReladomoObject.
 */
fun MithraObject.asReladomoObject(): ReladomoObject = MithraObjectAdapter(this)

/**
 * Extension function to convert RelatedFinder to ReladomoFinder.
 */
fun <T : MithraObject> RelatedFinder<T>.asReladomoFinder(): ReladomoFinder<ReladomoObject> {
    @Suppress("UNCHECKED_CAST")
    return RelatedFinderAdapter(this) as ReladomoFinder<ReladomoObject>
}