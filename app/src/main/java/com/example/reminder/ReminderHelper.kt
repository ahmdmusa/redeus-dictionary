package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import java.time.LocalDate
import java.util.Calendar

object ReminderHelper {
    private const val TAG = "ReminderHelper"
    const val CHANNEL_ID = "study_reminders_channel"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_REQ_CODE = 1002
    
    private const val PREFS_NAME = "vocab_ai_prefs"
    const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    const val KEY_ALWAYS_NOTIFY = "reminders_always_notify"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"
    private const val KEY_LAST_NOTIFIED_DATE = "last_notified_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun areRemindersEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMINDERS_ENABLED, true)
    }

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        Log.i(TAG, "Setting reminders enabled: $enabled")
        getPrefs(context).edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
        if (enabled) {
            scheduleDailyReminder(context)
        } else {
            cancelDailyReminder(context)
        }
    }

    fun isAlwaysNotifyEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALWAYS_NOTIFY, false)
    }

    fun setAlwaysNotifyEnabled(context: Context, enabled: Boolean) {
        Log.i(TAG, "Setting always notify: $enabled")
        getPrefs(context).edit().putBoolean(KEY_ALWAYS_NOTIFY, enabled).apply()
    }

    fun getReminderHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDER_HOUR, 12).coerceIn(0, 23)
    }

    fun getReminderMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59)
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        Log.i(TAG, "Setting reminder time: $safeHour:$safeMinute")
        getPrefs(context).edit()
            .putInt(KEY_REMINDER_HOUR, safeHour)
            .putInt(KEY_REMINDER_MINUTE, safeMinute)
            .apply()
        if (areRemindersEnabled(context)) {
            scheduleDailyReminder(context)
        }
    }

    fun getLastNotifiedDate(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_NOTIFIED_DATE, "") ?: ""
    }

    fun setLastNotifiedDate(context: Context, date: String) {
        Log.d(TAG, "Updating last notified date: $date")
        getPrefs(context).edit().putString(KEY_LAST_NOTIFIED_DATE, date).apply()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Study Reminders"
            val descriptionText = "Daily vocabulary study reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created/verified: $CHANNEL_ID")
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.i(TAG, "Notification fired successfully with message: \"$message\"")
    }

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.reminder.ACTION_SHOW_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderHour = getReminderHour(context)
        val reminderMinute = getReminderMinute(context)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the scheduled time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            // Schedule the alarm using setAndAllowWhileIdle to work even in Doze Mode
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.i(TAG, "Notification scheduled for: ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification alarm", e)
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.reminder.ACTION_SHOW_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Daily notification alarm canceled")
        } else {
            Log.d(TAG, "No active notification alarm to cancel")
        }
    }

    fun checkAndShowAppOpenReminder(context: Context, dueCount: Int) {
        if (!areRemindersEnabled(context)) {
            Log.d(TAG, "App-open reminder skipped: reminders are disabled")
            return
        }

        val todayStr = LocalDate.now().toString()
        val lastNotified = getLastNotifiedDate(context)
        if (lastNotified == todayStr) {
            Log.d(TAG, "App-open reminder skipped: already notified today ($todayStr)")
            return
        }

        if (dueCount > 0) {
            val title = "Words Due for Review 🧠"
            val message = "You have $dueCount words due for review today 🧠 — keep your streak going!"
            showNotification(context, title, message)
            setLastNotifiedDate(context, todayStr)
        } else if (isAlwaysNotifyEnabled(context)) {
            val title = "Daily Vocabulary Practice"
            val message = "Time for your daily vocabulary practice! 🧠 — keep your streak going!"
            showNotification(context, title, message)
            setLastNotifiedDate(context, todayStr)
        } else {
            Log.d(TAG, "App-open reminder: no due words and always-notify is disabled")
        }
    }
}
