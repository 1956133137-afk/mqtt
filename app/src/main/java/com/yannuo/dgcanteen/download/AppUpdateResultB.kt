package com.yannuo.dgcanteen.download

class AppUpdateResultB{
     var code: Int = 0
     var data: Data?= null
     var msg: String ?=null


    inner class Data(
        var createTime: String,
        var fileUrl: String,
        var needUpgrade: Boolean,
        var version: String,
        var versionCode: String,
        var versionDesc: String
    )

}



