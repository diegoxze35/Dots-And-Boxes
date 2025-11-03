package com.mobile.dab.domain

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val type: PlayerType = PlayerType.HUMAN
)
