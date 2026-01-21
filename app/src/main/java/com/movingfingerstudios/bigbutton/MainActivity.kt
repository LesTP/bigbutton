package com.movingfingerstudios.bigbutton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movingfingerstudios.bigbutton.ui.CalendarScreen
import com.movingfingerstudios.bigbutton.ui.InfoScreen
import com.movingfingerstudios.bigbutton.ui.SettingsScreen
import com.movingfingerstudios.bigbutton.ui.theme.BigButtonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BigButtonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        onResetComplete = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(onResetComplete: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Settings", "Calendar", "Info")

    Column(modifier = Modifier.fillMaxSize()) {
        // App title
        Text(
            text = "BigButton",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // Tab row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> SettingsScreen(onResetComplete = onResetComplete)
            1 -> CalendarScreen()
            2 -> InfoScreen()
        }
    }
}
