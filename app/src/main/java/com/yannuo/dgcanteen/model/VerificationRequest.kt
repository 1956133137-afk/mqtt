package com.yannuo.dgcanteen.model

class VerificationRequest {
    var campusId: String = ""
    var dcEncryptParam: String = ""
    var flag: Int = 1 // 1-订餐查询
    var corpId: String = ""
}

class BookMealRequest{
    var dcEncryptParam: String = ""
    var flag: Int = 1 // 1-订餐查询
    var campusId: String = ""
}