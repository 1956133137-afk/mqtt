package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel;
import com.yannuo.dgcanteen.adapters.ShopsAdapter;
import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.interfaces.CallbackListener;
import com.yannuo.dgcanteen.interfaces.CloseEvent;
import com.yannuo.dgcanteen.interfaces.ReadCardListener;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.networkstate.NetworkStateManager;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.views.LoadingDialog;
import com.yannuo.dgcanteen.views.WaitForPayDialog;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ChooseDisplay extends BaseDisplay implements CallbackListener {

    private ChooseSecondDisplayBinding binding;
    private String TAG = getClass().getSimpleName();
    private ShopsAdapter mShopsAdapter;
    private ProductsDetail mDishes;
    private WaitForPayDialog waitForPayDialog;
    private LoadingDialog loadingDialog;
    private PayViewModel payViewModel;
    private volatile boolean sendCancel = false;
    private MMKV kv = MMKV.defaultMMKV();

    public ChooseDisplay(Context outerContext, ProductsDetail dishes, Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDishes = dishes;
        payViewModel = new ViewModelProvider((ViewModelStoreOwner) outerContext).get(PayViewModel.class);
    }

    public ChooseDisplay(Context context, Display display) {
        super(context, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
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

        //设置菜品数据
        payViewModel.setMDishes(mDishes);
    }

    private void initView() {
        if (mDishes == null) {
            binding.tvPayMoney.setText("￥0.0");
        } else {
            binding.tvPayMoney.setText("￥" + mDishes.getTotalMoney());
        }
        if (mDishes != null) mShopsAdapter.setData(mDishes.getProducts());
//        binding.btPayFace.requestFocus();

    }

    private void initEvent() {
        binding.btPayFace.setOnClickListener(view -> {
            LogUtil.e(TAG, "btPayFace");
            payViewModel.closePayStatus();//注意释放扫码和串口，防止干扰AIDL
            binding.btPayFace.setEnabled(false);
            if (sendCancel) return;
            sendCancel = true;
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_SECOND, mDishes));
//            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_SECOND,mDishes));
        });

        binding.btPayQrcode.setOnClickListener(view -> {
            if (!NetworkStateManager.getInstance().isOnline(getContext()) && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)) {
                CommonAndDpToPxUtil.speakWork("当前无网络，请打开设备离线模式");
                return;
            }
            CommonAndDpToPxUtil.speakWork("请出示付款码支付");
            if (waitForPayDialog != null) waitForPayDialog.cancel();
            if (waitForPayDialog == null) {
                waitForPayDialog = new WaitForPayDialog(getContext());
                waitForPayDialog.setListener(new WaitDialogEvent());
            }
            waitForPayDialog.show();

            payViewModel.setListener(this);
            payViewModel.openPayStatus();
        });

    }

    @Override
    protected void onStop() {
        if (waitForPayDialog != null) {
            waitForPayDialog.conclude();
        }
        payViewModel.closePayStatus();
        LogUtil.i(TAG, "stop...");
        super.onStop();
    }

    @Override
    public void onOtherListener(int event, @Nullable Object any) {
        LogUtil.i(TAG, "扫码处理code: " + event);
        switch (event) {
            case 1:
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(integer -> {
                            if (loadingDialog != null) loadingDialog.cancel();
                            if (loadingDialog == null) {
                                loadingDialog = new LoadingDialog(getContext());
                                loadingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            }
                            if (waitForPayDialog != null) waitForPayDialog.cancel();
                            loadingDialog.show();
                        });
                break;

            case 2: //异常
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(integer -> {
                            if (loadingDialog != null) loadingDialog.cancel();
                            Toast.makeText(getContext(), (String) any, Toast.LENGTH_SHORT).show();
                        });
                break;

            case 3: //在线订单
            case 4: //离线订单
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(integer -> {
                            if (loadingDialog != null) loadingDialog.cancel();
                        });
                if (sendCancel) return;
                sendCancel = true;
                EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FOURTH, any));
//                EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FOURTH,any));
                break;
            case 5: //离线码过期或者二维码无效
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(integer -> {
                            if ((Integer) any == 1) {
                                CommonAndDpToPxUtil.speakWork("无效码，请刷新付款码再支付");
                            } else if ((Integer) any == 2) {
                                CommonAndDpToPxUtil.speakWork("请检查网络,不支持离线聚合支付!");
                            } else {
                                CommonAndDpToPxUtil.speakWork("请切换离线码再支付");
                            }

                            if (waitForPayDialog != null) waitForPayDialog.cancel();
                            if (waitForPayDialog == null) {
                                waitForPayDialog = new WaitForPayDialog(getContext());
                                waitForPayDialog.setListener(new WaitDialogEvent());
                            }
                            waitForPayDialog.show();
                            payViewModel.setPayState(PayViewModel.PayStatus.PAY);
                        });
                break;
        }
    }


    private class WaitDialogEvent implements CloseEvent {

        @Override
        public void onEvent(int code, @Nullable String msg) {
            LogUtil.i(TAG, "扫码交易:" + msg);
            if (code == 1) {
                CommonAndDpToPxUtil.speakWork("超时未完成支付");
            } else if (code == 0) {
                payViewModel.setPayState(PayViewModel.PayStatus.INVALID);
            }
        }
    }

    static class CardCallBack implements ReadCardListener {
        @Override
        public void cardCallback(boolean state) {
            if (state) {
                CommonAndDpToPxUtil.speakWork("当前无网络，请打开设备离线模式");
            }
        }
    }
}
