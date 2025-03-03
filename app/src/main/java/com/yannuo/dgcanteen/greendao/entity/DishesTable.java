package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DishesTable {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private String dishesId;  //菜品ID
    @NotNull
    private String dishesName;  //菜品名称
    @NotNull
    private int mealId;  //餐别ID
//    @NotNull
//    private String windowId;  //档口ID
    @NotNull
    private Double price;  //菜品单价
    private String unit;  //菜品单位（个/份/碗 等）
    private String imgUrl;  //菜品图片资源Url
    private int status = 1;  //上架状态：0-下架，1-上架
    private String categoryName; //餐别名称


    public DishesTable(Long id, @NotNull String dishesId,
                       @NotNull String dishesName, int mealId, @NotNull String windowId,
                       @NotNull Double price, String unit, String imgUrl, int status) {
        this.id = id;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.mealId = mealId;
//        this.windowId = windowId;
        this.price = price;
        this.unit = unit;
        this.imgUrl = imgUrl;
        this.status = status;
    }

    @Generated(hash = 1073252157)
    public DishesTable() {
    }

    @Generated(hash = 714880082)
    public DishesTable(Long id, @NotNull String dishesId, @NotNull String dishesName, int mealId, @NotNull Double price, String unit, String imgUrl, int status, String categoryName) {
        this.id = id;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.mealId = mealId;
        this.price = price;
        this.unit = unit;
        this.imgUrl = imgUrl;
        this.status = status;
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDishesId() {
        return this.dishesId;
    }

    public void setDishesId(String dishesId) {
        this.dishesId = dishesId;
    }

    public String getDishesName() {
        return this.dishesName;
    }

    public void setDishesName(String dishesName) {
        this.dishesName = dishesName;
    }

    public int getMealId() {
        return this.mealId;
    }

    public void setMealId(int mealId) {
        this.mealId = mealId;
    }

    //    public String getWindowId() {
//        return this.windowId;
//    }
//    public void setWindowId(String windowId) {
//        this.windowId = windowId;
//    }
    public Double getPrice() {
        return this.price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getImgUrl() {
        return this.imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
