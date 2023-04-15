package com.yannuo.dgcanteen.activitys;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.appcompat.app.AppCompatActivity;

import com.yannuo.dgcanteen.adapters.DropDownAdapter;
import com.yannuo.dgcanteen.adapters.MealDataAdapter;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.MealTable;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.databinding.ActivityDishManageBinding;

import java.util.ArrayList;
import java.util.List;

public class DishManageActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ActivityDishManageBinding binding;
    private List<MealTable> mealTables;
    private ArrayList<String> dataMeal;
    private ListView listView;
    private PopupWindow popup;
    private List<DishesTable> mMealData;
    private MealDataAdapter adapter;
    private DishesDBHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        initView();
        initData();
        initObject();
        initEvent();
    }

    private void initView(){
        binding = ActivityDishManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initObject(){
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(new DropDownAdapter(this,dataMeal));
        listView.setOnItemClickListener(this);

        mHelper = DishesDBHelper.getInstance(this);
        mMealData = mHelper.queryDishes();
        adapter = new MealDataAdapter(this,mMealData,mHelper);
        binding.gridManage.setAdapter(adapter);
    }

    private void initEvent(){
        binding.spinnerText.setOnClickListener(view -> {
            popupWindow();
        });
        binding.spinnerImg.setOnClickListener(view -> {
            popupWindow();
        });

        binding.ibtBack.setOnClickListener(view -> {
            finish();
        });
    }

    private void initData(){
        mealTables = new ArrayList<>();
        dataMeal = new ArrayList<>();
        mealTables = DishesDBHelper.getInstance().queryAllMeals();
        for (MealTable u : mealTables){
            dataMeal.add(u.getMealName());
        }
    }

    private void popupWindow(){
        popup = new PopupWindow();
        popup.setWidth(binding.spinnerText.getWidth() + binding.spinnerImg.getWidth() - 15);
        popup.setHeight(600);
        popup.setContentView(listView);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(binding.spinnerText,0,0);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        binding.spinnerText.setText(dataMeal.get(position));
        popup.dismiss();
        runOnUiThread(() -> {
            int mealId = 0;
            for (MealTable u : mealTables){
                if (dataMeal.get(position).equals(u.getMealName())){
                    mealId = u.getMealId();
                }
            }
            mMealData = mHelper.queryDishesByMealId(mealId);
            adapter = new MealDataAdapter(DishManageActivity.this,mMealData,mHelper);
            binding.gridManage.setAdapter(adapter);
        });
    }

    private void initScreen(){
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().setAttributes(params);
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
        if (Build.VERSION.SDK_INT >= 19) {
            uiFlags |= 0x00001000;  //SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide navigation bars - compatibility: building API level is lower thatn 19, use magic number directly for higher API target level
        } else {
            uiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }

}