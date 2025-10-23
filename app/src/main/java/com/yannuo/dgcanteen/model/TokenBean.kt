package com.yannuo.dgcanteen.model

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/23 14:35
 **/
data class TokenBean(var CAMPUS_ID: String = "", var CORP_ID: String = "", var ccbSafeParamBZ: String = "")

class TokenReceive {
    var custId: String = ""
    var campusId: String = ""
    var businessId: String = ""
    var dcccbToken: String = "" //token
}