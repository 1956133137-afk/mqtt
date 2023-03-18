package com.yannuo.dgcanteen.views

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import com.yannuo.dgcanteen.R
import com.yannuo.paylib.model.PayResultForUI

class PayFinishDialog(context :Context,dat : PayResultForUI) :BaseDialog(context, R.layout.dialog_pay_finish) {
    private val TAG = javaClass.simpleName
    private lateinit var tv_close : ImageButton
    private lateinit var tv_count  :TextView
    private lateinit var tv_title :TextView
    private lateinit var tv_time :TextView
    private lateinit var tv_order_number :TextView
    private lateinit var tv_pay_way :TextView
    private lateinit var tv_pay_account :TextView
    private lateinit var tv_product_name :TextView
    private lateinit var tv_error_hit :TextView
    private lateinit var tv_full_names :TextView
    private var data : PayResultForUI

    init {
        data = dat
    }


    override fun initViewAndEvent() {
        kotlin.runCatching {
            tv_close = findViewById(R.id.ib_close)
            tv_title = findViewById(R.id.tv_payment_result)
            tv_time = findViewById(R.id.tv_payment_time)
            tv_order_number = findViewById(R.id.tv_order_number)
            tv_pay_way = findViewById(R.id.tv_mode_of_payment)
            tv_pay_account = findViewById(R.id.tv_payment_account)
            tv_count = findViewById(R.id.tv_product_count)
            tv_product_name = findViewById(R.id.tv_product_name)
            tv_error_hit = findViewById(R.id.tv_error_hit)


            tv_close.setOnClickListener(this)
            tv_product_name.setOnClickListener(this)

            if (data.result == PayResultForUI.Result.FAIL) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tv_title.setTextColor(context.getColor(R.color.red))
                }
                tv_title.text = "支付失败"
            }
            tv_time.text = data.timestamp
            tv_order_number.text = data.orderid
            tv_pay_way.text = data.way
            tv_pay_account.text = "￥${data.amount}"
            tv_count.text = "${data.piece} 件"
            tv_product_name.text = data.cmdty_nm
            tv_error_hit.text = data.errormsg
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ib_close -> cancel()
            R.id.tv_product_name -> {
                kotlin.runCatching {
                val popupWindow = PopupWindow()
                val view = LayoutInflater.from(context).inflate(R.layout.popwindow_name_list,null,false)
                tv_full_names = view.findViewById(R.id.tv_names_list)
                val str = data.cmdty_nm
                tv_full_names.text = str!!.replace(";","\r\n")
                popupWindow.contentView = view
                popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
                popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
                popupWindow.isTouchable = true
                popupWindow.setBackgroundDrawable( ColorDrawable())
                popupWindow.isOutsideTouchable = true
                popupWindow.showAtLocation(v,Gravity.CENTER,0,0)
                }
            }
        }

    }


}