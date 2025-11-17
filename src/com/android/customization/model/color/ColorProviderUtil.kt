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

package com.android.customization.model.color

import android.content.Context
import android.content.res.ColorStateList
import android.content.theming.ThemeStyle
import androidx.annotation.ColorInt
import androidx.annotation.StringDef
import androidx.core.graphics.ColorUtils.setAlphaComponent
import com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_COLOR
import com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_SYSTEM_PALETTE
import com.android.customization.model.color.ColorUtils.toColorString
import com.android.customization.picker.color.shared.model.ColorType
import com.android.systemui.monet.ColorScheme
import com.android.themepicker.R

object ColorProviderUtil {
    private const val ALPHA_MASK = 0xFF

    /**
     * Extra setting indicating the source of the color overlays (it can be one of
     * COLOR_SOURCE_PRESET, COLOR_SOURCE_HOME or COLOR_SOURCE_LOCK)
     */
    const val OVERLAY_COLOR_SOURCE: String = "android.theme.customization.color_source"

    /** Extra setting indicating the style of the color overlays (it can be one of [ThemeStyle]). */
    const val OVERLAY_THEME_STYLE: String = "android.theme.customization.theme_style"

    /** Users selected color option, its value starts from 1 (which means first option). */
    const val OVERLAY_COLOR_INDEX: String = "android.theme.customization.color_index"

    /**
     * Users selected color from both home and lock screen. Example value: 0 means home or lock
     * screen, 1 means both.
     */
    const val OVERLAY_COLOR_BOTH: String = "android.theme.customization.color_both"

    const val COLOR_SOURCE_PRESET = "preset"
    const val COLOR_SOURCE_HOME = "home_wallpaper"
    const val COLOR_SOURCE_LOCK = "lock_wallpaper"

    @StringDef(COLOR_SOURCE_PRESET, COLOR_SOURCE_HOME, COLOR_SOURCE_LOCK)
    annotation class ColorSource

    /** Builds a [ColorOptionImpl] for a wallpaper-based color with a specified color and style */
    fun buildBundle(
        context: Context,
        colorInt: Int,
        index: Int,
        @ThemeStyle.Type style: Int,
        isDefault: Boolean,
    ): ColorOptionImpl {
        val lightColorScheme = ColorScheme(colorInt, /* darkTheme= */ false, style)
        val darkColorScheme = ColorScheme(colorInt, /* darkTheme= */ true, style)
        val builder = ColorOptionImpl.Builder()
        builder.lightColors = getLightColorPreview(lightColorScheme)
        builder.darkColors = getDarkColorPreview(darkColorScheme)
        builder.seedColor = colorInt
        builder.addOverlayPackage(
            OVERLAY_CATEGORY_SYSTEM_PALETTE,
            if (isDefault) "" else toColorString(colorInt),
        )
        builder.title =
            when (style) {
                ThemeStyle.TONAL_SPOT ->
                    context.getString(R.string.content_description_dynamic_color_option)
                ThemeStyle.SPRITZ ->
                    context.getString(R.string.content_description_neutral_color_option)
                ThemeStyle.VIBRANT ->
                    context.getString(R.string.content_description_vibrant_color_option)
                ThemeStyle.EXPRESSIVE ->
                    context.getString(R.string.content_description_expressive_color_option)
                else -> context.getString(R.string.content_description_dynamic_color_option)
            }
        builder.source = COLOR_SOURCE_HOME
        builder.style = style
        builder.index = index
        builder.isDefault = isDefault
        builder.type = ColorType.WALLPAPER_COLOR
        return builder.build()
    }

    /** Builds a [ColorOptionImpl] for a preset color with a specified color and style */
    fun buildPreset(
        title: String,
        color: Int,
        index: Int,
        @ThemeStyle.Type style: Int? = null,
        type: ColorType = ColorType.PRESET_COLOR,
    ): ColorOptionImpl {
        val builder = ColorOptionImpl.Builder()
        builder.title = title
        builder.index = index
        builder.source = COLOR_SOURCE_PRESET
        builder.type = type
        var darkColorScheme = ColorScheme(color, /* darkTheme= */ true)
        var lightColorScheme = ColorScheme(color, /* darkTheme= */ false)
        val lightColor = lightColorScheme.accentColor
        val darkColor = darkColorScheme.accentColor
        var lightColors = intArrayOf(lightColor, lightColor, lightColor, lightColor)
        var darkColors = intArrayOf(darkColor, darkColor, darkColor, darkColor)
        builder.seedColor = color
        builder.addOverlayPackage(OVERLAY_CATEGORY_COLOR, toColorString(color))
        builder.addOverlayPackage(OVERLAY_CATEGORY_SYSTEM_PALETTE, toColorString(color))
        if (style != null) {
            builder.style = style

            lightColorScheme = ColorScheme(color, /* darkTheme= */ false, style)
            darkColorScheme = ColorScheme(color, /* darkTheme= */ true, style)

            when (style) {
                ThemeStyle.MONOCHROMATIC -> {
                    darkColors = getDarkMonochromePreview(darkColorScheme)
                    lightColors = getLightMonochromePreview(lightColorScheme)
                }
                else -> {
                    darkColors = getDarkPresetColorPreview(darkColorScheme)
                    lightColors = getLightPresetColorPreview(lightColorScheme)
                }
            }
        }
        builder.lightColors = lightColors
        builder.darkColors = darkColors
        return builder.build()
    }

