package com.example.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import java.util.Locale

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val iccId: String,
    val phoneAccountHandle: PhoneAccountHandle? = null
)

object TelecomHelper {

    fun isDefaultDialer(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == context.packageName
        }
    }

    fun createDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun placeCall(context: Context, number: String, accountHandle: PhoneAccountHandle? = null): Boolean {
        if (number.isBlank()) return false
        val cleanNumber = number.replace(" ", "").replace("-", "")
        val uri = Uri.fromParts("tel", cleanNumber, null)

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return false

        val extras = Bundle().apply {
            if (accountHandle != null) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
            }
        }

        return try {
            telecomManager.placeCall(uri, extras)
            true
        } catch (e: SecurityException) {
            // If CALL_PHONE permission not granted, fallback to ACTION_DIAL
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getSimCards(context: Context): List<SimCardInfo> {
        val list = mutableListOf<SimCardInfo>()
        try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

            val callCapableAccounts = try {
                telecomManager?.callCapablePhoneAccounts ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val subs: List<SubscriptionInfo>? = try {
                subManager?.activeSubscriptionInfoList
            } catch (e: SecurityException) {
                null
            }

            subs?.forEach { sub ->
                val handle = callCapableAccounts.firstOrNull { account ->
                    account.id == sub.iccId || account.id == sub.subscriptionId.toString()
                }

                list.add(
                    SimCardInfo(
                        subscriptionId = sub.subscriptionId,
                        slotIndex = sub.simSlotIndex,
                        displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                        carrierName = sub.carrierName?.toString() ?: "Carrier",
                        iccId = sub.iccId ?: "",
                        phoneAccountHandle = handle
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getVoicemailNumber(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val vm = telephonyManager?.voiceMailNumber
            if (!vm.isNullOrBlank()) vm else "123"
        } catch (e: Exception) {
            "123"
        }
    }

    fun formatNumber(number: String): String {
        return try {
            PhoneNumberUtils.formatNumber(number, Locale.getDefault().country) ?: number
        } catch (e: Exception) {
            number
        }
    }
}
