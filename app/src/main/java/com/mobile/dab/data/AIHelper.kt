package com.mobile.dab.data

import com.mobile.dab.domain.Box
import com.mobile.dab.domain.Line
import com.mobile.dab.domain.Orientation

object AIHelper {
    // Choose a move: prefer moves that complete boxes, otherwise random
    fun chooseMove(gridRows: Int, gridCols: Int, placed: Set<Line>): Line? {
        val allLines = mutableListOf<Line>()
        // horizontal lines
        for (r in 0 until gridRows) {
            for (c in 0 until gridCols - 1) {
                allLines.add(Line(r, c, Orientation.HORIZONTAL))
            }
        }
        // vertical lines
        for (r in 0 until gridRows - 1) {
            for (c in 0 until gridCols) {
                allLines.add(Line(r, c, Orientation.VERTICAL))
            }
        }
        val available = allLines.filter { it !in placed }
        if (available.isEmpty()) return null

        // greedy: if placing a line completes a box, take it
        val completionMoves = available.filter {
            moveCompletesAnyBox(
                move = it,
                gridRows = gridRows,
                gridCols = gridCols,
                placed = placed
            )
        }
        if (completionMoves.isNotEmpty()) return completionMoves.random()

        // otherwise random
        return available.random()
    }

    private fun moveCompletesAnyBox(
        move: Line,
        gridRows: Int,
        gridCols: Int,
        placed: Set<Line>
    ): Boolean {
        // simulate placing move and check boxes around it
        val newPlaced = placed + move
        // check neighboring boxes
        val boxesToCheck = mutableListOf<Box>()
        if (move.orientation == Orientation.HORIZONTAL) {
            // box above: top-left at (row-1, col)
            if (move.row - 1 >= 0) boxesToCheck.add(Box(move.row - 1, move.col))
            // box below: top-left at (row, col)
            if (move.row < gridRows - 1) boxesToCheck.add(Box(move.row, move.col))
        } else {
            // vertical: box left and right
            if (move.col - 1 >= 0) boxesToCheck.add(Box(move.row, move.col - 1))
            if (move.col < gridCols - 1) boxesToCheck.add(Box(move.row, move.col))
        }
        for (box in boxesToCheck) {
            if (isBoxCompleted(box, newPlaced)) return true
        }
        return false
    }

    private fun isBoxCompleted(box: Box, placed: Set<Line>): Boolean {
        // box at top-left (row,col) has 4 sides:
        // top: H at (row, col)
        // bottom: H at (row+1, col)
        // left: V at (row, col)
        // right: V at (row, col+1)
        val top = Line(box.row, box.col, Orientation.HORIZONTAL)
        val bottom = Line(box.row + 1, box.col, Orientation.HORIZONTAL)
        val left = Line(box.row, box.col, Orientation.VERTICAL)
        val right = Line(box.row, box.col + 1, Orientation.VERTICAL)
        return top in placed && bottom in placed && left in placed && right in placed
    }
}