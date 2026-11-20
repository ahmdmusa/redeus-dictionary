package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    private val TAG = "ReminderReceiver"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "onReceive called with action: $action")

        // 1. Reschedule if booted or if alarm fired (reschedules for next day)
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "com.example.reminder.ACTION_SHOW_REMINDER") {
            if (ReminderHelper.areRemindersEnabled(context)) {
                ReminderHelper.scheduleDailyReminder(context)
                Log.d(TAG, "Alarm rescheduled successfully")
            }
        }

        // 2. If alarm fired, perform the database query and trigger notification if due
        if (action == "com.example.reminder.ACTION_SHOW_REMINDER") {
            if (!ReminderHelper.areRemindersEnabled(context)) {
                Log.d(TAG, "Skipping notification: Reminders are disabled by user settings")
                return
            }

            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val bookmarked = db.wordDao().getBookmarkedWordsList()
                    val today = LocalDate.now().toEpochDay()
                    val dueCount = bookmarked.count { it.dueDate <= today }

                    val todayStr = LocalDate.now().toString()
                    val lastNotified = ReminderHelper.getLastNotifiedDate(context)

                    if (lastNotified == todayStr) {
                        Log.i(TAG, "Already notified today ($todayStr). Skipping daily scheduled notification.")
                        return@launch
                    }

                    if (dueCount > 0) {
                        val title = "Words Due for Review 🧠"
                        val message = "You have $dueCount words due for review today 🧠 — keep your streak going!"
                        ReminderHelper.showNotification(context, title, message)
                        ReminderHelper.setLastNotifiedDate(context, todayStr)
                    } else if (ReminderHelper.isAlwaysNotifyEnabled(context)) {
                        val title = "Daily Vocabulary Practice"
                        val message = "Time for your daily vocabulary practice! 🧠 — keep your streak going!"
                        ReminderHelper.showNotification(context, title, message)
                        ReminderHelper.setLastNotifiedDate(context, todayStr)
                    } else {
                        Log.i(TAG, "No words due today and always-notify is disabled. No notification triggered.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check due words in background", e)
                }
            }
        }
    }
}
