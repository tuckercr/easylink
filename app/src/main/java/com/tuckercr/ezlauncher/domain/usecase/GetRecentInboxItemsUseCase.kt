package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.InboxItem
import com.tuckercr.ezlauncher.domain.repository.InboxRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a live stream of the most recent inbox items (calls + messages),
 * newest first.
 *
 * The Flow updates automatically whenever the device's call log or SMS
 * database changes, so the UI stays in sync without polling.
 */
class GetRecentInboxItemsUseCase @Inject constructor(
    private val repository: InboxRepository,
) {
    operator fun invoke(): Flow<List<InboxItem>> = repository.getRecentItems()
}
