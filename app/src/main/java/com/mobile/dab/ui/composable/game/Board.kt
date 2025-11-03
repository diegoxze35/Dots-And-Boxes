package com.mobile.dab.ui.composable.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mobile.dab.domain.Line
import com.mobile.dab.domain.Orientation
import com.mobile.dab.ui.composable.game.state.GameUiState
import kotlinx.coroutines.launch

@Composable
internal fun Board(
	modifier: Modifier = Modifier,
	gridRows: Int,
	gridCols: Int,
	state: GameUiState,
	onLineSelected: (Line) -> Unit,
	isInteractionEnabled: Boolean,
	onDebug: (String) -> Unit = {}
) {
    BoxWithConstraints(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		val maxW = constraints.maxWidth.toFloat()
		val maxH = constraints.maxHeight.toFloat()
		val density = LocalDensity.current
		val paddingPx = with(density) { 16.dp.toPx() }

		val boardSize = (kotlin.math.min(maxW, maxH) - paddingPx * 2).coerceAtLeast(100f)
		val spacingPx = boardSize / (kotlin.math.max(1, gridCols - 1))

		val colors = MaterialTheme.colorScheme
		val primaryColor = colors.primary
		val secondaryColor = colors.secondary

		val coroutineScope = rememberCoroutineScope()
		val lineAnims = remember { mutableStateMapOf<Line, Animatable<Float, AnimationVector1D>>() }
		val touchAnim = remember { Animatable(0f) }
		var touchPoint by remember { mutableStateOf<Offset?>(null) }
		val pendingPlaced = remember { mutableStateOf<Map<Line, Int>>(emptyMap()) }
		var activeStartDot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
		var validTargets by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
		var dragPosition by remember { mutableStateOf<Offset?>(null) }

        // (LaunchedEffect(state.placedLines) sin cambios)
        LaunchedEffect(state.placedLines) {
			if (state.placedLines.isEmpty()) {
				lineAnims.clear()
				pendingPlaced.value = emptyMap()
			} else {
				val added = state.placedLines - lineAnims.keys
				for (line in added) {
					val anim = Animatable(0f)
					lineAnims[line] = anim
					launch { anim.animateTo(1f, animationSpec = tween(350)) }
				}
				pendingPlaced.value = pendingPlaced.value.filterKeys { it in state.placedLines }
			}
		}

        // (Precompute offsets y dots sin cambios)
		val totalWidth = spacingPx * (gridCols - 1)
		val totalHeight = spacingPx * (gridRows - 1)
		val offsetX = (boardSize - totalWidth) / 2f
		val offsetY = (boardSize - totalHeight) / 2f

		val dotRadius =
			with(density) { 10.dp.toPx() }
		val touchThreshold = dotRadius * 2.5f
		val dots = remember(gridRows, gridCols, spacingPx, offsetX, offsetY) {
			val map = mutableMapOf<Pair<Int, Int>, Offset>()
			for (r in 0 until gridRows) {
				for (c in 0 until gridCols) {
					val x = offsetX + c * spacingPx
					val y = offsetY + r * spacingPx
					map[Pair(r, c)] = Offset(x, y)
				}
			}
			map
		}

        // (adjacentAvailableTargets helper sin cambios)
        fun adjacentAvailableTargets(r: Int, c: Int): Set<Pair<Int, Int>> {
			val res = mutableSetOf<Pair<Int, Int>>()
			if (r - 1 >= 0) {
				val line = Line(r - 1, c, Orientation.VERTICAL)
				if (line !in state.placedLines) res.add(Pair(r - 1, c))
			}
			if (r < gridRows - 1) {
				val line = Line(r, c, Orientation.VERTICAL)
				if (line !in state.placedLines) res.add(Pair(r + 1, c))
			}
			if (c - 1 >= 0) {
				val line = Line(r, c - 1, Orientation.HORIZONTAL)
				if (line !in state.placedLines) res.add(Pair(r, c - 1))
			}
			if (c < gridCols - 1) {
				val line = Line(r, c, Orientation.HORIZONTAL)
				if (line !in state.placedLines) res.add(Pair(r, c + 1))
			}
			return res
		}

		Canvas(
			modifier = Modifier
				.size(with(density) { boardSize.toDp() })
				.pointerInput(state.placedLines, isInteractionEnabled) {
                    if (!isInteractionEnabled) {
                        return@pointerInput
                    }
					detectDragGestures(
						onDragStart = { downPos ->
							val start =
								dots.entries.minByOrNull { (_, ofs) -> (ofs - downPos).getDistance() }
							if (start == null) return@detectDragGestures
							val (startKey, startOffset) = start
							if ((startOffset - downPos).getDistance() <= touchThreshold) {
								activeStartDot = startKey
								validTargets =
									adjacentAvailableTargets(startKey.first, startKey.second)
								dragPosition = downPos
								onDebug("Start drag from $startKey, targets=$validTargets")
							}
						},
						onDrag = { change, _ ->
							if (activeStartDot != null) {
								dragPosition = change.position
							}
						},
						onDragEnd = {
							val upPos = dragPosition
							var matchedTarget: Pair<Int, Int>? = null
							if (upPos != null && activeStartDot != null) {
								for (t in validTargets) {
									val tpos = dots[t] ?: continue
									if ((tpos - upPos).getDistance() <= touchThreshold) {
										matchedTarget = t
										break
									}
								}
							}
							if (matchedTarget != null && activeStartDot != null) {
								val (sr, sc) = activeStartDot!!
								val (tr, tc) = matchedTarget
								val line = when {
									sr == tr && kotlin.math.abs(sc - tc) == 1 -> {
										Line(sr, kotlin.math.min(sc, tc), Orientation.HORIZONTAL)
									}
									sc == tc && kotlin.math.abs(sr - tr) == 1 -> {
										Line(kotlin.math.min(sr, tr), sc, Orientation.VERTICAL)
									}

									else -> null
								}
								if (line != null) {
									if (line !in state.placedLines && line !in pendingPlaced.value.keys) {
										pendingPlaced.value =
											pendingPlaced.value + (line to state.currentPlayerIndex)
										if (line !in lineAnims) {
											val anim = Animatable(0f)
											lineAnims[line] = anim
											coroutineScope.launch {
												anim.animateTo(
													1f,
													animationSpec = tween(350)
												)
											}
										}
										onDebug("Placed optimistic line: $line")
										onLineSelected(line)
									}
								}
							}

							activeStartDot = null
							validTargets = emptySet()
							dragPosition = null
						},
						onDragCancel = {
							activeStartDot = null
							validTargets = emptySet()
							dragPosition = null
						}
					)
				}) {
			for (r in 0 until gridRows - 1) {
				for (c in 0 until gridCols - 1) {
					val left = offsetX + c * spacingPx
					val top = offsetY + r * spacingPx
					drawRect(
						color = Color.Transparent,
						topLeft = Offset(left, top),
						size = Size(spacingPx, spacingPx)
					)
				}
			}

			for ((box, owner) in state.boxOwners) {
				val left = offsetX + box.col * spacingPx
				val top = offsetY + box.row * spacingPx
				val right = left + spacingPx
				val bottom = top + spacingPx
				val color =
					if (owner == 0) primaryColor.copy(alpha = 0.35f) else secondaryColor.copy(alpha = 0.35f)
				drawRect(
					color = color,
					topLeft = Offset(left, top),
					size = Size(right - left, bottom - top)
				)
			}

			val dotRadiusLocal = dotRadius
			for (r in 0 until gridRows) {
				for (c in 0 until gridCols) {
					val x = offsetX + c * spacingPx
					val y = offsetY + r * spacingPx
					val center = Offset(x, y)
					val key = Pair(r, c)
					when {
						activeStartDot == key -> drawCircle(
							color = primaryColor,
							radius = dotRadiusLocal * 1.6f,
							center = center
						)

						key in validTargets -> drawCircle(
							color = secondaryColor,
							radius = dotRadiusLocal * 1.4f,
							center = center
						)

						else -> drawCircle(
							color = Color.Gray,
							radius = dotRadiusLocal,
							center = center
						)
					}
				}
			}

			val allLinesKeys: Set<Line> = state.placedLines + pendingPlaced.value.keys
			for (line in allLinesKeys) {
				val progress = lineAnims[line]?.value ?: 1f
				val owner = state.lineOwners[line] ?: pendingPlaced.value[line] ?: 0
				val lineColor = if (owner == 0) primaryColor else secondaryColor
				if (line.orientation == Orientation.HORIZONTAL) {
					val r = line.row
					val c = line.col
					val x1 = offsetX + c * spacingPx
					val y1 = offsetY + r * spacingPx
					val x2 = offsetX + (c + 1) * spacingPx
					val y2 = y1
					val drawToX = x1 + (x2 - x1) * progress
					drawLine(
						color = lineColor,
						start = Offset(x1, y1),
						end = Offset(drawToX, y2),
						strokeWidth = 10f
					)
				} else {
					val r = line.row
					val c = line.col
					val x1 = offsetX + c * spacingPx
					val y1 = offsetY + r * spacingPx
					val x2 = x1
					val y2 = offsetY + (r + 1) * spacingPx
					val drawToY = y1 + (y2 - y1) * progress
					drawLine(
						color = lineColor,
						start = Offset(x1, y1),
						end = Offset(x2, drawToY),
						strokeWidth = 10f
					)
				}
			}

			dragPosition?.let { dp ->
				activeStartDot?.let { s ->
					val sPos = dots[s] ?: return@let
					drawLine(
						color = primaryColor.copy(alpha = 0.9f),
						start = sPos,
						end = dp,
						strokeWidth = 10f
					)
				}
			}

			touchPoint?.let { tp ->
				val radius = touchAnim.value * (spacingPx * 0.6f)
				drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = radius, center = tp)
			}
		}
	}
}
