package com.yannuo.dgcanteen.model

import com.yannuo.dgcanteen.greendao.entity.UserFaceData

data class FaceResponse(
    var list: List<UserFaceData>,
    var page: Int, // 当前页
    var pageSize: Int, // 分页大小
    var totalPage: Int, // 总页码数
    var totalRecord: Int // 总记录数
)