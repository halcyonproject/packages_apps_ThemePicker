package com.android.customization.testing

import android.content.Context
import com.android.customization.module.CustomizationInjector
import com.android.customization.module.CustomizationPreferences
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.network.Requester
import com.android.wallpaper.picker.broadcast.BroadcastDispatcher
import com.android.wallpaper.picker.category.wrapper.WallpaperCategoryWrapper
import com.android.wallpaper.picker.customization.domain.interactor.WallpaperInteractor
import com.android.wallpaper.testing.FakeCurrentWallpaperInfoFactory
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.FakeWallpaperRefresher
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestPackageStatusNotifier
import com.android.wallpaper.util.DisplayUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Deprecated("Use Hilt instead, see b/459863716")
open class TestCustomizationInjector
@Inject
constructor(
    private val customPrefs: TestDefaultCustomizationPreferences,
    private val themesUserEventLogger: ThemesUserEventLogger,
    displayUtils: DisplayUtils,
    requester: Requester,
    networkStatusNotifier: NetworkStatusNotifier,
    partnerProvider: PartnerProvider,
    wallpaperClient: FakeWallpaperClient,
    injectedWallpaperInteractor: WallpaperInteractor,
    prefs: WallpaperPreferences,
    private val fakeWallpaperCategoryWrapper: WallpaperCategoryWrapper,
    testStatusNotifier: TestPackageStatusNotifier,
    currentWallpaperInfoFactory: FakeCurrentWallpaperInfoFactory,
    wallpaperRefresher: FakeWallpaperRefresher,
    broadcastDispatcher: BroadcastDispatcher,
) :
    TestInjector(
        themesUserEventLogger,
        displayUtils,
        requester,
        networkStatusNotifier,
        partnerProvider,
        wallpaperClient,
        injectedWallpaperInteractor,
        prefs,
        fakeWallpaperCategoryWrapper,
        testStatusNotifier,
        currentWallpaperInfoFactory,
        wallpaperRefresher,
        broadcastDispatcher,
    ),
    CustomizationInjector {
    /////////////////
    // CustomizationInjector implementations
    /////////////////

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return customPrefs
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        throw UnsupportedOperationException("not implemented")
    }

    /////////////////
    // TestInjector overrides
    /////////////////

    override fun getUserEventLogger(): UserEventLogger {
        return themesUserEventLogger
    }

    override fun getWallpaperCategoryWrapper(): WallpaperCategoryWrapper {
        return fakeWallpaperCategoryWrapper
    }
}
