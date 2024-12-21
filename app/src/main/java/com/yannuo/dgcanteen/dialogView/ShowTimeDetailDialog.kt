package com.yannuo.dgcanteen.dialogView

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.DialogMealTimeDetailBinding
import com.yannuo.dgcanteen.model.TimeDetail
import com.yannuo.dgcanteen.views.TipPopupWindow

/**
 * 餐别餐次详细
 */
class ShowTimeDetailDialog(context: Context) : BaseDialog<DialogMealTimeDetailBinding>(context) {

    private var meal01Window: TipPopupWindow? = null
    private var meal02Window: TipPopupWindow? = null
    private var meal03Window: TipPopupWindow? = null
    private var meal04Window: TipPopupWindow? = null

    override fun initDialogView() {
        binding = DialogMealTimeDetailBinding.inflate(layoutInflater)
    }

    override fun initOperation() {

        binding.meal01Tip.setOnClickListener {
            if (meal01Window == null) {
                meal01Window = TipPopupWindow(context)
            }
            //显示
            meal01Window?.showAtRight(it)
        }

        binding.meal02Tip.setOnClickListener {

        }

        binding.meal03Tip.setOnClickListener {

        }

        binding.meal04Tip.setOnClickListener {

        }

        binding.btnClose.setOnClickListener { cancel() }
    }



    @SuppressLint("SetTextI18n")
    fun setTimeDetail(map: HashMap<String, TimeDetail>) {
        if (map["meal01"] != null) {
            binding.meal01Info.visibility = View.VISIBLE
            binding.meal01Name.text = "${map["meal01"]!!.mealName}:"
            binding.meal01Time.text = "${map["meal01"]!!.allTime} 次 / ${map["meal01"]!!.externalTime} 次"
        } else binding.meal01Info.visibility = View.INVISIBLE

        if (map["meal02"] != null) {
            binding.meal02Info.visibility = View.VISIBLE
            binding.meal02Name.text = "${map["meal02"]!!.mealName}:"
            binding.meal02Time.text = "${map["meal02"]!!.allTime} 次 / ${map["meal02"]!!.externalTime} 次"
        } else binding.meal02Info.visibility = View.INVISIBLE

        if (map["meal03"] != null) {
            binding.meal03Info.visibility = View.VISIBLE
            binding.meal03Name.text = "${map["meal03"]!!.mealName}:"
            binding.meal03Time.text = "${map["meal03"]!!.allTime} 次 / ${map["meal03"]!!.externalTime} 次"
        } else binding.meal03Info.visibility = View.INVISIBLE

        if (map["meal04"] != null) {
            binding.meal04Info.visibility = View.VISIBLE
            binding.meal04Name.text = "${map["meal04"]!!.mealName}:"
            binding.meal04Time.text = "${map["meal04"]!!.allTime} 次 / ${map["meal04"]!!.externalTime} 次"
        } else binding.meal04Info.visibility = View.INVISIBLE
    }

}