package com.yannuo.dgcanteen.activitys;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.activitys.presenters.DataPresenter;
import com.yannuo.dgcanteen.adapters.DifferentCategoryAdapter;
import com.yannuo.dgcanteen.adapters.PayForAdapter;
import com.yannuo.dgcanteen.adapters.ProductsOnAdapter;
import com.yannuo.dgcanteen.databinding.DifferrentOnDialogBinding;
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper;
import com.yannuo.dgcanteen.greendao.entity.CategoryTable;
import com.yannuo.dgcanteen.greendao.entity.DishesTable;
import com.yannuo.dgcanteen.greendao.entity.MealTable;
import com.yannuo.dgcanteen.interfaces.FoodsCallback;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferentOnDisplay extends BaseDisplay implements ProductsOnAdapter.WorkListener, PayForAdapter.WorkListener, DifferentCategoryAdapter.CategoryListener {
    private String TAG = getClass().getSimpleName();

    private DifferrentOnDialogBinding binding;
    private int mealIds = 0;
    private ProductsOnAdapter adapterDishes;
    private ProductsOnAdapter adapterDishes2;
    private DifferentCategoryAdapter adapterCategory;
    private List<CategoryTable> categoryTables = new ArrayList<>();
    private List<DishesInfo> dishList;
    private Map<String,List<DishesInfo>> dishMap = new HashMap<>();

    private MMKV kv;

    private DataPresenter presenter;
    //购物车适配器
    private PayForAdapter adapterPayFor;
    private boolean flag = false;

    public void setFoodsCallback(FoodsCallback foodsCallback) {
        LogUtil.d(TAG, "原本 foodsCallback: " + foodsCallback);
        if (this.foodsCallback == null) this.foodsCallback = foodsCallback;
    }

    public FoodsCallback getFoodsCallback() {
        return foodsCallback;
    }

    private FoodsCallback foodsCallback = null;

    public DifferentOnDisplay(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DifferrentOnDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initObject();
        initView();
        initEvent();
    }

    private void initObject() {
        kv = MMKV.defaultMMKV();
        refreshMeal();
        adapterDishes = new ProductsOnAdapter(getContext(),1);
        adapterDishes.setListener(this);
        adapterDishes2 = new ProductsOnAdapter(getContext(),2);
        adapterDishes2.setListener(this);
        adapterCategory  = new DifferentCategoryAdapter(getContext());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        GridLayoutManager gridLayoutManager2 = new GridLayoutManager(getContext(), 1);
        GridLayoutManager categoryManager = new GridLayoutManager(getContext(), 1);
        binding.rvManInfo.setLayoutManager(gridLayoutManager);
        binding.rvManInfo.setAdapter(adapterDishes);
        binding.rvManInfo2.setLayoutManager(gridLayoutManager2);
        binding.rvManInfo2.setAdapter(adapterDishes2);
        binding.categoryInfo.setLayoutManager(categoryManager);
        binding.categoryInfo.setAdapter(adapterCategory);
        adapterCategory.setListener(this);
        adapterDishes.setImgSize(gridLayoutManager);
        adapterDishes2.setImgSize(gridLayoutManager2);
        categoryData();
        getDishesData();
        if(categoryTables.size() > 1){
            dishesData(0,categoryTables.get(0).getCategoryName());
            dishesData(1,categoryTables.get(1).getCategoryName());
        }
        presenter = new DataPresenter();
        adapterPayFor = new PayForAdapter();
        adapterPayFor.setListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.rvSelectItem.setLayoutManager(linearLayoutManager);
        initData();
    }

    private void initView() {
        binding.rvSelectItem.setAdapter(adapterPayFor);
        PayCfg payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg.class);
        if (payCfg != null) {
            binding.selectStopper.setText(payCfg.getWindowName());
        }
        if (kv.decodeBool(Constant.CODE_VERIFICATION_SET, false)) {
            binding.btVerification.setVisibility(View.VISIBLE);
        } else {
            binding.btVerification.setVisibility(View.GONE);
        }
    }

    private void initData() {
        EventBus.getDefault().register(this);
    }

    public void dishesData(Integer id,String name) {
        List<DishesInfo> list = dishMap.get(name);
        if(id == 0){
            adapterDishes.setData(list);
        }else{
            adapterDishes2.setData(list);
        }
    }

    //获取数据并分装
    private void getDishesData(){
        //获取数据
        List<DishesTable> disheTables = DishesDBHelper.getInstance(getContext()).queryDishesByMealIdAneStatusDesc(mealIds, 1);
        dishList = toDishesInfo(disheTables);
        //重置map
        dishMap.clear();
        if(dishList != null && dishList.size() > 0){
            //数据分装
            for(DishesInfo info : dishList){
                if(dishMap.containsKey(info.getCategoryName())){
                    List<DishesInfo> dishesTables = dishMap.get(info.getCategoryName());
                    if(dishesTables != null){
                        dishesTables.add(info);
                    }
                }else{
                    ArrayList<DishesInfo> dishesTables = new ArrayList<>();
                    dishesTables.add(info);
                    dishMap.put(info.getCategoryName(),dishesTables);
                }
            }
        }
    }

    private List<DishesInfo> toDishesInfo(List<DishesTable> list){
        List<DishesInfo> dataList = new ArrayList<>();
        for (DishesTable u : list) {
            String imgUrl = (u.getImgUrl() == null || u.getImgUrl().isEmpty()) ? "" : u.getImgUrl();
            Integer status = u.getStatus();
            if (status == null) status = 1;
            DishesInfo dishesInfo = new DishesInfo(u.getDishesId(),
                    u.getDishesName(),
                    u.getMealId(),
                    null,
                    u.getPrice(),
                    u.getUnit(),
                    imgUrl,
                    status,
                    0);
            dishesInfo.setCategoryName(u.getCategoryName());
            dataList.add(dishesInfo);
        }
        return dataList;
    }

    /**
     * 获取当前餐别的菜品类别
     */
    private void categoryData(){
        categoryTables = DishesDBHelper.getInstance(getContext()).queryCategoryByMealId(mealIds);
        adapterCategory.setData(categoryTables);
    }

    private void initEvent() {

        binding.ibDelAll.setOnClickListener(view -> {
            if (adapterPayFor.getData().size() < 1)
                return;
            clearShoppingCart();
        });

        binding.btSureMeal.setOnClickListener(v -> {
            if (adapterPayFor.getData().size() < 1) {
                CommonAndDpToPxUtil.speakWork("请添加菜品");
                return;
            }
            MMKV mv = MMKV.defaultMMKV();
            PayCfg payCfg = mv.decodeParcelable(Constant.PAY_CONFIG, PayCfg.class);
            if (payCfg == null || TextUtils.isEmpty(payCfg.getCampusId()) ||
                    TextUtils.isEmpty(payCfg.getBusinessId()) || TextUtils.isEmpty(payCfg.getCounterId())
            ) {
                CommonAndDpToPxUtil.speakWork("请配置支付环境");
                LogUtil.e(TAG, "请配置支付环境");
                return;
            }
            if (flag) return;
            flag = true;
//            binding.btSureMeal.setEnabled(false);
            ProductsDetail prods = new ProductsDetail(adapterPayFor.getData(), binding.tvTotalMoney.getText().toString(), binding.tvTotalCount.getText().toString());
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FIRST, prods));

        });

        binding.btVerification.setOnClickListener(v -> {
            MMKV mv = MMKV.defaultMMKV();
            PayCfg payCfg = mv.decodeParcelable(Constant.PAY_CONFIG, PayCfg.class);
            if (payCfg == null || TextUtils.isEmpty(payCfg.getCampusId()) ||
                    TextUtils.isEmpty(payCfg.getBusinessId()) || TextUtils.isEmpty(payCfg.getCounterId())
            ) {
                CommonAndDpToPxUtil.speakWork("请配置支付环境");
                LogUtil.e(TAG, "请配置支付环境");
                return;
            }
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_CODE, null));
        });
    }

    //清空购物车
    private void clearShoppingCart() {
        for(DishesInfo d : adapterPayFor.getData()){
            d.setCount(0);
        }
        adapterDishes.notifyItemRangeChanged(0,adapterDishes.getData().size());
        adapterDishes2.notifyItemRangeChanged(0,adapterDishes2.getData().size());
        adapterPayFor.clear();
        binding.tvTotalMoney.setText("");
        binding.tvTotalCount.setText("0");
        if (foodsCallback != null)
            foodsCallback.onFoodsUpdate(adapterPayFor.getData());
    }

    //更新购物车UI
    private void updateUiItems(DishesInfo it, Boolean accumulation) {
        adapterPayFor.insertedData(it, accumulation);
        binding.rvSelectItem.scrollToPosition(adapterPayFor.getData().size() - 1); //插入数据后滑动到底部
        float[] res = presenter.calculate(adapterPayFor.getData());
        binding.tvTotalMoney.setText(String.valueOf(res[0]));
        binding.tvTotalCount.setText(String.valueOf(res[1]).replace(".0", ""));
    }

    //EvenBus事件监听处理
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void arrive(MessageEvent event) {
        LogUtil.d(TAG, "event : " + event.getCode());
        if (event.getCode() == Constant.EVENT_FIFTH) {
            categoryData();
            getDishesData();
            //清空购物车
            clearShoppingCart();
            if(categoryTables.size() > 1){
                dishesData(0,categoryTables.get(0).getCategoryName());
                dishesData(1,categoryTables.get(1).getCategoryName());
            }
        }
    }

    @Override
    public void onEventClick(int position,int type) {
        //选择菜品加入购物车
        DishesInfo data;
        if(type == 1){
            data = adapterDishes.getData(position);
        }else{
            data = adapterDishes2.getData(position);
        }
        updateUiItems(data, false);
        if (foodsCallback != null) foodsCallback.onFoodsUpdate(adapterPayFor.getData());

    }

    @Override
    public void onEventClick(@NonNull DishesInfo data) {
//      清除单个菜品
        adapterDishes.notifyItemChanged(adapterDishes.getData().indexOf(data), "count");
        adapterDishes2.notifyItemChanged(adapterDishes2.getData().indexOf(data), "count");
        float[] res = presenter.calculate(adapterPayFor.getData());
        binding.tvTotalMoney.setText(String.valueOf(res[0]));
        binding.tvTotalCount.setText(String.valueOf(res[1]).replace(".0", ""));
        if (foodsCallback != null) foodsCallback.onFoodsUpdate(adapterPayFor.getData());
    }

    //根据餐别时间，更新餐别
    public void subScreenView(int mealId, StringBuilder str) {
        mealIds = mealId;
        binding.mealTime.setText(str);
        getDishesData();
        if(categoryTables.size() > 1){
            dishesData(0,categoryTables.get(0).getCategoryName());
            dishesData(1,categoryTables.get(1).getCategoryName());
        }
    }

    //副屏重新加载时，更新餐别
    private void refreshMeal() {
        mealIds = TimeUtil.CurrentTimeSection();
        StringBuilder str = new StringBuilder();
        str.append(getResources().getString(R.string.unOpen_meal));
        if (mealIds != 0) {
            MealTable meal = DishesDBHelper.getInstance().queryToMeals(mealIds);
            if(meal != null){
                str.setLength(0);
                str.append(meal.getMealName() + " ");
                str.append(DateFormat.format("HH:mm", meal.getStartTime()).toString() + "~");
                str.append(DateFormat.format("HH:mm", meal.getEndTime()).toString());
            }
        }
        binding.mealTime.setText(str);
    }

    @Override
    protected void onStop() {

        EventBus.getDefault().unregister(this);
        LogUtil.i(TAG, "stop...");
        foodsCallback = null;
        super.onStop();
    }

    @Override
    public void onCategoryClick(int position, String name) {
        Log.d(TAG, "onCategoryClick: 餐别ID返回:"+name);
        //选择刷新窗口回调
        dishesData(position,name);
    }
}
