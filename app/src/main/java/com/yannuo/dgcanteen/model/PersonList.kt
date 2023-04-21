package com.yannuo.dgcanteen.model

import com.yannuo.dgcanteen.dao.Persons

class PersonList(){
    var list :MutableList<Persons> ?= null
    var page: Int = 1
    val pageSize: Int  = 0
    val totalPage: Int  = 0
    val totalRecord: Int  = 0
}
