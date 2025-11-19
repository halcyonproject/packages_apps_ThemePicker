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

import android.app.WallpaperColors
import android.content.Context
import android.content.res.Resources
import android.content.theming.ThemeStyle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.customization.model.CustomizationManager.OptionsFetchedListener
import com.android.customization.model.ResourceConstants.COLOR_BUNDLES_ARRAY_NAME
import com.android.customization.model.ResourceConstants.COLOR_BUNDLE_MAIN_COLOR_PREFIX
import com.android.customization.model.ResourceConstants.COLOR_BUNDLE_NAME_PREFIX
import com.android.customization.model.ResourceConstants.COLOR_BUNDLE_STYLE_PREFIX
import com.android.customization.model.ResourcesApkProvider
import com.android.customization.picker.color.shared.model.ColorType
import com.android.systemui.monet.ColorScheme
import com.android.wallpaper.config.BaseFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creates dynamic and preset color options. Reads preset colors from a stub APK. TODO
 * (b/311212666): Make [ColorProvider] and [ColorCustomizationManager] injectable
 */
open class ColorProvider(private val context: Context, stubPackageName: String) {

    internal open val resourcesApkProvider = ResourcesApkProvider(context, stubPackageName)

    private var loaderJob: Job? = null
    internal open val monetEnabled = ColorUtils.isMonetEnabled(context)
    // TODO(b/202145216): Use style method to fetch the list of style.
    @ThemeStyle.Type
    private var styleList =
        arrayOf(ThemeStyle.TONAL_SPOT, ThemeStyle.SPRITZ, ThemeStyle.VIBRANT, ThemeStyle.EXPRESSIVE)

    private var monochromeBundleName: String? = null

    private val scope =
        if (context is LifecycleOwner) {
            context.lifecycleScope
        } else {
            CoroutineScope(Dispatchers.Default + SupervisorJob())
        }

    private val isColorPickerUpdateEnabled = BaseFlags.get(context).isColorPickerUpdateEnabled()

    private var colorsAvailable = true
    private var presetColorBundles: List<ColorOption>? = null
    private var wallpaperColorBundles: List<ColorOption>? = null
    private var homeWallpaperColors: WallpaperColors? = null

    fun isAvailable(): Boolean {
        return monetEnabled && resourcesApkProvider.isAvailable && colorsAvailable
    }

    fun fetch(
        callback: OptionsFetchedListener<ColorOption>?,
        reload: Boolean,
        homeWallpaperColors: WallpaperColors?,
    ) {
        val wallpaperColorsChanged = this.homeWallpaperColors != homeWallpaperColors
        if (wallpaperColorsChanged || reload) {
            loadWallpaperOptions(homeWallpaperColors)
            this.homeWallpaperColors = homeWallpaperColors
        }

        scope.launch {
            // Wait for the previous preset color loading job to finish before evaluating whether to
            // start a new one
            loaderJob?.join()
            if (presetColorBundles == null || reload) {
                loaderJob = launch {
                    try {
                        loadPresetOptions()
                        callback?.onOptionsLoaded(buildFinalList())
                    } catch (e: Throwable) {
                        colorsAvailable = false
                        callback?.onError(e)
                    }
                }
            } else {
                callback?.onOptionsLoaded(buildFinalList())
            }
        }
    }

    private fun loadWallpaperOptions(homeWallpaperColors: WallpaperColors?) {
        if (homeWallpaperColors == null) return

        wallpaperColorBundles =
            if (isColorPickerUpdateEnabled) {
                buildColorSeedOptions(wallpaperColors = homeWallpaperColors)
            } else {
                buildColorAndStyleOptions(wallpaperColors = homeWallpaperColors)
            }
    }

    private fun buildColorAndStyleOptions(
        wallpaperColors: WallpaperColors
    ): MutableList<ColorOption> {
        val bundles: MutableList<ColorOption> = ArrayList()
        val seedColors = ColorScheme.getSeedColors(wallpaperColors)
        for ((i, colorInt) in seedColors.take(MAX_SEED_COLORS).withIndex()) {
            // TODO(b/202145216): Measure time cost in the loop.
            for (style in styleList) {
                val colorOption =
                    ColorProviderUtil.buildBundle(
                        context = context,
                        colorInt = colorInt,
                        // Color option index value starts from 1.
                        index = i + 1,
                        style = style,
                        // The first seed color is the default.
                        isDefault = i == 0,
                        getLightColorPreview = ColorProviderUtil::getLightThreeColorPreview,
                        getDarkColorPreview = ColorProviderUtil::getDarkThreeColorPreview,
                    )
                bundles.add(colorOption)
            }
        }
        return bundles
    }

