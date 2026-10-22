package cl.duoc.smcft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duoc.smcft.ui.SmcftApp
import cl.duoc.smcft.ui.SmcftTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmcftTema {
                SmcftApp()
            }
        }
    }
}
