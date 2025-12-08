/*
 * Copyright (C) 2024 The Android Open Source Project
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
 *
 */
package com.android.customization.picker.color.data.repository

import android.app.ThemeManager
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.theming.ThemeStyle
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.graphics.toColorInt
import com.android.customization.model.CustomizationManager
import com.android.customization.model.ResourceConstants
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.model.color.ColorProviderUtil
import com.android.customization.picker.color.shared.model.ColorType
import com.android.systemui.monet.ColorScheme
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class ColorPickerRepositoryImpl2
@Inject
constructor(
    @BackgroundDispatcher scope: CoroutineScope,
    @BackgroundDispatcher backgroundDispatcher: CoroutineDispatcher,
    private val colorManager: ColorCustomizationManager,
    private val themeManager: ThemeManager?,
    client: WallpaperClient,
    baseFlags: BaseFlags,
) : ColorPickerRepository2 {

    private val shouldUseThemeService =
        baseFlags.isColorPickerUpdateEnabled() && themeManager != null

    init {
        if (shouldUseThemeService) {
            Log.d(TAG, "Theme service is enabled")
        }
    }

    private val wallpaperColorsCallback: Flow<Pair<Screen, WallpaperColors?>> =
        callbackFlow {
                trySend(Pair(Screen.HOME_SCREEN, client.getWallpaperColors(Screen.HOME_SCREEN)))
                trySend(Pair(Screen.LOCK_SCREEN, client.getWallpaperColors(Screen.LOCK_SCREEN)))
                val listener = { colors: WallpaperColors?, which: Int ->
                    if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                        trySend(Pair(Screen.HOME_SCREEN, colors))
                    }
                    if (which and WallpaperManager.FLAG_LOCK != 0) {
                        trySend(Pair(Screen.LOCK_SCREEN, colors))
                    }
                }
                client.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
                awaitClose { client.removeOnColorsChangedListener(listener) }
            }
            .flowOn(backgroundDispatcher)
            // Make this a shared flow to make sure only one listener is added.
            .shareIn(scope = scope, started = SharingStarted.WhileSubscribed(), replay = 1)
    private val homeWallpaperColors: Flow<WallpaperColors?> =
        wallpaperColorsCallback
            .filter { (screen, _) -> screen == Screen.HOME_SCREEN }
            .map { (_, colors) -> colors }
    private val lockWallpaperColors: Flow<WallpaperColors?> =
        wallpaperColorsCallback
            .filter { (screen, _) -> screen == Screen.LOCK_SCREEN }
            .map { (_, colors) -> colors }

    override val colorOptions: Flow<Map<ColorType, List<ColorOption>>> =
        combine(homeWallpaperColors, lockWallpaperColors) { homeColors, lockColors ->
                suspendCancellableCoroutine { continuation ->
                    colorManager.setWallpaperColors(homeColors, lockColors)
                    colorManager.fetchOptions(
                        object : CustomizationManager.OptionsFetchedListener<ColorOption> {
                            override fun onOptionsLoaded(options: MutableList<ColorOption>?) {
                                val wallpaperColorOptions: MutableList<ColorOption> =
                                    mutableListOf()
                                val presetColorOptions: MutableList<ColorOption> = mutableListOf()
                                options?.forEach { option ->
                                    when ((option as ColorOptionImpl).type) {
                                        ColorType.WALLPAPER_COLOR ->
                                            wallpaperColorOptions.add(option)
                                        ColorType.PRESET_COLOR -> presetColorOptions.add(option)
                                    }
                                }
                                continuation.resumeWith(
                                    Result.success(
                                        mapOf(
                                            ColorType.WALLPAPER_COLOR to wallpaperColorOptions,
                                            ColorType.PRESET_COLOR to presetColorOptions,
                                        )
                                    )
                                )
                            }

                            override fun onError(throwable: Throwable?) {
                                Log.e(TAG, "Error loading theme bundles", throwable)
                                continuation.resumeWith(
                                    Result.failure(
                                        throwable ?: Throwable("Error loading theme bundles")
                                    )
                                )
                            }
                        },
                        /* reload= */ false,
                    )
                }
            }
            .shareIn(scope = scope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val settingsChanged =
        callbackFlow {
                trySend(Unit)
                colorManager.setListener { trySend(Unit) }
                awaitClose { colorManager.setListener(null) }
            }
            // Make this a shared flow to prevent colorManager.setListener from being called
            // every time this flow is collected, since colorManager is a singleton.
            .shareIn(scope = scope, started = SharingStarted.WhileSubscribed(), replay = 1)

    override val selectedColorOption =
        combine(colorOptions, settingsChanged) { options, _ ->
                options.forEach { (_, optionsByType) ->
                    optionsByType.forEach {
                        if (it.isActive(colorManager)) {
                            return@combine it
                        }
                    }
                }
                return@combine getSettingsColorOption()
            }
            .shareIn(scope = scope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private fun getSettingsColorOption(): ColorOption {
        val overlays = colorManager.currentOverlays
        val styleOrNull = colorManager.currentStyle
        val style = styleOrNull?.let { ThemeStyle.valueOf(it) } ?: ThemeStyle.TONAL_SPOT
        val source = colorManager.currentColorSource
        val builder = ColorOptionImpl.Builder()
        builder.source = source
        builder.style = style
        for (overlay in overlays) {
            builder.addOverlayPackage(overlay.key, overlay.value)
        }
        val seedColorStr = overlays[ResourceConstants.OVERLAY_CATEGORY_SYSTEM_PALETTE]
        if (seedColorStr != null && !seedColorStr.startsWith("#")) {
            val seedColorInt = "#$seedColorStr".toColorInt()
            builder.lightColors =
                ColorProviderUtil.getColorPreview(
                    ColorScheme(seedColorInt, /* darkTheme= */ false, style),
                    source,
                    /* darkTheme= */ false,
                )
            builder.darkColors =
                ColorProviderUtil.getColorPreview(
                    ColorScheme(seedColorInt, /* darkTheme= */ true, style),
                    source,
                    /* darkTheme= */ true,
                )
        } else {
            builder.lightColors =
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                )
            builder.darkColors =
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                )
        }
        return builder.build()
    }

    override suspend fun select(colorOption: ColorOption): Boolean {
        return suspendCancellableCoroutine { continuation ->
            colorManager.apply(
                colorOption,
                object : CustomizationManager.Callback {
                    override fun onSuccess() {
                        continuation.resumeWith(Result.success(true))
                    }

                    override fun onError(throwable: Throwable?) {
                        Log.w(TAG, "Apply theme with error", throwable)
                        continuation.resumeWith(Result.success(false))
                    }
                },
            )
        }
    }

    companion object {
        private const val TAG = "ColorPickerRepositoryImpl2"
    }
}
