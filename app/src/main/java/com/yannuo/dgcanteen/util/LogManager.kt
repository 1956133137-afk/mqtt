package com.yannuo.dgcanteen.util

import android.os.Environment
import com.safframework.log.configL
import com.safframework.log.converter.gson.GsonConverter
import com.safframework.log.printer.FilePrinter
import com.safframework.log.printer.file.FileBuilder
import com.safframework.log.printer.file.clean.FileLastModifiedCleanStrategy
import com.safframework.log.utils.CrashUtils

object LogManager {

    const val PREFIX_APP = "recycle_machine-app"
    const val SEVEN_DAYS:Long = 5*24*3600*1000
    private val filePrinter: FilePrinter by lazy {
        var sdcardPath = "/sdcard/recycle_machine"
        val f = Environment.getExternalStorageDirectory()
        if (f != null) {
            sdcardPath = f.absolutePath + "/device-record/pay/log"
        }

        FileBuilder().folderPath(sdcardPath).cleanStrategy(FileLastModifiedCleanStrategy(SEVEN_DAYS)).build()
    }

    @JvmStatic
    fun initLog() {

        CrashUtils.init(tag = "crashTag",printer = filePrinter, onCrashListener = object : CrashUtils.OnCrashListener {
            override fun onCrash(crashInfo: String, e: Throwable) {

            }
        })

        configL {
            converter = GsonConverter()
        }.apply {
            addPrinter(filePrinter)
        }

    }
}