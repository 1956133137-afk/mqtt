package com.yannuo.dgcanteen.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.nio.charset.StandardCharsets;

public class PictureUtil {
    public static final String getPictureName(String url, Context context) {
        if (url == null || TextUtils.isEmpty(url)) return "";
        return context.getFilesDir().getAbsolutePath() + "/pic/" + url.substring(url.lastIndexOf("/") + 1);
    }
}
