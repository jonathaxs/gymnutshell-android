/*
 * Copyright 2025 The Android Open Source Project
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

package com.jonathaxs.gymnutshell.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.SelfImprovement: ImageVector
    get() {
        if (_selfImprovement != null) {
            return _selfImprovement!!
        }
        _selfImprovement = materialIcon(name = "Filled.SelfImprovement") {
            materialPath {
                moveTo(12.0f, 6.0f)
                moveToRelative(-2.0f, 0.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, true, true, 4.0f, 0.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, true, true, -4.0f, 0.0f)
            }
            materialPath {
                moveTo(21.0f, 16.0f)
                verticalLineToRelative(-2.0f)
                curveToRelative(-2.24f, 0.0f, -4.16f, -0.96f, -5.6f, -2.68f)
                lineToRelative(-1.34f, -1.6f)
                curveTo(13.68f, 9.26f, 13.12f, 9.0f, 12.53f, 9.0f)
                horizontalLineToRelative(-1.05f)
                curveToRelative(-0.59f, 0.0f, -1.15f, 0.26f, -1.53f, 0.72f)
                lineToRelative(-1.34f, 1.6f)
                curveTo(7.16f, 13.04f, 5.24f, 14.0f, 3.0f, 14.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(2.77f, 0.0f, 5.19f, -1.17f, 7.0f, -3.25f)
                verticalLineTo(15.0f)
                lineToRelative(-3.88f, 1.55f)
                curveTo(5.45f, 16.82f, 5.0f, 17.48f, 5.0f, 18.21f)
                curveTo(5.0f, 19.2f, 5.8f, 20.0f, 6.79f, 20.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(-0.5f)
                curveToRelative(0.0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
                horizontalLineToRelative(3.0f)
                curveToRelative(0.28f, 0.0f, 0.5f, 0.22f, 0.5f, 0.5f)
                reflectiveCurveTo(14.78f, 18.0f, 14.5f, 18.0f)
                horizontalLineToRelative(-3.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineTo(20.0f)
                horizontalLineToRelative(7.21f)
                curveTo(18.2f, 20.0f, 19.0f, 19.2f, 19.0f, 18.21f)
                curveToRelative(0.0f, -0.73f, -0.45f, -1.39f, -1.12f, -1.66f)
                lineTo(14.0f, 15.0f)
                verticalLineToRelative(-2.25f)
                curveTo(15.81f, 14.83f, 18.23f, 16.0f, 21.0f, 16.0f)
                close()
            }
        }
        return _selfImprovement!!
    }

private var _selfImprovement: ImageVector? = null
