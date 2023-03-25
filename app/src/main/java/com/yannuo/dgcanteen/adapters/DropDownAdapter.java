package com.yannuo.dgcanteen.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.yannuo.dgcanteen.R;

import java.util.ArrayList;

public class DropDownAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<String> mDataMeal;

    public DropDownAdapter(Context context,ArrayList<String> dataMeal){
        this.mContext = context;
        this.mDataMeal = dataMeal;
    }

    @Override
    public int getCount() {
        return mDataMeal.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataMeal.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_meal_down,null);
            holder = new ViewHolder();
            holder.selectOther =convertView.findViewById(R.id.select_other);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.selectOther.setText(mDataMeal.get(position));

        return convertView;
    }

    class ViewHolder{
        public TextView selectOther;
    }
}
