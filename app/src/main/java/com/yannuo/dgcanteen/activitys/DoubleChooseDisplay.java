package com.yannuo.dgcanteen.activitys;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.activitys.viewModel.PayViewModel;
import com.yannuo.dgcanteen.adapters.DoubleShopsAdapter;
import com.yannuo.dgcanteen.databinding.DoubleChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.dialogView.ConfirmDialog;
import com.yannuo.dgcanteen.interfaces.CallbackListener;
import com.yannuo.dgcanteen.interfaces.CloseEvent;
import com.yannuo.dgcanteen.model.DishesInfo;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.PayForUI;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.networkstate.NetworkStateManager;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ToastShowUtil;
import com.yannuo.dgcanteen.views.LoadingDialog;
import com.yannuo.dgcanteen.views.WaitForPayDialog;

import org.greenrobot.eventbus.EventBus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DoubleChooseDisplay extends BaseDisplay implements CallbackListener, DoubleShopsAdapter.DishListener {
    private ProductsDetail mDishes;
    private PayViewModel payViewModel;
    private DoubleShopsAdapter mShopsAdapter;
    private LoadingDialog loadingDialog;
    private Handler handler = new Handler();
    private PayForUI payData = new PayForUI();
    private WaitForPayDialog waitForPayDialog;
    private ConfirmDialog confirmDialog = null;
    private volatile boolean sendCancel = false;
    private String TAG = getClass().getSimpleName();
    private DoubleChooseSecondDisplayBinding binding;
    private String mTotalMoney = "0.00";
    private List<DishesInfo> mDishList;

    public DoubleChooseDisplay(Context outerContext, ProductsDetail dishes, Display display) {
        super(outerContext, display);
        mDishes = dishes;
        payViewModel = new ViewModelProvider((ViewModelStoreOwner) outerContext).get(PayViewModel.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DoubleChooseSecondDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initData();
        initEvent();
        initView();
    }

    private void initData() {

        mShopsAdapter = new DoubleShopsAdapter(getContext());
        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSecondDetail.setAdapter(mShopsAdapter);

        //设置菜品数据
        payViewModel.setMDishes(mDishes);

        confirmDialog = new ConfirmDialog(this.getContext());
        confirmDialog.setListener(new ConfirmDialog.OnConfirmCallback() {
            @Override
            public void confirmCallback(boolean flag) {
                if (flag) payViewModel.confirmPay(payData);
                else payViewModel.setPayState(PayViewModel.PayStatus.PAY);
                if (confirmDialog.isShowing()) confirmDialog.dismiss();
            }
        });
    }

    private void initView() {
        if (mDishes != null) {
            mTotalMoney = mDishes.getTotalMoney();
        }
        binding.tvPayMoney.setText("￥" + mTotalMoney);
        if (mDishes != null) {
            mDishList = mDishes.getProducts();
            mShopsAdapter.setData(mDishList);
        }
        mShopsAdapter.setListener(this);
    }

    private void initEvent() {
        binding.btPayFace.setOnClickListener(view -> {
            if(!(mDishList.size() > 0)) {
                handler.post(() -> ToastShowUtil.show("未选择菜品"));
                return;
            }
            LogUtil.e(TAG, "btPayFace");
            payViewModel.closePayStatus();//注意释放扫码和串口，防止干扰AIDL
            binding.btPayFace.setEnabled(false);
            if (sendCancel) return;
            sendCancel = true;
            mDishes.setProducts(mDishList);
            mDishes.setTotalMoney(mTotalMoney);
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_SECOND, mDishes));
        });

        binding.btnClose.setOnClickListener(v -> {
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_THIRD, null));
        });

        binding.btPayQrcode.setOnClickListener(view -> {
            if(!(mDishList.size() > 0)) {
                handler.post(() -> ToastShowUtil.show("未选择菜品"));
                return;
            }
            mDishes.setProducts(mDishList);
            mDishes.setTotalMoney(mTotalMoney);
            payViewModel.setMDishes(mDishes);
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
            payViewModel.openPayStatus(Constant.PAY_CODE_IC_TYPE);
        });

    }

    /**
     * 菜品修改回调
     * @param bean 对象
     * @param type 添加/删除
     */
    @Override
    public void onModifyDish(@NonNull DishesInfo bean, boolean type) {
        int i = mDishList.indexOf(bean);
        DishesInfo dishesInfo = mDishList.get(i);
        if(type){
            //添加菜品
            dishesInfo.setCount(dishesInfo.getCount() + 1);
            BigDecimal dishPrice = BigDecimal.valueOf(dishesInfo.getPrice());
            mTotalMoney = new BigDecimal(mTotalMoney).add(dishPrice).toEngineeringString();
            binding.tvPayMoney.setText("￥" + mTotalMoney);
            mShopsAdapter.notifyItemChanged(i);
        }else{
            //删除菜品
            if(dishesInfo.getCount() > 1){
                dishesInfo.setCount(dishesInfo.getCount() - 1);
                mShopsAdapter.notifyItemChanged(i);
            }else{
                mDishList.remove(dishesInfo);
                mShopsAdapter.removeData(i);
            }
            BigDecimal dishPrice = BigDecimal.valueOf(dishesInfo.getPrice());
            mTotalMoney = new BigDecimal(mTotalMoney).subtract(dishPrice).toEngineeringString();
            binding.tvPayMoney.setText("￥" + mTotalMoney);
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

    @Override
    public void onOtherListener(int event, @Nullable Object any) {
        LogUtil.i(TAG, "扫码处理code: " + event);
        payData = new PayForUI();
        switch (event) {
            case 1:
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(integer -> {
                            if (loadingDialog != null) loadingDialog.cancel();
                            if (loadingDialog == null) {
                                loadingDialog = new LoadingDialog(this.getContext());
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
            case 8:
                handler.post(() -> {
                    payData = (PayForUI) any;
                    if (!confirmDialog.isShowing()) confirmDialog.show();
                    confirmDialog.setTextMsg("重复支付，您是否确定继续支付？");
                });
                break;
        }
    }
}