    private fun buildColorSeedOptions(wallpaperColors: WallpaperColors): MutableList<ColorOption> {
        val bundles: MutableList<ColorOption> = ArrayList()
        val seedColors = ColorScheme.getSeedColors(wallpaperColors)
        for ((i, colorInt) in seedColors.take(MAX_SEED_COLORS).withIndex()) {
            // TODO (b/441279631): enable updating color titles
            val colorOption =
                ColorProviderUtil.buildBundle(
                    context = context,
                    colorInt = colorInt,
                    // Color option index value starts from 1.
                    index = i + 1,
                    style = ThemeStyle.TONAL_SPOT,
                    // The first seed color is the default.
                    isDefault = i == 0,
                    getLightColorPreview = ColorProviderUtil::getLightOneOrTwoColorPreview,
                    getDarkColorPreview = ColorProviderUtil::getDarkOneOrTwoColorPreview,
                )
            bundles.add(colorOption)
        }
        return bundles
    }

    private suspend fun loadPresetOptions() =
        withContext(Dispatchers.IO) {
            val bundles: MutableList<ColorOption> = ArrayList()

            val bundleNames =
                if (isAvailable()) resourcesApkProvider.getItemsFromStub(COLOR_BUNDLES_ARRAY_NAME)
                else emptyArray()

            // keep track of whether monochrome is included in preset colors to determine
            // inclusion in wallpaper colors
            var hasMonochrome = false
            for ((i, bundleName) in bundleNames.withIndex()) {
                val title =
                    resourcesApkProvider.getItemStringFromStub(COLOR_BUNDLE_NAME_PREFIX, bundleName)
                val color =
                    resourcesApkProvider.getItemColorFromStub(
                        COLOR_BUNDLE_MAIN_COLOR_PREFIX,
                        bundleName,
                    )
                val styleName =
                    try {
                        resourcesApkProvider.getItemStringFromStub(
                            COLOR_BUNDLE_STYLE_PREFIX,
                            bundleName,
                        )
                    } catch (e: Resources.NotFoundException) {
                        null
                    }
                @ThemeStyle.Type
                val style =
                    try {
                        if (styleName != null) ThemeStyle.valueOf(styleName)
                        else ThemeStyle.TONAL_SPOT
                    } catch (e: IllegalArgumentException) {
                        ThemeStyle.TONAL_SPOT
                    }

                if (style == ThemeStyle.MONOCHROMATIC) {
                    hasMonochrome = true
                    monochromeBundleName = bundleName
                }
                bundles.add(
                    ColorProviderUtil.buildPreset(
                        title = title,
                        color = color,
                        // Color option index value starts from 1.
                        index = i + 1,
                        style = style,
                    )
                )
            }
            if (!hasMonochrome) {
                monochromeBundleName = null
            }

            presetColorBundles = bundles
            loaderJob = null
        }

    private fun buildFinalList(): List<ColorOption> {
        val presetColors = presetColorBundles ?: emptyList()
        val wallpaperColors = wallpaperColorBundles?.toMutableList() ?: mutableListOf()
        if (!isColorPickerUpdateEnabled) {
            insertMonochrome(wallpaperColors)
        }
        return wallpaperColors + presetColors
    }

    private fun insertMonochrome(colorList: MutableList<ColorOption>) {
        // Insert monochrome in the second position if it is enabled and included in preset
        // colors
        monochromeBundleName?.let {
            val title = resourcesApkProvider.getItemStringFromStub(COLOR_BUNDLE_NAME_PREFIX, it)
            val color =
                resourcesApkProvider.getItemColorFromStub(COLOR_BUNDLE_MAIN_COLOR_PREFIX, it)
            if (colorList.isNotEmpty()) {
                colorList.add(
                    1,
                    ColorProviderUtil.buildPreset(
                        title = title,
                        color = color,
                        index = -1,
                        style = ThemeStyle.MONOCHROMATIC,
                        type = ColorType.WALLPAPER_COLOR,
                    ),
                )
            }
        }
    }

    companion object {
        private const val TAG = "ColorProvider"
        private const val MAX_SEED_COLORS = 4
    }
}
