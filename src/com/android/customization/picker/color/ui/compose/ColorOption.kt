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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.unit.dp

@Composable
fun ColorOption(
    modifier: Modifier,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDrawWithCache: CacheDrawScope.() -> DrawResult,
) {
    val colorScheme = LocalAnimatedColorScheme.current
    val roundedCornerShape = RoundedCornerShape(18.dp)
    Box(modifier = modifier) {
        // selection ring
        if (isSelected) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .clip(roundedCornerShape)
                        .border(
                            width = 3.dp,
                            color = colorScheme.primary,
                            shape = roundedCornerShape,
                        )
                        .drawBehind { drawRect(colorScheme.surfaceBright) }
            )
        }
        // content
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(7.dp)
                    .clip(
                        if (isSelected) {
                            //  Inner corners are less rounded to visually compensate border effect
                            RoundedCornerShape(12.dp)
                        } else {
                            CircleShape
                        }
                    )
                    .clickable(onClick = onClick)
                    .drawWithCache(onBuildDrawCache = onDrawWithCache)
        )
    }
}
