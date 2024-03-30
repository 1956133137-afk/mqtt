package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.adapters.PayResultAdapter;
import com.yannuo.dgcanteen.dao.Persons;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.databinding.PayFailureBinding;
import com.yannuo.dgcanteen.databinding.PaySuccessBinding;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.PayResultForUI;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogUtil;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;

import androidx.recyclerview.widget.LinearLayoutManager;

public class PayResultDisplay extends Presentation {

    private PaySuccessBinding mBinding;  //成功
    private PayFailureBinding mFailBinding;  //失败
    private String TAG = getClass().getSimpleName();


    private PayResultAdapter mPayResultAdapter;
    private PayResultForUI mPayResult;
    private CountDownTimer countDownTimer;
    private int time = 0;
    private volatile boolean sendCancel = false;

    public PayResultDisplay(Context outerContext, PayResultForUI payResult , Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mPayResult = payResult;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MMKV kv = MMKV.defaultMMKV();
        time = kv.decodeInt(Constant.SHOW_TIME);

        if (mPayResult.getResult() == PayResultForUI.Result.FAIL){
            mFailBinding = PayFailureBinding.inflate(getLayoutInflater());
            setContentView(mFailBinding.getRoot());
            initFail();
        }else {
            mBinding = PaySuccessBinding.inflate(getLayoutInflater());
            setContentView(mBinding.getRoot());
            initData();
            initEvent();
            initView();
        }
        int totalTime = 0;
        if (time >= 0)totalTime = time;
        if (mPayResult.getWay().equals("人脸支付")) {
            if (mPayResult.getResult() == PayResultForUI.Result.FAIL)
                mFailBinding.btBack.setEnabled(false);
            else mBinding.btBack.setEnabled(false);
            if (time < 3) totalTime = 3;
        }
        dida(totalTime);
    }

    private void initFail() {
        if (!TextUtils.isEmpty(mPayResult.getErrormsg())) {
            mFailBinding.payFailMsg.setText(mPayResult.getErrormsg());
        }
        if (!TextUtils.isEmpty(mPayResult.getTimestamp())) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(mPayResult.getTimestamp().substring(0,4))
                    .append("-")
                    .append(mPayResult.getTimestamp().substring(4,6)).append("-")
                    .append(mPayResult.getTimestamp().substring(6,8))
                    .append(" ")
                    .append(mPayResult.getTimestamp().substring(8,10))
                    .append(":")
                    .append(mPayResult.getTimestamp().substring(10,12))
                    .append(":")
                    .append(mPayResult.getTimestamp().substring(12)).toString();
            mFailBinding.payTime.setText(buffer.toString());
        }
        CommonAndDpToPxUtil.speakWork("支付失败");
        initFailEvent();
    }

    private void initFailEvent() {
        if (time < 0) {
            mFailBinding.btBack.setOnClickListener(v -> {
                back();
            });
        }
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
        mPayResultAdapter.setData(mPayResult.getDishes());
        mBinding.tvSum.setText(""+mPayResult.getPiece()+"件");
        mBinding.payTotalMoney.setText(String.format("￥ %s 元",mPayResult.getPayment()));

        if (mPayResult.getWay().equals("20")||mPayResult.getWay().equals("21")){
            String str ="支付宝收款";
            if (mPayResult.getWay().equals("20"))str ="微信收款";
            CommonAndDpToPxUtil.speakWork(str + mPayResult.getPayment()+"元");
            mBinding.tvTransNumber.setText(mPayResult.getTraceid());
        }else {
            Persons persons = DishesDBHelper.getInstance().queryPersonToCustId(mPayResult.getCustId());
            String cls = "***";
            if (persons != null){
                cls = persons.getGrade() + "("+persons.getUserClass()+")";
            }
            mBinding.tvClass.setText(cls);
            cls = mPayResult.getCust_name();
            if (TextUtils.isEmpty(mPayResult.getCust_name())){
                cls ="***";
            }
            mBinding.tvName.setText(cls);
            CommonAndDpToPxUtil.speakWork("已支付"+mPayResult.getPayment()+"元");
            mBinding.tvTransNumber.setText(mPayResult.getOrderid());
            String cont = (mPayResult.getAcc_bal().isEmpty() ? "" :mPayResult.getAcc_bal()) + "元";
            mBinding.tvBalance.setText( cont);
        }

        String time = mPayResult.getTimestamp();
        if (!mPayResult.getTimestamp().isEmpty() && !mPayResult.getWay().equals("人脸支付")){
            StringBuffer buffer = new StringBuffer();
            time = buffer.append(mPayResult.getTimestamp().substring(0,4))
                    .append("-")
                    .append(mPayResult.getTimestamp().substring(4,6))
                    .append("-")
                    .append(mPayResult.getTimestamp().substring(6,8))
                    .append(" ")
                    .append(mPayResult.getTimestamp().substring(8,10))
                    .append(":")
                    .append(mPayResult.getTimestamp().substring(10,12))
                    .append(":")
                    .append(mPayResult.getTimestamp().substring(12)).toString();
        }
        mBinding.tvPayTime.setText(time);

    }

    private void initEvent() {
        if (time < 0) {
            mBinding.btBack.setOnClickListener(v -> {
                back();
            });
        }
    }

    private void back(){
        if (sendCancel)return;
        sendCancel = true;
        EventBus.getDefault().post(new MessageEvent(Constant.EVENT_THIRD,null));
    }


    private void dida(int tm){
        if (tm == 0)return;
        countDownTimer = new CountDownTimer(tm * 1000 +100, 1000) {
            @Override
            public void onTick(long mil) {
                if (mPayResult.getResult() == PayResultForUI.Result.FAIL)
                    mFailBinding.btBack.setText("返回"+(mil)/1000+"秒");
                else
                    mBinding.btBack.setText("返回"+(mil)/1000+"秒");
            }

            @Override
            public void onFinish() {
                if (time < 0) {
                    if (mPayResult.getResult() == PayResultForUI.Result.FAIL) {
                        mFailBinding.btBack.setEnabled(true);
                        mFailBinding.btBack.setText("返回");
                    } else {
                        mBinding.btBack.setEnabled(true);
                        mBinding.btBack.setText("返回");
                    }
                }else {
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
        LogUtil.i(TAG,"stop...");
        super.onStop();
    }
}
