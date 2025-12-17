package com.yannuo.dgcanteen.model

//class OpenApiBean {
//    var code: Int = 200
//    var data: Data? = null
//}

class OpenApiBean {
    var metrics: Metrics? = null
    var dbHealth: DbHealth? = null
    var targetUrl: String = ""
    var host: String = ""
    var timestamp: String = ""
}

class Metrics {
    var responseTime: String = ""
    var httpCode: String = ""
    var downloadSpeed: String = ""
    var downloadSpeedUnit: String = ""
}

class DbHealth {
    var app: String = ""
    var dbStatus: String = ""
    var dbLatencyMs: String = ""
    var error: String = ""
    var timestamp: String = ""
}