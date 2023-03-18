package com.yannuo.dgcanteen.interfaces

import com.yannuo.dgcanteen.model.Result

interface ImportExportListener{
        // 导出进行中
//       fun  inProgress()
        // 导入状态结果
        fun processState(result : Result)
        // 导出失败
//        fun error(msg : String )
}