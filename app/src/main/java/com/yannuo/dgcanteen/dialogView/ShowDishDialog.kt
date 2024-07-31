//package com.yannuo.dgcanteen.dialogView
//
//import android.content.Context
//import android.os.CountDownTimer
//import com.google.gson.Gson
//import com.tencent.mmkv.MMKV
//import com.yannuo.dgcanteen.databinding.DialogShowDishBinding
//import com.yannuo.dgcanteen.util.Constant
//import com.yannuo.dgcanteen.util.LogUtil
//
//class ShowDishDialog(context: Context): BaseDialog<DialogShowDishBinding>(context) {
//    private val TAG = this.javaClass.simpleName
//    private var dishes: String? = null
//    private var mDownTimer: CountDownTimer? = null
//    override fun initDialogView() {
//        binding = DialogShowDishBinding.inflate(layoutInflater)
//    }
//
//    override fun initOperation() {
//        var stringBuilder = StringBuilder()
//        val dishesVerification = Gson().fromJson(dishes, Array<String>::class.java)
//        for (s in dishesVerification) {
//            stringBuilder.append("$s\n")
//        }
//        LogUtil.i(TAG,"dishes:$stringBuilder")
//        binding.tvDishes.text = stringBuilder
//        binding.btnClose.setOnClickListener {
//            dismiss()
//        }
//        mDownTimer = object : CountDownTimer(MMKV.defaultMMKV().decodeString(Constant.MEAL_TIME, "10")?.toInt()!! * 1000L, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                binding.btnClose.text = String.format("关闭(%ds)", millisUntilFinished / 1000)
//            }
//
//            override fun onFinish() {
//                dishes = null
//                dismiss()
//            }
//        }.start()
//    }
//    override fun dismiss() {
//        super.dismiss()
//        mDownTimer?.cancel()
//    }
//
//    fun showDishes(dish: String) {
//        dishes = dish
//    }
//
//}