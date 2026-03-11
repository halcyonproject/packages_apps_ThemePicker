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
package com.android.customization.model.picker.color.domain.interactor

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorPickerInteractorTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var underTest: ColorPickerInteractor
    private lateinit var repository: FakeColorPickerRepository

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()

        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = FakeColorPickerRepository(baseFlags = BaseFlags.get(context))
        underTest = ColorPickerInteractor(repository = repository)
        repository.setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
    }

    @Test
    fun apply() = runTest {
        val colorOptions = collectLastValue(underTest.colorOptions)
        val selectedColorOption = collectLastValue(underTest.selectedColorOption)

        val wallpaperColors = colorOptions()?.get(0)
        assertThat(wallpaperColors?.first).isEqualTo(ColorType.WALLPAPER_COLOR)
        val wallpaperColorOption = wallpaperColors?.second?.get(2)
        assertThat(selectedColorOption()).isNotEqualTo(wallpaperColorOption)

        wallpaperColorOption?.let { underTest.apply(colorOption = it) }
        assertThat(selectedColorOption()).isEqualTo(wallpaperColorOption)

        val presetColors = colorOptions()?.get(1)
        assertThat(presetColors?.first).isEqualTo(ColorType.PRESET_COLOR)
        val presetColorOption = presetColors?.second?.get(1)
        assertThat(selectedColorOption()).isNotEqualTo(presetColorOption)

        presetColorOption?.let { underTest.apply(colorOption = it) }
        assertThat(selectedColorOption()).isEqualTo(presetColorOption)
    }
}
