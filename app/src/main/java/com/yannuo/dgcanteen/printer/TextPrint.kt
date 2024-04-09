package com.yannuo.dgcanteen.printer

/**
 * 添加一行文本
 * @property font Int
 * @property align Int
 * @property newline Boolean
 * @property needChangeLargeFont Boolean
 * @property text String
 */
class TextPrint {
     var font = 0  //字体， (0x00 标准 ASCII 12x24) (0x01 压缩 ASCII 9x17)
     var scaleW = 0 //
     var scaleH = 0//
     var style = 0x00   //(0x00 正常) (0x08 加粗)
     var align = 0  //对齐方式， 默认左对齐， 0:left, 1:center, 2:right
     //newline 为 false 时， 只有 text 检查到/n 才开始打印。
//     var newline = true  //是否换行， true-正常打印（默认） ；false-mixText 打印
//     var needChangeLargeFont = false  //是否修改大字体， true：改大字体的宽度 normal 样式， 高度为 large 样式； false： 不修改， 按照原有 large 字体打印
     var text = "" //内容
}