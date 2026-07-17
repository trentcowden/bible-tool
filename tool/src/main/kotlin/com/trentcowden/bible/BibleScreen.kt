package com.trentcowden.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.designVerticalPxToDp
import com.trentcowden.bible.data.BibleDbHolder
import com.trentcowden.bible.data.ReadingPositionStore
import com.trentcowden.bible.data.SettingsStore
import com.trentcowden.bible.data.Verse

@InitialScreen
class BibleScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, BibleViewModel>(sealedActivity) {

    override val viewModelClass: Class<BibleViewModel>
        get() = BibleViewModel::class.java

    override fun createViewModel() = BibleViewModel(
        db = BibleDbHolder.get(lightContext),
        positionStore = ReadingPositionStore(lightContext.dataStore),
        settingsStore = SettingsStore(lightContext.dataStore),
    )

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsState()
        val hideVerseNumbers by viewModel.hideVerseNumbers.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

        // Jump back to the top whenever the chapter changes. Keyed on the reference so
        // it fires on next/previous/goTo, but not on unrelated recompositions (e.g.
        // toggling verse numbers, which should keep your place).
        val scrollState = rememberScrollState()
        LaunchedEffect(state.reference) { scrollState.scrollTo(0) }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                LightTopBar(
                    leftButton = if (state.hasPrevious) {
                        LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { viewModel.previous() },
                        )
                    } else null,
                    center = LightTopBarCenter.Text(state.reference.ifEmpty { "Loading…" }),
                    rightButton = if (state.hasNext) {
                        LightBarButton.LightIcon(
                            icon = LightIcons.ARROW_RIGHT,
                            onClick = { viewModel.next() },
                        )
                    } else null,
                )

                LightScrollView(
                    scrollState = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    val paragraphs = chapterToParagraphs(state.verses, hideVerseNumbers)
                    paragraphs.forEachIndexed { index, paragraph ->
                        LightText(
                            text = paragraph,
                            variant = LightTextVariant.Paragraph,
                        )
                        if (index < paragraphs.lastIndex) {
                            Spacer(
                                modifier = Modifier.height(
                                    (PARAGRAPH_FONT_PX * PARAGRAPH_SPACING_MULTIPLIER)
                                        .designVerticalPxToDp()
                                )
                            )
                        }
                    }
                }

                // Two items: settings on the left, the book picker on the right.
                LightBottomBar(
                    modifier = Modifier.padding(top = 0.dp),
                    items = listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SETTINGS,
                            onClick = {
                                navigateTo({ activity -> SettingsScreen(activity) })
                            },
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.LIST,
                            onClick = {
                                navigateTo({ activity -> BookListScreen(activity) }) { ref ->
                                    viewModel.goTo(ref.book, ref.chapter)
                                }
                            },
                        ),
                    ),
                )
            }
        }
    }
}

private fun chapterToParagraphs(
    verses: List<Verse>,
    hideVerseNumbers: Boolean,
): List<String> =
    verses.joinToString(separator = "") { verse ->
        // Each verse's text already carries a trailing space (or "\n\n" to end a
        // paragraph), so with numbers hidden the verses still read as flowing prose.
        if (hideVerseNumbers) verse.text else "${verse.verse} ${verse.text}"
    }
        .split("\n\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private const val PARAGRAPH_FONT_PX = 24.5f
private const val PARAGRAPH_SPACING_MULTIPLIER = 0.7f
