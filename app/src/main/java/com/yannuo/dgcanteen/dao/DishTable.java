package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DishTable {

    private String userId;  //用户ID
    private Long businessId;  //商家号ID
    @NotNull
    private String dishesId;  //菜品ID
    @NotNull
    private String dishesName;  //菜品名称
    @NotNull
    private String windowId;  //档口ID
    @NotNull
    private String mealID;  //餐别ID
    @NotNull
    private Double price;  //菜品单价
    private String unit;  //菜品单位（个/份/碗 等）
    private String imgUrl;  //菜品图片资源Url
    private String status;  //上架状态：0-下架，1-上架
    @Generated(hash = 147081444)
    public DishTable(String userId, Long businessId, @NotNull String dishesId,
            @NotNull String dishesName, @NotNull String windowId,
            @NotNull String mealID, @NotNull Double price, String unit,
            String imgUrl, String status) {
        this.userId = userId;
        this.businessId = businessId;
        this.dishesId = dishesId;
        this.dishesName = dishesName;
        this.windowId = windowId;
        this.mealID = mealID;
        this.price = price;
        this.unit = unit;
        this.imgUrl = imgUrl;
        this.status = status;
    }
    @Generated(hash = 1039192113)
    public DishTable() {
    }
    public String getUserId() {
        return this.userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public Long getBusinessId() {
        return this.businessId;
    }
    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
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
    public String getWindowId() {
        return this.windowId;
    }
    public void setWindowId(String windowId) {
        this.windowId = windowId;
    }
    public String getMealID() {
        return this.mealID;
    }
    public void setMealID(String mealID) {
        this.mealID = mealID;
    }
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
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

}
