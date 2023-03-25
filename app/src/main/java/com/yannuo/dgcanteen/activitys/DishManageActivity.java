package com.yannuo.dgcanteen.activitys;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.adapters.DropDownAdapter;
import com.yannuo.dgcanteen.adapters.MealDataAdapter;
import com.yannuo.dgcanteen.entity.MealData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DishManageActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private final static String[] mealArray = {"早餐","午餐","晚餐"};
    private ArrayList<String> dataMeal;
    private TextView spinnerText;
    private ListView listView;
    private PopupWindow popup;
    private ImageButton spinnerImg;
    private List<MealData> mMealData;
    private GridView gridManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_manage);
        initUI();
    }

    private void initUI(){
        findViewById(R.id.ibt_back).setOnClickListener(this);
        spinnerText = findViewById(R.id.spinner_text);
        spinnerText.setOnClickListener(this);
        spinnerImg = findViewById(R.id.spinner_img);
        spinnerImg.setOnClickListener(this);

        listView = new ListView(this);
        dataMeal = new ArrayList<String>();
        dataMeal.addAll(Arrays.asList(mealArray));
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(new DropDownAdapter(this,dataMeal));
        listView.setOnItemClickListener(this);

        gridManage = findViewById(R.id.grid_manage);
        gridManage.setNumColumns(6);
        mMealData = MealData.getDefaultList();
        MealDataAdapter adapter = new MealDataAdapter(this,mMealData);
        gridManage.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.spinner_text:
            case R.id.spinner_img:
                popupWindow();
                break;
            case R.id.ibt_back:
                finish();
                break;
            default:break;
        }
    }
    private void popupWindow(){
        popup = new PopupWindow();
        popup.setWidth(spinnerText.getWidth() + spinnerImg.getWidth() - 15);
        popup.setHeight(600);
        popup.setContentView(listView);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(spinnerText,0,0);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        spinnerText.setText(dataMeal.get(position));
        popup.dismiss();
    }

}