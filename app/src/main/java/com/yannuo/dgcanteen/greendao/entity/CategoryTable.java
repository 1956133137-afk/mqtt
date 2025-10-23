package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;


@Entity
public class CategoryTable {
    @Id
    private String categoryId;
    private String categoryName;
    private String sort;
    private int mealId;

    @Generated(hash = 901610798)
    public CategoryTable(String categoryId, String categoryName, String sort,
            int mealId) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.sort = sort;
        this.mealId = mealId;
    }

    @Generated(hash = 1679078959)
    public CategoryTable() {
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSort() {
        return this.sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getMealId() {
        return this.mealId;
    }

    public void setMealId(int mealId) {
        this.mealId = mealId;
    }
}
