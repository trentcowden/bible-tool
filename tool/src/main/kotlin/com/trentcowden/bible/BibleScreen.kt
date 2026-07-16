package com.trentcowden.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trentcowden.bible.data.BibleDbHolder
import com.trentcowden.bible.data.ReadingPositionStore
import com.trentcowden.bible.data.Verse
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

@InitialScreen
class BibleScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, BibleViewModel>(sealedActivity) {

    override val viewModelClass: Class<BibleViewModel>
        get() = BibleViewModel::class.java

    override fun createViewModel() = BibleViewModel(
        db = BibleDbHolder.get(lightContext),
        positionStore = ReadingPositionStore(lightContext.dataStore),
    )

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
            ) {
                // Calendar-style top bar: a chevron on each side of the reference.
                // Passing null for a button when there's nowhere to go leaves a blank
                // slot, so the reference stays centered at the first/last chapter.
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

                // The reading area. One flowing block of text so the source's
                // paragraph breaks (the ",\n\n") lay out naturally.
                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    LightText(
                        text = chapterToText(state.verses),
                        variant = LightTextVariant.Paragraph,
                    )
                }

                // Bottom bar: a single right-aligned button that opens the book picker.
                // A null left slot renders a spacer, pushing the list icon to the end.
                LightBottomBar(
                    items = listOf(
                        null,
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

/**
 * Join a chapter's verses into one string with inline verse numbers. Each verse's
 * text already carries its own trailing whitespace (a space to continue a paragraph,
 * "\n\n" to end one), so concatenating them reproduces the original paragraphing.
 */
private fun chapterToText(verses: List<Verse>): String =
    verses.joinToString(separator = "") { "${it.verse} ${it.text}" }
