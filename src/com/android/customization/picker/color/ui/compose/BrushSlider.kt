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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** [Slider] that allows defining a custom [Brush] for the track background. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushSlider(
    brush: Brush,
    sliderPosition: MutableState<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val trackHeight: Dp = 28.dp

    Slider(
        value = sliderPosition.value,
        onValueChange = { sliderPosition.value = it },
        modifier = modifier,
        valueRange = valueRange,
        track = { sliderState ->
            SliderDefaults.Track(
                modifier =
                    Modifier.height(trackHeight).drawBehind {
                        val trackCornerSize: Dp = trackHeight * 3 / 8
                        val trackInsideCornerSize: Dp = 2.dp
                        val thumbTrackGapSize: Dp = 6.0.dp

                        val activeTrackThreshold = 0f
                        val activeTrackStart = 0f
                        val activeTrackEnd =
                            size.width * sliderState.coercedValueAsFraction -
                                thumbTrackGapSize.toPx()
                        val activeTrackWidth = activeTrackEnd - activeTrackStart
                        if (activeTrackWidth > activeTrackThreshold) {
                            drawTrackPath(
                                orientation = Orientation.Horizontal,
                                offset = Offset.Zero,
                                size = Size(activeTrackWidth, trackHeight.toPx()),
                                brush = brush,
                                startCornerRadius = trackCornerSize.toPx(),
                                endCornerRadius = trackInsideCornerSize.toPx(),
                            )
                        }

                        val inactiveTrackThreshold = size.width - thumbTrackGapSize.toPx()
                        val inactiveTrackStart =
                            size.width * sliderState.coercedValueAsFraction +
                                thumbTrackGapSize.toPx()
                        val inactiveTrackEnd = size.width
                        val inactiveTrackWidth = inactiveTrackEnd - inactiveTrackStart
                        if (inactiveTrackStart < inactiveTrackThreshold) {
                            drawTrackPath(
                                orientation = Orientation.Horizontal,
                                offset = Offset(inactiveTrackStart, 0f),
                                size = Size(inactiveTrackWidth, trackHeight.toPx()),
                                brush = brush,
                                startCornerRadius = trackInsideCornerSize.toPx(),
                                endCornerRadius = trackCornerSize.toPx(),
                            )
                        }
                    },
                sliderState = sliderState,
                colors =
                    SliderDefaults.colors(
                        inactiveTickColor = SliderDefaults.colors().activeTickColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
            )
        },
    )
}

private val trackPath = Path()

private fun DrawScope.drawTrackPath(
    orientation: Orientation,
    offset: Offset,
    size: Size,
    brush: Brush,
    startCornerRadius: Float,
    endCornerRadius: Float,
) {
    val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
    val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
    val track =
        if (orientation == Orientation.Vertical) {
            RoundRect(
                rect = Rect(offset, size = Size(size.width, size.height)),
                topLeft = startCorner,
                topRight = startCorner,
                bottomRight = endCorner,
                bottomLeft = endCorner,
            )
        } else {
            RoundRect(
                rect = Rect(offset, size = Size(size.width, size.height)),
                topLeft = startCorner,
                topRight = endCorner,
                bottomRight = endCorner,
                bottomLeft = startCorner,
            )
        }
    trackPath.addRoundRect(track)
    drawPath(trackPath, brush)
    trackPath.rewind()
}
