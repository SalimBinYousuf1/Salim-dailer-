package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApp
import com.example.data.local.QuickDeclineMessageEntity
import com.example.data.local.ScreeningRuleEntity
import com.example.data.repository.BlockedNumber
import com.example.data.repository.BlockedNumberRepository
import com.example.data.repository.SettingsRepository
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val isTrueBlack: Boolean = true,
    val glassOpacity: Float = 0.72f,
    val dialpadSound: Boolean = true,
    val dialpadHaptic: Boolean = true,
    val isDefaultDialer: Boolean = false,
    val screeningRules: List<ScreeningRuleEntity> = emptyList(),
    val quickDeclineMessages: List<QuickDeclineMessageEntity> = emptyList(),
    val blockedNumbers: List<BlockedNumber> = emptyList()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as SalimApp).database
    private val settingsRepo = SettingsRepository(application)
    private val blockedRepo = BlockedNumberRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkDefaultDialer()

        viewModelScope.launch {
            settingsRepo.darkModeFlow.collect { dm ->
                _uiState.value = _uiState.value.copy(isDarkMode = dm)
            }
        }
        viewModelScope.launch {
            settingsRepo.trueBlackFlow.collect { tb ->
                _uiState.value = _uiState.value.copy(isTrueBlack = tb)
            }
        }
        viewModelScope.launch {
            settingsRepo.glassOpacityFlow.collect { op ->
                _uiState.value = _uiState.value.copy(glassOpacity = op)
            }
        }
        viewModelScope.launch {
            settingsRepo.dialpadSoundFlow.collect { s ->
                _uiState.value = _uiState.value.copy(dialpadSound = s)
            }
        }
        viewModelScope.launch {
            settingsRepo.dialpadHapticFlow.collect { h ->
                _uiState.value = _uiState.value.copy(dialpadHaptic = h)
            }
        }
        viewModelScope.launch {
            db.screeningRuleDao().getAllRules().collect { rules ->
                _uiState.value = _uiState.value.copy(screeningRules = rules)
            }
        }
        viewModelScope.launch {
            db.screeningRuleDao().getAllQuickMessages().collect { msgs ->
                _uiState.value = _uiState.value.copy(quickDeclineMessages = msgs)
            }
        }

        loadBlockedNumbers()
    }

    fun checkDefaultDialer() {
        val isDefault = TelecomHelper.isDefaultDialer(getApplication())
        _uiState.value = _uiState.value.copy(isDefaultDialer = isDefault)
    }

    fun loadBlockedNumbers() {
        viewModelScope.launch {
            val list = blockedRepo.getBlockedNumbers()
            _uiState.value = _uiState.value.copy(blockedNumbers = list)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setDarkMode(enabled)
        }
    }

    fun setTrueBlack(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setTrueBlack(enabled)
        }
    }

    fun setGlassOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepo.setGlassOpacity(opacity)
        }
    }

    fun setDialpadSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setDialpadSound(enabled)
        }
    }

    fun setDialpadHaptic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setDialpadHaptic(enabled)
        }
    }

    fun toggleScreeningRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            db.screeningRuleDao().setRule(ScreeningRuleEntity(id = ruleId, enabled = isEnabled))
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            blockedRepo.unblockNumber(number)
            loadBlockedNumbers()
        }
    }

    fun addBlockedNumber(number: String) {
        viewModelScope.launch {
            blockedRepo.blockNumber(number)
            loadBlockedNumbers()
        }
    }

    fun updateQuickDeclineMessage(id: Long, newText: String) {
        viewModelScope.launch {
            db.screeningRuleDao().insertQuickMessage(QuickDeclineMessageEntity(id = id, text = newText))
        }
    }
}

