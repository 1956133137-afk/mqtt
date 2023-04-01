package com.yannuo.dgcanteen.model

class CanteenResponse<T>(code: Int?, message: String?) {
    var code: Int? = code
    var msg: String? = message
    var data: T? = null

}