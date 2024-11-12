package com.yannuo.dgcanteen.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class BaseAdapter<T,B extends ViewBinding> extends RecyclerView.Adapter<BaseAdapter.Holder> {
    protected String TAG = getClass().getSimpleName();
    protected List<T> mData = new ArrayList<>();
    protected int mWidth = -1;
    protected HashMap<String,Integer> map = null;


    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new Holder(getB(inflater, parent));
    }

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter.Holder holder, int position) {
        bindHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull  BaseAdapter.Holder holder, int position, @NonNull List<Object> payloads) {
        bindHolder(holder, position, payloads);
    }

    protected void bindHolder(Holder holder, int position, List<Object> payloads) {

    }

//   public void bindHolder(Holder holder, int position)

    abstract B getB(LayoutInflater inflater, ViewGroup parent);

    abstract void bindHolder(Holder holder, int position);


    //添加元素
    public void setData(List<T> data) {
        if (data == null) return;
        mData.clear();
        mData.addAll(data);
        if (map != null)
            map.clear();
        notifyDataSetChanged();
    }

    //添加元素
    public void addData(List<T> data) {
        if (data == null) return;
        mData.addAll(data);
        notifyItemRangeInserted((mData.size() - data.size()), data.size());
    }

    public void insertedData(List<T> data){
        if (data == null)return;
        mData.addAll(0, data);
        Collections.reverse(mData);
        notifyDataSetChanged();

    }

    public void insertDataTop(T data, Integer item) {
        if (data == null) return;
        mData.add(0, data);
        while (item != 0 && getItemCount() > item) mData.remove(getItemCount() - 1);
//        notifyItemInserted(0);
        notifyDataSetChanged();
    }

    public void removeData(String key){
        if (key == null || map == null)return;
        Integer position = map.get(key);
        if (position != null) {
            mData.remove(position);
            map.remove(key);
            notifyItemRemoved(position);
        }
    }

    /**
     * 获取数据
     */
    public List<T> getData() {
        return mData;
    }

    public T getData(int position) {
        return mData.get(position);
    }

    public void removeData(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
    }

    public void clear() {
        if (mData != null) mData.clear();
        if (map != null) map.clear();
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }


    public class Holder extends RecyclerView.ViewHolder {
        protected B binding;

        public Holder(B itemView) {
            super(itemView.getRoot());
            binding = itemView;
            addEventListener(this);
        }
    }

    public void addEventListener(Holder holder) {

    }
}
