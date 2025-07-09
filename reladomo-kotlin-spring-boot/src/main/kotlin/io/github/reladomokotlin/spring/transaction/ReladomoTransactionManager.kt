package io.github.reladomokotlin.spring.transaction

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraTransaction
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus

/**
 * Spring transaction manager for Reladomo.
 */
class ReladomoTransactionManager(
    private val mithraManager: MithraManager
) : AbstractPlatformTransactionManager() {
    
    override fun doGetTransaction(): Any {
        return ReladomoTransactionObject()
    }
    
    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        val txObject = transaction as ReladomoTransactionObject
        val mithraTransaction = mithraManager.startOrContinueTransaction()
        txObject.mithraTransaction = mithraTransaction
        
        // Note: Transaction timeout is configured at MithraManager level, not per transaction
        // For MVP, we'll use the default timeout configuration
    }
    
    override fun doCommit(status: DefaultTransactionStatus) {
        val txObject = status.transaction as ReladomoTransactionObject
        txObject.mithraTransaction?.commit()
    }
    
    override fun doRollback(status: DefaultTransactionStatus) {
        val txObject = status.transaction as ReladomoTransactionObject
        txObject.mithraTransaction?.rollback()
    }
    
    override fun doCleanupAfterCompletion(transaction: Any) {
        val txObject = transaction as ReladomoTransactionObject
        txObject.mithraTransaction = null
    }
    
    override fun isExistingTransaction(transaction: Any): Boolean {
        val txObject = transaction as ReladomoTransactionObject
        return txObject.mithraTransaction != null
    }
}

/**
 * Transaction object holder for Reladomo transactions.
 */
class ReladomoTransactionObject {
    var mithraTransaction: MithraTransaction? = null
}