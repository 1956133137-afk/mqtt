package com.yannuo.dgcanteen.util;

import android.content.Context;

import java.nio.charset.StandardCharsets;

public class PictureUtil {
    public static final String getPictureName(String url,Context context){
        byte[] res = url.getBytes(StandardCharsets.UTF_8);
        StringBuilder str = new StringBuilder();
        for (int i = res.length - 1; i > 0; i--){
            if (res[i] == '/'){
                break;
            }else {
                str.insert(0,(char) res[i]);
            }
        }
        return context.getFilesDir().getAbsolutePath() + "/pic/" + str.toString();
    }
}
