/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ActiveFocusListener
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import kotlin.math.roundToInt

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithEdgeButtonSample
 * @param scrollState The scroll state for [ScalingLazyColumn], used to drive screen transitions
 *   such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param edgeButton Slot for an [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: ScalingLazyListState,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        edgeButton = edgeButton,
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        edgeButtonSpacing = edgeButtonSpacing,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [ScalingLazyColumn], used to drive screen transitions
 *   such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        scrollIndicator = scrollIndicator,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithEdgeButtonSample
 *
 * Example of using ScreenScaffold with a [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.EdgeButtonListSample
 * @param scrollState The scroll state for [TransformingLazyColumn], used to drive screen
 *   transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param edgeButton Slot for an [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: TransformingLazyColumnState,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        edgeButton = edgeButton,
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        edgeButtonSpacing = edgeButtonSpacing,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 *
 * Example of using ScreenScaffold with a [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.EdgeButtonListSample
 * @param scrollState The scroll state for [TransformingLazyColumn], used to drive screen
 *   transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    scrollState: TransformingLazyColumnState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithEdgeButtonSample
 * @param scrollState The scroll state for [androidx.compose.foundation.lazy.LazyColumn], used to
 *   drive screen transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param edgeButton Slot for an [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: LazyListState,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        edgeButton = edgeButton,
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        edgeButtonSpacing = edgeButtonSpacing,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [androidx.compose.foundation.lazy.LazyColumn], used to
 *   drive screen transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState]. Note that
 * this version doesn't support a bottom button slot, for that use the overload that takes
 * [LazyListState] or the one that takes a [ScalingLazyListState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for a Column, used to drive screen transitions such as
 *   [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        content = content
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText], [ScrollIndicator] and the bottom button according to a
 * [scrollInfoProvider].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content. In this overload, both
 * edgeButton and scrollInfoProvider must be specified.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithEdgeButtonSample
 * @param scrollInfoProvider Provider for scroll information used to scroll away screen elements
 *   such as [TimeText] and coordinate showing/hiding the [ScrollIndicator], this needs to be a
 *   [ScrollInfoProvider].
 * @param edgeButton slot for a [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content. This gap size could
 *   not be smaller then [ScreenScaffoldDefaults.EdgeButtonMinSpacing].
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollInfoProvider: ScrollInfoProvider,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit {
    val effectiveEdgeButtonSpacing =
        (edgeButtonSpacing - ScreenScaffoldDefaults.EdgeButtonMinSpacing).coerceAtLeast(0.dp)
    // Adds the gap between content and edge button.
    val lastItemOffsetCorrection = with(LocalDensity.current) { effectiveEdgeButtonSpacing.toPx() }
    val localDensity = LocalDensity.current

    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollInfoProvider = scrollInfoProvider,
        scrollIndicator = scrollIndicator,
        content = {
            var intrinsicButtonHeight by remember(edgeButton) { mutableStateOf<Float?>(null) }

            content(
                // Replace bottom content padding adjusted for the edge button.
                ReplacePaddingValues(
                    contentPadding,
                    with(localDensity) {
                        (intrinsicButtonHeight?.toDp() ?: 0.dp) + effectiveEdgeButtonSpacing
                    }
                )
            )
            Box(
                contentAlignment = Alignment.BottomCenter,
                content = edgeButton,
                modifier =
                    Modifier.align(Alignment.BottomCenter).dynamicHeight(
                        onIntrinsicHeightMeasured = {
                            if (intrinsicButtonHeight != it) {
                                intrinsicButtonHeight = it
                            }
                        }
                    ) {
                        (scrollInfoProvider.lastItemOffset - lastItemOffsetCorrection)
                            .coerceAtLeast(0f)
                    },
            )
        }
    )
}

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollInfoProvider].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param modifier The modifier for the screen scaffold.
 * @param scrollInfoProvider Provider for scroll information used to scroll away screen elements
 *   such as [TimeText] and coordinate showing/hiding the [ScrollIndicator].
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    modifier: Modifier = Modifier,
    scrollInfoProvider: ScrollInfoProvider? = null,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit {
    val scaffoldState = LocalScaffoldState.current
    val key = remember { Any() }

    key(scrollInfoProvider) {
        DisposableEffect(key) { onDispose { scaffoldState.removeScreen(key) } }

        ActiveFocusListener { focused ->
            if (focused) {
                scaffoldState.addScreen(key, timeText, scrollInfoProvider)
            } else {
                scaffoldState.removeScreen(key)
            }
        }
    }

    scaffoldState.UpdateIdlingDetectorIfNeeded()

    Box(modifier = modifier.fillMaxSize()) {
        content(contentPadding)
        scrollInfoProvider?.let {
            AnimatedIndicator(
                isVisible = {
                    scaffoldState.screenStage.value != ScreenStage.Idle &&
                        scrollInfoProvider.isScrollable
                },
                content = scrollIndicator,
            )
        } ?: scrollIndicator?.let { it() }
    }
}

/** Contains the default values used by [ScreenScaffold] */
public object ScreenScaffoldDefaults {
    /**
     * Creates padding values with extra bottom padding for an EdgeButton.
     *
     * @param edgeButtonSize The size of the EdgeButton.
     * @param start The padding on the start side of the content.
     * @param top The padding on the top side of the content.
     * @param end The padding on the end side of the content.
     * @param extraBottom Additional padding to be added to the bottom padding calculated from the
     *   edge button size.
     * @return A [PaddingValues] object with the calculated padding.
     */
    @Deprecated(
        "Use the combination of a new contentPadding parameter on the ScreenScaffold to " +
            "get back the padding values inside of the content lambda."
    )
    public fun contentPaddingWithEdgeButton(
        edgeButtonSize: EdgeButtonSize,
        start: Dp = 0.dp,
        top: Dp = 0.dp,
        end: Dp = 0.dp,
        extraBottom: Dp = 0.dp,
    ): PaddingValues =
        PaddingValues(start, top, end, extraBottom + edgeButtonSize.maximumHeightPlusPadding())

    /** The default space between [EdgeButton] and list content in [ScreenScaffold]. */
    public val EdgeButtonSpacing: Dp = 16.dp

    /** The minimum space between [EdgeButton] and list content in [ScreenScaffold]. */
    public val EdgeButtonMinSpacing: Dp = EdgeButtonVerticalPadding

    /** Default contentPadding added to the [ScreenScaffold]. */
    public val contentPadding: PaddingValues
        @Composable
        get() =
            PaddingValues(
                horizontal = PaddingDefaults.horizontalContentPadding(),
                vertical = PaddingDefaults.verticalContentPadding()
            )
}

// Sets the height that will be used down the line, using a state as parameter, to avoid
// recompositions when the height changes.
internal fun Modifier.dynamicHeight(
    onIntrinsicHeightMeasured: (Float) -> Unit,
    heightState: () -> Float
) = this.then(DynamicHeightElement(onIntrinsicHeightMeasured, heightState))

// Following classes 'inspired' by 'WrapContentElement' / 'WrapContentNode'
private class DynamicHeightElement(
    val onIntrinsicHeightMeasured: (Float) -> Unit,
    val heightState: () -> Float
) : ModifierNodeElement<DynamicHeightNode>() {
    override fun create(): DynamicHeightNode =
        DynamicHeightNode(onIntrinsicHeightMeasured, heightState)

    override fun update(node: DynamicHeightNode) {
        node.heightState = heightState
        node.onIntrinsicHeightMeasured = onIntrinsicHeightMeasured
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "DynamicHeightElement"
    }

    override fun equals(other: Any?) =
        other is DynamicHeightElement &&
            heightState === other.heightState &&
            onIntrinsicHeightMeasured === other.onIntrinsicHeightMeasured

    override fun hashCode() = 31 * heightState.hashCode() + onIntrinsicHeightMeasured.hashCode()
}

private class DynamicHeightNode(
    var onIntrinsicHeightMeasured: (Float) -> Unit,
    var heightState: () -> Float
) : LayoutModifierNode, Modifier.Node() {

    var lastMeasureHeight: Int? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Similar to .fillMaxWidth().height(heightState.value) but we observe the state in the
        // measurement pass, not on Composition.
        val height = heightState().roundToInt()
        if (lastMeasureHeight == null || height > 0 && lastMeasureHeight != height) {
            onIntrinsicHeightMeasured(measurable.maxIntrinsicHeight(constraints.maxWidth).toFloat())
            lastMeasureHeight = height
        }
        val wrappedConstraints =
            Constraints(constraints.maxWidth, constraints.maxWidth, height, height)
        val placeable = measurable.measure(wrappedConstraints)
        // Report that we take the full space, and BottomCenter align the content.
        val wrapperWidth = constraints.maxWidth
        val wrapperHeight = constraints.maxHeight
        return layout(wrapperWidth, wrapperHeight) {
            val position =
                IntOffset(
                    x = (wrapperWidth - placeable.width) / 2,
                    y = wrapperHeight - placeable.height
                )
            placeable.place(position)
        }
    }
}

private class ReplacePaddingValues(paddingValues: PaddingValues, val bottomPadding: Dp) :
    PaddingValues by paddingValues {
    override fun calculateBottomPadding(): Dp = bottomPadding
}
