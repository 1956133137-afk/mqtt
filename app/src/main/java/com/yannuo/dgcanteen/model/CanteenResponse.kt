package com.yannuo.dgcanteen.model

class CanteenResponse<T>(code: String = "200", message: String = "") {
    var code: String = code
    var msg: String = message
    var data: T? = null
    override fun toString(): String {
        return "CanteenResponse(code='$code', msg='$msg', data=$data)"
    }
}