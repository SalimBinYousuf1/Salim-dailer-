package com.example.ui.screens.dialpad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApp
import com.example.data.model.SpeedDialEntry
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SpeedDialRepository
import com.example.telecom.DtmfPlayer
import com.example.telecom.SimCardInfo
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DialPadUiState(
    val enteredNumber: String = "",
    val formattedNumber: String = "",
    val simCards: List<SimCardInfo> = emptyList(),
    val selectedSimIndex: Int = 0,
    val speedDials: List<SpeedDialEntry> = emptyList(),
    val isDefaultDialer: Boolean = false,
    val playSound: Boolean = true,
    val enableHaptic: Boolean = true
)

class DialPadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SalimApp).database
    private val speedDialRepo = SpeedDialRepository(db.speedDialDao())
    private val settingsRepo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(DialPadUiState())
    val uiState: StateFlow<DialPadUiState> = _uiState.asStateFlow()

    init {
        refreshDialerStatus()
        loadSimCards()

        viewModelScope.launch {
            speedDialRepo.speedDialsFlow.collect { list ->
                _uiState.value = _uiState.value.copy(speedDials = list)
            }
        }

        viewModelScope.launch {
            settingsRepo.dialpadSoundFlow.collect { sound ->
                _uiState.value = _uiState.value.copy(playSound = sound)
            }
        }

        viewModelScope.launch {
            settingsRepo.dialpadHapticFlow.collect { haptic ->
                _uiState.value = _uiState.value.copy(enableHaptic = haptic)
            }
        }
    }

    fun refreshDialerStatus() {
        val isDefault = TelecomHelper.isDefaultDialer(getApplication())
        _uiState.value = _uiState.value.copy(isDefaultDialer = isDefault)
    }

    fun loadSimCards() {
        val sims = TelecomHelper.getSimCards(getApplication())
        _uiState.value = _uiState.value.copy(simCards = sims)
    }

    fun selectSim(index: Int) {
        if (index in _uiState.value.simCards.indices) {
            _uiState.value = _uiState.value.copy(selectedSimIndex = index)
        }
    }

    fun onDigitPress(char: Char) {
        if (_uiState.value.playSound) {
            DtmfPlayer.playTone(char)
        }
        val newRaw = _uiState.value.enteredNumber + char
        val formatted = TelecomHelper.formatNumber(newRaw)
        _uiState.value = _uiState.value.copy(
            enteredNumber = newRaw,
            formattedNumber = formatted
        )
    }

    fun onDigitLongPress(digit: Char): SpeedDialEntry? {
        when (digit) {
            '0' -> {
                val newRaw = _uiState.value.enteredNumber + '+'
                _uiState.value = _uiState.value.copy(
                    enteredNumber = newRaw,
                    formattedNumber = TelecomHelper.formatNumber(newRaw)
                )
                return null
            }
            '1' -> {
                // Call Voicemail
                callVoicemail()
                return null
            }
            in '2'..'9' -> {
                val slot = digit.digitToInt()
                val entry = _uiState.value.speedDials.find { it.key == slot }
                if (entry != null && entry.number.isNotBlank()) {
                    placeCall(entry.number)
                }
                return entry
            }
            else -> return null
        }
    }

    fun onBackspace() {
        val current = _uiState.value.enteredNumber
        if (current.isNotEmpty()) {
            val newRaw = current.dropLast(1)
            _uiState.value = _uiState.value.copy(
                enteredNumber = newRaw,
                formattedNumber = TelecomHelper.formatNumber(newRaw)
            )
        }
    }

    fun onClearAll() {
        _uiState.value = _uiState.value.copy(
            enteredNumber = "",
            formattedNumber = ""
        )
    }

    fun setNumber(number: String) {
        _uiState.value = _uiState.value.copy(
            enteredNumber = number,
            formattedNumber = TelecomHelper.formatNumber(number)
        )
    }

    fun placeCall(targetNumber: String = _uiState.value.enteredNumber) {
        if (targetNumber.isBlank()) return
        val currentSims = _uiState.value.simCards
        val selectedIndex = _uiState.value.selectedSimIndex
        val handle = if (currentSims.isNotEmpty() && selectedIndex in currentSims.indices) {
            currentSims[selectedIndex].phoneAccountHandle
        } else null

        TelecomHelper.placeCall(getApplication(), targetNumber, handle)
    }

    fun callVoicemail() {
        val vmNumber = TelecomHelper.getVoicemailNumber(getApplication())
        placeCall(vmNumber)
    }
}
