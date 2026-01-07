package com.yannuo.dgcanteen.model

data class ClothingItem(
    val id: Int,
    val name: String,
    var isSelected: Boolean = false
)
