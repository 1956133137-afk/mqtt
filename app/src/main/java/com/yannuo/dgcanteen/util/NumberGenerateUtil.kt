package com.yannuo.dgcanteen.util

import android.os.Build
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom

object NumberGenerateUtil {
    private val TAG = javaClass.simpleName

    private fun getRandomNumberString(len: Int): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
           val mRandom = ThreadLocalRandom.current()
            var start = "1"
            var end = ""
            for (i in 1 until len) {
                start += "0"
            }
            end = start + "0"
            return mRandom.nextLong(
                start.toLong(),
                end.toLong()
            ).toString()
        }
        return Random().nextInt(100).toString()
    }


    private fun snToNumber():String{
        val content = StringBuilder()
        val sn = CommonAndDpToPxUtil.getDeviceSerial().substring(0,7)
//        for (i in sn.indices){
//           content.append(sn.substring(i,i+1).toInt(16))
//        }
        val re = 8 - content.length
        for (i in 0 until re){
            content.append(0)
        }
        LogUtil.d(TAG,"$content")
        return content.toString()
    }

    private fun timeStr():String{
        val sDateFormat = SimpleDateFormat("yyMMdd")
        val sDateTimeFormat = SimpleDateFormat("HHmmss")

        val minDate = sDateTimeFormat.format(Date())
        val date = sDateFormat.format(Date())
//        date = date.substring(2)
         LogUtil.d(TAG,"$date$minDate")
        return "$date$minDate"
    }


    fun getOrderNumber(len :Int = 23): String{
        var value =  snToNumber()
        value += timeStr()
        value += getRandomNumberString(len - value.length)
        LogUtil.d(TAG, value)
        return value
    }


}