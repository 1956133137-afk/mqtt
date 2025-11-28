package com.yannuo.dgcanteen.model

data class FaceRequest(
    var campusId: String = "", //园区ID
    var custId: String? = "",
    var lastUpdateTime: String? = "",
    var page: Int? = 1, // 1
    var pageSize: Int? = 500, // 500
    var userId: String? = ""
)