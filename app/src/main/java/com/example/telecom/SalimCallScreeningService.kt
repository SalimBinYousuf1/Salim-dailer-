package com.example.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import com.example.data.local.SalimDatabase
import com.example.data.repository.BlockedNumberRepository
import com.example.data.repository.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SalimCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""
        val isPrivateOrRestricted = number.isBlank() ||
                callDetails.callerDisplayNamePresentation == TelecomManager.PRESENTATION_RESTRICTED ||
                callDetails.callerDisplayNamePresentation == TelecomManager.PRESENTATION_UNKNOWN

        serviceScope.launch {
            val db = SalimDatabase.getDatabase(applicationContext)
            val blockedRepo = BlockedNumberRepository(applicationContext)
            val contactsRepo = ContactsRepository(applicationContext)

            val blockPrivate = db.screeningRuleDao().isRuleEnabled("block_private") ?: false
            val blockUnknown = db.screeningRuleDao().isRuleEnabled("block_unknown") ?: false

            var shouldBlock = false

            if (isPrivateOrRestricted && blockPrivate) {
                shouldBlock = true
            }

            if (!shouldBlock && number.isNotBlank()) {
                if (blockedRepo.isNumberBlocked(number)) {
                    shouldBlock = true
                } else if (blockUnknown) {
                    val contacts = contactsRepo.getContacts()
                    val match = contacts.any { contact ->
                        contact.phoneNumbers.any { p ->
                            p.number.replace(" ", "").endsWith(number.takeLast(7))
                        }
                    }
                    if (!match) {
                        shouldBlock = true
                    }
                }
            }

            val response = if (shouldBlock) {
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
            } else {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build()
            }

            respondToCall(callDetails, response)
        }
    }
}
