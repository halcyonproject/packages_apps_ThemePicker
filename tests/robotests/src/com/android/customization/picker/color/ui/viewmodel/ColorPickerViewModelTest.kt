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

package com.android.customization.picker.color.ui.viewmodel

import android.content.Context
import android.content.theming.ThemeStyle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.stats.style.StyleEnums
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorProviderUtil
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.mode.data.repository.DarkModeStateRepository
import com.android.wallpaper.Flags.FLAG_COLOR_PICKER_UPDATE_FLAG
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.internal.lifecycle.RetainedLifecycleImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorPickerViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val logger = TestThemesUserEventLogger()
    private lateinit var underTest: ColorPickerViewModel
    private lateinit var colorUpdateViewModel: ColorUpdateViewModel

    private lateinit var context: Context
    private lateinit var testScope: TestScope

    @Inject lateinit var repository: FakeColorPickerRepository
    @Inject lateinit var interactor: ColorPickerInteractor
    @Inject lateinit var darkModeStateRepository: DarkModeStateRepository

    @Before
    fun setUp() {
        hiltRule.inject()

        context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)

        colorUpdateViewModel =
            ColorUpdateViewModel(context, RetainedLifecycleImpl(), darkModeStateRepository)

        underTest =
            ColorPickerViewModel(
                context = context,
                interactor = interactor,
                logger = logger,
                colorUpdateViewModel = colorUpdateViewModel,
                viewModelScope = testScope.backgroundScope,
                baseFlags = BaseFlags.get(context),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_suspendsUntilColorUpdate() =
        testScope.runTest {
            repository.setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(0)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions(), 1)
            // Apply the selected color option
            val job = testScope.launch { onApply()?.invoke() }

            assertThat(job.isActive).isTrue()

            colorUpdateViewModel.updateColors()

            assertThat(job.isActive).isFalse()
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_isNullWhenApplyingSameColorOption_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val colorOptions = collectLastValue(underTest.colorSeedOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select a color option that is already applied
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)?.onClick?.invoke()
            // Apply the selection
            assertThat(onApply()).isNull()
        }

    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_isNotNullWhenApplyingStyle_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val colorOptions = collectLastValue(underTest.colorSeedOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select a color option that is already applied
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)?.onClick?.invoke()
            // Select a style that is new
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)
            // Apply the selection
            assertThat(onApply()).isNotNull()
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_suspendsUntilColorUpdate_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val colorOptions = collectLastValue(underTest.colorSeedOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select a color option to preview
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(1)?.onClick?.invoke()
            // Apply the selection
            val job = testScope.launch { onApply()?.invoke() }

            assertThat(job.isActive).isTrue()

            colorUpdateViewModel.updateColors()

            assertThat(job.isActive).isFalse()
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_suspendsUntilTimeout() =
        testScope.runTest {
            repository.setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(0)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions(), 1)
            // Apply the selected color option
            val job = testScope.launch { onApply()?.invoke() }

            assertThat(job.isActive).isTrue()

            advanceTimeBy(ColorPickerViewModel.COLOR_UPDATE_TIMEOUT_MILLIS)
            runCurrent()

            assertThat(job.isActive).isFalse()
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_suspendsUntilTimeout_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val colorOptions = collectLastValue(underTest.colorSeedOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Select a color option to preview
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(1)?.onClick?.invoke()
            // Apply the selection
            val job = testScope.launch { onApply()?.invoke() }

            assertThat(job.isActive).isTrue()

            advanceTimeBy(ColorPickerViewModel.COLOR_UPDATE_TIMEOUT_MILLIS)
            runCurrent()

            assertThat(job.isActive).isFalse()
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_wallpaperColor_shouldLogColor() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_HOME,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.PRESET_COLOR,
                0,
            )

            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(0)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions(), 0)
            // Apply the selected color option
            applySelection()

            assertThat(logger.themeColorSource)
                .isEqualTo(StyleEnums.COLOR_SOURCE_HOME_SCREEN_WALLPAPER)
            assertThat(logger.themeColorStyle)
                .isEqualTo(ThemeStyle.toString(ThemeStyle.EXPRESSIVE).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(121212)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_wallpaperColor_shouldLogSelection_colorPickerUpdate() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_HOME,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.PRESET_COLOR,
                0,
            )
            val colorOptions = collectLastValue(underTest.colorSeedOptions)

            // Select a color option to preview
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)?.onClick?.invoke()
            // Apply the selection
            applySelection()

            assertThat(logger.themeColorSource)
                .isEqualTo(StyleEnums.COLOR_SOURCE_HOME_SCREEN_WALLPAPER)
            assertThat(logger.themeColorStyle)
                .isEqualTo(ThemeStyle.toString(ThemeStyle.EXPRESSIVE).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(121212)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_presetColor_shouldLogColor() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_LOCK,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.WALLPAPER_COLOR,
                0,
            )

            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            // Select "Wallpaper colors" tab
            colorTypes()?.get(1)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions(), 0)
            // Apply the selected color option
            applySelection()

            assertThat(logger.themeColorSource).isEqualTo(StyleEnums.COLOR_SOURCE_PRESET_COLOR)
            assertThat(logger.themeColorStyle)
                .isEqualTo(ThemeStyle.toString(ThemeStyle.FRUIT_SALAD).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(-54321)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_presetColor_shouldLogSelection_colorPickerUpdate() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_HOME,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.WALLPAPER_COLOR,
                0,
            )
            val colorOptions = collectLastValue(underTest.colorSeedOptions)

            // Select a color option to preview
            colorOptions()?.get(ColorType.PRESET_COLOR)?.get(0)?.onClick?.invoke()
            // Apply the selection
            applySelection()

            assertThat(logger.themeColorSource).isEqualTo(StyleEnums.COLOR_SOURCE_PRESET_COLOR)
            assertThat(logger.themeColorStyle)
                .isEqualTo(ThemeStyle.toString(ThemeStyle.FRUIT_SALAD).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(-54321)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_style_shouldLogSelection_colorPickerUpdate() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_HOME,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.WALLPAPER_COLOR,
                0,
            )
            val colorOptions = collectLastValue(underTest.colorSeedOptions)

            // Select a color option that is already applied
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)?.onClick?.invoke()
            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)
            // Apply the selection
            applySelection()

            assertThat(logger.themeColorSource)
                .isEqualTo(StyleEnums.COLOR_SOURCE_HOME_SCREEN_WALLPAPER)
            assertThat(logger.themeColorStyle)
                .isEqualTo(ThemeStyle.toString(ThemeStyle.VIBRANT).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(121212)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_seedAndStyle_shouldLogSelection_colorPickerUpdate() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_HOME,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.WALLPAPER_COLOR,
                0,
            )
            val colorOptions = collectLastValue(underTest.colorSeedOptions)

            // Select a color option to preview
            colorOptions()?.get(ColorType.PRESET_COLOR)?.get(0)?.onClick?.invoke()
            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)
            // Apply the selection
            applySelection()

            assertThat(logger.themeColorSource).isEqualTo(StyleEnums.COLOR_SOURCE_PRESET_COLOR)
            assertThat(logger.themeColorStyle)
                .isEqualTo(ThemeStyle.toString(ThemeStyle.VIBRANT).hashCode())
            assertThat(logger.themeSeedColor).isEqualTo(-54321)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_failure_shouldNotLogColor() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_LOCK,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.PRESET_COLOR,
                0,
            )

            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            repository.applySuccess = false
            // Select "Wallpaper colors" tab
            colorTypes()?.get(0)?.onClick?.invoke()
            // Select a color option to preview
            selectColorOption(colorOptions(), 0)
            // Apply the selected color option
            applySelection()

            assertThat(logger.themeColorSource).isEqualTo(StyleEnums.COLOR_SOURCE_UNSPECIFIED)
            assertThat(logger.themeColorStyle).isEqualTo(-1)
            assertThat(logger.themeSeedColor).isEqualTo(-1)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun onApply_failure_shouldNotLogSelection_colorPickerUpdate() =
        testScope.runTest {
            repository.setOptions(
                listOf(
                    repository.buildWallpaperOption(
                        ColorProviderUtil.COLOR_SOURCE_HOME,
                        ThemeStyle.EXPRESSIVE,
                        121212,
                    )
                ),
                listOf(repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, -54321)),
                ColorType.PRESET_COLOR,
                0,
            )
            val colorOptions = collectLastValue(underTest.colorSeedOptions)

            repository.applySuccess = false
            // Select a color option to preview
            colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)?.onClick?.invoke()
            // Apply the selected color option
            applySelection()

            assertThat(logger.themeColorSource).isEqualTo(StyleEnums.COLOR_SOURCE_UNSPECIFIED)
            assertThat(logger.themeColorStyle).isEqualTo(-1)
            assertThat(logger.themeSeedColor).isEqualTo(-1)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun selectColorOption() =
        testScope.runTest {
            repository.setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
            val colorTypes = collectLastValue(underTest.colorTypeTabs)
            val colorOptions = collectLastValue(underTest.colorOptions)

            // Initially, the wallpaper color tab should be selected
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Wallpaper colors",
                selectedColorOptionIndex = 0,
            )

            // Select "Basic colors" tab
            colorTypes()?.get(1)?.onClick?.invoke()
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Basic colors",
                selectedColorOptionIndex = -1,
            )

            // Select a color option
            selectColorOption(colorOptions(), 2)

            // Check original option is no longer selected
            colorTypes()?.get(0)?.onClick?.invoke()
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Wallpaper colors",
                selectedColorOptionIndex = -1,
            )

            // Check new option is selected
            colorTypes()?.get(1)?.onClick?.invoke()
            assertPickerUiState(
                colorTypes = colorTypes(),
                colorOptions = colorOptions(),
                selectedColorTypeText = "Basic colors",
                selectedColorOptionIndex = 2,
            )
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun selectSeedAndStyle_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val colorOptions = collectLastValue(underTest.colorSeedOptions)
            val previewingStyle = collectLastValue(underTest.previewingStyle)
            val previewingColorOptionKey = collectLastValue(underTest.previewingColorOptionKey)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.TONAL_SPOT)
            val getInitialOption: () -> ColorOptionViewModel? = {
                colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)
            }
            assertThat(previewingColorOptionKey()).isEqualTo(getInitialOption()?.key)

            // Select a new color option
            val getNewOption: () -> ColorOptionViewModel? = {
                colorOptions()?.get(ColorType.PRESET_COLOR)?.get(0)
            }
            getNewOption()?.onClick?.invoke()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.EXPRESSIVE)
            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)

            assertThat(previewingColorOptionKey()).isEqualTo(getNewOption()?.key)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun confirmAndCancelStyleOptionSelection_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val previewingStyle = collectLastValue(underTest.previewingStyle)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.TONAL_SPOT)

            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Confirm style selection
            underTest.confirmStyleOptionSelection()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.SPRITZ)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.SPRITZ)

            // Cancel style selection
            underTest.cancelStyleOptionSelection()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Reset style selection
            underTest.resetStyleOptionSelection()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.TONAL_SPOT)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun applyAndResetStyleOptionSelection_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val previewingStyle = collectLastValue(underTest.previewingStyle)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.TONAL_SPOT)

            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Confirm style selection
            underTest.confirmStyleOptionSelection()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Apply style selection
            applySelection()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.SPRITZ)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.SPRITZ)

            // Reset style selection
            underTest.resetStyleOptionSelection()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)
        }

    @Test
    @EnableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun resetPreview_colorPickerUpdate() =
        testScope.runTest {
            setupColorPickerUpdateTest()
            val colorOptions = collectLastValue(underTest.colorSeedOptions)
            val previewingStyle = collectLastValue(underTest.previewingStyle)
            val previewingColorOptionKey = collectLastValue(underTest.previewingColorOptionKey)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.TONAL_SPOT)
            val getInitialOption: () -> ColorOptionViewModel? = {
                colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(0)
            }
            assertThat(previewingColorOptionKey()).isEqualTo(getInitialOption()?.key)

            // Select a new color option
            val getNewOption: () -> ColorOptionViewModel? = {
                colorOptions()?.get(ColorType.PRESET_COLOR)?.get(0)
            }
            getNewOption()?.onClick?.invoke()
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.EXPRESSIVE)
            // Select a new style
            underTest.onStyleOptionClick(ThemeStyle.VIBRANT)
            // Confirm style selection
            underTest.confirmStyleOptionSelection()

            assertThat(previewingColorOptionKey()).isEqualTo(getNewOption()?.key)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.VIBRANT)

            // Reset preview
            underTest.resetPreview()

            assertThat(previewingColorOptionKey()).isEqualTo(getInitialOption()?.key)
            assertThat(previewingStyle()).isEqualTo(ThemeStyle.TONAL_SPOT)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun previewingAndOverriding_initialState() =
        testScope.runTest {
            val wallpaperOptions = setupPreviewingTest()

            val overridingColorOption = collectLastValue(underTest.overridingColorOption)
            val previewingColorOption = collectLastValue(underTest.previewingColorOption)
            val overridingColorOptionIndex = collectLastValue(underTest.overridingColorOptionIndex)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Assert Initial state: no override, previewing the selected option.
            assertThat(overridingColorOption()).isNull()
            assertThat(previewingColorOption()?.isEquivalent(wallpaperOptions[0])).isTrue()
            assertThat(overridingColorOptionIndex()).isEqualTo(0) // Default value.
            assertThat(onApply()).isNull()
            assertColorOptionUiState(colorOptions(), selectedColorOptionIndex = 0)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun previewingAndOverriding_previews() =
        testScope.runTest {
            val wallpaperOptions = setupPreviewingTest()

            val overridingColorOption = collectLastValue(underTest.overridingColorOption)
            val previewingColorOption = collectLastValue(underTest.previewingColorOption)
            val overridingColorOptionIndex = collectLastValue(underTest.overridingColorOptionIndex)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Act: User clicks a different option (index 1) to preview it.
            selectColorOption(colorOptions(), 1)
            runCurrent()

            // Assert state after previewing: overriding option is set.
            assertThat(overridingColorOption()?.isEquivalent(wallpaperOptions[1])).isTrue()
            assertThat(previewingColorOption()?.isEquivalent(wallpaperOptions[1])).isTrue()
            assertThat(overridingColorOptionIndex()).isEqualTo(1)
            assertThat(onApply()).isNotNull()
            assertColorOptionUiState(colorOptions(), selectedColorOptionIndex = 1)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun previewingAndOverriding_resets() =
        testScope.runTest {
            val wallpaperOptions = setupPreviewingTest()

            val overridingColorOption = collectLastValue(underTest.overridingColorOption)
            val previewingColorOption = collectLastValue(underTest.previewingColorOption)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Preview an option.
            selectColorOption(colorOptions(), 1)
            runCurrent()

            // Act: User resets the preview without applying.
            underTest.resetPreview()
            runCurrent()

            // Assert state after reset: back to initial state, previewing the selected option.
            assertThat(overridingColorOption()).isNull()
            assertThat(previewingColorOption()?.isEquivalent(wallpaperOptions[0])).isTrue()
            assertThat(onApply()).isNull()
            assertColorOptionUiState(colorOptions(), selectedColorOptionIndex = 0)
        }

    @Test
    @DisableFlags(FLAG_COLOR_PICKER_UPDATE_FLAG)
    fun previewingAndOverriding_applies() =
        testScope.runTest {
            val wallpaperOptions = setupPreviewingTest()

            val overridingColorOption = collectLastValue(underTest.overridingColorOption)
            val previewingColorOption = collectLastValue(underTest.previewingColorOption)
            val colorOptions = collectLastValue(underTest.colorOptions)
            val onApply = collectLastValue(underTest.onApply)

            // Preview an option.
            selectColorOption(colorOptions(), 1)
            runCurrent()

            // Act: apply the previewed option.
            applySelection()
            runCurrent()

            // Assert state after apply: the new option is now the "selected" one.
            // The overriding option is still set, but it's equivalent to the new selected one.
            assertThat(overridingColorOption()?.isEquivalent(wallpaperOptions[1])).isTrue()
            assertThat(previewingColorOption()?.isEquivalent(wallpaperOptions[1])).isTrue()
            // onApply should now be null because the overriding and selected options are the same.
            assertThat(onApply()).isNull()
            // The UI should still show option 1 as selected.
            assertColorOptionUiState(colorOptions(), selectedColorOptionIndex = 1)
        }

    /** Simulates a user selecting the color option at the given index. */
    private fun TestScope.selectColorOption(
        colorOptions: List<OptionItemViewModel2<ColorOptionIconViewModel>>?,
        index: Int,
    ) {
        val onClickedFlow = colorOptions?.get(index)?.onClicked
        val onClickedLastValueOrNull: (() -> (() -> Unit)?)? =
            onClickedFlow?.let { collectLastValue(it) }
        onClickedLastValueOrNull?.let { onClickedLastValue ->
            val onClickedOrNull: (() -> Unit)? = onClickedLastValue()
            onClickedOrNull?.invoke()
        }
    }

    private fun setupPreviewingTest(): List<ColorOption> {
        // Arrange: set up distinct options for clarity.
        val wallpaperOptions =
            listOf(
                repository.buildWallpaperOption(
                    ColorProviderUtil.COLOR_SOURCE_HOME,
                    ThemeStyle.TONAL_SPOT,
                    1,
                ),
                repository.buildWallpaperOption(
                    ColorProviderUtil.COLOR_SOURCE_HOME,
                    ThemeStyle.VIBRANT,
                    2,
                ),
            )
        val presetOptions =
            listOf(
                repository.buildPresetOption(ThemeStyle.TONAL_SPOT, 1),
                repository.buildPresetOption(ThemeStyle.FRUIT_SALAD, 3),
            )
        // Initially select the first wallpaper option (index 0).
        repository.setOptions(wallpaperOptions, presetOptions, ColorType.WALLPAPER_COLOR, 0)
        return wallpaperOptions
    }

    private fun setupColorPickerUpdateTest(): Map<ColorType, List<ColorOption>> {
        // Arrange: set up distinct options for clarity.
        val wallpaperOptions =
            listOf(
                repository.buildWallpaperOption(
                    ColorProviderUtil.COLOR_SOURCE_HOME,
                    ThemeStyle.TONAL_SPOT,
                    1,
                ),
                repository.buildWallpaperOption(
                    ColorProviderUtil.COLOR_SOURCE_HOME,
                    ThemeStyle.VIBRANT,
                    2,
                ),
            )
        val presetOptions = listOf(repository.buildPresetOption(ThemeStyle.EXPRESSIVE, 3))
        // Initially select the first wallpaper option (index 0).
        repository.setOptions(wallpaperOptions, presetOptions, ColorType.WALLPAPER_COLOR, 0)
        return mapOf(
            ColorType.WALLPAPER_COLOR to wallpaperOptions,
            ColorType.PRESET_COLOR to presetOptions,
        )
    }

    /** Simulates a user tapping the apply button, and the apply completes. */
    private suspend fun TestScope.applySelection() {
        val onApply = collectLastValue(underTest.onApply)()
        testScope.launch { onApply?.invoke() }
        colorUpdateViewModel.updateColors()
    }

    /**
     * Asserts the entire picker UI state is what is expected. This includes the color type tabs and
     * the color options list.
     *
     * @param colorTypes The observed color type view-models, keyed by ColorType
     * @param colorOptions The observed color options
     * @param selectedColorTypeText The text of the color type that's expected to be selected
     * @param selectedColorOptionIndex The index of the color option that's expected to be selected,
     *   -1 stands for no color option should be selected
     */
    private fun TestScope.assertPickerUiState(
        colorTypes: List<FloatingToolbarTabViewModel>?,
        colorOptions: List<OptionItemViewModel2<ColorOptionIconViewModel>>?,
        selectedColorTypeText: String,
        selectedColorOptionIndex: Int,
    ) {
        assertColorTypeTabUiState(
            colorTypes = colorTypes,
            colorTypeId = ColorType.WALLPAPER_COLOR,
            isSelected = "Wallpaper colors" == selectedColorTypeText,
        )
        assertColorTypeTabUiState(
            colorTypes = colorTypes,
            colorTypeId = ColorType.PRESET_COLOR,
            isSelected = "Basic colors" == selectedColorTypeText,
        )
        assertColorOptionUiState(colorOptions, selectedColorOptionIndex)
    }

    /**
     * Asserts the picker section UI state is what is expected.
     *
     * @param colorOptions The observed color options
     * @param selectedColorOptionIndex The index of the color option that's expected to be selected,
     *   -1 stands for no color option should be selected
     */
    private fun TestScope.assertColorOptionUiState(
        colorOptions: List<OptionItemViewModel2<ColorOptionIconViewModel>>?,
        selectedColorOptionIndex: Int,
    ) {
        var foundSelectedColorOption = false
        assertThat(colorOptions).isNotNull()
        if (colorOptions != null) {
            for (i in colorOptions.indices) {
                val colorOptionHasSelectedIndex = i == selectedColorOptionIndex
                val isSelected: Boolean? = collectLastValue(colorOptions[i].isSelected).invoke()
                assertWithMessage(
                        "Expected color option with index \"${i}\" to have" +
                            " isSelected=$colorOptionHasSelectedIndex but it was" +
                            " ${isSelected}, num options: ${colorOptions.size}"
                    )
                    .that(isSelected)
                    .isEqualTo(colorOptionHasSelectedIndex)
                foundSelectedColorOption = foundSelectedColorOption || colorOptionHasSelectedIndex
            }
            if (selectedColorOptionIndex == -1) {
                assertWithMessage(
                        "Expected no color options to be selected, but a color option is" +
                            " selected"
                    )
                    .that(foundSelectedColorOption)
                    .isFalse()
            } else {
                assertWithMessage(
                        "Expected a color option to be selected, but no color option is" +
                            " selected"
                    )
                    .that(foundSelectedColorOption)
                    .isTrue()
            }
        }
    }

    /**
     * Asserts that a color type tab has the correct UI state.
     *
     * @param colorTypes The observed color type view-models, keyed by ColorType enum
     * @param colorTypeId the ID of the color type to assert
     * @param isSelected Whether that color type should be selected
     */
    private fun assertColorTypeTabUiState(
        colorTypes: List<FloatingToolbarTabViewModel>?,
        colorTypeId: ColorType,
        isSelected: Boolean,
    ) {
        val position = if (colorTypeId == ColorType.WALLPAPER_COLOR) 0 else 1
        val viewModel =
            colorTypes?.get(position) ?: error("No color type with ID \"$colorTypeId\"!")
        assertThat(viewModel.isSelected).isEqualTo(isSelected)
    }
}
