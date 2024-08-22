package com.yannuo.dgcanteen.model

import com.yannuo.dgcanteen.greendao.entity.Persons


data class PeopleBean(var type :Int, var messageId :String?, var data : List<Persons>,
                      var cmd :String?, var param :String?, var deviceNum :String?)
