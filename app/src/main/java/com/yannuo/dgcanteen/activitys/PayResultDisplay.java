package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.adapters.PayResultAdapter;
import com.yannuo.dgcanteen.databinding.PayFailureBinding;
import com.yannuo.dgcanteen.databinding.PaySuccessBinding;
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper;
import com.yannuo.dgcanteen.greendao.entity.Persons;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.PayForUI;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogUtil;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;

import androidx.recyclerview.widget.LinearLayoutManager;

public class PayResultDisplay extends BaseDisplay {

    private PaySuccessBinding mBinding;  //成功
    private PayFailureBinding mFailBinding;  //失败
    private String TAG = getClass().getSimpleName();


    private PayResultAdapter mPayResultAdapter;
    private PayForUI mPayForUI;
    private CountDownTimer countDownTimer;
    private int time = 0;
    private volatile boolean sendCancel = false;

    public PayResultDisplay(Context outerContext, PayForUI payForUI, Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mPayForUI = payForUI;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MMKV kv = MMKV.defaultMMKV();
        time = kv.decodeInt(Constant.SHOW_TIME, 5);

        if (!mPayForUI.getResult().equals("Y")) {
            mFailBinding = PayFailureBinding.inflate(getLayoutInflater());
            setContentView(mFailBinding.getRoot());
            initFail();
        } else {
            mBinding = PaySuccessBinding.inflate(getLayoutInflater());
            setContentView(mBinding.getRoot());
            initData();
            initEvent();
            initView();
        }
        int totalTime = 0;
        if (time >= 0) totalTime = time;
        if (mPayForUI.getPayType().equals("1")) {
            if (mPayForUI.getResult().equals("N"))
                mFailBinding.btBack.setEnabled(false);
            else mBinding.btBack.setEnabled(false);
//            if (time < 3) totalTime = 3;
        }
        dida(totalTime);
    }

    private void initFail() {
        mFailBinding.payFailMsg.setText(mPayForUI.getErrMsg());
        mFailBinding.payTime.setText(mPayForUI.getPayTime());
        CommonAndDpToPxUtil.speakWork("支付失败");
        initFailEvent();
    }

    private void initFailEvent() {
//        if (time < 0) {
        mFailBinding.btBack.setOnClickListener(v -> {
            back();
        });
//        }
    }


    private void initData() {
        mPayResultAdapter = new PayResultAdapter(this.getContext());
        mBinding.rvDishList.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.rvDishList.setAdapter(mPayResultAdapter);

//        if (!kv.decodeBool(Constant.SWITCH)){
        CommonAndDpToPxUtil.speakWork("欢迎用餐");
//        }else{
//            CommonAndDpToPxUtil.speakWork("离线订单后续补扣");
//        }

    }

    private void initView() {
        mPayResultAdapter.setData(mPayForUI.getPaymentDishes());
        mBinding.tvSum.setText("" + mPayForUI.getPaymentDishes().size() + "件");

        String actualPayment = mPayForUI.getActualPayment().isEmpty() ? mPayForUI.getPayment() : mPayForUI.getActualPayment();
        mBinding.tvActualPayment.setText(String.format("￥ %s 元", actualPayment));
        String str = "";
        if (mPayForUI.getPayType().equals("1")) str = "刷脸支付";
        else if (mPayForUI.getPayType().equals("2")) str = "扫码支付";
        else str = "刷卡支付";
        CommonAndDpToPxUtil.speakWork(str + actualPayment + "元");

        mBinding.tvName.setText(mPayForUI.getUsername());
        mBinding.tvBalance.setText(mPayForUI.getAccBal().isEmpty() ? "" : mPayForUI.getAccBal() + "元");
        mBinding.tvPayment.setText(mPayForUI.getPayment().isEmpty() ? "" : mPayForUI.getPayment() + "元");
        if (mPayForUI.getDiscountMsg().isEmpty()) mBinding.tvDiscount.setVisibility(View.GONE);
        else {
            mBinding.tvDiscount.setVisibility(View.VISIBLE);
            mBinding.tvDiscount.setText(mPayForUI.getDiscountMsg());
        }
        mBinding.tvPayTime.setText(mPayForUI.getPayTime());
        mBinding.tvTransNumber.setText(mPayForUI.getOrderId().isEmpty() ? mPayForUI.getTraceId() : mPayForUI.getOrderId());
    }

    private void initEvent() {
//        if (time < 0) {
        mBinding.btBack.setOnClickListener(v -> {
            back();
        });
//        }
    }

    private void back() {
        if (sendCancel) return;
        sendCancel = true;
        EventBus.getDefault().post(new MessageEvent(Constant.EVENT_THIRD, null));
    }


    private void dida(int tm) {
        if (tm == 0) return;
        countDownTimer = new CountDownTimer(tm * 1000 + 100, 1000) {
            @Override
            public void onTick(long mil) {
                if (!mPayForUI.getResult().equals("Y")) mFailBinding.btBack.setText("返回" + (mil) / 1000 + "秒");
                else mBinding.btBack.setText("返回" + (mil) / 1000 + "秒");

                if ((mil / 1000) == (tm - 3)) {
                    if (mPayForUI.getPayType().equals("1")) {
                        if (!mPayForUI.getResult().equals("Y")) mFailBinding.btBack.setEnabled(true);
                        else mBinding.btBack.setEnabled(true);
                    }
                }
            }

            @Override
            public void onFinish() {
                if (time < 0) {
                    if (!mPayForUI.getResult().equals("Y")) {
                        mFailBinding.btBack.setEnabled(true);
                        mFailBinding.btBack.setText("返回");
                    } else {
                        mBinding.btBack.setEnabled(true);
                        mBinding.btBack.setText("返回");
                    }
                } else {
                    back();
                }
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onStop() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        LogUtil.i(TAG, "stop...");
        super.onStop();
    }
}
