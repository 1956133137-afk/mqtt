package com.yannuo.dgcanteen.util;

import com.yannuo.dgcanteen.printer.TextPrint;

import java.util.ArrayList;
import java.util.List;

public class EscapeUtil {

    private final static String[] hex = {

    };


    public static String escape(String s) {
        StringBuffer sbuf = new StringBuffer();
        int ch;
        String str;

        for (int i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
//            if (ch==' '){
//                sbuf.append("+");
//            }
            if (('A' <= ch && ch <= 'Z') ||
                    ('a' <= ch && ch <= 'z') ||
                    ('0' <= ch && ch <= '9')
//                     ||
//                    ch =='-' || ch =='_' ||
//                    ch =='.' || ch =='!' ||
//                    ch =='~' || ch =='*' ||
//                    ch =='/' || ch =='(' ||
//                    ch ==')'
            ) {
                sbuf.append((char) ch);
            }
//            else if (ch <= 0x007f){
//                sbuf.append('%');
//                 str = Integer.toHexString(ch & 0xff);
//                if (str.length()<2) {
//                    sbuf.append('0');
//                }
//                sbuf.append(str.toUpperCase());
//            }
            else {
                sbuf.append('%');
                sbuf.append('u');
                str = Integer.toHexString((ch >> 8) & 0xff);
                if (str.length() < 2) {
                    sbuf.append('0');
                }
                sbuf.append(str.toUpperCase());

                str = Integer.toHexString(ch & 0xff);
                if (str.length() < 2) {
                    sbuf.append('0');
                }
                sbuf.append(str.toUpperCase());
            }
        }
        return sbuf.toString();
    }

    /**
     * @param s    内容
     * @param size 字体放大倍数
     * @param len  纸宽
     * @return
     */
    public static List<String> strLeng(TextPrint textPrint, String s, int size, int len) {
        int ch;
        List<String> list = new ArrayList<>();
        int temp = 0;
        int start = 0;
        int stop = 0;

        int charW = textPrint.getFont() == 0 ? 12 : 9;
        int multi = textPrint.getScaleW() == 0 ? 1 : 2;
        int zWidth, yWidth;
        zWidth = 12 * 2 * multi;
        yWidth = charW * multi;


        //行线特殊处理
        if (s.startsWith("---") && s.endsWith("---")) {
            int numbers = 0;
            numbers = (int) (len / yWidth);
            String str = "";
            for (int i = 0; i < numbers; i++) {
                str += "-";
            }
            list.add(str);
            return list;
        }

        for (int i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
            if (ch <= 0x007f) {
                int aa = temp + yWidth;
                if (aa <= len) {
                    temp = aa;
                } else {
                    stop = i;
                    list.add(s.substring(start, stop));
                    start = stop;
                    temp = yWidth;
                }
            } else {
                int aa = temp + zWidth;
                if (aa <= len) {
                    temp = aa;
                } else {
                    stop = i;
                    list.add(s.substring(start, stop));
                    start = stop;
                    temp = zWidth;
                }
            }
            //处理最后一个不满一行的
            if (i == (s.length() - 1)) {
                list.add(s.substring(start));
            }
        }
        return list;
    }


    public static int calculateSize(String cnt,int len) {
        int ds =0,ss = 0;
        char ch;
        for (int i = 0; i < cnt.length(); i++) {
            ch = cnt.charAt(i);
            if (ch <= 0x007f)
                ds++;
            else
                ss++;
        }
        ss = len - ss;
        ds = (Math.max(0,ss) *2) - ss - ds;
        return Math.max(0,ds+len);
    }
}

/**
 *
 * @param s 内容
 * @param size 字体放大倍数
 * @param len 纸宽
 * @return
 */
//    public static List<String> strLeng(TextPrint textPrint, String s, int size, int len){
//        int ch;
//        List<String> list = new ArrayList<>();
//        int temp = 0;
//        int start = 0;
//        int stop = 0;
//
//        //行线特殊处理
//        if (s.startsWith("---") && s.endsWith("---")){
//            int numbers = 0 ;
//            if (size == 1){
//                numbers = (int) (len / 12f);
//            }else{
//                numbers = (int) (len / 24f);
//            }
//            String str = "";
//            for (int i = 0 ;i <numbers ;i++){
//                str +="-";
//            }
//            list.add(str);
//            return list;
//        }
//
//        for (int i= 0 ;i <s.length();i++){
//            ch = s.charAt(i);
//            if (ch <= 0x007f){
//                int aa = temp + 12 * size;
//                if (aa <=len){
//                    temp = aa;
//                }else {
//                    stop = i;
//                    list.add(s.substring(start,stop));
//                    start = stop;
//                    temp = 12 * size;
//                }
//            }
//            else {
//                int aa = temp + 24 * size;
//                if (aa <=len){
//                    temp = aa;
//                }else {
//                    stop = i;
//                    list.add(s.substring(start,stop));
//                    start = stop;
//                    temp = 24 * size;
//                }
//            }
//            //处理最后一个不满一行的
//            if (i == (s.length()-1)){
//                list.add(s.substring(start));
//            }
//        }
//        return list;
//    }
//
//}
