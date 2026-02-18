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

package com.android.customization.picker.color.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.themepicker.R

/** UI for defining custom colors. */
@Composable
fun FreeformColorPicker(
    hueSliderPosition: Float,
    onHueChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    navigateToLanding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current

    DrillDownFloatingSheet(
        onCancel = onCancel,
        onConfirm = onConfirm,
        navigateToLanding = navigateToLanding,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.color_freeform_editor_description),
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            val huePhaseShift = -30f
            val hueColorList = buildList {
                for (i in 0..360 step 30) {
                    add(
                        Color.hsv(
                            hue = (i + huePhaseShift).mod(360.0).toFloat(),
                            saturation = 0.6f,
                            value = 0.8f,
                        )
                    )
                }
            }
            BrushSlider(
                brush = Brush.horizontalGradient(colors = hueColorList),
                sliderPosition = hueSliderPosition,
                valueRange = 0f..360f,
                onValueChange = onHueChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
