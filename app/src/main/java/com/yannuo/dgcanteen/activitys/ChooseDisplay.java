package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.ccb.smartcanteen.PayResultListener;
import com.ccb.smartcanteen.ZHSTFacePayService;
import com.google.gson.Gson;
import com.yannuo.dgcanteen.adapters.ShopsAdapter;
import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.interfaces.CallbackListener;
import com.yannuo.dgcanteen.model.CcbFacePayBean;
import com.yannuo.dgcanteen.model.DishesInfo;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.LogUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

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
        initView();
        initData();
        initEvent();

    }


    private void initView() {
         binding.tvPayMoney.setText("合计:￥ "+mDishes.getTotalMoney());
    }

    private void initData() {
        mShopsAdapter = new ShopsAdapter(getContext());
        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSecondDetail.setAdapter(mShopsAdapter);
        binding.rvSecondDetail.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getDisplay().getMetrics(displayMetrics);
        LogUtil.d(TAG,"开始绑定服务: "+displayMetrics.density);
    }


    private void initEvent() {
        binding.btPayFace.setOnClickListener(view -> {
            CommonAndDpToPxUtil.speakWork("开始人脸支付");
            EventBus.getDefault().post(mDishes);
            dismiss();
        });

        binding.btPayQrcode.setOnClickListener(view -> {
            CommonAndDpToPxUtil.speakWork("请出示付款码支付");
        });
    }

}
