package com.yannuo.dgcanteen.activitys.fragment

import android.os.CountDownTimer
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.PayResultAdapter
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.FragmentOrderSFBinding
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.printer.PrinterOperator
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil


open class OrderSFFragment() : BaseFragment<FragmentOrderSFBinding>() {


    private lateinit var model : ProductsVM
    private var time = 0
    private var mPayResultAdapter: PayResultAdapter? = null
    private var countDownTimer: CountDownTimer? = null

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOrderSFBinding.inflate(inflater, container, false)
    }


    override fun onInit() {
        initObject()
        initView()
        initEvent()
        loadData()
    }





    private fun loadData(){

    }


    private fun initObject() {
        val kv = MMKV.defaultMMKV()
        time = kv.decodeInt(Constant.SHOW_TIME)
        model = ViewModelProvider(requireActivity()).get(ProductsVM::class.java)
        binding.subS.rvDishList.layoutManager = LinearLayoutManager(context)
        mPayResultAdapter = PayResultAdapter(requireContext())
        binding.subS.rvDishList.adapter = mPayResultAdapter


    }


    private fun initView() {
        LogUtil.i(TAG,"initView")
//        val gridLayoutManager = GridLayoutManager(context,6,)
//        binding.rvManInfo.layoutManager = gridLayoutManager
//        binding.rvManInfo.adapter = adapter
        model.uiData.observe(this){
            if (it.result == PayResultForUI.Result.FAIL) {
                updateFChange(it)
            }else updateSChange(it)
//            LogUtil.i(TAG,"initView")
        }
    }

    private fun release(){
//        adapter.setListener(null)
//        model.loadDialog.value = false
    }



    private fun initEvent() {
//        if (time < 0) {
        binding.subS.btBack.setOnClickListener { v ->
            back()
        }
        binding.subF.btBack.setOnClickListener { v ->
            back()
        }
//        }
    }

    private fun back() {
        model.tab.postValue(0)
        model.getDisplay()?.also {
            if (it.isShowing.not()) {
                it.show()
            }
        }
    }


    private fun updateFChange(mPayResult : PayResultForUI){
        binding.subF.btBack.isEnabled = true
        binding.subS.root.visibility = View.GONE
        if (binding.subF.root.visibility == View.GONE){
            binding.subF.root.visibility = View.VISIBLE
        }
        mPayResult.errormsg?.also {
            binding.subF.payFailMsg.text = it
        }


        mPayResult.timestamp?.also {
            val buffer = StringBuffer()
            buffer.append(it.substring(0, 4))
                .append("-")
                .append(it.substring(4, 6)).append("-")
                .append(it.substring(6, 8))
                .append(" ")
                .append(it.substring(8, 10))
                .append(":")
                .append(it.substring(10, 12))
                .append(":")
                .append(it.substring(12)).toString()
            binding.subF.payTime.text = buffer.toString()
        }
        CommonAndDpToPxUtil.speakWork("支付失败")
        startTime(mPayResult)
    }

    private fun updateSChange(mPayResult : PayResultForUI){
        binding.subS.btBack.isEnabled = true
        binding.subF.root.visibility = View.GONE;
        if (binding.subS.root.visibility == View.GONE){
            binding.subS.root.visibility = View.VISIBLE
        }

        if (mPayResult.way.equals("20") || mPayResult.way.equals("21")){
            mPayResultAdapter!!.data = mPayResult.dishes
            binding.subS.tvSum.text = "${mPayResult.piece} 件"
            binding.subS.payTotalMoney.text = "￥ ${mPayResult.payment} 元"
            var str ="支付宝"
            if (mPayResult.way.equals("20"))str ="微信"
            CommonAndDpToPxUtil.speakWork("${str}收款${mPayResult.payment } 元")
            var time = mPayResult.timestamp
            if (time.isNullOrEmpty().not()  && mPayResult.way != "人脸支付") {
                val buffer = StringBuffer()
                time = buffer.append(mPayResult.timestamp?.substring(0, 4))
                    .append("-")
                    .append(mPayResult.timestamp?.substring(4, 6))
                    .append("-")
                    .append(mPayResult.timestamp?.substring(6, 8))
                    .append(" ")
                    .append(mPayResult.timestamp?.substring(8, 10))
                    .append(":")
                    .append(mPayResult.timestamp?.substring(10, 12))
                    .append(":")
                    .append(mPayResult.timestamp?.substring(12)).toString()
            }
            binding.subS.tvPayTime.text = time
            binding.subS.tvTransNumber.text = mPayResult.traceid

        }else {

            val persons = DishesDBHelper.getInstance().queryPersonToCustId(mPayResult.custId)
            var cls = "***"
            if (persons != null) {
                cls = persons.grade + "(" + persons.userClass + ")"
            }
            mPayResultAdapter!!.data = mPayResult.dishes
            binding.subS.tvSum.text = "${mPayResult.piece} 件"

            binding.subS.payTotalMoney.text = "￥ ${mPayResult.payment} 元"
            CommonAndDpToPxUtil.speakWork("收款${mPayResult.payment} 元")
            binding.subS.tvClass.text = cls
            cls = mPayResult.cust_name.toString()
            if (TextUtils.isEmpty(mPayResult.cust_name)) {
                cls = "***"
            }
            binding.subS.tvName.text = cls
            var time = mPayResult.timestamp
            if (time.isNullOrEmpty().not() && mPayResult.way != "人脸支付") {
                val buffer = StringBuffer()
                time = buffer.append(mPayResult.timestamp?.substring(0, 4))
                    .append("-")
                    .append(mPayResult.timestamp?.substring(4, 6))
                    .append("-")
                    .append(mPayResult.timestamp?.substring(6, 8))
                    .append(" ")
                    .append(mPayResult.timestamp?.substring(8, 10))
                    .append(":")
                    .append(mPayResult.timestamp?.substring(10, 12))
                    .append(":")
                    .append(mPayResult.timestamp?.substring(12)).toString()
            }
            binding.subS.tvPayTime.text = time
            binding.subS.tvTransNumber.text = mPayResult.orderid
            val cont = (if (mPayResult.acc_bal.isNullOrEmpty()) "" else mPayResult.acc_bal) + "元"
            binding.subS.tvBalance.text = cont
        }
//        CommonAndDpToPxUtil.speakWork("欢迎用餐")
        PrinterOperator.printerFoodsList(mPayResult)
        startTime(mPayResult)
    }

    fun startTime(mPayResult : PayResultForUI){
        var totalTime = 0
        if (time >= 0) totalTime = time
        if (mPayResult.way == "人脸支付") {
            if (mPayResult.result === PayResultForUI.Result.FAIL)
                binding.subF.btBack.isEnabled = false
            else binding.subS.btBack.isEnabled = false
            if (time < 3) totalTime = 3
        }
        dida(totalTime,mPayResult)
    }


    private fun dida(tm: Int, mPayResult: PayResultForUI) {
        if (tm == 0) return
        countDownTimer = object : CountDownTimer((tm * 1000 + 100).toLong(), 1000) {
            override fun onTick(mil: Long) {
                if (mPayResult.result === PayResultForUI.Result.FAIL)
                    binding.subF.btBack.text = "返回${ mil / 1000}秒"
                else binding.subS.btBack.text = "返回${ mil / 1000}秒"

                if (mil / 1000 == (tm - 3).toLong()) {
                    if (mPayResult.way == "人脸支付") {
                        if (mPayResult.result === PayResultForUI.Result.FAIL) {
                            binding.subF.btBack.isEnabled = true
                        } else {
                            binding.subS.btBack.isEnabled = true
                        }
                    }
                }
            }

            override fun onFinish() {
                if (time < 0) {
                    if (mPayResult.result === PayResultForUI.Result.FAIL) {
                        binding.subF.btBack.isEnabled = true
                        binding.subF.btBack.text = "返回"
                    } else {
                        binding.subS.btBack.isEnabled = true
                        binding.subS.btBack.text = "返回"
                    }
                } else {
                    back()
                }
            }
        }
        countDownTimer?.start()
    }




}