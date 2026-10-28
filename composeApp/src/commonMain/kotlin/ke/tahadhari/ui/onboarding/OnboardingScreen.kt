package ke.tahadhari.ui.onboarding

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.*
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

/**
 * First-launch intro: what the app does, "guidance, not a warning", and a language choice.
 * Skip and the last button both call [onFinish]. The language toggle is on every page so a
 * Swahili reader is never stuck on English text; page 3 also has large language buttons.
 */
@Composable
fun OnboardingScreen(
    language: Language,
    onLanguage: (Language) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val strings = Strings.of(language)
    val pager = rememberPagerState(initialPage = initialPage, pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGE_COUNT - 1

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageToggle(language, onLanguage)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = Spacing.touch).testTag("onboarding_skip")) { Text(strings.onboardSkip) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                when (page) {
                    0 -> IntroPage(AppIcons.Drop, strings.onboardWhatTitle, strings.onboardWhatBody)
                    1 -> IntroPage(AppIcons.Warning, strings.onboardGuidanceTitle, strings.onboardGuidanceBody)
                    else -> IntroPage(AppIcons.Language, strings.onboardLanguageTitle, strings.onboardLanguageBody) {
                        LanguageChoice(language, onLanguage)
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                PageDots(pager.currentPage, strings)
                Button(
                    onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(if (last) strings.onboardStart else strings.onboardNext, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun IntroPage(icon: ImageVector, title: String, body: String, extra: (@Composable ColumnScope.() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(scheme.primaryContainer)
                .drawBehind { drawWaves(scheme.primary.copy(alpha = 0.14f), bandHeight = 22.dp.toPx()) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(64.dp), tint = scheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.md))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (extra != null) {
            Spacer(Modifier.height(Spacing.xl))
            extra()
        }
    }
}

/** Two big radio-style buttons. Names are always written in their own language. */
@Composable
private fun LanguageChoice(language: Language, onLanguage: (Language) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(Language.ENGLISH to "English", Language.SWAHILI to "Kiswahili").forEach { (lang, name) ->
            val selected = lang == language
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLow)
                    .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, MaterialTheme.shapes.large)
                    .selectable(selected = selected, role = Role.RadioButton) { onLanguage(lang) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (selected) Icon(AppIcons.Check, null, tint = scheme.primary)
            }
        }
    }
}

@Composable
private fun PageDots(page: Int, strings: Strings) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.clearAndSetSemantics { contentDescription = strings.onboardPage(page + 1, PAGE_COUNT) },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGE_COUNT) { i ->
            val active = i == page
            Box(
                Modifier
                    .height(10.dp)
                    .width(if (active) 28.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (active) scheme.primary else scheme.outlineVariant),
            )
        }
    }
}
