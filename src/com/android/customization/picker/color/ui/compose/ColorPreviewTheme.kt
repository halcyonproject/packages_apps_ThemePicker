/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.customization.picker.color.ui.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors

@Immutable
interface ColorTransitionData {
    val primary: Color
    val onPrimary: Color
    val onPrimaryFixedVariant: Color
    val secondaryContainer: Color
    val onSecondaryContainer: Color
    val surfaceBright: Color
    val onSurface: Color
    val onSurfaceVariant: Color
}

val defaultColorTransitionData =
    object : ColorTransitionData {
        override val primary: Color = Color.Transparent
        override val onPrimary: Color = Color.Transparent
        override val onPrimaryFixedVariant: Color = Color.Transparent
        override val secondaryContainer: Color = Color.Transparent
        override val onSecondaryContainer: Color = Color.Transparent
        override val surfaceBright: Color = Color.Transparent
        override val onSurface: Color = Color.Transparent
        override val onSurfaceVariant: Color = Color.Transparent
    }

val LocalAnimatedColorScheme = compositionLocalOf { defaultColorTransitionData }

@Composable
fun ColorPreviewTheme(scheme: DynamicScheme?, content: @Composable () -> Unit) {
    if (scheme == null) {
        return content()
    }
    val colorScheme = remember(scheme) { getColorScheme(scheme) }
    val colorTransitionData = updateTransitionData(colorScheme)

    // Done at the root so that the whole content tree will receive the LocalAnimatedColorScheme.
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            value = LocalAnimatedColorScheme provides colorTransitionData,
            content = content,
        )
    }
}

@Composable
fun updateTransitionData(colorScheme: ColorScheme): ColorTransitionData {
    val transition = updateTransition(colorScheme)
    val primary = transition.animateThemeColor { state -> state.primary }
    val onPrimary = transition.animateThemeColor { state -> state.onPrimary }
    val onPrimaryFixedVariant =
        transition.animateThemeColor { state -> state.onPrimaryFixedVariant }
    val secondaryContainer = transition.animateThemeColor { state -> state.secondaryContainer }
    val onSecondaryContainer = transition.animateThemeColor { state -> state.onSecondaryContainer }
    val surfaceBright = transition.animateThemeColor { state -> state.surfaceBright }
    val onSurface = transition.animateThemeColor { state -> state.onSurface }
    val onSurfaceVariant = transition.animateThemeColor { state -> state.onSurfaceVariant }
    return remember(transition) {
        object : ColorTransitionData {
            override val primary: Color by primary
            override val onPrimary: Color by onPrimary
            override val onPrimaryFixedVariant: Color by onPrimaryFixedVariant
            override val secondaryContainer: Color by secondaryContainer
            override val onSecondaryContainer: Color by onSecondaryContainer
            override val surfaceBright: Color by surfaceBright
            override val onSurface: Color by onSurface
            override val onSurfaceVariant: Color by onSurfaceVariant
        }
    }
}

@Composable
fun Transition<ColorScheme>.animateThemeColor(
    targetValueByState: @Composable (state: ColorScheme) -> Color
): State<Color> {
    return animateColor(
        transitionSpec = {
            tween(
                durationMillis = ColorUpdateViewModel.COLOR_ANIMATION_DURATION_MILLIS.toInt(),
                easing = LinearEasing,
            )
        },
        targetValueByState = targetValueByState,
    )
}

