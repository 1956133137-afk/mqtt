package com.yannuo.dgcanteen.model

data class FacePayRequest(
    var deviceId: String = "", //设备序列号
    var campusId: String = "", //园区ID
    var businessId: String = "", //商家id
    var businessName: String = "", //商家名称
    var vposId: String = "", //柜台号
    var payment: String = "", //支付金额
    var actualPayment: String = "", //真实支付金额
    var offline: String = "", //离线订单标识：0：联机支付，1离线补扣
    var signTime: String? = "",//离线订单签单时间
    var personNumber: String? = "", //用户编号
    var custId: String? = "",
    var faceBase64: String? = "", //抓拍人脸base64编码
    var faceScore: String? = "", //相似分数值
)