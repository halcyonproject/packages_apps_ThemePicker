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
import android.util.Log
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorType
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Models UI state for a color picker experience. */
class ColorPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    private val colorUpdateViewModel: ColorUpdateViewModel,
    private val interactor: ColorPickerInteractor,
    private val logger: ThemesUserEventLogger,
    private val baseFlags: BaseFlags,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    val selectedColorOption = interactor.selectedColorOption

    private val _overridingColorOption = MutableStateFlow<ColorOption?>(null)
    val overridingColorOption = _overridingColorOption.asStateFlow()
    val previewingColorOption =
        combine(overridingColorOption, selectedColorOption) { overriding, selected ->
            overriding ?: selected
        }

    // Used in seed & variant picker
    val previewingColorOptionKey = previewingColorOption.map { it?.getKey() }

    /**
     * Returns a key to uniquely identify the color option for equivalence. This function only
     * compares source and seed color, and is meant to be used in the color seed & variant picker.
     */
    private fun ColorOption.getKey(): String {
        return "${this.source}::${this.seedColor}"
    }

    val styleOptions = interactor.styleList
    val selectedStyle = interactor.selectedStyle
    private val _overridingStyle = MutableStateFlow<Int?>(null)
    // Style selected but not yet confirmed by user, this is reset to null if the user cancels
    private val _tempOverridingStyle = MutableStateFlow<Int?>(null)
    val overridingStyle =
        combine(_overridingStyle, _tempOverridingStyle) { saved, temp -> temp ?: saved }
    val previewingStyle =
        combine(selectedStyle, overridingStyle, selectedColorOption, overridingColorOption) {
            selected,
            overriding,
            selectedOption,
            overridingOption ->
            getPreviewingStyle(selected, overriding, selectedOption, overridingOption)
        }

    @ThemeStyle.Type
    fun getPreviewingStyle(
        @ThemeStyle.Type selectedStyle: Int?,
        @ThemeStyle.Type overridingStyle: Int?,
        selectedColorOption: ColorOption?,
        overridingColorOption: ColorOption?,
    ): Int? {
        // User selection takes precedence
        return overridingStyle
            // Otherwise, if the previewing color option matches the current system color option,
            // use the current system style
            ?: if (
                overridingColorOption == null ||
                    overridingColorOption.isEquivalent(selectedColorOption)
            ) {
                selectedStyle
            } else {
                // Otherwise, use the color option's default style
                overridingColorOption.style
            }
    }

    fun onStyleOptionClick(@ThemeStyle.Type style: Int) {
        _tempOverridingStyle.value = style
    }

    fun confirmStyleOptionSelection() {
        _tempOverridingStyle.value?.let { _overridingStyle.value = it }
        _tempOverridingStyle.value = null
    }

    fun cancelStyleOptionSelection() {
        _tempOverridingStyle.value = null
    }

    fun resetStyleOptionSelection() {
        _overridingStyle.value = null
        _tempOverridingStyle.value = null
    }

    val colorSeedOptions =
        interactor.colorOptions.map { colorOptions ->
            colorOptions
                .map { colorOptionEntry ->
                    colorOptionEntry.key to
                        colorOptionEntry.value.mapIndexed { index, colorOption ->
                            colorOption as ColorOptionImpl
                            ColorOptionViewModel(
                                icon = ColorOptionIconViewModel.fromColorOption(colorOption),
                                key = colorOption.getKey(),
                                onClick = {
                                    viewModelScope.launch {
                                        _overridingColorOption.value = colorOption
                                        _overridingColorOptionIndex.value = index
                                        // Reset overriding style when a new option
                                        // is selected.
                                        resetStyleOptionSelection()
                                    }
                                },
                                text =
                                    Text.Loaded(
                                        colorOption.getContentDescription(context).toString()
                                    ),
                            )
                        }
                }
                .toMap()
        }

    val _overridingColorOptionIndex = MutableStateFlow<Int>(0)
    val overridingColorOptionIndex = _overridingColorOptionIndex.asStateFlow()

    private val selectedColorTypeTabId = MutableStateFlow<ColorType?>(null)

    /** View-models for each color tab. */
    val colorTypeTabs: Flow<List<FloatingToolbarTabViewModel>> =
        combine(interactor.colorOptions, selectedColorTypeTabId) {
            colorOptions,
            selectedColorTypeIdOrNull ->
            colorOptions.keys.mapIndexed { index, colorType ->
                val isSelected =
                    (selectedColorTypeIdOrNull == null && index == 0) ||
                        selectedColorTypeIdOrNull == colorType

                val name =
                    when (colorType) {
                        ColorType.WALLPAPER_COLOR ->
                            context.resources.getString(R.string.wallpaper_color_tab)
                        ColorType.PRESET_COLOR ->
                            context.resources.getString(R.string.preset_color_tab_2)
                    }

                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res =
                            when (colorType) {
                                ColorType.WALLPAPER_COLOR ->
                                    com.android.wallpaper.R.drawable.ic_baseline_wallpaper_24
                                ColorType.PRESET_COLOR -> R.drawable.ic_colors
                            },
                        contentDescription = Text.Loaded(name),
                    ),
                    name,
                    isSelected,
                ) {
                    if (!isSelected) {
                        this.selectedColorTypeTabId.value = colorType
                    }
                }
            }
        }

    /** View-models for each color tab subheader */
    val colorTypeTabSubheader: Flow<String> =
        selectedColorTypeTabId.map { selectedColorTypeIdOrNull ->
            when (selectedColorTypeIdOrNull ?: ColorType.WALLPAPER_COLOR) {
                ColorType.WALLPAPER_COLOR ->
                    context.resources.getString(R.string.wallpaper_color_subheader)
                ColorType.PRESET_COLOR ->
                    context.resources.getString(R.string.preset_color_subheader)
            }
        }

    /** The list of all color options mapped by their color type */
    val allColorOptions:
        Flow<Map<ColorType, List<OptionItemViewModel2<ColorOptionIconViewModel>>>> =
        interactor.colorOptions.map { colorOptions ->
            colorOptions
                .map { colorOptionEntry ->
                    colorOptionEntry.key to
                        colorOptionEntry.value.mapIndexed { index, colorOption ->
                            colorOption as ColorOptionImpl
                            val isSelectedFlow: StateFlow<Boolean> =
                                combine(overridingColorOption, selectedColorOption) {
                                        overriding,
                                        selected ->
                                        overriding?.isEquivalent(colorOption)
                                            ?: selected?.isEquivalent(colorOption)
                                            ?: false
                                    }
                                    .stateIn(viewModelScope)
                            val key =
                                "${colorOption.type}::${colorOption.style}::${colorOption.serializedPackages}"
                            OptionItemViewModel2<ColorOptionIconViewModel>(
                                key = MutableStateFlow(key) as StateFlow<String>,
                                payload = ColorOptionIconViewModel.fromColorOption(colorOption),
                                text =
                                    Text.Loaded(
                                        colorOption.getContentDescription(context).toString()
                                    ),
                                isTextUserVisible = false,
                                isSelected = isSelectedFlow,
                                onClicked =
                                    isSelectedFlow.map { isSelected ->
                                        if (isSelected) {
                                            null
                                        } else {
                                            {
                                                viewModelScope.launch {
                                                    _overridingColorOption.value = colorOption
                                                    _overridingColorOptionIndex.value = index
                                                }
                                            }
                                        }
                                    },
                            )
                        }
                }
                .toMap()
        }

    /**
     * This function suspends until onApplyComplete is called to accommodate for configuration
     * change updates, which are applied with a latency.
     */
    val onApply: Flow<(suspend () -> Unit)?> =
        if (baseFlags.isColorPickerUpdateEnabled()) {
            combine(overridingColorOption, overridingStyle, selectedColorOption, selectedStyle) {
                overriding,
                overridingStyle,
                selected,
                selectedStyle ->
                val optionNeedsUpdate = overriding != null && !overriding.isEquivalent(selected)
                val styleNeedsUpdate = overridingStyle != null && overridingStyle != selectedStyle
                if ((optionNeedsUpdate || styleNeedsUpdate) && selected != null) {
                    {
                        val colorOption = overriding ?: selected
                        val style =
                            getPreviewingStyle(selectedStyle, overridingStyle, selected, overriding)
                                ?: colorOption.style
                        applyAndWaitForColorUpdate(
                            apply = { interactor.apply(colorOption, style) },
                            onSuccess = {
                                logger.logThemeColorApplied(
                                    colorOption.sourceForLogging,
                                    // TODO(b/473022455): centralize logging in
                                    //  ThemesUserEventLogger
                                    ThemeStyle.toString(style).hashCode(),
                                    colorOption.seedColor,
                                )
                            },
                        )
                    }
                } else null
            }
        } else {
            combine(overridingColorOption, selectedColorOption) { previewing, selected ->
                previewing?.let {
                    if (previewing.isEquivalent(selected)) {
                        null
                    } else {
                        {
                            applyAndWaitForColorUpdate(
                                apply = { interactor.apply(it) },
                                onSuccess = {
                                    logger.logThemeColorApplied(
                                        it.sourceForLogging,
                                        it.styleForLogging,
                                        it.seedColor,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

    private suspend fun applyAndWaitForColorUpdate(
        apply: suspend () -> Boolean,
        onSuccess: () -> Unit,
    ) {
        return coroutineScope {
            val waitForColorUpdateJob = launch {
                // Suspend until first color update, or time out after 3 seconds
                try {
                    withTimeout(COLOR_UPDATE_TIMEOUT_MILLIS) {
                        colorUpdateViewModel.systemColorsUpdatedNoReplay.take(1).collect {
                            return@collect
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timed out waiting for color update", e)
                }
            }
            val success = apply()
            if (success) {
                onSuccess()
                waitForColorUpdateJob.join()
            } else {
                waitForColorUpdateJob.cancel()
            }
        }
    }

    fun resetPreview() {
        _overridingColorOption.value = null
        _overridingStyle.value = null
        _tempOverridingStyle.value = null
    }

    /** The list of all available color options for the selected Color Type. */
    val colorOptions: Flow<List<OptionItemViewModel2<ColorOptionIconViewModel>>> =
        combine(allColorOptions, selectedColorTypeTabId) {
            allColorOptions: Map<ColorType, List<OptionItemViewModel2<ColorOptionIconViewModel>>>,
            selectedColorTypeIdOrNull ->
            val selectedColorTypeId = selectedColorTypeIdOrNull ?: ColorType.WALLPAPER_COLOR
            allColorOptions[selectedColorTypeId] ?: emptyList()
        }

    enum class Screen {
        LANDING,
        VARIANT_PICKER,
    }

    private val _currentScreen = MutableStateFlow(Screen.LANDING)
    val currentScreen = _currentScreen.asStateFlow()

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): ColorPickerViewModel
    }

    companion object {
        const val TAG = "ColorPickerViewModel"
        const val COLOR_UPDATE_TIMEOUT_MILLIS = 3000L
    }
}
