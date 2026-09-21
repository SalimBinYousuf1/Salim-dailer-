package com.example.ui.screens.contacts

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactItem
import com.example.data.repository.BlockedNumberRepository
import com.example.data.repository.ContactsRepository
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactsUiState(
    val isLoading: Boolean = true,
    val contacts: List<ContactItem> = emptyList(),
    val filteredContacts: List<ContactItem> = emptyList(),
    val searchQuery: String = "",
    val selectedContact: ContactItem? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsRepo = ContactsRepository(application)
    private val blockedRepo = BlockedNumberRepository(application)

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.contacts.isEmpty())
            val list = contactsRepo.getContacts()
            _uiState.value = _uiState.value.copy(
                contacts = list,
                filteredContacts = filterList(list, _uiState.value.searchQuery),
                isLoading = false
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredContacts = filterList(_uiState.value.contacts, query)
        )
    }

    private fun filterList(list: List<ContactItem>, query: String): List<ContactItem> {
        if (query.isBlank()) return list
        val clean = query.trim().lowercase()
        return list.filter { contact ->
            contact.displayName.lowercase().contains(clean) ||
                    contact.phoneNumbers.any { it.number.contains(clean) }
        }
    }

    fun selectContact(contact: ContactItem) {
        viewModelScope.launch {
            val detailed = contactsRepo.getContactDetails(contact.id) ?: contact
            _uiState.value = _uiState.value.copy(selectedContact = detailed)
        }
    }

    fun clearSelectedContact() {
        _uiState.value = _uiState.value.copy(selectedContact = null)
    }

    fun toggleStar(contact: ContactItem) {
        viewModelScope.launch {
            val newStar = !contact.isStarred
            contactsRepo.toggleStar(contact.id, newStar)
            selectContact(contact.copy(isStarred = newStar))
            loadContacts()
        }
    }

    fun callContact(number: String) {
        TelecomHelper.placeCall(getApplication(), number)
    }

    fun messageContact(number: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun blockContact(number: String) {
        viewModelScope.launch {
            val res = blockedRepo.blockNumber(number)
            val msg = if (res) "Blocked $number" else "Unable to block"
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteContact(contactId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val success = contactsRepo.deleteContact(contactId)
            if (success) {
                Toast.makeText(getApplication(), "Contact deleted", Toast.LENGTH_SHORT).show()
                clearSelectedContact()
                loadContacts()
                onDeleted()
            }
        }
    }

    fun addContact(name: String, number: String, email: String?, onAdded: () -> Unit) {
        viewModelScope.launch {
            val success = contactsRepo.addContact(name, number, email)
            if (success) {
                Toast.makeText(getApplication(), "Contact created", Toast.LENGTH_SHORT).show()
                loadContacts()
                onAdded()
            } else {
                Toast.makeText(getApplication(), "Failed to create contact", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
