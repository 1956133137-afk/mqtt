package com.yannuo.dgcanteen.dao;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class ProductsTable {
    @Id(autoincrement = true)
    private Long id;



    @Unique
    private String barCode ; //商品条码
    
    private String pName ; //商品名
    @NotNull
    private String pMoney ; //商品价钱，单位元
    @NotNull
    private String type;//商品类型

    private String pictureName; //商品图片名



    @Generated(hash = 1603915384)
    public ProductsTable(Long id, String barCode, String pName,
            @NotNull String pMoney, @NotNull String type, String pictureName) {
        this.id = id;
        this.barCode = barCode;
        this.pName = pName;
        this.pMoney = pMoney;
        this.type = type;
        this.pictureName = pictureName;
    }

    @Generated(hash = 2119452762)
    public ProductsTable() {
    }



    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPName() {
        return this.pName;
    }

    public void setPName(String pName) {
        this.pName = pName;
    }

    public String getPMoney() {
        return this.pMoney;
    }

    public void setPMoney(String pMoney) {
        this.pMoney = pMoney;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPictureName() {
        return this.pictureName;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    public String getBarCode() {
        return this.barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }


    
}
