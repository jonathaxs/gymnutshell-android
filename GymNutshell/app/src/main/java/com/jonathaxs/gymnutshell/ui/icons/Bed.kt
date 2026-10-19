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

public val Icons.Filled.Bed: ImageVector
    get() {
        if (_bed != null) {
            return _bed!!
        }
        _bed = materialIcon(name = "Filled.Bed") {
            materialPath {
                moveTo(21.0f, 10.78f)
                verticalLineTo(8.0f)
                curveToRelative(0.0f, -1.65f, -1.35f, -3.0f, -3.0f, -3.0f)
                horizontalLineToRelative(-4.0f)
                curveToRelative(-0.77f, 0.0f, -1.47f, 0.3f, -2.0f, 0.78f)
                curveTo(11.47f, 5.3f, 10.77f, 5.0f, 10.0f, 5.0f)
                horizontalLineTo(6.0f)
                curveTo(4.35f, 5.0f, 3.0f, 6.35f, 3.0f, 8.0f)
                verticalLineToRelative(2.78f)
                curveTo(2.39f, 11.33f, 2.0f, 12.12f, 2.0f, 13.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-6.0f)
                curveTo(22.0f, 12.12f, 21.61f, 11.33f, 21.0f, 10.78f)
                close()
                moveTo(14.0f, 7.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-6.0f)
                verticalLineTo(8.0f)
                curveTo(13.0f, 7.45f, 13.45f, 7.0f, 14.0f, 7.0f)
                close()
                moveTo(5.0f, 8.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(8.0f)
                close()
            }
        }
        return _bed!!
    }

private var _bed: ImageVector? = null
