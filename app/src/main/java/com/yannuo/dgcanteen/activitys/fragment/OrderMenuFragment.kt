package com.yannuo.dgcanteen.activitys.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ccb.smartcanteen.ZHSTFacePayService
import com.yannuo.dgcanteen.activitys.presenters.OrderMenuPresenter
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.CategoryAdapter
import com.yannuo.dgcanteen.adapters.PayForAdapter
import com.yannuo.dgcanteen.adapters.ProductsAdapter
import com.yannuo.dgcanteen.databinding.FragmentOrderMenuBinding
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper
import com.yannuo.dgcanteen.greendao.entity.CategoryTable
import com.yannuo.dgcanteen.greendao.entity.DishesTable
import com.yannuo.dgcanteen.interfaces.CallbackListener
import com.yannuo.dgcanteen.interfaces.CloseEvent
import com.yannuo.dgcanteen.interfaces.IProductsVM
import com.yannuo.dgcanteen.interfaces.ReadCardListener
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.model.PayForUI
import com.yannuo.dgcanteen.model.ProductsDetail
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import com.yannuo.dgcanteen.views.HintDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * 点餐模式2：左侧购物车栏
 */
open class OrderMenuFragment : BaseFragment<FragmentOrderMenuBinding>(), ProductsAdapter.WorkListener,
    PayForAdapter.WorkListener, CallbackListener, IProductsVM, CategoryAdapter.WorkListener{
    private lateinit var mAdapter: ProductsAdapter
    private lateinit var mCategory: CategoryAdapter
    private lateinit var model: ProductsVM
    private lateinit var mPresenter: OrderMenuPresenter
    private val payViewModel by lazy { ViewModelProvider(requireActivity())[PayViewModel::class.java] }
    private lateinit var mAdapterPayFor: PayForAdapter
    private val MONEY_FMT = "￥ %s"
    private val COUNT_FMT = "%s 件"
    private var mealIds = 0

    private var categoryList: MutableList<DishesInfo>? = null

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
        EventBus.getDefault().register(this)
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
        mCategory = CategoryAdapter()
        mCategory.setListener(this)
        model = ViewModelProvider(requireActivity()).get(ProductsVM::class.java)
        model.listener = this
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvSelectItem.layoutManager = linearLayoutManager

        val gridLayoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvManInfo.layoutManager = gridLayoutManager
        mAdapter.setImgSize(gridLayoutManager)

        val categoryLayoutManager = LinearLayoutManager(requireContext(),RecyclerView.HORIZONTAL,false)
        binding.rvCategory.layoutManager = categoryLayoutManager
    }

    private fun openIcQr() {
        payViewModel.openPayStatus(Constant.PAY_CODE_IC_TYPE)
        if (state_opened) return
        state_opened = true
        payViewModel.listener = this
    }

    private fun closeIcQr() {
        state_opened = false
        payViewModel.closePayStatus()
    }


    //更新购物车UI
    private fun updateUiItems(it: MutableList<DishesInfo>) {
        mAdapterPayFor.data = it
        binding.rvSelectItem.scrollToPosition(it.size - 1) //插入数据后滑动到底部
        val res: FloatArray = mPresenter.calculate(it)
        binding.tvTotalMoney.text = res[0].toString()
        //数量
        binding.tvTotalCount.text = res[1].toString().replace(".0", "")
    }


    private fun initView() {
        binding.rvSelectItem.adapter = mAdapterPayFor
        binding.rvManInfo.adapter = mAdapter
        binding.rvCategory.adapter = mCategory
    }


    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        closeIcQr()
        requireActivity().unbindService(mServiceConnection)
        super.onDestroy()
    }

    private fun initEvent() {
        model.menuChange.observe(this) {
            mealIds = it
            mPresenter.dishesData(mAdapter, it, this)
            mPresenter.categoryData(mCategory,it)
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
        binding.MenuAll.setOnClickListener {
            updateMenu()
        }
    }

    fun setCategoryList(dataList: MutableList<DishesInfo>) {
        this.categoryList = dataList
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


    fun clearShoppingCart() {
        for (u in mAdapter.data) {
            if (u.count != 0) {
                u.count = 0
                mAdapter.notifyItemChanged(mAdapter.data.indexOf(u), "count")
            }
        }
        mAdapterPayFor.clear()
        model.clearDishes()
        binding.tvTotalMoney.text = ""
        binding.tvTotalCount.text = "0"
        changeDishPlay(mAdapterPayFor.data, String.format(MONEY_FMT, "0.00"), String.format(COUNT_FMT, "0"))
    }

    //菜品回调
    override fun onEventClick(position: Int) {
        //获取到添加的菜品
        val data = mAdapter.getData(position)
        val selectDateCategoryDish = model.selectDateCategoryDish(data)
        //选择的购买商品添加到购物车
        updateUiItems(selectDateCategoryDish)
        //副屏显示
        changeDishPlay(
            mAdapterPayFor.data, String.format(MONEY_FMT, binding.tvTotalMoney.text),
            String.format(COUNT_FMT, binding.tvTotalCount.text)
        )
    }

    //购物车菜品回调
    override fun onEventClick(data: DishesInfo) {
        categoryList?.forEach {
            if(it.dishesId == data.dishesId) it.count = data.count
        }
        mAdapter.setData(categoryList)
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

    private fun updateMenu(){
        val list = DishesDBHelper.getInstance(context).queryDishesByMealIdAneStatus(mealIds, 1)
        val tabelToInfo = tabelToInfo(list)
        this.categoryList = tabelToInfo
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: MessageEvent) {
        // 处理接收到的事件
        when (event.code) {
            Constant.EVENT_FIFTH -> {
                Log.d(TAG, "eventArrive: 更新菜品")
                // 清空购物车
                if (mAdapterPayFor.data.size >= 1){
                    clearShoppingCart()
                }
                // 更新菜品页面
                updateMenu()
            }
        }
    }

    //菜品类别回调
    override fun onEventClickCategory(category: CategoryTable) {
        val list: List<DishesTable>? = DishesDBHelper.getInstance(context).queryDishesByMealIdAneStatusAnCategory(category.mealId, 1, category.categoryName)
        val tabelToInfo = tabelToInfo(list)
        this.categoryList = tabelToInfo
    }

    private fun tabelToInfo(list: List<DishesTable>?): MutableList<DishesInfo> {
        val dataList: MutableList<DishesInfo> = ArrayList()
        val dishesList = model.getDishesList()
        if (list != null) {
            for (u in list) {
                var count = 0
                for(n in dishesList){
                    if(u.dishesId == n.dishesId) count = n.count
                }
                val imgUrl = if (u.imgUrl == null || u.imgUrl.isEmpty()) "" else u.imgUrl
                dataList.add(
                    DishesInfo(
                        u.dishesId,
                        u.dishesName,
                        u.mealId,
                        null,
                        u.price,
                        u.unit,
                        imgUrl,
                        1,
                        count
                    )
                )
            }
        }
        mAdapter.setData(dataList)
        return dataList
    }
}

