package com.yannuo.dgcanteen.activitys.fragment

import android.app.job.JobScheduler
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.FragmentBasicSettingBinding
import com.yannuo.dgcanteen.download.CheckVersionWorker
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.nets.RetrofitClient
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.serialtool.YNSerialPortFinder
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

/**
 * Author: filowl
 * Description: ***
 * Date: 2023/7/29 15:48
 **/
class BasicSettingFragment : Fragment() {
    private lateinit var binding: FragmentBasicSettingBinding
    private lateinit var kv: MMKV
    private val mContext = MyApplication.applicationContext
    private lateinit var portAdapter : ArrayAdapter<String>
    private lateinit var baudrateAdapter : ArrayAdapter<String>
    private lateinit var spinnerAdapter : ArrayAdapter<String>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBasicSettingBinding.inflate(inflater, container, false)
        initObject()
        initData()
        initEvent()
        cardFormat()
        return binding.root
    }

    private fun initObject() {
        kv = MMKV.defaultMMKV()
    }

    private fun initData() {
        try {
            binding.tvVersion.text =
                mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
        reload()
    }

    private fun initEvent() {
        binding.switchLine.setOnClickListener { //离线模式
            kv.encode(Constant.SWITCH, binding.switchLine.isChecked)
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_OFF_CHANGE, null))
        }

        binding.cbPrinterEnable.setOnClickListener {
            kv.encode(Constant.EN_PRINTER, binding.cbPrinterEnable.isChecked)
        }


        binding.btnVersion.setOnClickListener { view: View? ->  //版本更新
            val work = PeriodicWorkRequest.Builder(
                CheckVersionWorker::class.java,
                15,
                TimeUnit.MINUTES
            ).build()
            ToastShowUtil.show(mContext, "正在检查版本是否需要更新~")
            WorkManager.getInstance(mContext).enqueueUniquePeriodicWork(
                Constant.PERIODIC_WORK_KEY,
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
        }
        binding.btnExitAlive.setOnClickListener { view: View? ->  //退出保活
            // 创建 JobScheduler
            val jobScheduler =
                mContext!!.getSystemService(AppCompatActivity.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler?.cancelAll()
            ToastShowUtil.show("已取消保活")
        }


        binding.snPrinterPort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long) {
                (parent?.getItemAtPosition(position) as? String)?.also {
                    kv.encode(Constant.PRINTER_PATH_SET, it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.snPrinterBaudrate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                (parent?.getItemAtPosition(position) as? String)?.also {
                    kv.encode(Constant.PRINTER_BAUD_SET,it);
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


    }

    private fun reload() {
        val bauds = resources.getStringArray(R.array.baud) //波特率

        val portFinder = YNSerialPortFinder()
        val options = portFinder.allDevicesPath
        val card = resources.getStringArray(R.array.spiCard)
        portAdapter = ArrayAdapter<String>(requireContext(), R.layout.item_text,options)
        baudrateAdapter = ArrayAdapter<String>(requireContext(), R.layout.item_text,bauds)
        spinnerAdapter = ArrayAdapter<String>(requireContext(), R.layout.item_text,card)

        binding.snCardformat.adapter = spinnerAdapter
        binding.snPrinterPort.adapter = portAdapter
        binding.snPrinterBaudrate.adapter = baudrateAdapter

        var cnt = kv.decodeString(Constant.PRINTER_PATH_SET)
        for (da in options.indices){
            if (options[da].equals(cnt)) {
                binding.snPrinterPort.setSelection(da)
                break
            }
        }

        cnt = kv.decodeString(Constant.PRINTER_BAUD_SET)
        for (da in bauds.indices){
            if (bauds[da].equals(cnt)) {
                binding.snPrinterBaudrate.setSelection(da)
                break
            }
        }

        cnt = kv.decodeInt(Constant.CARD_FORMAT).toString()
        for(da in card.indices){
            if (card[da].equals(cnt)){
                binding.snCardformat.setSelection(da)
            }
        }

        binding.etAddress.setText(kv.decodeString(Constant.ADDRESS))
        binding.switchLine.isChecked = kv.decodeBool(Constant.SWITCH, false)
        binding.cbPrinterEnable.isChecked = kv.decodeBool(Constant.EN_PRINTER, false)
        binding.etMqttAddress.setText(kv.decodeString(Constant.MQTT_ADDRESS))
        binding.etMqttAccount.setText(kv.decodeString(Constant.MQTT_ACCOUNT))
        binding.etMqttPassword.setText(kv.decodeString(Constant.MQTT_PASSWORD))
        binding.etShowTime.setText("${kv.decodeInt(Constant.SHOW_TIME)}")
        binding.awaitPayTime.setText("${kv.decodeInt(Constant.AWAIT_PAY_TIME, 30)}")
        binding.snCardformat.setSelection(kv.decodeInt(Constant.CARD_FORMAT))
    }

    fun save() {
        var flag = true
        if (kv.decodeString(Constant.ADDRESS) != binding.etAddress.text.toString()) {
            if (isValidUrl(binding.etAddress.text.toString())) {
                kv.encode(Constant.ADDRESS, binding.etAddress.text.toString())
                RetrofitClient.overLoad() //更新服务器地址
            } else {
                flag = false
                ToastShowUtil.show("服务器地址有误!!!")
            }
        }
        var change = true
        if (kv.decodeString(Constant.MQTT_ADDRESS) != binding.etMqttAddress.text.toString()) {
            if (isValidUrl(binding.etMqttAddress.text.toString())) {
                change = false
                kv.encode(Constant.MQTT_ADDRESS, binding.etMqttAddress.text.toString())
            } else {
                flag = false
                ToastShowUtil.show("MQTT服务地址有误!!!")
            }
        }
        if (kv.decodeString(Constant.MQTT_ACCOUNT) != binding.etMqttAccount.text.toString()) {
            change = false
            kv.encode(Constant.MQTT_ACCOUNT, binding.etMqttAccount.text.toString())
        }
        if (kv.decodeString(Constant.MQTT_PASSWORD) != binding.etMqttPassword.text.toString()) {
            change = false
            kv.encode(Constant.MQTT_PASSWORD, binding.etMqttPassword.text.toString())
        }
        if (!change) {
            //mqtt配置变更
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_NINTH, null))
        }
        kv.encode(Constant.SHOW_TIME, binding.etShowTime.text.toString().toInt())
        kv.encode(Constant.AWAIT_PAY_TIME, binding.awaitPayTime.text.toString().toInt())
        if (flag) ToastShowUtil.show("保存成功: ${mContext.filesDir.absolutePath}/mmkv")
    }
    //卡号格式选择
    private fun cardFormat(){
        binding.snCardformat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                kv.encode(Constant.CARD_FORMAT, position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }
    private fun isValidUrl(url: String): Boolean {
        val regex = Regex(
            """^(https|tcp|http)://(\w{2,6}\.\w{1,61}\.[a-zA-Z]{2,6}|localhost|\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?::\d+)?/?(?:\w+/)*$""",
            RegexOption.IGNORE_CASE
        )
        return regex.matches(url)
    }
}