package com.yannuo.dgcanteen.activitys

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.yannuo.dgcanteen.activitys.presenters.PayForPresenter
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.PayForAdapter
import com.yannuo.dgcanteen.databinding.PayforBinding
import com.yannuo.dgcanteen.model.PayResultForUI
import com.yannuo.dgcanteen.model.ProductInfo
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.lang.ref.WeakReference


class PayForFragment : Fragment(), PayForAdapter.WorkListener {
    private lateinit var binding : PayforBinding
    private lateinit var presenter : PayForPresenter
    private lateinit var model : ProductsVM
    private var TAG = javaClass.simpleName
    private lateinit var adapter :PayForAdapter
    private  var loadingDialog : LoadingDialog? =null
    private lateinit var scope :CoroutineScope
    private lateinit var handler : MyHandler
    private var waitDialog :WaitForPayDialog ?= null
    private var payQrCodeDialog :PayQRCodeDialog ?= null
    private var mProductsDir :String ?= null   //获取商品图片目录
    private var mPayJob :Job ?= null //支付job
    private var payTimeout = 60  //支付超时时间



    //
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PayforBinding.inflate(inflater, container, false)
        initObject()
        initView()
        initEvent()
        NumberGenerateUtil.getOrderNumber()

        return binding.root
    }



    private fun initObject() {
        mProductsDir =  context?.filesDir?.absolutePath.let {
            "$it/myPic/"
        }
        scope = CoroutineScope(Dispatchers.Default)
        handler = MyHandler(this)
        presenter = PayForPresenter(handler,context)
        adapter = PayForAdapter()
        adapter.setListener(this)
        val manager = LinearLayoutManager(context)
        binding.rvSelectItem.layoutManager = manager
        presenter.scanListener()  //监听扫码头数据





    }

    private fun initView() {
        binding.rvSelectItem.adapter = adapter
        model = ViewModelProvider(requireActivity()).get(ProductsVM::class.java)

        //选择的购买商品添加到购物车
        model.sendCountNotify.observe(viewLifecycleOwner) {
            LogUtil.d(TAG, it.filename)
            updateUiItems( it,false)
        }

        //吐司信息显示
        presenter.showToastEvent.observe(viewLifecycleOwner){
            ToastShowUtil.show(it)
        }
        //加载对话框显示
        presenter.loadingEvent.observe(viewLifecycleOwner){
            loadingDialog?.cancel()
            loadingDialog = LoadingDialog(requireContext())
            if (it)loadingDialog?.show()
            else {
                loadingDialog?.cancel()
                loadingDialog = null
            }
        }

    }

    private fun initEvent() {
        binding.ibDelAll.setOnClickListener {
            //1.判断购物车是否为空
            if (adapter.data.size < 1)return@setOnClickListener
            clearShoppingCart()
        }

        //建行熊猫被扫
        binding.btPayFace.setOnClickListener {
            if (checkAndGeneratePrinter()) return@setOnClickListener


        }
        //建行熊猫主扫
        binding.btPayQrcode.setOnClickListener {
            if (checkAndGeneratePrinter()) return@setOnClickListener

        }

       /* //建行聚合支付
        binding.btPayThree.setOnClickListener {
            if (checkAndGeneratePrinter()) return@setOnClickListener
        }*/
    }



    private fun checkAndGeneratePrinter(): Boolean {
        if (adapter.data.size < 1) {
            ToastShowUtil.show("请添加商品")
            CommonAndDpToPxUtil.speakWork("请添加商品")
            return true
        }

        return false
    }


    override fun onEventClick(data: ProductInfo) {
        model.receiveCountNotify.postValue(data)
        val res = presenter.calculate(adapter.data)
        binding.tvTotalMoney.text = "${res.get(0).toString()}元"
//        binding.tvTotalMoney.text = res.get(0).toString()
        binding.tvTotalCount.text = res.get(1).toInt().toString()
    }

    private fun clearShoppingCart() {
        //2.清空购物车
        adapter.clear()
        binding.tvTotalMoney.text ="0.0元"
        binding.tvTotalCount.text = "0"
        //3.重置商品列表角标
        model.receiveCountNotify.postValue(null)
    }

    //更新购物车UI
    private fun updateUiItems( it: ProductInfo,accumulation :Boolean){
        adapter.insertedData(it,accumulation)
        binding.rvSelectItem.scrollToPosition(adapter.data.size -1)  //插入数据后滑动到底部
        val res = presenter.calculate(adapter.data)
        binding.tvTotalMoney.text = "${res.get(0).toString()}元"
//        binding.tvTotalMoney.text = res.get(0).toString()
        binding.tvTotalCount.text = res.get(1).toInt().toString()
    }

    inner class MyHandler(context : PayForFragment) : Handler(){
        private var reference : WeakReference<PayForFragment> = WeakReference(context)
        private var finishDialog : PayFinishDialog ?= null

        override fun handleMessage(msg: Message) {
            val  ref = reference.get() ?: return
            when(msg.what){
                Constant.EVENT_ONE ->{ //弹出对话框，等待用户扫码
                    startWaitDialog()
                }
                Constant.EVENT_TWO ->{ //1.接收到扫码数据,关闭对话框
                    ref.waitDialog?.dismiss()


//                    ref.presenter.payMoney( ref.adapter.data,msg.obj as String,1)
                }
                Constant.EVENT_THREE ->{ //1.交易结果通知，展示结果

                    payQrCodeDialog?.cancel() //关闭扫码界面
                    val  data = msg.obj as PayResultForUI
                    finishDialog?.cancel()
                    finishDialog =  PayFinishDialog(ref.requireContext(),data)
                    finishDialog?.show()
                    //2.清空购物车




                    CommonAndDpToPxUtil.speakWork("支付失败")
                }
                Constant.EVENT_FOUR ->{
                    //展示扫码界面
                    val data = msg.obj as Bitmap
                    ref.startPayQrCodeDialog(data)
                }

                Constant.EVENT_FIVE ->{
                    //是商品条码
                    val product = ref.presenter.getBarcodeProduct(msg.obj as String)?.also {
                        val path = it.pictureName.let {
                            "$mProductsDir$it"
                        }
                        val data =  ProductInfo (
                            it.pName,
                            it.pMoney,
                            path,
                            it.type,
                            it.barCode
                        )
                        //1、更新购物车
                        updateUiItems( data,true)
                        //2、更新商品选择列表fragment
                        ref.model.receiveCountNotify.postValue(ref.adapter.getSpecifyBarcode(data.barCode))
                    }
                    if (product == null) ToastShowUtil.show("无效商品条码")
                }

            }
        }
    }




    /**
     * 使用了扫码
     */
    fun startWaitDialog(){
        waitDialog =  WaitForPayDialog(requireContext())
        waitDialog?.setListener(object : WaitForPayDialog.CloseEvent {
            override fun onEvent(code: Int, msg: String?) {
                mPayJob?.cancel()
                presenter.setScanState(PayForPresenter.ScanState.ENTERING)
            }
        })
        waitDialog?.show()
    }

    fun startPayQrCodeDialog(bitmap: Bitmap){
        payQrCodeDialog?.dismiss()
        payQrCodeDialog = PayQRCodeDialog(requireContext())
        payQrCodeDialog?.setListener(object : PayQRCodeDialog.CloseEvent {
            override fun onEvent(code: Int, msg: String?) {
                mPayJob?.cancel()
            }
        })
        payQrCodeDialog?.setQrCode(bitmap)
        payQrCodeDialog?.show()
    }


}