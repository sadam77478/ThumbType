package com.sadam.thumbtype.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sadam.thumbtype.mobile.app.HomeUiData
import java.util.Calendar

/**
 * V4.1 home dashboard.
 *
 * The dashboard keeps the highest-value information above the fold: current skill, today's
 * adaptive recommendation, core performance metrics and daily progress. Secondary content is
 * intentionally pushed lower so the screen feels calmer and more purposeful on a phone.
 */
@Composable
fun PremiumHomeScreen(
    data: HomeUiData,
    onStart: (Lesson) -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val spacing = ThumbTypeDesign.spacing
    val profile = data.profile
    val completed = data.completedLessonIds
    val next = LessonRepository.nextLesson(completed)
    val todayMinutes = data.todaySeconds / 60f
    val dailyProgress = (todayMinutes / profile.dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    val curriculumProgress = if (LessonRepository.lessons.isEmpty()) 0f else {
        completed.size.toFloat() / LessonRepository.lessons.size.toFloat()
    }.coerceIn(0f, 1f)
    val score = data.thumbScore
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val challenge = LessonRepository.dailyChallenge(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item {
            AppPageHeader(
                title = greeting,
                subtitle = "Train what your recent typing data says matters most."
            ) {
                TinyPill("${data.streak} day", Icons.Default.LocalFireDepartment, ThumbAmber)
            }
        }

        item {
            GradientHero(
                eyebrow = if (score == 0) "Start here" else "Current skill",
                title = if (score == 0) "Build your baseline" else "ThumbScore $score",
                subtitle = if (score == 0) {
                    "Complete one measured session to unlock personalized coaching."
                } else {
                    premiumSkillLabel(score)
                }
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(76.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (score == 0) "—" else "${score / 10}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                "skill",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = .80f)
                            )
                        }
                    }
                }
            }
        }

        item { AdaptiveCoachCard(data.intelligence) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MetricCard(
                    value = data.lastWpm.toString(),
                    label = "Current WPM",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    value = "${data.lastAccuracy}%",
                    label = "Accuracy",
                    icon = Icons.Default.GpsFixed,
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            PremiumCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily training", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${todayMinutes.toInt()} of ${profile.dailyGoalMinutes} minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TinyPill(
                        text = if (dailyProgress >= 1f) "Complete" else "${(dailyProgress * 100).toInt()}%",
                        icon = Icons.Default.Timer,
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(13.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        item {
            SectionHeading("Continue learning", "View path") { onNavigate(AppScreen.Learn) }
        }

        item {
            PremiumCard(onClick = { onStart(next) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(.10f)) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Lesson ${next.id} • ${next.skill}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(next.title, style = MaterialTheme.typography.titleLarge)
                        Text(next.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TinyPill("+${next.xp} XP", Icons.Default.Stars, ThumbAmber)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Learning path", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${completed.size}/${LessonRepository.lessons.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { curriculumProgress },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        item {
            SectionHeading("Your adaptive plan")
        }

        items(data.workout) { workout ->
            val icon = when (workout.title) {
                "Warm-up" -> Icons.Default.Whatshot
                "Weakness trainer" -> Icons.Default.AutoFixHigh
                "Adaptive focus" -> Icons.Default.Tune
                else -> Icons.Default.Bolt
            }
            ActionRow(
                title = workout.title,
                subtitle = "${workout.minutes} min • ${workout.subtitle}",
                icon = icon,
                badge = if (workout.title == "Adaptive focus") "PERSONAL" else null,
                onClick = { onStart(workout.lesson) }
            )
        }

        item { SectionHeading("Daily challenge") }

        item {
            PremiumCard(onClick = { onStart(challenge) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = ThumbAmber.copy(.13f)) {
                        Icon(
                            Icons.Default.MilitaryTech,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = ThumbAmber
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(challenge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "60 seconds • +${challenge.xp} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TinyPill("Today", Icons.Default.EmojiEvents, ThumbAmber)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MetricCard(
                    value = data.bestWpm.toString(),
                    label = "Best WPM",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f),
                    accent = ThumbAmber
                )
                MetricCard(
                    value = data.xp.toString(),
                    label = "Lifetime XP",
                    icon = Icons.Default.Stars,
                    modifier = Modifier.weight(1f),
                    accent = ThumbAmber
                )
            }
        }

        item {
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondary.copy(.10f)) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${data.totalCharacters} measured characters", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Training stays local unless you explicitly export it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun premiumSkillLabel(score: Int): String = when {
    score >= 900 -> "Elite control — increase difficulty without sacrificing precision."
    score >= 800 -> "Advanced — strong speed, accuracy and reach control."
    score >= 650 -> "Skilled — polish consistency and weak transitions."
    score >= 450 -> "Developing — reliable habits are taking shape."
    else -> "Foundation — accuracy and rhythm come first."
}
