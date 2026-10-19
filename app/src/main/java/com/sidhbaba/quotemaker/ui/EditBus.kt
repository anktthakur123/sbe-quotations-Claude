package com.sidhbaba.quotemaker.ui

/** Tiny in-memory hand-off: History tells Create which saved quotation to load, then switches tabs. */
object EditBus {
    var pendingEditQuotationId: Long? = null
}
