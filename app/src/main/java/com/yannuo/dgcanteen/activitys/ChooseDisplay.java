package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ccb.smartcanteen.ZHSTFacePayService;
import com.yannuo.dgcanteen.activitys.presenters.ScanPayPresenter;
import com.yannuo.dgcanteen.adapters.ShopsAdapter;
import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;

import org.greenrobot.eventbus.EventBus;

public class ChooseDisplay extends Presentation {

    private ChooseSecondDisplayBinding binding;
    private String TAG = getClass().getSimpleName();

    private ZHSTFacePayService  mFacePayService = null;
    private ShopsAdapter mShopsAdapter;
    private ProductsDetail mDishes;

    public ChooseDisplay(Context outerContext, ProductsDetail dishes , Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDishes = dishes;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ChooseSecondDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initData();
        initEvent();
        initView();
    }



    private void initData() {
        mShopsAdapter = new ShopsAdapter(getContext());
        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSecondDetail.setAdapter(mShopsAdapter);
        binding.rvSecondDetail.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

    }

    private void initView() {
        binding.tvPayMoney.setText("合计:￥ "+mDishes.getTotalMoney());
        mShopsAdapter.setData(mDishes.getProducts());
    }

    private void initEvent() {
        binding.btPayFace.setOnClickListener(view -> {
            CommonAndDpToPxUtil.speakWork("开始人脸支付");
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_SECOND,mDishes));
            dismiss();
        });

        binding.btPayQrcode.setOnClickListener(view -> {
            CommonAndDpToPxUtil.speakWork("请出示付款码支付");
            ScanPayPresenter mScanPresenter = new ScanPayPresenter(mDishes,getContext());
            mScanPresenter.scanListener();
            mScanPresenter.setScanState(ScanPayPresenter.ScanState.PAY);
            dismiss();
//            mScanPresenter.closeScan();
        });
    }

}
