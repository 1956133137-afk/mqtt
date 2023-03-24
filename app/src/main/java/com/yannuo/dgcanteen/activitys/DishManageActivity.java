package com.yannuo.dgcanteen.activitys;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yannuo.dgcanteen.R;

import java.util.ArrayList;
import java.util.Arrays;

public class DishManageActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String[] mealArray = {"早餐","午餐","晚餐"};
    private ArrayList<String> dataMeal;
    private TextView spinnerText;
    private ListView listView;
    private PopupWindow popup;
    private ImageButton spinnerImg;

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
        listView.setAdapter(new DropDownAdapter());
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
        popup.setWidth(spinnerText.getWidth() + spinnerImg.getWidth());
        popup.setHeight(600);
        popup.setContentView(listView);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(spinnerText,0,0);
    }

    public class DropDownAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return dataMeal.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            if (convertView == null){
                convertView = LayoutInflater.from(DishManageActivity.this).inflate(R.layout.item_meal_down,null);
                holder = new ViewHolder();
                holder.selectOther =convertView.findViewById(R.id.select_other);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.selectOther.setText(dataMeal.get(position));

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spinnerText.setText(dataMeal.get(position));
                    popup.dismiss();
                }
            });

            return convertView;
        }

        class ViewHolder{
            public TextView selectOther;
        }
    }
}