package com.documentpro.office.business.fileviewer.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import io.docview.push.earthquake.EarthquakeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 地震信息获取模拟广播接收器
 * 用于接收精准闹钟触发事件，模拟地震信息获取
 */
class EarthquakeAlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    companion object {
        private const val TAG = "EarthquakeAlarmReceiver"
        const val ACTION_EARTHQUAKE_ALARM = "com.documentpro.office.business.fileviewer.ACTION_EARTHQUAKE_ALARM"
        const val EXTRA_TIME = "extra_time"
        
        /**
         * 设置精准闹钟
         * @param context 上下文
         * @param triggerTimeMillis 触发时间（毫秒时间戳）
         */
        fun scheduleExactAlarm(context: Context, triggerTimeMillis: Long) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                
                // Android 12+ 需要检查精准闹钟权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        ToastUtils.showShort("需要精准闹钟权限")
                        // 跳转到系统设置页面
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        return
                    }
                }
                
                // 创建Intent
                val intent = Intent(context, EarthquakeAlarmReceiver::class.java).apply {
                    action = ACTION_EARTHQUAKE_ALARM
                    putExtra(EXTRA_TIME, triggerTimeMillis)
                }
                
                // 创建PendingIntent
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // 设置精准闹钟
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
                
                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                ToastUtils.showShort("精准闹钟已设置: ${timeFormat.format(triggerTimeMillis)}")
            } catch (e: Exception) {
                ToastUtils.showShort("设置精准闹钟失败: ${e.message}")
                Log.e(TAG, "设置精准闹钟失败", e)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_EARTHQUAKE_ALARM) {
            val scheduledTime = intent.getLongExtra(EXTRA_TIME, 0L)
            Log.d(TAG, "地震信息获取模拟广播触发，预定时间: $scheduledTime，当前时间: ${System.currentTimeMillis()}")
            scope.launch {
//                val e = EarthquakeController.getMajorEarthquakes()
//                val i = EarthquakeController.getMinorEarthquakes()
//                Log.d(TAG, "地震信息获取条目数：${e.size} ${i.size}")
            }
        }
    }
}

