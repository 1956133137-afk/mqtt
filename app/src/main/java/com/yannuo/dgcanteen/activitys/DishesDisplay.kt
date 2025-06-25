package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.adapters.DishListAdapter
import com.yannuo.dgcanteen.databinding.DishesDisplayBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.views.LoadingDialog
import org.greenrobot.eventbus.EventBus

/**
 * Author: filowl
 * Description: 简易异现
 * Date: 2023/8/4 15:41
 **/
class DishesDisplay(context: Context, display: Display) : BaseDisplay(context, display) {
    private val TAG = javaClass.simpleName
    private lateinit var binding: DishesDisplayBinding
    private lateinit var kv: MMKV
    private lateinit var mAdapter: DishListAdapter
    private var secondLoadingDialog: LoadingDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
//        window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        super.onCreate(savedInstanceState)
        binding = DishesDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initEvent()

    }


    private fun initView() {
        kv = MMKV.defaultMMKV()
        var strText = kv.decodeString(Constant.TITLE_CONTENT, "")
        if (strText.isNullOrEmpty().not()) strText += "•"
        binding.tvFpTitle.text = "${strText}智慧食堂"

        val gridLayoutManager = GridLayoutManager(context, 6)
        mAdapter = DishListAdapter(context)
        binding.rvFoods.layoutManager = gridLayoutManager
        binding.rvFoods.adapter = mAdapter
        //        binding.btPayFace.requestFocus();
        if (kv.decodeInt(Constant.VERIFY_MODE) == 0) {
            binding.tvVerification.text = "  刷脸核销  "
        } else {
            binding.tvVerification.text = "  订餐核销  "
        }
        if (kv.decodeBool(Constant.CODE_VERIFICATION_SET, false)) {
            binding.tvVerification.visibility = View.VISIBLE
        } else {
            binding.tvVerification.visibility = View.GONE
        }
    }


    private fun initEvent() {
        binding.tvVerification.setOnClickListener {
            if (kv.decodeInt(Constant.VERIFY_MODE) == 0) {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE, null))
            } else {
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_CODE, null))
            }
        }
    }

    fun showLoading() {
        if (secondLoadingDialog == null) {
            secondLoadingDialog = LoadingDialog(this.context)
            secondLoadingDialog!!.window
                ?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        secondLoadingDialog?.show()
    }

    fun closeLoading() {
        secondLoadingDialog?.dismiss()
    }

    override fun dismiss() {
        closeLoading()
        super.dismiss()
    }

    fun showWaitHit(show: Boolean) {
        when (show) {
            true -> binding.tvPayWait.visibility = View.VISIBLE
            else -> binding.tvPayWait.visibility = View.GONE
        }
    }

    fun upDataWithUi(data: MutableList<DishesInfo>, money: String, count: String) {
        mAdapter.data = data
        binding.tvAmount.text = money
        binding.tvDishesCount.text = count
    }
}