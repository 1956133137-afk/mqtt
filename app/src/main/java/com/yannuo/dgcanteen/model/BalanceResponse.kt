package com.yannuo.dgcanteen.model;

import java.util.List

data class BalanceResponse(
    val ACC_DATA:List<ACCDATA>,
    val CAMPUS_ID: String,
    val CID_NO: String,
    val CUST_ID: String,
    val ERRCODE: String,
    val ERRMSG: String,
    val RESULT: String
)

data class ACCDATA(
    val ACC_NO: String,
    val ACC_TYPE: String,
    val MONEY: String
)