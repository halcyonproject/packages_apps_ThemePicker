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

import android.text.TextUtils
import android.view.SurfaceControl
import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.compose.grid.VerticalGrid
import com.android.customization.picker.color.ui.compose.CustomColorScheme
import com.android.customization.picker.color.ui.compose.LocalAnimatedColorScheme
import com.android.internal.policy.SystemBarUtils
import com.android.themepicker.R
import com.android.wallpaper.picker.common.preview.ui.binder.BasePreviewBinder.MEDIA_OVERLAY_SURFACE_LAYER
import com.android.wallpaper.picker.common.preview.ui.view.CustomizationSurfaceView
import kotlin.math.abs

/**
 * An abstract version of the quick settings and notifications shade, intended for previewing colors
 * in the color picker.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SkeletonShade(isDualShade: Boolean, modifier: Modifier = Modifier) {
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

        Row(
            modifier =
                Modifier.fillMaxSize()
                    .background(color = colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(
                modifier =
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(fraction = if (isDualShade) 0.5f else 1f)
                        .padding(horizontal = 16.dp)
            ) {
                Spacer(
                    modifier =
                        Modifier.height(
                            with(LocalDensity.current) {
                                SystemBarUtils.getStatusBarHeight(LocalContext.current).toDp()
                            }
                        )
                )
                // TODO (b/441279631): add scalable, translatable preview content with correct
                // colors
                Text(
                    text = "Tue, Feb...",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = "9:30",
                    style =
                        MaterialTheme.typography.headlineLargeEmphasized.copy(
                            fontWeight = FontWeight(600),
                            fontSize = TextUnit(64f, TextUnitType.Sp),
                        ),
                    color = colorScheme.onSurface,
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

                Spacer(
                    modifier = Modifier.height(dimensionResource(R.dimen.qs_tile_margin_vertical))
                )

                VerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = 2,
                    verticalSpacing = dimensionResource(R.dimen.qs_tile_margin_vertical),
                    horizontalSpacing = dimensionResource(R.dimen.qs_tile_margin_horizontal),
                ) {
                    LargeTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        label = "Home",
                        secondaryLabel = "Studio Wifi",
                        modifier = Modifier.weight(1f),
                    )
                    LargeTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                    LargeTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                    LargeTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(
                    modifier = Modifier.height(dimensionResource(R.dimen.qs_tile_margin_vertical))
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            space = dimensionResource(R.dimen.qs_tile_margin_horizontal)
                        )
                ) {
                    SmallTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                    SmallTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                    SmallTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                    SmallTile(
                        icon = painterResource(R.drawable.ic_palette_filled_24px),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (isDualShade) {
                Column(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(
                        modifier =
                            Modifier.height(
                                with(LocalDensity.current) {
                                    SystemBarUtils.getStatusBarHeight(LocalContext.current).toDp()
                                }
                            )
                    )
                    Notification(icon = painterResource(R.drawable.ic_palette_filled_24px))
                    Notification(icon = painterResource(R.drawable.ic_palette_filled_24px))
                }
            }
        }
    }
}

@Composable
private fun Notification(
    icon: Painter,
    modifier: Modifier = Modifier,
    label: String? = null,
    secondaryLabel: String? = null,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Box(
        modifier =
            modifier
                .height(dimensionResource(R.dimen.common_tile_default_tile_height))
                .clip(RoundedCornerShape(28.dp))
                .background(color = colorScheme.surfaceDim)
                .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp, alignment = Alignment.Start)
        ) {
            Box(modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = icon,
                    tint = colorScheme.onSurface,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                )
            }
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                TileLabel(
                    text = label ?: "",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = colorScheme.onSurface,
                )
                if (secondaryLabel != null && !TextUtils.isEmpty(secondaryLabel)) {
                    TileLabel(
                        // TODO(b/441279631): figure out why entire words are cut off in
                        //  long text
                        text = secondaryLabel,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun LargeTile(
    icon: Painter,
    modifier: Modifier = Modifier,
    label: String? = null,
    secondaryLabel: String? = null,
) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Box(
        modifier =
            modifier
                .height(dimensionResource(R.dimen.common_tile_default_tile_height))
                .clip(RoundedCornerShape(50.dp))
                .background(color = colorScheme.surfaceDim)
                .padding(
                    start = dimensionResource(R.dimen.common_tile_default_start_padding),
                    end = 12.dp,
                )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp, alignment = Alignment.Start),
        ) {
            Box(
                modifier =
                    Modifier.size(dimensionResource(R.dimen.common_tile_default_toggle_target_size))
            ) {
                Icon(
                    painter = icon,
                    tint = colorScheme.onSurface,
                    contentDescription = null,
                    modifier =
                        Modifier.size(dimensionResource(R.dimen.common_tile_default_icon_size))
                            .align(Alignment.Center),
                )
            }
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                TileLabel(
                    text = label ?: "",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = colorScheme.onSurface,
                )
                if (secondaryLabel != null && !TextUtils.isEmpty(secondaryLabel)) {
                    TileLabel(
                        // TODO(b/441279631): figure out why entire words are cut off in
                        //  long text
                        text = secondaryLabel,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallTile(icon: Painter, modifier: Modifier = Modifier) {
    val colorScheme: CustomColorScheme = LocalAnimatedColorScheme.current
    Box(
        modifier =
            modifier
                .height(dimensionResource(R.dimen.common_tile_default_tile_height))
                .clip(RoundedCornerShape(50.dp))
                .background(color = colorScheme.surfaceDim)
                .padding(
                    start = dimensionResource(R.dimen.common_tile_default_start_padding),
                    end = 12.dp,
                )
    ) {
        Box(
            modifier =
                Modifier.size(dimensionResource(R.dimen.common_tile_default_toggle_target_size))
                    .align(Alignment.Center)
        ) {
            Icon(
                painter = icon,
                tint = colorScheme.onSurface,
                contentDescription = null,
                modifier =
                    Modifier.size(dimensionResource(R.dimen.common_tile_default_icon_size))
                        .align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun TileLabel(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    var textSize by remember { mutableIntStateOf(0) }

    BasicText(
        text = text,
        color = { color },
        style = style,
        maxLines = 1,
        onTextLayout = { textSize = it.size.width },
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (textSize > size.width) {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (textSize > size.width) {
                        // Draw a blur over the end of the text
                        val edgeWidthPx = 32.dp.toPx()
                        if (layoutDirection == LayoutDirection.Rtl) {
                            drawFadedEdge(
                                startX = 0f,
                                endX = edgeWidthPx,
                                colors = listOf(Color.Transparent, Color.Black),
                            )
                        } else {
                            drawFadedEdge(
                                startX = size.width - edgeWidthPx,
                                endX = size.width,
                                colors = listOf(Color.Black, Color.Transparent),
                            )
                        }
                    }
                },
    )
}

private fun DrawScope.drawFadedEdge(startX: Float, endX: Float, colors: List<Color>) {
    drawRect(
        topLeft = Offset(startX, 0f),
        size = Size(abs(endX - startX), size.height),
        brush = Brush.horizontalGradient(colors = colors, startX = startX, endX = endX),
        blendMode = BlendMode.DstIn,
    )
}