// Convert DynamicScheme to Compose ColorScheme
// TODO (b/450071500): use dynamiccolors library to get Compose ColorScheme when it is ready
fun getColorScheme(scheme: DynamicScheme): ColorScheme {
    val materialColors = MaterialDynamicColors()
    return ColorScheme(
        primary = Color(materialColors.primary().getArgb(scheme)),
        onPrimary = Color(materialColors.onPrimary().getArgb(scheme)),
        primaryContainer = Color(materialColors.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = Color(materialColors.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = Color(materialColors.inversePrimary().getArgb(scheme)),
        secondary = Color(materialColors.secondary().getArgb(scheme)),
        onSecondary = Color(materialColors.onSecondary().getArgb(scheme)),
        secondaryContainer = Color(materialColors.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = Color(materialColors.onSecondaryContainer().getArgb(scheme)),
        tertiary = Color(materialColors.tertiary().getArgb(scheme)),
        onTertiary = Color(materialColors.onTertiary().getArgb(scheme)),
        tertiaryContainer = Color(materialColors.tertiaryContainer().getArgb(scheme)),
        onTertiaryContainer = Color(materialColors.onTertiaryContainer().getArgb(scheme)),
        background = Color(materialColors.background().getArgb(scheme)),
        onBackground = Color(materialColors.onBackground().getArgb(scheme)),
        surface = Color(materialColors.surface().getArgb(scheme)),
        onSurface = Color(materialColors.onSurface().getArgb(scheme)),
        surfaceVariant = Color(materialColors.surfaceVariant().getArgb(scheme)),
        onSurfaceVariant = Color(materialColors.onSurfaceVariant().getArgb(scheme)),
        surfaceTint = Color(materialColors.surfaceTint().getArgb(scheme)),
        inverseSurface = Color(materialColors.inverseSurface().getArgb(scheme)),
        inverseOnSurface = Color(materialColors.inverseOnSurface().getArgb(scheme)),
        error = Color(materialColors.error().getArgb(scheme)),
        onError = Color(materialColors.onError().getArgb(scheme)),
        errorContainer = Color(materialColors.errorContainer().getArgb(scheme)),
        onErrorContainer = Color(materialColors.onErrorContainer().getArgb(scheme)),
        outline = Color(materialColors.outline().getArgb(scheme)),
        outlineVariant = Color(materialColors.outlineVariant().getArgb(scheme)),
        scrim = Color(materialColors.scrim().getArgb(scheme)),
        surfaceBright = Color(materialColors.surfaceBright().getArgb(scheme)),
        surfaceDim = Color(materialColors.surfaceDim().getArgb(scheme)),
        surfaceContainer = Color(materialColors.surfaceContainer().getArgb(scheme)),
        surfaceContainerHigh = Color(materialColors.surfaceContainerHigh().getArgb(scheme)),
        surfaceContainerHighest = Color(materialColors.surfaceContainerHighest().getArgb(scheme)),
        surfaceContainerLow = Color(materialColors.surfaceContainerLow().getArgb(scheme)),
        surfaceContainerLowest = Color(materialColors.surfaceContainerLowest().getArgb(scheme)),
        primaryFixed = Color(materialColors.primaryFixed().getArgb(scheme)),
        primaryFixedDim = Color(materialColors.primaryFixedDim().getArgb(scheme)),
        onPrimaryFixed = Color(materialColors.onPrimaryFixed().getArgb(scheme)),
        onPrimaryFixedVariant = Color(materialColors.onPrimaryFixedVariant().getArgb(scheme)),
        secondaryFixed = Color(materialColors.secondaryFixed().getArgb(scheme)),
        secondaryFixedDim = Color(materialColors.secondaryFixedDim().getArgb(scheme)),
        onSecondaryFixed = Color(materialColors.onSecondaryFixed().getArgb(scheme)),
        onSecondaryFixedVariant = Color(materialColors.onSecondaryFixedVariant().getArgb(scheme)),
        tertiaryFixed = Color(materialColors.tertiaryFixed().getArgb(scheme)),
        tertiaryFixedDim = Color(materialColors.tertiaryFixedDim().getArgb(scheme)),
        onTertiaryFixed = Color(materialColors.onTertiaryFixed().getArgb(scheme)),
        onTertiaryFixedVariant = Color(materialColors.onTertiaryFixedVariant().getArgb(scheme)),
    )
}
