package com.yannuo.dgcanteen.exception

/**
 * @dsc     简介
 * @Author  LiWeiZhong
 * @Date    2024/11/14 9:27
 * @Version 1.0
 */
data class ResponseException(val errorCode: String, val msg: String): Exception(msg) {
}