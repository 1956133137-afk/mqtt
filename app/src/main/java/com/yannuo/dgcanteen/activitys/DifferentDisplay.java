package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.activitys.presenters.DataPresenter;
import com.yannuo.dgcanteen.adapters.PayForAdapter;
import com.yannuo.dgcanteen.adapters.ProductsAdapter;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.MealTable;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.databinding.DifferrentDialogBinding;
import com.yannuo.dgcanteen.model.DishesInfo;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.model.PayCfg;
import com.yannuo.dgcanteen.model.ProductsDetail;
import com.yannuo.dgcanteen.util.CommonAndDpToPxUtil;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.dgcanteen.util.TimeUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.CoroutineScope;

public class DifferentDisplay extends Presentation implements ProductsAdapter.WorkListener,PayForAdapter.WorkListener{
    private String TAG = getClass().getSimpleName();

    private DifferrentDialogBinding binding;
    private int mealIds = 0;
    private ProductsAdapter adapterDishes;

    private DataPresenter presenter;
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
        initView();
        initEvent();

    }

    private void initObject() {
        refreshMeal();
        adapterDishes = new ProductsAdapter(mealIds,getContext());
        adapterDishes.setListener(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(),3);
        binding.rvManInfo.setLayoutManager(gridLayoutManager);
        binding.rvManInfo.setAdapter(adapterDishes);
        adapterDishes.setImgSize(gridLayoutManager);
        dishesData();

        presenter = new DataPresenter();
        adapterPayFor = new PayForAdapter();
        adapterPayFor.setListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.rvSelectItem.setLayoutManager(linearLayoutManager);
        initData();

    }

    private void initView() {
        binding.rvSelectItem.setAdapter(adapterPayFor);
//        binding.rvSelectItem.requestFocus();
    }

    private void initData() {
        EventBus.getDefault().register(this);
    }

    public void dishesData(){
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



    private void initEvent() {
        binding.ibDelAll.setOnClickListener(view -> {
            if (adapterPayFor.getData().size() < 1)
                return;
            clearShoppingCart();
        });


        binding.btSureMeal.setOnClickListener(v -> {
            if (adapterPayFor.getData().size() < 1){
                CommonAndDpToPxUtil.speakWork("请添加菜品");
                return;
            }
            MMKV mv = MMKV.defaultMMKV();
            PayCfg payCfg = mv.decodeParcelable(Constant.PAY_CONFIG, PayCfg.class);
            if (payCfg == null || TextUtils.isEmpty(payCfg.getCampusId()) ||
                    TextUtils.isEmpty(payCfg.getBusinessId()) || TextUtils.isEmpty(payCfg.getCounterId())
            ){
                CommonAndDpToPxUtil.speakWork("请配置支付环境");
                LogUtil.e(TAG,"请配置支付环境");
                return;
            }
            binding.btSureMeal.setEnabled(false);
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
        binding.tvTotalMoney.setText("");
        binding.tvTotalCount.setText("0");
    }

    //更新购物车UI
    private void updateUiItems(DishesInfo it, Boolean accumulation){
        adapterPayFor.insertedData(it,accumulation);
        binding.rvSelectItem.scrollToPosition(adapterPayFor.getData().size() -1); //插入数据后滑动到底部
        float[] res = presenter.calculate(adapterPayFor.getData());
        binding.tvTotalMoney.setText(String.valueOf(res[0]));
        binding.tvTotalCount.setText(String.valueOf(res[1]).replace(".0", ""));
    }


    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void arrive(MessageEvent event){
        LogUtil.d(TAG, "event : "+event.getCode());
        if(event.getCode() == Constant.EVENT_FIFTH ){
            clearShoppingCart();
            dishesData();
        }
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
        adapterDishes.notifyItemChanged(adapterDishes.getData().indexOf(data), "count");
        float[] res = presenter.calculate(adapterPayFor.getData());
        binding.tvTotalMoney.setText(String.valueOf(res[0]));
        binding.tvTotalCount.setText(String.valueOf(res[1]).replace(".0", ""));
    }


    //根据餐别时间，更新餐别
    public void subScreenView(int mealId, StringBuilder str){
        mealIds = mealId;
        binding.mealTime.setText(str);
        dishesData();
    }

    //副屏重新加载时，更新餐别
    private void refreshMeal(){
        mealIds = TimeUtil.CurrentTimeSection();
        StringBuilder str = new StringBuilder();
        if (mealIds == 0){
            str.append(getResources().getString(R.string.unOpen_meal));
        }else {
            MealTable meal = DishesDBHelper.getInstance().queryToMeals(mealIds);
            str.append(meal.getMealName() + " ");
            str.append(DateFormat.format("HH:mm",meal.getStartTime()).toString() + "~");
            str.append(DateFormat.format("HH:mm",meal.getEndTime()).toString());
        }
        binding.mealTime.setText(str);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        LogUtil.i(TAG,"stop...");
        super.onStop();
    }
}
