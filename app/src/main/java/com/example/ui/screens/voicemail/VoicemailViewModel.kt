package com.example.ui.screens.voicemail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallLogEntry
import com.example.data.model.CallType
import com.example.data.repository.CallLogRepository
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoicemailUiState(
    val voicemailNumber: String = "123",
    val voicemailEntries: List<CallLogEntry> = emptyList(),
    val isLoading: Boolean = true
)

class VoicemailViewModel(application: Application) : AndroidViewModel(application) {

    private val callLogRepo = CallLogRepository(application)
    private val _uiState = MutableStateFlow(VoicemailUiState())
    val uiState: StateFlow<VoicemailUiState> = _uiState.asStateFlow()

    init {
        loadVoicemails()
    }

    fun loadVoicemails() {
        viewModelScope.launch {
            val vmNum = TelecomHelper.getVoicemailNumber(getApplication())
            val allLogs = callLogRepo.getCallLogs()
            val vmLogs = allLogs.filter { it.type == CallType.VOICEMAIL }

            _uiState.value = VoicemailUiState(
                voicemailNumber = vmNum,
                voicemailEntries = vmLogs,
                isLoading = false
            )
        }
    }

    fun callVoicemail() {
        TelecomHelper.placeCall(getApplication(), _uiState.value.voicemailNumber)
    }

    fun deleteVoicemail(entry: CallLogEntry) {
        viewModelScope.launch {
            callLogRepo.deleteCall(entry.callIds)
            loadVoicemails()
        }
    }
}
