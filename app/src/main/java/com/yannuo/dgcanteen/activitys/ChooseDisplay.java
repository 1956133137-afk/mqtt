package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import com.ccb.smartcanteen.ZHSTFacePayService;
import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.activitys.presenters.PayPresenter;
import com.yannuo.dgcanteen.adapters.ShopsAdapter;
import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.interfaces.CallbackListener;
import com.yannuo.dgcanteen.interfaces.CloseEvent;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.networkstate.NetworkStateManager;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.ScanDevice;
import com.yannuo.dgcanteen.util.ToastShowUtil;
import com.yannuo.dgcanteen.views.LoadingDialog;
import com.yannuo.dgcanteen.views.WaitForPayDialog;

import org.greenrobot.eventbus.EventBus;

public class ChooseDisplay extends Presentation implements CallbackListener {

    private ChooseSecondDisplayBinding binding;
    private String TAG = getClass().getSimpleName();
    private ShopsAdapter mShopsAdapter;
    private ProductsDetail mDishes;
    private WaitForPayDialog waitForPayDialog;
    private LoadingDialog loadingDialog;
    private PayPresenter mPresenter;



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
        mPresenter = new PayPresenter();

        //设置菜品数据
        mPresenter.setMDishes(mDishes);
        //监听交易过程
        mPresenter.setListener(this);
        //打开IC开
        mPresenter.openIcCard();



    }

    private void initView() {
        binding.tvPayMoney.setText("￥"+mDishes.getTotalMoney());
        mShopsAdapter.setData(mDishes.getProducts());
//        binding.btPayFace.requestFocus();
    }

    private void initEvent() {
        binding.btPayFace.setOnClickListener(view -> {
            mPresenter.release();//注意释放扫码和串口，防止干扰AIDL
            binding.btPayFace.setEnabled(false);
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_SECOND,mDishes));
        });

        binding.btPayQrcode.setOnClickListener(view -> {
            if (!NetworkStateManager.getInstance().isOnline(getContext()) && !MMKV.defaultMMKV().decodeBool(Constant.SWITCH)){
                CommonAndDpToPxUtil.speakWork("请打开设备离线模式");
                return;
            }
            CommonAndDpToPxUtil.speakWork("请出示付款码支付");
            if (waitForPayDialog != null) waitForPayDialog.cancel();
            if (waitForPayDialog == null) {
                waitForPayDialog = new WaitForPayDialog(getContext());
                waitForPayDialog.setListener(new WaitDialogEvent());
            }
            waitForPayDialog.show();
            //扫码
            mPresenter.openScan();

            //使能扫码支付
            mPresenter.setScanState(PayPresenter.ScanState.PAY);
        });
    }

    @Override
    protected void onStop() {
        if (waitForPayDialog != null) {
            waitForPayDialog.conclude();
        }
        if (mPresenter != null) {
            mPresenter.release();
        }
        LogUtil.d(TAG,"stop...");
        super.onStop();
    }

    @Override
    public void onOtherListener(int event, @Nullable Object any) {
        LogUtil.i(TAG,"event: " +event);
       switch (event){
           case 1:
               Observable.just(1)
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(integer -> {
                           if (loadingDialog != null) loadingDialog.cancel();
                           if (loadingDialog == null) {
                               loadingDialog = new LoadingDialog(getContext());
                               loadingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                           }
                           waitForPayDialog.cancel();
                           loadingDialog.show();
                       });
               break;

           case 2: //异常
               Observable.just(1)
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(integer -> {
                           if (loadingDialog != null) loadingDialog.cancel();
                           Toast.makeText(getContext(), (String)any, Toast.LENGTH_SHORT).show();
                       });
               break;

           case 3: //在线订单
           case 4: //离线订单
               Observable.just(1)
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(integer -> {
                           if (loadingDialog != null) loadingDialog.cancel();
                       });
               EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FOURTH,any));
               break;
           case 5: //离线码过期或者二维码无效
               Observable.just(1)
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(integer -> {
                           if ((Integer) any == 1){
                               CommonAndDpToPxUtil.speakWork("请刷新付款码再支付");
                           }else {
                               CommonAndDpToPxUtil.speakWork("请切换离线码再支付");
                           }

                           if (waitForPayDialog != null) waitForPayDialog.cancel();
                           if (waitForPayDialog == null) {
                               waitForPayDialog = new WaitForPayDialog(getContext());
                               waitForPayDialog.setListener(new WaitDialogEvent());
                           }
                           waitForPayDialog.show();

                           //使能扫码支付
                           mPresenter.setScanState(PayPresenter.ScanState.PAY);
                       });
               break;
       }
    }


    private class WaitDialogEvent implements CloseEvent {

        @Override
        public void onEvent(int code, @Nullable String msg) {
            LogUtil.i(TAG,"扫码交易:" + msg);
            if (code == 1){
                CommonAndDpToPxUtil.speakWork("超时未完成支付");
            }else if (code == 0){
                //todo 取消处理逻辑
            }
        }
    }




}
