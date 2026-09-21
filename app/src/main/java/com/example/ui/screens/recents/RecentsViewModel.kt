package com.example.ui.screens.recents

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallLogEntry
import com.example.data.repository.BlockedNumberRepository
import com.example.data.repository.CallLogRepository
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecentsUiState(
    val isLoading: Boolean = true,
    val filterMissed: Boolean = false,
    val callLogs: List<CallLogEntry> = emptyList(),
    val isDefaultDialer: Boolean = true
)

class RecentsViewModel(application: Application) : AndroidViewModel(application) {

    private val callLogRepo = CallLogRepository(application)
    private val blockedRepo = BlockedNumberRepository(application)

    private val _uiState = MutableStateFlow(RecentsUiState())
    val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()

    init {
        loadCallLogs()
    }

    fun setFilter(missedOnly: Boolean) {
        _uiState.value = _uiState.value.copy(filterMissed = missedOnly)
        loadCallLogs()
    }

    fun loadCallLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.callLogs.isEmpty(),
                isDefaultDialer = TelecomHelper.isDefaultDialer(getApplication())
            )
            val logs = callLogRepo.getCallLogs(filterMissedOnly = _uiState.value.filterMissed)
            _uiState.value = _uiState.value.copy(
                callLogs = logs,
                isLoading = false
            )
        }
    }

    fun callBack(number: String) {
        TelecomHelper.placeCall(getApplication(), number)
    }

    fun sendSms(number: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyNumber(number: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Phone Number", number)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun deleteCall(entry: CallLogEntry) {
        viewModelScope.launch {
            callLogRepo.deleteCall(entry.callIds)
            loadCallLogs()
        }
    }

    fun blockNumber(number: String) {
        viewModelScope.launch {
            val success = blockedRepo.blockNumber(number)
            val msg = if (success) "Blocked $number" else "Unable to block number"
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
            loadCallLogs()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            callLogRepo.clearCallLog()
            loadCallLogs()
        }
    }
}
