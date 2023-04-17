package com.yannuo.dgcanteen.util;

import android.content.Context;
import android.util.Log;

import java.nio.charset.StandardCharsets;

public class PictureUtil {
    public static final String getPictureName(String url,Context context){
        return context.getFilesDir().getAbsolutePath() + "/pic/" + url.substring(url.lastIndexOf("/")+1);
    }
}
