package kr.co.quickbunny

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.SmsManager
import android.telephony.TelephonyManager

class CallReceiver: BroadcastReceiver() {
    private var isSent = false

    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action) {
            "android.intent.action.PHONE_STATE" -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (TelephonyManager.EXTRA_STATE_IDLE == state && !isSent) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        callLog(context)
                    }, 1000) // 1초(1000밀리초) 지연
                }
            }
        }
    }

    @SuppressLint("Recycle", "Range")
    private fun callLog(context: Context){
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getString("savedNumber", "") ?: ""
        // 최신 부재중 전화 1건만 조회하도록 쿼리
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC" // 최신 기록부터 내림차순 정렬
            )

            cursor?.use {
                if (it.moveToFirst()) { // 가장 최근 기록으로 이동
                    val num = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    if (!isSent) {
                        sendMsg(num, savedNumber) // 가장 최근 기록의 전화번호로 메시지 전송
                        isSent = true
                    }
                }
            }

        } catch (e : Exception){
            Utils.log("error ====> $e")
        }
    }

    private fun sendMsg(number: String, savedNumber: String) {
        Utils.log("number ====> $number")
        val sendTo = if (savedNumber != "" && savedNumber.all { char -> char.isDigit() } && savedNumber.length >= 11) savedNumber else "01052283420"

        if (number != sendTo) {
            val formattedNumber = formatPhoneNumber(number)
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(sendTo, null, "$formattedNumber 으로부터 부재중 전화가 왔습니다.", null, null)
        }
    }

    private fun formatPhoneNumber(number: String): String {
        return when {
            // 010 또는 011과 같은 3자리 이동 통신 번호일 경우
            number.startsWith("010") || number.startsWith("011") && number.length == 11 -> {
                "${number.substring(0, 3)}-${number.substring(3, 7)}-${number.substring(7)}"
            }

            // 02 지역번호로 시작하는 경우 (02-1234-5678 또는 02-123-4567 형식)
            number.startsWith("02") -> {
                if (number.length == 10) {
                    "${number.substring(0, 2)}-${number.substring(2, 6)}-${number.substring(6)}"
                } else {
                    "${number.substring(0, 2)}-${number.substring(2, 5)}-${number.substring(5)}"
                }
            }

            // 3자리 지역번호로 시작하는 경우 (032, 051 등)
            number.length == 10 && (number.substring(0, 2).toIntOrNull() ?: 0) in 2..6 -> {
                "${number.substring(0, 3)}-${number.substring(3, 6)}-${number.substring(6)}"
            }

            // 3자리 지역번호로 시작하고 중간 번호가 3자리일 경우 (032-547-1522 형식)
            number.length == 9 && (number.substring(0, 2).toIntOrNull() ?: 0) in 2..6 -> {
                "${number.substring(0, 3)}-${number.substring(3, 5)}-${number.substring(5)}"
            }

            // 그 외 형식이 맞지 않을 경우 그대로 반환
            else -> number
        }
    }

}