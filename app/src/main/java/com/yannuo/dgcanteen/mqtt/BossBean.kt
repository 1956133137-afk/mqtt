package com.yannuo.portable.mqtt

class BossBean {


     var data: DataBeanZ? = null
     var messageId: String? = null
     var type: Int? = null


    class DataBeanZ {
        /**
         * authEndTime : 2022-04-13 21:06:00
         * authStartTime : 2022-04-11 08:06:00
         * reviewStatus : 2
         * visitorName : 哈哈
         * visitorPhone : 44444444444
         */
        var authEndTime: String? = null
        var authStartTime: String? = null
        var reviewStatus: Int? = null
        var visitorName: String? = null
        var visitorPhone: String? = null
    }
}