package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.ccb.smartcanteen.ZHSTFacePayService;
import com.yannuo.dgcanteen.adapters.ShopsAdapter;
import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.databinding.PayFailureBinding;
import com.yannuo.dgcanteen.databinding.PaySuccessBinding;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.PayResultForUI;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;

import org.greenrobot.eventbus.EventBus;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

public class PayResultDisplay extends Presentation {

    private PaySuccessBinding mBinding;  //成功
    private PayFailureBinding mFailBinding;  //失败
    private String TAG = getClass().getSimpleName();


    private ShopsAdapter mShopsAdapter;
    private PayResultForUI mPayResult;

    public PayResultDisplay(Context outerContext, PayResultForUI payResult , Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mPayResult = payResult;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


    }

    private void initFail() {
        if (!TextUtils.isEmpty(mPayResult.getErrormsg())) {
            mFailBinding.payFailMsg.setText(mPayResult.getErrormsg());
        }
        if (!TextUtils.isEmpty(mPayResult.getTimestamp())) {
            mFailBinding.payTime.setText(mPayResult.getTimestamp());
        }
        CommonAndDpToPxUtil.speakWork("支付失败");
        initFailEvent();
    }

    private void initFailEvent() {
        mFailBinding.btBack.setOnClickListener(v -> {
           cancel();
        });
    }


    private void initData() {
//        mShopsAdapter = new ShopsAdapter(getContext());
//        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
//        binding.rvSecondDetail.setAdapter(mShopsAdapter);
//        binding.rvSecondDetail.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

    }

    private void initView() {

//        binding.tvPayMoney.setText("合计:￥ "+mDishes.getTotalMoney());
//        mShopsAdapter.setData(mDishes.getProducts());
    }

    private void initEvent() {


    }

}
