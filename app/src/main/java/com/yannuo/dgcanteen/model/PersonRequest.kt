package com.yannuo.dgcanteen.model

class PersonRequest(
    var page: Int,
    var pageSize: Int,
    var campusId: String = "",
    var deviceId: String = ""
)