package com.yannuo.dgcanteen.activitys.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ccb.smartcanteen.ZHSTFacePayService
import com.yannuo.dgcanteen.activitys.presenters.OrderMenuPresenter
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.PayForAdapter
import com.yannuo.dgcanteen.adapters.ProductsAdapter
import com.yannuo.dgcanteen.databinding.FragmentOrderMenuBinding
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.interfaces.ReadCardListener
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.HintDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

open class OrderMenuFragment() : BaseFragment<FragmentOrderMenuBinding>(), ProductsAdapter.WorkListener,
    PayForAdapter.WorkListener, CallbackListener, IProductsVM {
    private lateinit var mAdapter: ProductsAdapter
    private lateinit var model: ProductsVM
    private lateinit var mPresenter: OrderMenuPresenter
    private val payViewModel by lazy { ViewModelProvider(requireActivity())[PayViewModel::class.java] }
    private lateinit var mAdapterPayFor: PayForAdapter
    private val MONEY_FMT = "￥ %s"
    private val COUNT_FMT = "%s 件"

    //    private var secondLoadingDialog :LoadingDialog ?= null
    @Volatile
    private var state_opened = false
    private var mHintDialog: HintDialog? = null
    private var mFacePayService: ZHSTFacePayService? = null

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, "onServiceConnected")
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.d(TAG, " onServiceDisconnected")
        }
    }


    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentOrderMenuBinding.inflate(inflater, container, false)
    }


    override fun onInit() {
        initObject()
        initView()
        initEvent()
        loadData()
    }


    private fun loadData() {
        val lIntent = Intent()
        lIntent.action = "com.ccb.smartcanteen.FacePayService"
        lIntent.setPackage("com.ccb.smartcanteen")
        requireContext().bindService(lIntent, mServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }


    private fun initObject() {
        mPresenter = OrderMenuPresenter(requireContext())
        mAdapterPayFor = PayForAdapter()
        mAdapterPayFor.setListener(this)
        mAdapter = ProductsAdapter(context)
        mAdapter.setListener(this)
        model = ViewModelProvider(requireActivity()).get(ProductsVM::class.java)
        model.listener = this
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvSelectItem.layoutManager = linearLayoutManager

        val gridLayoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvManInfo.layoutManager = gridLayoutManager
        mAdapter.setImgSize(gridLayoutManager)
    }

    private fun openIcQr() {
        payViewModel.openPayStatus()
        if (state_opened) return
        state_opened = true
        payViewModel.listener = this
    }

    private fun closeIcQr() {
        state_opened = false
        payViewModel.closePayStatus()
    }


    //更新购物车UI
    private fun updateUiItems(it: DishesInfo, accumulation: Boolean) {
        mAdapterPayFor.insertedData(it, accumulation)
        binding.rvSelectItem.scrollToPosition(mAdapterPayFor.data.size - 1) //插入数据后滑动到底部
        val res: FloatArray = mPresenter.calculate(mAdapterPayFor.data)
        binding.tvTotalMoney.text = res[0].toString()
        binding.tvTotalCount.text = res[1].toString().replace(".0", "")
    }


    private fun initView() {

        binding.rvSelectItem.adapter = mAdapterPayFor
        binding.rvManInfo.adapter = mAdapter


    }


    override fun onDestroy() {
        closeIcQr()
        requireActivity().unbindService(mServiceConnection)
        super.onDestroy()
    }

    private fun initEvent() {
        model.menuChange.observe(this) {
            mPresenter.dishesData(mAdapter, it)
        }

        binding.ibDelAll.setOnClickListener {
            if (mAdapterPayFor.data.size < 1) return@setOnClickListener
            clearShoppingCart()
        }

        binding.btPayWayFace.setOnClickListener {
            if (mAdapterPayFor.data.size < 1) return@setOnClickListener
            closeIcQr()
            if (mFacePayService == null) {
                ToastShowUtil.show("人脸服务连接异常")
                CommonAndDpToPxUtil.speakWork("人脸服务连接异常")
                LogUtil.e(TAG, "获取不到人脸句柄")
                return@setOnClickListener
            }
            val dat = MutableList(mAdapterPayFor.data.size) {
                mAdapterPayFor.data[it].copy()
            }

            val productsDetail = ProductsDetail(
                dat,
                binding.tvTotalMoney.text.toString(),
                binding.tvTotalCount.text.toString()
            )
            model.getDisplay()?.dismiss()
            model.startPayWithFace(mFacePayService, productsDetail)
        }

        binding.btPayWayIcOrCode.setOnClickListener {
            if (mAdapterPayFor.data.size < 1) return@setOnClickListener
            setFoodsPayList()
            model.getDisplay()?.showWaitHit(true)
            if (mHintDialog == null) {
                mHintDialog = HintDialog(requireContext()).apply {
                    this.setListener(object : CloseEvent {
                        override fun onEvent(code: Int, msg: String?) {
                            if (code == 0) payViewModel.setPayState(PayViewModel.PayStatus.INVALID)
                            model.getDisplay()?.showWaitHit(false)
                        }
                    })
                }
            }
            mHintDialog!!.show()
            openIcQr()
            CommonAndDpToPxUtil.speakWork("请刷卡或扫码")
        }
    }


    private fun setFoodsPayList() {
        //            binding.btSureMeal.setEnabled(false);
        val dat = MutableList(mAdapterPayFor.data.size) {
            mAdapterPayFor.data[it].copy()
        }
        val productsDetail = ProductsDetail(
            dat,
            binding.tvTotalMoney.text.toString(),
            binding.tvTotalCount.text.toString()
        )
        payViewModel.mDishes = productsDetail.copy()
    }


    private fun clearShoppingCart() {
        for (u in mAdapter.data) {
            if (u.count != 0) {
                u.count = 0
                mAdapter.notifyItemChanged(mAdapter.data.indexOf(u), "count")
            }
        }
        mAdapterPayFor.clear()
        binding.tvTotalMoney.text = ""
        binding.tvTotalCount.text = "0"
        changeDishPlay(mAdapterPayFor.data, String.format(MONEY_FMT, "0.00"), String.format(COUNT_FMT, "0"))
    }

    override fun onEventClick(position: Int) {
        val data = mAdapter.getData(position)
        //选择的购买商品添加到购物车
        updateUiItems(data, false)
        changeDishPlay(
            mAdapterPayFor.data, String.format(MONEY_FMT, binding.tvTotalMoney.text),
            String.format(COUNT_FMT, binding.tvTotalCount.text)
        )
    }

    override fun onEventClick(data: DishesInfo) {
        mAdapter.notifyItemChanged(mAdapter.data.indexOf(data), "count")
        val res: FloatArray = mPresenter.calculate(mAdapterPayFor.data)
        binding.tvTotalMoney.text = res[0].toString()
        binding.tvTotalCount.text = res[1].toString().replace(".0", "")
        changeDishPlay(
            mAdapterPayFor.data, String.format(MONEY_FMT, binding.tvTotalMoney.text),
            String.format(COUNT_FMT, binding.tvTotalCount.text)
        )
    }


    private fun changeDishPlay(data: MutableList<DishesInfo>, money: String, count: String) {
        model.getDisplay()?.also {
            it.upDataWithUi(data, money, count)
        }
    }


    class CardCallBack : ReadCardListener {
        override fun cardCallback(state: Boolean) {
            if (state) {
                CommonAndDpToPxUtil.speakWork("当前无网络，请打开设备离线模式")
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun onOtherListener(event: Int, any: Any?) {
        LogUtil.i(TAG, "扫码处理code: $event")
        when (event) {
            1 -> Observable.just(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { integer: Int? ->
                    model.getDisplay()?.showLoading()
                    mHintDialog?.dismiss()
                    closeIcQr()
                    model.getDisplay()?.showWaitHit(false)
                    model.loadingEvent.value = true
                }
            2 -> Observable.just(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { integer: Int? ->
                    model.loadingEvent.value = false
                    model.getDisplay()?.closeLoading()
                    closeIcQr()
                    Toast.makeText(context, any as String?, Toast.LENGTH_SHORT).show()
                }
            3, 4 -> {
                Observable.just(1)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { integer: Int? ->
                        model.loadingEvent.value = false
                        model.getDisplay()?.closeLoading()
                        mHintDialog?.dismiss()
                        closeIcQr()
                        model.getDisplay()?.showWaitHit(false)
                        clearShoppingCart()
                        model.getDisplay()?.dismiss()
                        model.tab.postValue(1)
                        model.uiData.postValue(any as PayForUI)
                    }
            }
            5 -> Observable.just(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { integer: Int? ->
                    if (any as Int == 1) {
                        CommonAndDpToPxUtil.speakWork("无效码，请刷新付款码再支付")
                    } else if (any == 2) {
                        CommonAndDpToPxUtil.speakWork("请检查网络,不支持离线聚合支付!")
                    } else if (any == 3) {
                        CommonAndDpToPxUtil.speakWork("非本园区人员")
                    } else {
                        CommonAndDpToPxUtil.speakWork("请切换离线码再支付")
                    }
                    model.loadingEvent.value = false
                    model.getDisplay()?.closeLoading()
                    closeIcQr()
                }
        }
    }

    override fun onFacePayResult(data: PayForUI) {
        LogUtil.d(TAG, "人脸支付结束，准备跳转结果展示~")
        Handler(Looper.getMainLooper()).postDelayed({
            clearShoppingCart()
            
            model.getDisplay()?.dismiss()
            model.tab.postValue(1)
            model.uiData.postValue(data)
        }, 300)
    }
}

