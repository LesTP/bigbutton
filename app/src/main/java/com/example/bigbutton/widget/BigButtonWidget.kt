package com.example.bigbutton.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.bigbutton.R
import com.example.bigbutton.util.ResetCalculator

class BigButtonWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = BigButtonStateDefinition

    companion object {
        // Design spec colors
        val BackgroundColor = Color(0xFFD4C5A9)      // Warm beige/tan
        val ButtonDoColor = Color(0xFFE57373)         // Soft red
        val ButtonDoneColor = Color(0xFF81C784)       // Soft green
        val ButtonBorderColor = Color.White           // White border
        val ButtonTextColor = Color.White             // White text
        val SettingsIconColor = Color(0xFF8B7355)     // Muted brown
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Read state (composable call - cannot be in try-catch)
            val prefs = currentState<Preferences>()
            val storedIsDone = prefs[BigButtonStateDefinition.Keys.IS_DONE] ?: false

            // Determine display state (check if reset should have happened)
            // Wrap calculation in try-catch to handle any errors gracefully
            val isDone = if (storedIsDone) {
                try {
                    val lastChanged = prefs[BigButtonStateDefinition.Keys.LAST_CHANGED] ?: 0L
                    val periodDays = prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS]
                        ?: BigButtonStateDefinition.DEFAULT_PERIOD_DAYS
                    val resetHour = prefs[BigButtonStateDefinition.Keys.RESET_HOUR]
                        ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR
                    val resetMinute = prefs[BigButtonStateDefinition.Keys.RESET_MINUTE]
                        ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE

                    !ResetCalculator.shouldReset(lastChanged, periodDays, resetHour, resetMinute)
                } catch (e: Exception) {
                    // If calculation fails, show as Done (safer default when storedIsDone=true)
                    true
                }
            } else {
                false
            }

            BigButtonContent(isDone = isDone)
        }
    }

    @Composable
    private fun BigButtonContent(isDone: Boolean) {
        val buttonColor = if (isDone) ButtonDoneColor else ButtonDoColor
        val buttonText = if (isDone) "Done!" else "Do"

        // Main widget container (1x1 size)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            // White border ring (slightly larger circle behind the button)
            Box(
                modifier = GlanceModifier
                    .size(60.dp)
                    .cornerRadius(30.dp)
                    .background(ButtonBorderColor)
                    .clickable(onClick = actionRunCallback<MarkDoneAction>()),
                contentAlignment = Alignment.Center
            ) {
                // Main colored button with gradient
                val buttonDrawable = if (isDone) R.drawable.button_done_gradient else R.drawable.button_do_gradient
                Box(
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(ImageProvider(buttonDrawable)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        style = TextStyle(
                            color = ColorProvider(ButtonTextColor),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Settings icon in bottom-right corner
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    modifier = GlanceModifier
                        .size(16.dp)
                        .clickable(onClick = actionRunCallback<OpenSettingsAction>())
                )
            }
        }
    }
}
