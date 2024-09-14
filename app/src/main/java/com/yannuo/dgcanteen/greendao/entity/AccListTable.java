package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class AccListTable {
    @Id(autoincrement = true)
    private Long id;
    private Long accId;        //外键
    private String ACC_BAL;
    private String ACC_NO;
    private String ACC_TYPE;
    private String PAYMENT;
    private String TRAN_ID;
    @Generated(hash = 975868530)
    public AccListTable(Long id, Long accId, String ACC_BAL, String ACC_NO,
            String ACC_TYPE, String PAYMENT, String TRAN_ID) {
        this.id = id;
        this.accId = accId;
        this.ACC_BAL = ACC_BAL;
        this.ACC_NO = ACC_NO;
        this.ACC_TYPE = ACC_TYPE;
        this.PAYMENT = PAYMENT;
        this.TRAN_ID = TRAN_ID;
    }
    @Generated(hash = 1540985685)
    public AccListTable() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getAccId() {
        return this.accId;
    }
    public void setAccId(Long accId) {
        this.accId = accId;
    }
    public String getACC_BAL() {
        return this.ACC_BAL;
    }
    public void setACC_BAL(String ACC_BAL) {
        this.ACC_BAL = ACC_BAL;
    }
    public String getACC_NO() {
        return this.ACC_NO;
    }
    public void setACC_NO(String ACC_NO) {
        this.ACC_NO = ACC_NO;
    }
    public String getACC_TYPE() {
        return this.ACC_TYPE;
    }
    public void setACC_TYPE(String ACC_TYPE) {
        this.ACC_TYPE = ACC_TYPE;
    }
    public String getPAYMENT() {
        return this.PAYMENT;
    }
    public void setPAYMENT(String PAYMENT) {
        this.PAYMENT = PAYMENT;
    }
    public String getTRAN_ID() {
        return this.TRAN_ID;
    }
    public void setTRAN_ID(String TRAN_ID) {
        this.TRAN_ID = TRAN_ID;
    }
}
