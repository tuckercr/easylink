package com.fangjet.care.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fangjet.care.data.config.CareConfigRepository
import com.fangjet.care.data.pairing.CarePairingRepository
import com.fangjet.shared.model.ContactConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** A contact being created or edited in the dialog. */
data class ContactDraft(
    val existingId: String? = null,
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val isPrimary: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && phoneNumber.filter { it.isDigit() }.length >= 7
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel
    @Inject
    constructor(
        private val pairingRepository: CarePairingRepository,
        private val configRepository: CareConfigRepository,
    ) : ViewModel() {

        /** Live list straight from Firestore — the single source of truth. */
        val contacts: StateFlow<List<ContactConfig>> =
            pairingRepository.linkId
                .filterNotNull()
                .flatMapLatest { configRepository.observeEmergencyContacts(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        private val _draft = MutableStateFlow<ContactDraft?>(null)
        val draft: StateFlow<ContactDraft?> = _draft.asStateFlow()

        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        // ── Dialog lifecycle ──────────────────────────────────────────────────

        fun onAddClicked() {
            // First contact defaults to primary — SOS needs someone to call.
            _draft.value = ContactDraft(isPrimary = contacts.value.isEmpty())
        }

        fun onEditClicked(contact: ContactConfig) {
            _draft.value = ContactDraft(
                existingId = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                relationship = contact.relationship,
                isPrimary = contact.position == 0,
            )
        }

        fun onDraftChanged(draft: ContactDraft) {
            _draft.value = draft
        }

        fun onDismissDialog() {
            _draft.value = null
        }

        // ── Persistence — every action writes the whole list ──────────────────

        fun onSaveDraft() {
            val d = _draft.value?.takeIf { it.isValid } ?: return
            val current = contacts.value
            val id = d.existingId ?: UUID.randomUUID().toString()

            val others = current.filterNot { it.id == id }
            val updated = ContactConfig(
                id = id,
                name = d.name.trim(),
                phoneNumber = d.phoneNumber.trim(),
                relationship = d.relationship.trim(),
                position = if (d.isPrimary) 0 else others.size + 1,
            )
            // Primary lives at position 0; everyone else keeps stable order after it.
            val reordered = if (d.isPrimary) {
                listOf(updated) + others
            } else {
                others + updated
            }
            persist(reindex(reordered))
            _draft.value = null
        }

        fun onDelete(contact: ContactConfig) {
            persist(reindex(contacts.value.filterNot { it.id == contact.id }))
            _draft.value = null
        }

        private fun reindex(list: List<ContactConfig>): List<ContactConfig> = list.mapIndexed { index, c -> c.copy(position = index) }

        private fun persist(list: List<ContactConfig>) {
            viewModelScope.launch {
                _isSaving.value = true
                try {
                    val linkId = pairingRepository.linkId.filterNotNull().first()
                    configRepository.saveEmergencyContacts(linkId, list)
                } catch (_: Exception) {
                    // Firestore queues writes offline; a hard failure here is rare.
                    // The listener keeps the UI consistent with what actually stored.
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }
