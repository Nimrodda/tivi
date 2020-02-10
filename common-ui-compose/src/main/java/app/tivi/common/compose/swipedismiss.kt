/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.common.compose

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.animation.animatedFloat
import androidx.ui.core.DensityAmbient
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.layout.Container
import androidx.ui.layout.Stack
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import kotlin.math.absoluteValue

enum class SwipeDirection {
    LEFT, RIGHT
}

private val defaultDirections = listOf(SwipeDirection.LEFT, SwipeDirection.RIGHT)

@Composable
fun SwipeToDismiss(
    onSwipe: ((Float) -> Unit)? = null,
    onSwipeComplete: (SwipeDirection) -> Unit,
    swipeDirections: List<SwipeDirection> = defaultDirections,
    swipeCompletePercentage: Float = 0.6f,
    backgroundChildren: @Composable() (swipeProgress: Float, wouldCompleteOnRelease: Boolean) -> Unit,
    swipeChildren: @Composable() (swipeProgress: Float, wouldCompleteOnRelease: Boolean) -> Unit
) = Stack {
    val position = animatedFloat(0f)

    OnPositioned(onPositioned = { coordinates ->
        var min = position.min
        var max = position.max

        if (SwipeDirection.LEFT in swipeDirections) {
            min = -coordinates.size.width.value.toFloat()
        }
        if (SwipeDirection.RIGHT in swipeDirections) {
            max = coordinates.size.width.value.toFloat()
        }
        position.setBounds(min, max)
    })

    var swipeChildrenSize by state { IntPxSize(IntPx.Zero, IntPx.Zero) }
    val progress = state { 0f }

    if (position.value != 0f) {
        with(DensityAmbient.current) {
            Container(
                width = swipeChildrenSize.width.toDp(),
                height = swipeChildrenSize.height.toDp()
            ) {
                backgroundChildren(
                    progress.value,
                    progress.value.absoluteValue >= swipeCompletePercentage
                )
            }
        }
    }

    OnChildPositioned(onPositioned = { coords -> swipeChildrenSize = coords.size }) {
        Draggable(
            dragDirection = DragDirection.Horizontal,
            dragValue = position,
            onDragStopped = {
                // We can't reference AnimatedValueHolder.animatedFloat.min/max so we have to keep
                // our own state /sadface
                // TODO: raise FR to open up AnimatedFloat.min/max
                if (position.max > 0f && position.value / position.max >= swipeCompletePercentage) {
                    onSwipeComplete(SwipeDirection.RIGHT)
                } else if (position.min < 0f && position.value / position.min >= swipeCompletePercentage) {
                    onSwipeComplete(SwipeDirection.LEFT)
                }
                position.animateTo(0f)
            },
            onDragValueChangeRequested = { dragValue ->
                // Update the position using snapTo
                position.snapTo(dragValue)

                progress.value = when {
                    dragValue < 0f && position.min < 0f -> dragValue / position.min
                    dragValue > 0f && position.max > 0f -> dragValue / position.max
                    else -> 0f
                }

                if (onSwipe != null) {
                    // If we have an onSwipe callback, invoke it
                    onSwipe(progress.value)
                }
            }
        ) {
            WithOffset(xOffset = position) {
                swipeChildren(
                    progress.value,
                    progress.value.absoluteValue >= swipeCompletePercentage
                )
            }
        }
    }
}
