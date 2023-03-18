package com.yannuo.dgcanteen.model;

import com.google.gson.annotations.SerializedName;

public class QRResult {

    @SerializedName("SUCCESS")
    private String sUCCESS;
    @SerializedName("PAYURL")
    private String pAYURL;

    public String getSUCCESS() {
        return sUCCESS;
    }

    public void setSUCCESS(String sUCCESS) {
        this.sUCCESS = sUCCESS;
    }

    public String getPAYURL() {
        return pAYURL;
    }

    public void setPAYURL(String pAYURL) {
        this.pAYURL = pAYURL;
    }
}
