package com.yannuo.dgcanteen.download

data class AppInfoB(var appId : String? ,
                    var channel :String? ,
                    var version :String? ,
                    var versionCode :String?,
                    var sn: String?) {
    override fun toString(): String {
        return "AppInfoB(appId=$appId, channel=$channel, version=$version, versionCode=$versionCode, sn=$sn)"
    }
}
