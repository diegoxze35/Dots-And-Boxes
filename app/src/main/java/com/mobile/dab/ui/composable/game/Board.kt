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
import com.mobile.dab.game.GameUiState
import com.mobile.dab.game.Line
import com.mobile.dab.game.Orientation
import kotlinx.coroutines.launch

@Composable
internal fun Board(
	modifier: Modifier = Modifier,
	gridRows: Int,
	gridCols: Int,
	state: GameUiState,
	onLineSelected: (Line) -> Unit,
	onDebug: (String) -> Unit = {}
) {
	// Make the board responsive: use available size to compute spacing
	BoxWithConstraints(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		val maxW = constraints.maxWidth.toFloat()
		val maxH = constraints.maxHeight.toFloat()
		val density = LocalDensity.current
		val paddingPx = with(density) { 16.dp.toPx() }
		
		// boardSize = min(width, height) - padding
		val boardSize = (kotlin.math.min(maxW, maxH) - paddingPx * 2).coerceAtLeast(100f)
		val spacingPx = boardSize / (kotlin.math.max(1, gridCols - 1))
		
		// capture colors outside of draw scope
		val colors = MaterialTheme.colorScheme
		val primaryColor = colors.primary
		val secondaryColor = colors.secondary
		
		// Animatables for lines
		val coroutineScope = rememberCoroutineScope()
		val lineAnims = remember { mutableStateMapOf<Line, Animatable<Float, AnimationVector1D>>() }
		val touchAnim = remember { Animatable(0f) }
		var touchPoint by remember { mutableStateOf<Offset?>(null) }
		// optimistic pending lines shown immediately on tap until ViewModel confirms
		val pendingPlaced = remember { mutableStateOf<Map<Line, Int>>(emptyMap()) }
		// for press+drag interaction
		var activeStartDot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
		var validTargets by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
		var dragPosition by remember { mutableStateOf<Offset?>(null) }
		
		// Sync anim map with placed lines: animate newly added lines and clear pending when ViewModel confirms
		LaunchedEffect(state.placedLines) {
			// clear if new game
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
				// remove confirmed lines from pending
				pendingPlaced.value = pendingPlaced.value.filterKeys { it in state.placedLines }
			}
			// clear candidate hint after a short delay
			// candidateLine = null  // no longer used
		}
		
		// Precompute offsets used both for drawing and hit-testing
		val totalWidth = spacingPx * (gridCols - 1)
		val totalHeight = spacingPx * (gridRows - 1)
		val offsetX = (boardSize - totalWidth) / 2f
		val offsetY = (boardSize - totalHeight) / 2f
		
		// precompute dot positions for hit-testing
		val dotRadius =
			with(density) { 10.dp.toPx() } // increased from 6.dp to make touch area larger
		val touchThreshold = dotRadius * 2.5f // larger hit area for presses/releases
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
		
		// helper to compute available adjacent targets for a given dot
		fun adjacentAvailableTargets(r: Int, c: Int): Set<Pair<Int, Int>> {
			val res = mutableSetOf<Pair<Int, Int>>()
			// up (connects to dot r-1,c) -> vertical line at row = r-1, col = c
			if (r - 1 >= 0) {
				val line = Line(r - 1, c, Orientation.VERTICAL)
				if (line !in state.placedLines) res.add(Pair(r - 1, c))
			}
			// down (connects to dot r+1,c) -> vertical line at row = r, col = c
			if (r < gridRows - 1) {
				val line = Line(r, c, Orientation.VERTICAL)
				if (line !in state.placedLines) res.add(Pair(r + 1, c)) // target dot at row+1,col
			}
			// left (connects to dot r,c-1) -> horizontal line at row = r, col = c-1
			if (c - 1 >= 0) {
				val line = Line(r, c - 1, Orientation.HORIZONTAL)
				if (line !in state.placedLines) res.add(Pair(r, c - 1))
			}
			// right (connects to dot r,c+1) -> horizontal line at row = r, col = c
			if (c < gridCols - 1) {
				val line = Line(r, c, Orientation.HORIZONTAL)
				if (line !in state.placedLines) res.add(Pair(r, c + 1))
			}
			return res
		}
		
		// pointer input: press on dot -> set activeStartDot and validTargets; drag shows provisional line; release on valid target -> place line
		Canvas(
			modifier = Modifier
				.size(with(density) { boardSize.toDp() })
				.pointerInput(state.placedLines) {
					detectDragGestures(
						onDragStart = { downPos ->
							// check if down is on a dot
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
							// update drag position while dragging
							if (activeStartDot != null) {
								dragPosition = change.position
								// don't call consume* APIs to remain compatible with this Compose version
							}
						},
						onDragEnd = {
							// release: check if released near any valid target
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
									// same row, adjacent cols -> HORIZONTAL at row=sr, col=min(sc,tc)
									sr == tr && kotlin.math.abs(sc - tc) == 1 -> {
										Line(sr, kotlin.math.min(sc, tc), Orientation.HORIZONTAL)
									}
									// same col, adjacent rows -> VERTICAL at row=min(sr,tr), col=sc
									sc == tc && kotlin.math.abs(sr - tr) == 1 -> {
										Line(kotlin.math.min(sr, tr), sc, Orientation.VERTICAL)
									}
									
									else -> null
								}
								if (line != null) {
									if (line !in state.placedLines && line !in pendingPlaced.value.keys) {
										// optimistic owner is current player at moment of placement
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
							
							// clear drag state
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
			// draw background grid offset to center (offsetX/offsetY precomputed above)
			// draw boxes background subtle
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
			
			// draw boxes owners
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
			
			// draw dots (and highlight valid targets and active start)
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
			
			// draw placed lines (animated by progress) including optimistic pending ones
			val allLinesKeys: Set<Line> = state.placedLines + pendingPlaced.value.keys
			for (line in allLinesKeys) {
				val progress = lineAnims[line]?.value ?: 1f
				// determine owner: confirmed owner in state.lineOwners or optimistic owner from pendingPlaced, default 0
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
			
			// draw the provisional dragged line if active
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
			
			// touch ripple
			touchPoint?.let { tp ->
				val radius = touchAnim.value * (spacingPx * 0.6f)
				drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = radius, center = tp)
			}
			
			// draw candidate highlight briefly if needed (no-op: we replaced with activeStartDot/validTargets)
		}
	}
}
