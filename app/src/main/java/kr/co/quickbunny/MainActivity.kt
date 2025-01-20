package kr.co.quickbunny

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kr.co.quickbunny.ui.theme.QuickBunnyTheme


class MainActivity : ComponentActivity() {

    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = this
        setContent {
            QuickBunnyTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(context = this@MainActivity)
                }
            }
        }

        requestPermission(context)

    }

    private fun requestPermission(context: Context) {
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    val receiver: BroadcastReceiver = CallReceiver()
                    val filter = IntentFilter().apply {
                        addAction("android.intent.action.PHONE_STATE")
                    }

                    if (Build.VERSION.SDK_INT >= 33) {
                        context.registerReceiver(receiver, filter, RECEIVER_EXPORTED)
                    } else {
                        context.registerReceiver(receiver, filter)
                    }
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Toast.makeText(this@MainActivity, "권한을 허용해주세요.", Toast.LENGTH_LONG).show()
                }
            })
            .setDeniedMessage("권한을 허용해주세요.\n[설정] > [앱 및 알림] > [고급] > [앱 권한]")
            .setPermissions(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.SEND_SMS)
            .check()
    }
}


@Composable
fun Greeting(modifier: Modifier = Modifier, context: Context) {
    val text = remember { mutableStateOf("") }
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    val savedNumber = sharedPreferences.getString("savedNumber", "") ?: ""
    Log.d("wooryeol", "savedNumber ====> $savedNumber")

    if (savedNumber != "") {
        text.value = savedNumber
    }

    Box(modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column {
            TextField(
                value = text.value,
                onValueChange = {if (it.length <= 11 && it.all { char -> char.isDigit() }) {

                    text.value = it
                    sharedPreferences.edit().putString("savedNumber", it).apply()
                }},
                label = { Text("전화번호를 적어주세요!") },
                placeholder = { Text("Type here...") },
                singleLine = true
            )
        }

    }
}