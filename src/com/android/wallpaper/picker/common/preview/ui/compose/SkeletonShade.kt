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

import android.view.SurfaceControl
import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.customization.picker.color.ui.compose.CustomColorScheme
import com.android.customization.picker.color.ui.compose.LocalAnimatedColorScheme
import com.android.internal.policy.SystemBarUtils
import com.android.wallpaper.picker.common.preview.ui.binder.BasePreviewBinder.MEDIA_OVERLAY_SURFACE_LAYER
import com.android.wallpaper.picker.common.preview.ui.view.CustomizationSurfaceView

/**
 * An abstract version of the quick settings and notifications shade, intended for previewing colors
 * in the color picker.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SkeletonShade(modifier: Modifier = Modifier) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CustomizationSurfaceView(context).apply {
                    compositionOrder = MEDIA_OVERLAY_SURFACE_LAYER
                    fun blur() {
                        val sc = this.surfaceControl
                        val t = SurfaceControl.Transaction()
                        t.setBackgroundBlurRadius(sc, 100)
                        t.apply()
                    }
                    holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                blur()
                            }

                            override fun surfaceChanged(
                                p0: SurfaceHolder,
                                p1: Int,
                                p2: Int,
                                p3: Int,
                            ) {}

                            override fun surfaceDestroyed(holder: SurfaceHolder) {}
                        }
                    )
                }
            },
        )
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(color = colorScheme.primary.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp)
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        with(LocalDensity.current) {
                            SystemBarUtils.getStatusBarHeight(LocalContext.current).toDp()
                        }
                    )
            )
            // TODO (b/441279631): add scalable, translatable preview content with correct colors
            Text(text = "Tue, Feb...", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "9:30",
                style =
                    MaterialTheme.typography.headlineLargeEmphasized.copy(
                        fontWeight = FontWeight(600),
                        fontSize = TextUnit(64f, TextUnitType.Sp),
                    ),
            )

            val interactionSource = remember { MutableInteractionSource() }
            val colors = SliderDefaults.colors()
            val enabled = true
            Slider(
                value = 0.5f,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
                track = { sliderState ->
                    SliderDefaults.Track(
                        modifier = Modifier.height(36.dp),
                        colors = colors,
                        enabled = enabled,
                        sliderState = sliderState,
                        drawStopIndicator = {},
                        drawTick = { _, _ -> },
                        trackCornerSize = 18.dp,
                        thumbTrackGapSize = 6.dp,
                        trackInsideCornerSize = 2.dp,
                    )
                },
            )
        }
    }
}
