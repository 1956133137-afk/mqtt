package com.yannuo.dgcanteen.printer

/**
 *
 * 打印 bitmap 图片
 * @property offset Int
 * @property width Int
 * @property height Int
 * @property imageData
 */
class PicturePrint {
    var rotation = 0  //旋转角度 0、90、180、270
    var iLeft = 0 //距离左边距离,单位 mm
    var iTop = 0 //距离顶部距离,单位 mm
    var width = 0  //宽度
    var height = 0  //高度
    var imageData = ""    //图片数据
}