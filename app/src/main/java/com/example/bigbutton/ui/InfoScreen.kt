package com.example.bigbutton.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // App info
        Text(
            text = "BigButton",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Get version from package info
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }

        Text(
            text = "Version $versionName",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A simple habit tracking widget.\nTrack daily, weekly, or custom period tasks\nwith a single tap.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // How to Use section
        SectionHeader("How to Use")

        InstructionItem(
            title = "Configure",
            text = "Before you start using the app, go to the Settings tab. Set how often you'd like to perform the action (Daily, Weekly, Monthly, or Custom), and what time it resets (default 4:00 AM)."
        )

        InstructionItem(
            title = "The Widget",
            text = "Add the BigButton widget to your home screen. Tap the button when you complete your task - it changes from \"Do\" (red) to \"Done!\" (green). That's it!"
        )

        InstructionItem(
            title = "Automatic Reset",
            text = "At your configured reset time, the widget automatically resets to \"Do\" and a new period begins. Your previous period is recorded in the calendar."
        )

        InstructionItem(
            title = "Undo Accidental Presses",
            text = "Pressed \"Done\" by mistake? Go to Settings and tap \"Reset\" to undo it before the period ends."
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Calendar Colors section
        SectionHeader("Calendar Colors")

        ColorItem(color = "Green", meaning = "You completed the task during this period")
        ColorItem(color = "Red", meaning = "You missed this period (didn't press Done)")
        ColorItem(color = "Grey", meaning = "Current period, not yet finalized")
        ColorItem(color = "No color", meaning = "Before you started tracking, future days, or gaps from changing settings")

        Spacer(modifier = Modifier.height(24.dp))

        // Multi-Day Periods section
        SectionHeader("Multi-Day Periods")

        Text(
            text = "If you set a period longer than 1 day (e.g., Weekly = 7 days):",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        BulletPoint("Press \"Done\" once anytime during the period")
        BulletPoint("All days in the current period show as grey until it ends")
        BulletPoint("All days in the period show the same final color (green or red)")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Example: A weekly task due by Sunday 4 AM - press Done anytime Mon-Sat, and all 7 days turn green at the reset date/time.",
            fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // When Periods Finalize section
        SectionHeader("When Periods Finalize")

        Text(
            text = "Your status is only \"locked in\" when a period ends:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        BulletPoint("The reset time passes, the period finalizes")
        BulletPoint("Days turn from grey to green or red")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Finalized days cannot be changed except by erasing all history with the \"Clear History\" button in the calendar footer.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Credits
        Text(
            text = "Made by The Moving Finger Studios",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
private fun InstructionItem(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorItem(color: String, meaning: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = "$color:",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = meaning,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
