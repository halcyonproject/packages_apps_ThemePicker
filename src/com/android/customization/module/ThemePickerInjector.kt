/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.module

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.color.ColorOptionsProvider.COLOR_SOURCE_PRESET
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.view.ThemePickerClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.module.PackageStatusNotifier
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.WallpaperPicker2Injector
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.WallpaperRefresher
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.network.Requester
import com.android.wallpaper.picker.category.wrapper.WallpaperCategoryWrapper
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.customization.data.repository.WallpaperColorsRepository
import com.android.wallpaper.picker.customization.domain.interactor.WallpaperInteractor
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.system.UiModeManagerWrapper
import com.android.wallpaper.util.DisplayUtils
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@Singleton
open class ThemePickerInjector
@Inject
constructor(
    @MainDispatcher private val mainScope: CoroutineScope,
    @BackgroundDispatcher private val bgScope: CoroutineScope,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
    private val keyguardQuickAffordancePickerInteractor:
        Lazy<KeyguardQuickAffordancePickerInteractor>,
    private val themesUserEventLogger: Lazy<ThemesUserEventLogger>,
    private val colorPickerInteractor: Lazy<ColorPickerInteractor>,
    private val clockRegistry: Lazy<ClockRegistry>,
    private val clockPickerInteractor: Lazy<ClockPickerInteractor>,
    displayUtils: Lazy<DisplayUtils>,
    requester: Lazy<Requester>,
    networkStatusNotifier: Lazy<NetworkStatusNotifier>,
    partnerProvider: Lazy<PartnerProvider>,
    val uiModeManager: Lazy<UiModeManagerWrapper>,
    userEventLogger: Lazy<UserEventLogger>,
    injectedWallpaperClient: Lazy<WallpaperClient>,
    private val injectedWallpaperInteractor: Lazy<WallpaperInteractor>,
    prefs: Lazy<WallpaperPreferences>,
    wallpaperColorsRepository: Lazy<WallpaperColorsRepository>,
    defaultWallpaperCategoryWrapper: Lazy<WallpaperCategoryWrapper>,
    packageNotifier: Lazy<PackageStatusNotifier>,
    wallpaperRefresher: Lazy<WallpaperRefresher>,
) :
    WallpaperPicker2Injector(
        mainScope,
        displayUtils,
        requester,
        networkStatusNotifier,
        partnerProvider,
        uiModeManager,
        userEventLogger,
        injectedWallpaperClient,
        injectedWallpaperInteractor,
        prefs,
        wallpaperColorsRepository,
        defaultWallpaperCategoryWrapper,
        packageNotifier,
        wallpaperRefresher,
    ),
    CustomizationInjector {
    private var clockCarouselViewModelFactory: ClockCarouselViewModel.Factory? = null
    private var clockViewFactory: ClockViewFactory? = null
    private var colorPickerViewModelFactory: ColorPickerViewModel.Factory? = null
    private var clockSettingsViewModelFactory: ClockSettingsViewModel.Factory? = null

    override fun getDeepLinkRedirectIntent(context: Context, uri: Uri): Intent {
        val intent = Intent()
        intent.setClass(context, CustomizationPickerActivity2::class.java)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return intent
    }

    override fun getDownloadableIntentAction(): String? {
        return null
    }

    @Synchronized
    override fun getUserEventLogger(): ThemesUserEventLogger {
        return themesUserEventLogger.get()
    }

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return getPreferences(context) as CustomizationPreferences
    }

    override fun getWallpaperInteractor(context: Context): WallpaperInteractor {
        return injectedWallpaperInteractor.get()
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        return keyguardQuickAffordancePickerInteractor.get()
    }

    override fun getClockCarouselViewModelFactory(
        interactor: ClockPickerInteractor,
        clockViewFactory: ClockViewFactory,
        resources: Resources,
    ): ClockCarouselViewModel.Factory {
        return clockCarouselViewModelFactory
            ?: ClockCarouselViewModel.Factory(
                    interactor,
                    bgDispatcher,
                    clockViewFactory,
                    resources,
                    getUserEventLogger(),
                )
                .also { clockCarouselViewModelFactory = it }
    }

    override fun getClockViewFactory(activity: ComponentActivity): ClockViewFactory {
        return clockViewFactory
            ?: ThemePickerClockViewFactory(
                    activity,
                    WallpaperManager.getInstance(activity.applicationContext),
                    clockRegistry.get(),
                )
                .also {
                    clockViewFactory = it
                    activity.lifecycle.addObserver(
                        object : DefaultLifecycleObserver {
                            override fun onDestroy(owner: LifecycleOwner) {
                                super.onDestroy(owner)
                                if ((owner as Activity).isChangingConfigurations()) return
                                clockViewFactory?.onDestroy()
                            }
                        }
                    )
                }
    }

    override fun getColorPickerViewModelFactory(context: Context): ColorPickerViewModel.Factory {
        return colorPickerViewModelFactory
            ?: ColorPickerViewModel.Factory(
                    context.applicationContext,
                    colorPickerInteractor.get(),
                    getUserEventLogger(),
                )
                .also { colorPickerViewModelFactory = it }
    }

    override fun getClockSettingsViewModelFactory(
        context: Context,
        wallpaperColorsRepository: WallpaperColorsRepository,
    ): ClockSettingsViewModel.Factory {
        return clockSettingsViewModelFactory
            ?: ClockSettingsViewModel.Factory(
                    context.applicationContext,
                    clockPickerInteractor.get(),
                    colorPickerInteractor.get(),
                    getUserEventLogger(),
                ) { clockId ->
                    clockId?.let { clockPickerInteractor.get().isReactiveToTone(it) } ?: false
                }
                .also { clockSettingsViewModelFactory = it }
    }

    override fun isCurrentSelectedColorPreset(context: Context): Boolean {
        val colorManager =
            ColorCustomizationManager.getInstance(context, OverlayManagerCompat(context))
        return COLOR_SOURCE_PRESET == colorManager.currentColorSource
    }
}