    fun getColorPreview(
        colorScheme: ColorScheme,
        colorSource: String?,
        darkTheme: Boolean,
    ): IntArray {
        return if (colorSource == COLOR_SOURCE_HOME || colorSource == COLOR_SOURCE_LOCK) {
            if (darkTheme) {
                getDarkColorPreview(colorScheme)
            } else {
                getLightColorPreview(colorScheme)
            }
        } else if (colorScheme.style == ThemeStyle.MONOCHROMATIC) {
            if (darkTheme) {
                getDarkMonochromePreview(colorScheme)
            } else {
                getLightMonochromePreview(colorScheme)
            }
        } else {
            if (darkTheme) {
                getDarkPresetColorPreview(colorScheme)
            } else {
                getLightPresetColorPreview(colorScheme)
            }
        }
    }

    /**
     * Returns the light theme preview of a dynamic ColorScheme based on this order: top left, top
     * right, bottom left, bottom right
     *
     * This color mapping corresponds to GM3 colors: Primary (light), Primary (light), Secondary
     * LStar 85, and Tertiary LStar 70
     */
    @ColorInt
    private fun getLightColorPreview(colorScheme: ColorScheme): IntArray {
        return intArrayOf(
            setAlphaComponent(colorScheme.accent1.s600, ALPHA_MASK),
            setAlphaComponent(colorScheme.accent1.s600, ALPHA_MASK),
            ColorStateList.valueOf(colorScheme.accent2.s500).withLStar(85f).colors[0],
            setAlphaComponent(colorScheme.accent3.s300, ALPHA_MASK),
        )
    }

    /**
     * Returns the dark theme preview of a dynamic ColorScheme based on this order: top left, top
     * right, bottom left, bottom right
     *
     * This color mapping corresponds to GM3 colors: Primary (dark), Primary (dark), Secondary LStar
     * 35, and Tertiary LStar 70
     */
    @ColorInt
    private fun getDarkColorPreview(colorScheme: ColorScheme): IntArray {
        return intArrayOf(
            setAlphaComponent(colorScheme.accent1.s200, ALPHA_MASK),
            setAlphaComponent(colorScheme.accent1.s200, ALPHA_MASK),
            ColorStateList.valueOf(colorScheme.accent2.s500).withLStar(35f).colors[0],
            setAlphaComponent(colorScheme.accent3.s300, ALPHA_MASK),
        )
    }

    /**
     * Returns the light theme preview of a monochrome ColorScheme based on this order: top left,
     * top right, bottom left, bottom right
     *
     * This color mapping corresponds to GM3 colors: Primary LStar 0, Primary LStar 0, Secondary
     * LStar 85, and Tertiary LStar 70
     */
    @ColorInt
    private fun getLightMonochromePreview(colorScheme: ColorScheme): IntArray {
        return intArrayOf(
            setAlphaComponent(colorScheme.accent1.s1000, ALPHA_MASK),
            setAlphaComponent(colorScheme.accent1.s1000, ALPHA_MASK),
            ColorStateList.valueOf(colorScheme.accent2.s500).withLStar(85f).colors[0],
            setAlphaComponent(colorScheme.accent3.s300, ALPHA_MASK),
        )
    }

    /**
     * Returns the dark theme preview of a monochrome ColorScheme based on this order: top left, top
     * right, bottom left, bottom right
     *
     * This color mapping corresponds to GM3 colors: Primary LStar 99, Primary LStar 99, Secondary
     * LStar 35, and Tertiary LStar 70
     */
    @ColorInt
    private fun getDarkMonochromePreview(colorScheme: ColorScheme): IntArray {
        return intArrayOf(
            setAlphaComponent(colorScheme.accent1.s10, ALPHA_MASK),
            setAlphaComponent(colorScheme.accent1.s10, ALPHA_MASK),
            ColorStateList.valueOf(colorScheme.accent2.s500).withLStar(35f).colors[0],
            setAlphaComponent(colorScheme.accent3.s300, ALPHA_MASK),
        )
    }

    /**
     * Returns the light theme contrast-adjusted preview of a preset ColorScheme, based on this
     * order: top left, top right, bottom left, bottom right
     */
    private fun getDarkPresetColorPreview(colorScheme: ColorScheme): IntArray {
        val colors =
            when (colorScheme.style) {
                ThemeStyle.FRUIT_SALAD ->
                    intArrayOf(colorScheme.accent3.s100, colorScheme.accent1.s200)
                else -> intArrayOf(colorScheme.accent1.s200, colorScheme.accent1.s200)
            }
        return intArrayOf(colors[0], colors[1], colors[0], colors[1])
    }

    /**
     * Returns the light theme contrast-adjusted preview of a preset ColorScheme, based on this
     * order: top left, top right, bottom left, bottom right
     */
    private fun getLightPresetColorPreview(colorScheme: ColorScheme): IntArray {
        val colors =
            when (colorScheme.style) {
                ThemeStyle.FRUIT_SALAD ->
                    intArrayOf(
                        colorScheme.accent3.getAtTone(450f),
                        colorScheme.accent1.getAtTone(550f),
                    )
                else ->
                    intArrayOf(
                        colorScheme.accent1.getAtTone(450f),
                        colorScheme.accent1.getAtTone(450f),
                    )
            }
        return intArrayOf(colors[0], colors[1], colors[0], colors[1])
    }
}
