/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.bounceable
import com.android.compose.theme.PlatformTheme
import com.android.customization.model.color.ColorOption
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.systemui.monet.ColorScheme
import com.android.themepicker.R
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun ColorFloatingSheet(
    isDarkMode: Flow<Boolean>,
    colorOptions: Flow<Map<ColorType, List<OptionItemViewModel2<ColorOptionIconViewModel>>>>,
    previewingColorOption: Flow<ColorOption?>,
    modifier: Modifier = Modifier,
) {
    val colorOptionState by colorOptions.collectAsStateWithLifecycle(initialValue = emptyMap())
    val darkModeState by isDarkMode.collectAsStateWithLifecycle(initialValue = false)
    val previewingColorOptionState by
        previewingColorOption.collectAsStateWithLifecycle(initialValue = null)

    // TODO (b/391927276): figure out how to animate color scheme changes
    PlatformTheme {
        // Set color scheme when wallpaper bitmap is selected or changed.
        val scheme =
            previewingColorOptionState?.let {
                ColorScheme(it.seedColor, darkModeState, it.style).materialScheme
            }

        ColorPreviewTheme(scheme) {
            val colorScheme = MaterialTheme.colorScheme
            Box(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(shape = RoundedCornerShape(28.dp))
                        .drawBehind { drawRect(colorScheme.surfaceBright) }
            ) {
                Column(modifier = Modifier.padding(vertical = 20.dp)) {
                    Text(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        text = stringResource(R.string.wallpaper_color_tab),
                        color = colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        ColorType.entries.forEachIndexed { colorTypeIdx, colorType ->
                            colorOptionState[colorType]?.let { colorList ->
                                if (colorTypeIdx != 0 && colorList.isNotEmpty()) {
                                    item { OptionListGroupDivider() }
                                }
                                itemsIndexed(colorList) { idx, option ->
                                    ColorOptionIcon(
                                        isDarkMode = darkModeState,
                                        optionItem = option,
                                        modifier =
                                            Modifier.size(
                                                    dimensionResource(
                                                        R.dimen.floating_sheet_color_option_size
                                                    )
                                                )
                                                .bounceable(
                                                    bounceable = colorList[idx],
                                                    previousBounceable =
                                                        if (idx > 0) colorList[idx - 1] else null,
                                                    nextBounceable =
                                                        if (idx < colorList.lastIndex)
                                                            colorList[idx + 1]
                                                        else null,
                                                    orientation = Horizontal,
                                                ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionListGroupDivider(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier.width(10.dp).height(28.dp).padding(horizontal = 4.dp).drawBehind {
                drawRoundRect(
                    color = colorScheme.onSurfaceVariant,
                    cornerRadius = CornerRadius(x = 1.dp.toPx(), y = 1.dp.toPx()),
                )
            }
    )
}

@Composable
fun ColorOptionIcon(
    isDarkMode: Boolean,
    optionItem: OptionItemViewModel2<ColorOptionIconViewModel>,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val colorIcon = optionItem.payload
    val onClickState by optionItem.onClicked.collectAsStateWithLifecycle(initialValue = null)
    val isSelectedState by optionItem.isSelected.collectAsStateWithLifecycle()
    val shapeModifier =
        if (isSelectedState) {
            modifier
                .clip(RoundedCornerShape(35))
                .border(width = 3.dp, color = colorScheme.primary, shape = RoundedCornerShape(35))
                .border(
                    width = 7.dp,
                    color = colorScheme.surfaceBright,
                    shape = RoundedCornerShape(35),
                )
        } else {
            modifier.padding(7.dp).clip(CircleShape)
        }
    Box(
        modifier =
            shapeModifier
                .clickable {
                    onClickState?.invoke()
                    coroutineScope.launch { optionItem.clickBounceAnimate() }
                }
                .drawBehind {
                    // Round up width to prevent empty pixels between quadrants in bounce animation.
                    val quadrantSize = Size(ceil(size.width / 2f), size.height / 2f)
                    colorIcon?.let {
                        drawRect(
                            color =
                                if (isDarkMode) {
                                    Color(it.darkThemeColor0)
                                } else {
                                    Color(it.lightThemeColor0)
                                },
                            size = quadrantSize,
                        )
                        drawRect(
                            color =
                                if (isDarkMode) {
                                    Color(it.darkThemeColor1)
                                } else {
                                    Color(it.lightThemeColor1)
                                },
                            topLeft = Offset(x = size.width / 2f, y = 0f),
                            size = quadrantSize,
                        )
                        drawRect(
                            color =
                                if (isDarkMode) {
                                    Color(it.darkThemeColor2)
                                } else {
                                    Color(it.lightThemeColor2)
                                },
                            topLeft = Offset(x = 0f, y = size.height / 2f),
                            size = quadrantSize,
                        )
                        drawRect(
                            color =
                                if (isDarkMode) {
                                    Color(it.darkThemeColor3)
                                } else {
                                    Color(it.lightThemeColor3)
                                },
                            topLeft = Offset(x = size.width / 2f, y = size.height / 2f),
                            size = quadrantSize,
                        )
                    }
                }
    )
}
