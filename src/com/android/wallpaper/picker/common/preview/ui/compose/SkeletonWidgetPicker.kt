/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.wallpaper.picker.common.preview.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.customization.picker.color.ui.compose.CustomColorScheme
import com.android.customization.picker.color.ui.compose.LocalAnimatedColorScheme

/** An abstract version of the widget picker, intended for previewing colors in the color picker. */
@Composable
fun SkeletonWidgetPicker(modifier: Modifier = Modifier) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = colorScheme.onPrimaryFixedVariant)
                .padding(horizontal = 14.dp)
    ) {
        // TODO (b/441279631): add scalable, translatable preview content with correct colors
    }
}
