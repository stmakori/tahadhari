package ke.tahadhari.ui.mobile

import androidx.compose.ui.platform.testTag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.guides.GuideTopic
import ke.tahadhari.data.guides.SafetyGuide
import ke.tahadhari.data.guides.SafetyGuides
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.*

/** Ticked guide steps, keyed by [stepKey]. Kept in memory only (not saved). */
typealias GuideTicks = SnapshotStateMap<String, Boolean>

internal fun stepKey(guideId: String, section: Int, step: Int) = "$guideId/$section/$step"

/**
 * Safety tab (Usalama): offline guides. Shows the guide list, or one guide when [openGuideId]
 * is set. Navigation state is hoisted to [ke.tahadhari.ui.MobileApp] so the top bar and Back work.
 */
@Composable
fun SafetyScreen(
    strings: Strings,
    language: Language,
    openGuideId: String?,
    onOpenGuide: (String?) -> Unit,
    ticks: GuideTicks,
    modifier: Modifier = Modifier,
) {
    val guide = openGuideId?.let { SafetyGuides.byId(it) }
    if (guide == null) GuideList(strings, language, onOpenGuide, modifier)
    else GuideDetail(guide, strings, language, ticks, modifier)
}

@Composable
private fun GuideList(strings: Strings, language: Language, onOpen: (String) -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenIntro(AppIcons.Shield, strings.safetyTitle, strings.safetyBody, Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm))
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.guidesHeading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            InfoChip(AppIcons.CloudOff, strings.worksOffline)
        }
        SafetyGuides.all.forEach { guide -> GuideRow(guide, strings, language) { onOpen(guide.id) } }
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun GuideRow(guide: SafetyGuide, strings: Strings, language: Language, onClick: () -> Unit) {
    val content = guide.content(language)
    val (container, onContainer) = guide.topic.tint()
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("guide_row_${guide.id}"),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(guide.topic.icon(), container, onContainer)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(content.title, style = MaterialTheme.typography.titleMedium)
                Text(content.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    strings.guideSectionCount(content.sections.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GuideDetail(guide: SafetyGuide, strings: Strings, language: Language, ticks: GuideTicks, modifier: Modifier) {
    val content = guide.content(language)
    // First section open by default; the rest open on tap. Survives language switches (same indices).
    val expanded = remember(guide.id) { mutableStateListOf(*Array(content.sections.size) { it == 0 }) }
    Column(
        modifier.testTag("guide_detail").fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        GuideHeader(guide, strings, language, ticks)
        content.sections.forEachIndexed { s, section ->
            val keys = section.steps.indices.map { stepKey(guide.id, s, it) }
            val done = keys.count { ticks[it] == true }
            SectionCard {
                val rotation by animateFloatAsState(if (expanded[s]) 180f else 0f)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.touch)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) { expanded[s] = !expanded[s] }
                        .semantics(mergeDescendants = true) {
                            heading()
                            stateDescription = if (expanded[s]) strings.hideSteps else strings.showSteps
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.guideProgress(done, section.steps.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (done == section.steps.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(AppIcons.ExpandMore, null, Modifier.rotate(rotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(expanded[s]) {
                    Column {
                        section.steps.forEachIndexed { i, step ->
                            StepRow(i + 1, step, ticks[keys[i]] == true, strings) { ticks[keys[i]] = it }
                        }
                    }
                }
            }
        }
        NoteRow(strings.guideOfficialNote)
        SourcesCard(guide, strings)
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun GuideHeader(guide: SafetyGuide, strings: Strings, language: Language, ticks: GuideTicks) {
    val content = guide.content(language)
    val scheme = MaterialTheme.colorScheme
    val total = content.sections.sumOf { it.steps.size }
    val done = content.sections.withIndex().sumOf { (s, sec) -> sec.steps.indices.count { ticks[stepKey(guide.id, s, it)] == true } }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer) {
        Column(
            Modifier.drawBehind { drawWaves(scheme.primary.copy(alpha = 0.14f), bandHeight = 24.dp.toPx()) }
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xl, bottom = Spacing.xxl + Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconBubble(guide.topic.icon(), scheme.primary, scheme.onPrimary, size = 52)
            Text(content.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(content.summary, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Check, null, Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(strings.guideProgress(done, total), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SourcesCard(guide: SafetyGuide, strings: Strings) {
    SectionCard {
        SectionTitle(AppIcons.Document, strings.sourcesTitle)
        guide.sources.forEach { source ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(source.name, style = MaterialTheme.typography.bodyMedium)
                Text(source.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

internal fun GuideTopic.icon(): ImageVector = when (this) {
    GuideTopic.FLOODS -> AppIcons.Waves
    GuideTopic.LANDSLIDES -> AppIcons.Terrain
    GuideTopic.CHOLERA -> AppIcons.Drink
    GuideTopic.LIVESTOCK -> AppIcons.Paw
    GuideTopic.LIGHTNING -> AppIcons.Bolt
}

/** Container/content colour pair so neighbouring guides are easy to tell apart. */
@Composable
private fun GuideTopic.tint(): Pair<Color, Color> {
    val s = MaterialTheme.colorScheme
    return when (this) {
        GuideTopic.FLOODS, GuideTopic.LIVESTOCK -> s.primaryContainer to s.onPrimaryContainer
        GuideTopic.LANDSLIDES, GuideTopic.LIGHTNING -> s.tertiaryContainer to s.onTertiaryContainer
        GuideTopic.CHOLERA -> s.secondaryContainer to s.onSecondaryContainer
    }
}
