/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.theming.ThemeStyle
import android.graphics.Color
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.server.Flags.FLAG_ENABLE_THEME_SERVICE
import com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_SYSTEM_PALETTE
import com.android.customization.model.color.ColorProviderUtil.COLOR_SOURCE_HOME
import com.android.customization.model.color.ColorProviderUtil.COLOR_SOURCE_LOCK
import com.android.customization.model.color.ColorProviderUtil.COLOR_SOURCE_PRESET
import com.android.customization.picker.color.shared.model.ColorType
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner

/** Tests of {@link ColorOption}. */
@RunWith(RobolectricTestRunner::class)
class ColorOptionTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val rule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var manager: ColorCustomizationManager

    @Test
    fun colorOption_Source() {
        testColorOptionSource(COLOR_SOURCE_HOME)
        testColorOptionSource(COLOR_SOURCE_LOCK)
        testColorOptionSource(COLOR_SOURCE_PRESET)
    }

    private fun testColorOptionSource(source: String) {
        val colorOption: ColorOption =
            ColorOptionImpl(
                title = "fake color",
                source = source,
                seedColor = 12345,
                style = ThemeStyle.TONAL_SPOT,
                isThemeServiceEnabled = false,
                overlayPackages = mapOf("fake_package" to "fake_color"),
                isDefault = false,
                index = 0,
                previewInfo = ColorOptionImpl.PreviewInfo(intArrayOf(0), intArrayOf(0)),
                type = ColorType.WALLPAPER_COLOR,
            )
        assertThat(colorOption.source).isEqualTo(source)
    }

    @Test
    fun colorOption_style() {
        testColorOptionStyle(ThemeStyle.TONAL_SPOT)
        testColorOptionStyle(ThemeStyle.SPRITZ)
        testColorOptionStyle(ThemeStyle.VIBRANT)
        testColorOptionStyle(ThemeStyle.EXPRESSIVE)
    }

    private fun testColorOptionStyle(@ThemeStyle.Type style: Int) {
        val colorOption: ColorOption =
            ColorOptionImpl(
                title = "fake color",
                source = "fake_source",
                seedColor = 12345,
                style = style,
                isThemeServiceEnabled = false,
                overlayPackages = mapOf("fake_package" to "fake_color"),
                isDefault = false,
                index = 0,
                previewInfo = ColorOptionImpl.PreviewInfo(intArrayOf(0), intArrayOf(0)),
                type = ColorType.WALLPAPER_COLOR,
            )
        assertThat(colorOption.style).isEqualTo(style)
    }

    @Test
    fun colorOption_index() {
        testColorOptionIndex(1)
        testColorOptionIndex(2)
        testColorOptionIndex(3)
        testColorOptionIndex(4)
    }

    private fun testColorOptionIndex(index: Int) {
        val colorOption: ColorOption =
            ColorOptionImpl(
                title = "fake color",
                source = "fake_source",
                seedColor = 12345,
                style = ThemeStyle.TONAL_SPOT,
                isThemeServiceEnabled = false,
                overlayPackages = mapOf("fake_package" to "fake_color"),
                isDefault = false,
                index = index,
                previewInfo = ColorOptionImpl.PreviewInfo(intArrayOf(0), intArrayOf(0)),
                type = ColorType.WALLPAPER_COLOR,
            )
        assertThat(colorOption.index).isEqualTo(index)
    }

    @Test
    fun colorOption_seedColor() {
        testColorOptionSeed(Color.RED)
        testColorOptionSeed(Color.WHITE)
        testColorOptionSeed(Color.BLACK)
    }

    private fun testColorOptionSeed(seedColor: Int) {
        val colorOption: ColorOption =
            ColorOptionImpl(
                title = "fake color",
                source = "fake_source",
                seedColor = seedColor,
                style = ThemeStyle.TONAL_SPOT,
                isThemeServiceEnabled = false,
                overlayPackages = mapOf("fake_package" to "fake_color"),
                isDefault = false,
                index = 0,
                previewInfo = ColorOptionImpl.PreviewInfo(intArrayOf(0), intArrayOf(0)),
                type = ColorType.WALLPAPER_COLOR,
            )
        assertThat(colorOption.seedColor).isEqualTo(seedColor)
    }

    private fun setUpWallpaperColorOption(
        isDefault: Boolean,
        source: String = "some_source",
    ): ColorOptionImpl {
        val overlays =
            if (isDefault) {
                HashMap()
            } else {
                mapOf(OVERLAY_CATEGORY_SYSTEM_PALETTE to "fake_color")
            }
        `when`(manager.currentOverlays).thenReturn(overlays)
        return ColorOptionImpl(
            title = "fake color",
            source = source,
            seedColor = 12345,
            style = ThemeStyle.TONAL_SPOT,
            isThemeServiceEnabled = false,
            overlayPackages = overlays,
            isDefault = isDefault,
            index = 0,
            previewInfo = ColorOptionImpl.PreviewInfo(intArrayOf(0), intArrayOf(0)),
            type = ColorType.WALLPAPER_COLOR,
        )
    }

    private fun setUpThemeServiceColorOptionAndManager(
        isDefault: Boolean,
        source: String = "some_source",
        overlays: Map<String, String?> = mapOf(OVERLAY_CATEGORY_SYSTEM_PALETTE to "fake_color"),
        @ThemeStyle.Type style: Int = ThemeStyle.TONAL_SPOT,
    ): ColorOptionImpl {
        val json = JSONObject(overlays).toString()
        `when`(manager.storedOverlays).thenReturn(json)
        `when`(manager.currentOverlays).thenReturn(overlays)
        `when`(manager.currentColorSource).thenReturn(source)
        `when`(manager.currentStyle).thenReturn(ThemeStyle.toString(style))
        return ColorOptionImpl(
            title = "fake color",
            source = source,
            seedColor = 12345,
            style = style,
            isThemeServiceEnabled = true,
            overlayPackages = overlays,
            isDefault = isDefault,
            index = 0,
            previewInfo = ColorOptionImpl.PreviewInfo(intArrayOf(0), intArrayOf(0)),
            type = ColorType.WALLPAPER_COLOR,
        )
    }

    @Test
    @DisableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun wallpaperColorOption_isActive_notDefault_SourceSet() {
        val source = "some_source"
        val colorOption = setUpWallpaperColorOption(isDefault = false, source = source)
        `when`(manager.currentColorSource).thenReturn(source)

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun wallpaperColorOption_isActive_notDefault_NoSource() {
        val colorOption = setUpWallpaperColorOption(isDefault = false)
        `when`(manager.currentColorSource).thenReturn(null)

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun wallpaperColorOption_isActive_notDefault_differentSource() {
        val colorOption = setUpWallpaperColorOption(isDefault = false)
        `when`(manager.currentColorSource).thenReturn("some_other_source")

        assertThat(colorOption.isActive(manager)).isFalse()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun wallpaperColorOption_isActive_default_emptyJson() {
        val colorOption = setUpWallpaperColorOption(isDefault = true)
        `when`(manager.storedOverlays).thenReturn("")

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun wallpaperColorOption_isActive_default_nonEmptyJson() {
        val colorOption = setUpWallpaperColorOption(isDefault = true)

        `when`(manager.storedOverlays).thenReturn("{non-empty-json}")

        // Should still be Active because overlays is empty
        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun wallpaperColorOption_isActive_default_nonEmptyOverlays() {
        val colorOption = setUpWallpaperColorOption(isDefault = true)

        val settings = mapOf(OVERLAY_CATEGORY_SYSTEM_PALETTE to "fake_color")
        val json = JSONObject(settings).toString()
        `when`(manager.storedOverlays).thenReturn(json)
        `when`(manager.currentOverlays).thenReturn(settings)
        assertThat(colorOption.isActive(manager)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_themeServiceEnabled() {
        val colorOption = setUpThemeServiceColorOptionAndManager(isDefault = false)

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_default_themeServiceEnabled() {
        val colorOption = setUpThemeServiceColorOptionAndManager(isDefault = true)

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_noSourceInManager_themeServiceEnabled() {
        val colorOption = setUpThemeServiceColorOptionAndManager(isDefault = false)
        `when`(manager.currentColorSource).thenReturn(null)

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_differentSourceInManager_themeServiceEnabled() {
        val colorOption =
            setUpThemeServiceColorOptionAndManager(isDefault = false, source = "some_source")
        `when`(manager.currentColorSource).thenReturn("some_other_source")

        assertThat(colorOption.isActive(manager)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_noStyleInManager_tonalSpotOption_themeServiceEnabled() {
        val colorOption =
            setUpThemeServiceColorOptionAndManager(isDefault = false, style = ThemeStyle.TONAL_SPOT)
        `when`(manager.currentStyle).thenReturn(null)

        assertThat(colorOption.isActive(manager)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_noStyleInManager_nonTonalSpotOption_themeServiceEnabled() {
        val colorOption =
            setUpThemeServiceColorOptionAndManager(isDefault = false, style = ThemeStyle.VIBRANT)
        `when`(manager.currentStyle).thenReturn(null)

        assertThat(colorOption.isActive(manager)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_differentStyleInManager_themeServiceEnabled() {
        val colorOption =
            setUpThemeServiceColorOptionAndManager(isDefault = false, style = ThemeStyle.VIBRANT)
        `when`(manager.currentStyle).thenReturn(ThemeStyle.toString(ThemeStyle.SPRITZ))

        assertThat(colorOption.isActive(manager)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_default_emptyJsonInManager_themeServiceEnabled() {
        val colorOption = setUpThemeServiceColorOptionAndManager(isDefault = true)
        `when`(manager.storedOverlays).thenReturn("")
        `when`(manager.currentOverlays).thenReturn(mapOf())

        assertThat(colorOption.isActive(manager)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_THEME_SERVICE)
    fun isActive_default_nonEmptyJsonInManager_themeServiceEnabled() {
        val colorOption = setUpThemeServiceColorOptionAndManager(isDefault = true)
        val overlays = mapOf("some_package" to "some_color", "other_package" to "other_color")
        val json = JSONObject(overlays).toString()
        `when`(manager.storedOverlays).thenReturn(json)
        `when`(manager.currentOverlays).thenReturn(overlays)

        assertThat(colorOption.isActive(manager)).isFalse()
    }
}
