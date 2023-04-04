package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.activitys.presenters.PayForPresenter;
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay;
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM;
import com.yannuo.dgcanteen.adapters.PayForAdapter;
import com.yannuo.dgcanteen.adapters.ProductsAdapter;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.databinding.DifferrentDialogBinding;
import com.yannuo.dgcanteen.model.CcbScanPayBean;
import com.yannuo.dgcanteen.model.DishesInfo;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.NumberGenerateUtil;
import com.yannuo.dgcanteen.util.TimeUtil;
import com.yannuo.dgcanteen.views.PayFinishDialog;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;

public class DifferentDisplay extends Presentation implements ProductsAdapter.WorkListener,PayForAdapter.WorkListener{

    private DifferrentDialogBinding binding;
    private int mealIds = 0,mMealId = 0 ;
    private ProductsAdapter adapterDishes;
    private CoroutineScope scope;
    private MyHandler handler;
    private PayForPresenter presenter;
    private PayForAdapter adapterPayFor;
    private DishesInfo data;

    public DifferentDisplay(Context outerContext, Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DifferrentDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initObject();
        Timer timer = new Timer();
        timer.schedule(timerTask,0,1000);
        initView();
        initEvent();
    }

    private void initObject() {
        adapterDishes = new ProductsAdapter(mealIds,getContext());
        adapterDishes.setListener(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(),3);
        binding.rvManInfo.setLayoutManager(gridLayoutManager);
        binding.rvManInfo.setAdapter(adapterDishes);
        adapterDishes.setImgSize(gridLayoutManager);
        dishesData();

        handler = new MyHandler();
        presenter = new PayForPresenter(handler,getContext());
        adapterPayFor = new PayForAdapter();
        adapterPayFor.setListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.rvSelectItem.setLayoutManager(linearLayoutManager);
        presenter.scanListener();  //监听扫码头数据

    }

    private void initView() {

        binding.rvSelectItem.setAdapter(adapterPayFor);

    }

    private void initData() {

    }

    private void dishesData(){
        List<DishesInfo> dataList = new ArrayList<>();
        List<DishesTable> list = DishesDBHelper.getInstance(getContext()).queryDishesByMealIdAneStatus(mealIds,1);
        for (DishesTable u : list){
            dataList.add(new DishesInfo(
                    u.getDishesId(),
                    u.getDishesName(),
                    u.getMealId(),
                    null,
                    u.getPrice(),
                    u.getUnit(),
                    u.getImgUrl(),
                    u.getStatus(),
                    0
            ));
        }
        adapterDishes.setData(dataList);
    }

    private final void initEvent() {
        binding.ibDelAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapterPayFor.getData().size() < 1)
                    return;
                clearShoppingCart();
            }
        });


        binding.btSureMeal.setOnClickListener(v -> {
            ProductsDetail prods =new ProductsDetail(adapterPayFor.getData(),binding.tvTotalMoney.getText().toString(),binding.tvTotalCount.getText().toString());
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FIRST,prods));
        });

    }

    //清空购物车
    private void clearShoppingCart() {
        for (DishesInfo u : adapterDishes.getData()) {
            if (u.getCount() != 0) {
                u.setCount(0);
                adapterDishes.notifyItemChanged(adapterDishes.getData().indexOf(u), "count");
            }
        }
        adapterPayFor.clear();
        binding.tvTotalMoney.setText("0.0元");
        binding.tvTotalCount.setText("0");
    }

    //更新购物车UI
    private void updateUiItems(DishesInfo it, Boolean accumulation){
        adapterPayFor.insertedData(it,accumulation);
        binding.rvSelectItem.scrollToPosition(adapterPayFor.getData().size() -1); //插入数据后滑动到底部
        float[] res = presenter.calculate(adapterPayFor.getData());
        binding.tvTotalMoney.setText(res[0] + "元");
        binding.tvTotalCount.setText(String.valueOf(res[1]));
    }




    @Override
    public void onEventClick(int position) {
        data = adapterDishes.getData(position);
        //选择的购买商品添加到购物车
        updateUiItems( data,false);

    }

    @Override
    public void onEventClick(@NonNull DishesInfo data) {
//      清除单个菜品
        if (data == null){
            for (DishesInfo u : adapterDishes.getData()){
                if (u.getCount() !=0){
                    u.setCount(0);
                    adapterDishes.notifyItemChanged(adapterDishes.getData().indexOf(u),"count");
                }
            }
        }else{
            adapterDishes.notifyItemChanged(adapterDishes.getData().indexOf(data), "count");
        }
        float[] res = presenter.calculate(adapterPayFor.getData());
        binding.tvTotalMoney.setText(res[0] + "元");
        binding.tvTotalCount.setText(res[1] + "");
    }




    public final class MyHandler extends Handler{

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    //    定时器
    public final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(TimeUtil.isCurrentInTimeScope(7,30,8,30)){
                        binding.mealTime.setText(R.string.breakfast_time);
                        mealIds = 1;
                    }else if (TimeUtil.isCurrentInTimeScope(11,30,13,0)){
                        binding.mealTime.setText(R.string.lunch_time);
                        mealIds = 2;
                    }else if (TimeUtil.isCurrentInTimeScope(18,30,19,30)){
                        binding.mealTime.setText(R.string.dinner_time);
                        mealIds = 3;
                    }else{
                        binding.mealTime.setText(R.string.unOpen_meal);
                        mealIds = 0;
                    }
                    if (mMealId != mealIds){
                        mMealId = mealIds;
                        clearShoppingCart();
                        dishesData();
                    }
                }
            });
        }
    };

}
