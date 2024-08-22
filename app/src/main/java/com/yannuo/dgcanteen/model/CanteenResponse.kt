package com.yannuo.dgcanteen.model

class CanteenResponse<T>(code: String = "", message: String = "") {
    var code: String = code
    var msg: String = message
    var data: T? = null
}