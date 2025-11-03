package com.mobile.dab.domain

import kotlinx.serialization.Serializable

@Serializable
data class Line(val row: Int, val col: Int, val orientation: Orientation)
