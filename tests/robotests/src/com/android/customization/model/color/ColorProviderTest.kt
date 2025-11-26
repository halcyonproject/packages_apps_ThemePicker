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

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Color
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import com.android.customization.model.CustomizationManager
import com.android.customization.model.ResourceConstants
import com.android.customization.model.ResourcesApkProvider
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.Flags.FLAG_COLOR_PICKER_UPDATE_FLAG
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@HiltAndroidTest
class ColorProviderTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val rule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var resourcesApkProvider: ResourcesApkProvider

    private lateinit var context: Context
    private lateinit var mockitoCloseable: AutoCloseable
    private lateinit var colorProvider: ColorProvider
    private lateinit var optionsResult: SettableFuture<List<ColorOption>?>
    private lateinit var listener: CustomizationManager.OptionsFetchedListener<ColorOption>

    private class TestColorProvider(
        context: Context,
        stubPackageName: String,
        override val resourcesApkProvider: ResourcesApkProvider,
        override val monetEnabled: Boolean,
    ) : ColorProvider(context, stubPackageName)

    @Before
    fun setUp() {
        hiltRule.inject()
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        mockitoCloseable = MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        colorProvider =
            TestColorProvider(
                context = context,
                stubPackageName = "com.android.customization.resources.test",
                resourcesApkProvider = resourcesApkProvider,
                monetEnabled = true,
            )
        optionsResult = SettableFuture.create()
        listener =
            object : CustomizationManager.OptionsFetchedListener<ColorOption> {
                override fun onOptionsLoaded(options: MutableList<ColorOption>?) {
                    optionsResult.set(options)
                }

                override fun onError(throwable: Throwable?) {
                    optionsResult.setException(throwable ?: RuntimeException("Fetch failed"))
                }
            }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mockitoCloseable.close()
    }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_nullWallpaperColors_flagOff() = fetch_nullWallpaperColors_noWallpaperColorOptions()

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_nullWallpaperColors_flagOn() = fetch_nullWallpaperColors_noWallpaperColorOptions()

    private fun fetch_nullWallpaperColors_noWallpaperColorOptions() = runTest {
        `when`(resourcesApkProvider.isAvailable).thenReturn(true)
        `when`(resourcesApkProvider.getItemsFromStub(ResourceConstants.COLOR_BUNDLES_ARRAY_NAME))
            .thenReturn(emptyArray())

        colorProvider.fetch(listener, reload = true, homeWallpaperColors = null)

        assertThat(optionsResult.await()?.filter { it.source == "home_wallpaper" }).isEmpty()
    }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_withWallpaperColors_generatesWallpaperColorOptions() = runTest {
        val wallpaperColors =
            WallpaperColors(Color.valueOf(Color.RED), Color.valueOf(Color.YELLOW), null)
        `when`(resourcesApkProvider.isAvailable).thenReturn(true)
        `when`(resourcesApkProvider.getItemsFromStub(ResourceConstants.COLOR_BUNDLES_ARRAY_NAME))
            .thenReturn(emptyArray())

        colorProvider.fetch(listener, reload = true, homeWallpaperColors = wallpaperColors)

        val wallpaperOptions = optionsResult.await()?.filter { it.source == "home_wallpaper" }
        // Flag isColorPickerUpdateEnabled is false, so we expect 4 options each seed, 1 per style.
        assertThat(wallpaperOptions).isNotNull()
        assertThat(wallpaperOptions!!).hasSize(8)
        assertThat(wallpaperOptions[0].index).isEqualTo(1)
        assertThat(wallpaperOptions[0].isDefault).isEqualTo(true)
        assertThat(wallpaperOptions[1].index).isEqualTo(1)
        assertThat(wallpaperOptions[1].isDefault).isEqualTo(true)
        assertThat(wallpaperOptions[2].index).isEqualTo(1)
        assertThat(wallpaperOptions[3].index).isEqualTo(1)
        assertThat(wallpaperOptions[4].index).isEqualTo(2)
        assertThat(wallpaperOptions[4].isDefault).isEqualTo(false)
        assertThat(wallpaperOptions[5].index).isEqualTo(2)
        assertThat(wallpaperOptions[6].index).isEqualTo(2)
        assertThat(wallpaperOptions[7].index).isEqualTo(2)
    }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_withWallpaperColors_colorPickerUpdateEnabled_generatesSeedColorOptions() = runTest {
        val wallpaperColors =
            WallpaperColors(Color.valueOf(Color.RED), Color.valueOf(Color.YELLOW), null)
        `when`(resourcesApkProvider.isAvailable).thenReturn(true)
        `when`(resourcesApkProvider.getItemsFromStub(ResourceConstants.COLOR_BUNDLES_ARRAY_NAME))
            .thenReturn(emptyArray())

        colorProvider.fetch(listener, reload = true, homeWallpaperColors = wallpaperColors)

        val wallpaperOptions = optionsResult.await()?.filter { it.source == "home_wallpaper" }
        // Flag isColorPickerUpdateEnabled is true, so we would expect 1 option for each seed.
        assertThat(wallpaperOptions).isNotNull()
        assertThat(wallpaperOptions!!).hasSize(2)
        assertThat(wallpaperOptions[0].index).isEqualTo(1)
        assertThat(wallpaperOptions[0].isDefault).isEqualTo(true)
        assertThat(wallpaperOptions[1].index).isEqualTo(2)
        assertThat(wallpaperOptions[1].isDefault).isEqualTo(false)
    }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_loadsPresetColors_flagOff() = fetch_loadsPresetColors()

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_loadsPresetColors_flagOn() = fetch_loadsPresetColors()

    private fun fetch_loadsPresetColors() = runTest {
        val bundleNames = arrayOf("preset_1", "preset_2")
        `when`(resourcesApkProvider.isAvailable).thenReturn(true)
        `when`(resourcesApkProvider.getItemsFromStub(ResourceConstants.COLOR_BUNDLES_ARRAY_NAME))
            .thenReturn(bundleNames)
        `when`(
                resourcesApkProvider.getItemStringFromStub(
                    ResourceConstants.COLOR_BUNDLE_NAME_PREFIX,
                    "preset_1",
                )
            )
            .thenReturn("Preset 1")
        `when`(
                resourcesApkProvider.getItemColorFromStub(
                    ResourceConstants.COLOR_BUNDLE_MAIN_COLOR_PREFIX,
                    "preset_1",
                )
            )
            .thenReturn(Color.BLUE)
        `when`(
                resourcesApkProvider.getItemStringFromStub(
                    ResourceConstants.COLOR_BUNDLE_NAME_PREFIX,
                    "preset_2",
                )
            )
            .thenReturn("Preset 2")
        `when`(
                resourcesApkProvider.getItemColorFromStub(
                    ResourceConstants.COLOR_BUNDLE_MAIN_COLOR_PREFIX,
                    "preset_2",
                )
            )
            .thenReturn(Color.GREEN)

        colorProvider.fetch(listener, reload = true, homeWallpaperColors = null)

        val presetOptions =
            optionsResult.await()?.filter { (it as ColorOptionImpl).type == ColorType.PRESET_COLOR }
        assertThat(presetOptions).isNotNull()
        assertThat(presetOptions!!).hasSize(2)
        assertThat(presetOptions[0].title).isEqualTo("Preset 1")
        assertThat(presetOptions[1].title).isEqualTo("Preset 2")
        assertThat(presetOptions[0].index).isEqualTo(1)
        assertThat(presetOptions[1].index).isEqualTo(2)
    }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_withMonochromePreset_insertsMonochromeWallpaperOption() = runTest {
        val wallpaperColors = WallpaperColors(Color.valueOf(Color.RED), null, null)
        val bundleNames = arrayOf("monochrome")
        `when`(resourcesApkProvider.isAvailable).thenReturn(true)
        `when`(resourcesApkProvider.getItemsFromStub(ResourceConstants.COLOR_BUNDLES_ARRAY_NAME))
            .thenReturn(bundleNames)
        `when`(
                resourcesApkProvider.getItemStringFromStub(
                    ResourceConstants.COLOR_BUNDLE_NAME_PREFIX,
                    "monochrome",
                )
            )
            .thenReturn("Monochrome")
        `when`(
                resourcesApkProvider.getItemColorFromStub(
                    ResourceConstants.COLOR_BUNDLE_MAIN_COLOR_PREFIX,
                    "monochrome",
                )
            )
            .thenReturn(Color.BLACK)
        `when`(
                resourcesApkProvider.getItemStringFromStub(
                    ResourceConstants.COLOR_BUNDLE_STYLE_PREFIX,
                    "monochrome",
                )
            )
            .thenReturn("MONOCHROMATIC")

        colorProvider.fetch(listener, reload = true, homeWallpaperColors = wallpaperColors)

        val wallpaperOptions =
            optionsResult.await()?.filter {
                (it as ColorOptionImpl).type == ColorType.WALLPAPER_COLOR
            }
        assertThat(wallpaperOptions).isNotNull()
        assertThat(wallpaperOptions!!).hasSize(5) // 4 wallpaper + 1 monochrome
        assertThat(wallpaperOptions[1].title).isEqualTo("Monochrome")
    }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun fetch_withMonochromePreset_doesNotInsertMonochromeWallpaperOption() = runTest {
        val wallpaperColors = WallpaperColors(Color.valueOf(Color.RED), null, null)
        val bundleNames = arrayOf("monochrome")
        `when`(resourcesApkProvider.isAvailable).thenReturn(true)
        `when`(resourcesApkProvider.getItemsFromStub(ResourceConstants.COLOR_BUNDLES_ARRAY_NAME))
            .thenReturn(bundleNames)
        `when`(
                resourcesApkProvider.getItemStringFromStub(
                    ResourceConstants.COLOR_BUNDLE_NAME_PREFIX,
                    "monochrome",
                )
            )
            .thenReturn("Monochrome")
        `when`(
                resourcesApkProvider.getItemColorFromStub(
                    ResourceConstants.COLOR_BUNDLE_MAIN_COLOR_PREFIX,
                    "monochrome",
                )
            )
            .thenReturn(Color.BLACK)
        `when`(
                resourcesApkProvider.getItemStringFromStub(
                    ResourceConstants.COLOR_BUNDLE_STYLE_PREFIX,
                    "monochrome",
                )
            )
            .thenReturn("MONOCHROMATIC")

        colorProvider.fetch(listener, reload = true, homeWallpaperColors = wallpaperColors)

        val wallpaperOptions =
            optionsResult.await()?.filter {
                (it as ColorOptionImpl).type == ColorType.WALLPAPER_COLOR
            }
        assertThat(wallpaperOptions).isNotNull()
        assertThat(wallpaperOptions!!).hasSize(1)
    }
}
