//package com.yannuo.dgcanteen.util;
//
//import android.content.Context;
//import android.text.TextUtils;
//
//
//import com.proembed.service.MyService;
//import com.yannuo.dgcanteen.dao.ProductsTable;
//import com.yannuo.dgcanteen.dao.dbhelp.DbHelper;
//import com.yannuo.dgcanteen.interfaces.ImportExportListener;
//import com.yannuo.dgcanteen.model.Result;
//
//import org.apache.poi.ss.usermodel.DataFormatter;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.xssf.usermodel.XSSFSheet;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.LinkedList;
//
//
///**
// * Excel导出导入
// * Created by zhanmian on 2021-04-14 16:02
// */
//public class ExcelUtils {
//
//    private static final String DEBUG_TAG = "ExcelUtils";
//
//    public static void importExc(Context cnt, ImportExportListener listener){
////        Result lResult ;
//        MyService mXS = new MyService(cnt);
//        String usbPath = mXS.getUSBStoragePath();
//        if (TextUtils.isEmpty(usbPath)) {
//            listener.processState(new Result(1020, "未发现U盘，如已插入U盘，请重新插拔后再试"));
//            return ;
//        }
//        usbPath += (File.separatorChar+"商品"+File.separatorChar);
//        File file = new File(usbPath +"商品信息.xlsx");
//        if (!file.exists()){
//            listener.processState(new Result(1021, "未发现导入文件"));
//            return ;
//        }
//        listener.processState(new Result(1001, "准备导入文件"));
//        DbHelper.getInstance(cnt).deleteProductAll(); //清空数据库
//
//        try {
//            InputStream inputStream = new FileInputStream(file);
//            // xlsx格式
//            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
//            XSSFSheet sheet = workbook.getSheetAt(0);
//            DataFormatter formatter = new DataFormatter();
//            int count = 0 ;
//            // 从第二行开始读取
//            LinkedList<ProductsTable> products = new LinkedList<>();
//            ProductsTable bean ;
//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row != null) {
//                    bean = new ProductsTable();
//                    bean.setPName(formatter.formatCellValue(row.getCell(0)));
//                    bean.setPMoney(formatter.formatCellValue(row.getCell(1)));
//                    bean.setType(formatter.formatCellValue(row.getCell(2)));
//                    bean.setPictureName(formatter.formatCellValue(row.getCell(3)));
//                    bean.setBarCode(formatter.formatCellValue(row.getCell(4)));
//                    products.add(bean);
//                    count++;
//                }
//            }
//            if (products.size()> 0)
//                DbHelper.getInstance().insertPeopleRecords(products);
//            LogUtil.d(DEBUG_TAG, "读取到"+count+"条商品信息");
//            usbPath+="图片";
//            File baseFile = cnt.getFilesDir();
//            baseFile = new File(baseFile,"myPic");
//            if (!baseFile.exists()) {
//                baseFile.mkdirs();
//            }
//            for (File lFile : baseFile.listFiles()){
//                lFile.delete();
//            }
//            String basePath = baseFile.getAbsolutePath()+File.separatorChar;
//
//
//            //拷贝
//            File usbPicturePath = new File(usbPath);
////            for(File lFile : usbPicturePath.listFiles()){
////                lFile.delete();
////            }
//
//            byte[] buffer = new byte[4096];
//            int len = 0 ;
//            for(File lFile : usbPicturePath.listFiles()){
//                LogUtil.d(DEBUG_TAG, "正在拷贝文件:"+lFile.getName());
//                FileInputStream inputFile = new FileInputStream(lFile);
//                FileOutputStream outputFile = new FileOutputStream(basePath + lFile.getName());
//                while ((len = inputFile.read(buffer)) !=-1){
//                    outputFile.write(buffer,0,len);
//                }
//                outputFile.flush();
//                outputFile.close();
//                inputFile.close();
//            }
//            listener.processState(new Result(1000, "信息导入完成"));
//        } catch (IOException e  ) {
//            listener.processState(new Result(1010, "异常:"+e.getMessage()));
//            e.printStackTrace();
//        }
//    }
//
////
////    /**
////     * 人员信息导入
////     * @param importExportListener 用于导入时弹窗的监听器
////     */
////    public static void importExcel(ImportExportListener importExportListener) {
////        if (!USBReceiver.mounted) {
////            importExportListener.error("未发现U盘，如已插入U盘，请重新插拔后再试");
////            return;
////        }
////        Context context = MyApplication.getApp().getContext();
////        String path = USBReceiver.mountPath + "/人员信息/";
////        File file = new File(path + "人员列表.xlsx");
////        if (!file.exists()) {
////            importExportListener.error("未发现导入文件");
////            return;
////        }
////        importExportListener.inProgress();
////        mFacePassHandler = FaceKSHelper.getFacePassHandler();
////        try {
////            InputStream inputStream = new FileInputStream(file);
////            // xlsx格式
////            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
////            XSSFSheet sheet = workbook.getSheetAt(0);
////            DataFormatter formatter = new DataFormatter();
////            int count = 0;
////            // 从第二行开始读取
////            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
////                Row row = sheet.getRow(i);
////                UserInfo userInfo = new UserInfo();
////                if (row != null) {
////                    String name = formatter.formatCellValue(row.getCell(0));
////                    String number = formatter.formatCellValue(row.getCell(1));
////                    String cardNumber = formatter.formatCellValue(row.getCell(2));
////                    String idCardNum = formatter.formatCellValue(row.getCell(3));
////                    if (name != null && number != null) {
////                        userInfo.setName(name);
////                        userInfo.setNumber(number);
////                        userInfo.setCardNumber(cardNumber);
////                        userInfo.setIdCardNumber(idCardNum);
////                        String faceToken = addFace(path + "相片/" + number + ".jpg");
////                        if (faceToken != null) {
////                            List<String> faceTokens = new ArrayList<>();
////                            faceTokens.add(faceToken);
////                            boolean result = DbHelper.getInstance(context)
////                                    .insertFace(userInfo, context.getResources().getString(R.string.group_name_face_database), faceTokens);
////                            if (result) {
////                                count++;
////                            }
////                        }
////                    }
////                }
////            }
////            Thread.sleep(4000);
////            importExportListener.success();
////        } catch (Exception e) {
////            LogUtil.e(DEBUG_TAG, e.toString());
////            importExportListener.error(e.getMessage());
////        }
////    }
////
////    /**
////     * U盘人脸图片导入人脸库
////     * @param imagePath 人脸图片路径
////     * @return 返回人脸特征值
////     */
////    private static String addFace(String imagePath) {
////        String faceToken = null;
////        File imageFile = new File(imagePath);
////        if (!imageFile.exists()) {
////            imageFile = new File(imagePath.replace(".jpg", ".JPG"));
////            if (!imageFile.exists()) {
////                imageFile = new File(imagePath.replace(".jpg", ".jpeg"));
////                if (!imageFile.exists()) {
////                    imageFile = new File(imagePath.replace(".jpg", ".JPEG"));
////                    if (!imageFile.exists()) {
////                        return null;
////                    }
////                }
////            }
////        }
////        try {
////            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
////            FacePassAddFaceResult result = mFacePassHandler.addFace(bitmap);
////            if (result != null) {
////                if (result.result == 0) {
////                    faceToken = new String(result.faceToken);
////                }
////            }
////            if (!bitmap.isRecycled()) {
////                bitmap.recycle();
////            }
////        } catch (FacePassException e) {
////            LogUtil.e(DEBUG_TAG, e.getMessage());
////        }
////        return faceToken;
////    }
////
////    /**
////     * 导出人员信息
////     * @param importExportListener 用于导出时的弹窗监听器
////     */
////    public static void exportExcel(ImportExportListener importExportListener) {
////        if (!USBReceiver.mounted) {
////            importExportListener.error("未发现U盘，如已插入U盘，请重新插拔后再试");
////            return;
////        }
////
////        importExportListener.inProgress();
////
////        String sdcard = USBReceiver.mountPath;
////        // 向成3288 部分设备需要使用该路径才能进行文件写入
////        if (!SharedPreferenceUtil.getInstance().getBoolean(IS_YS)) {
////            sdcard = "/mnt/media_rw/" + sdcard.substring(sdcard.lastIndexOf("/"));
////        }
////        // 向成3568 Android11
////        if (SystemUtils.getModel().equals("rk3568_r")) {
////            MyService myService = MyApplication.getApp().getMyService();
////            sdcard = myService.getUSBStoragePath();
////        }
////        String dirPath = sdcard + "/人员信息" + "_" + DateUtils.getDateToString(System.currentTimeMillis(), "yyyy-MM-dd");
////        File dir = new File (dirPath + "/相片");
////        if (!dir.exists()) {
////            boolean isMk = dir.mkdirs();
////            LogUtil.e(DEBUG_TAG, " 创建文件夹：" + isMk);
////        }
////
////        Context context = MyApplication.getApp().getContext();
////        List<UserInfo> userInfoList = DbHelper.getInstance(context).searchUserList(1, 10000);
////        String[] tableHeaders = {"姓名", "编号", "卡号", "身份证号码"};
////
////        // 新建工作簿
////        Workbook workbook = new XSSFWorkbook();
////        Sheet sheet = workbook.createSheet("sheet1");
////        sheet.setDefaultColumnWidth(15);
////        sheet.setColumnWidth(3, 25 * 256);
////        sheet.setColumnWidth(4, 25 * 256);
////        // 打印时水平居中
////        sheet.setHorizontallyCenter(true);
////        // 单元格样式
////        CellStyle cellStyle = workbook.createCellStyle();
////        cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
////        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
////        // 设置单元格格式为文本格式
////        DataFormat format = workbook.createDataFormat();
////        cellStyle.setDataFormat(format.getFormat("@")); // 设置样式-数据格式为文本
////
////        //字体格式 
////        Font font = workbook.createFont();
////        font.setFontName("黑体");
////        font.setFontHeightInPoints((short) 14);//设置字体大小   
////        font.setBold(true);
////        cellStyle.setFont(font);
////
////        // 第一行作为表头
////        Row row = sheet.createRow(0);
////
////        //创建表头
////        for (int i = 0; i < tableHeaders.length; i++) {
////            Cell cell = row.createCell(i);
////            cell.setCellValue(tableHeaders[i]);
////            cell.setCellStyle(cellStyle);
////        }
////
////        for (int j = 0; j < userInfoList.size(); j++) {
////            row = sheet.createRow(j + 1);
////            for (int k = 0; k <= tableHeaders.length - 1; k++) {
////                Cell cell = row.createCell(k);
////                switch (k) {
////                    case 0:
////                        cell.setCellValue(userInfoList.get(j).getName());
////                        break;
////                    case 1:
////                        cell.setCellValue(userInfoList.get(j).getNumber());
////                        break;
////                    case 2:
////                        cell.setCellValue(userInfoList.get(j).getCardNumber());
////                        break;
////                    case 3:
////                        cell.setCellValue(userInfoList.get(j).getIdCardNumber());
////                        break;
////                }
////                cell.setCellStyle(cellStyle);
////            }
////            Bitmap bitmap = DbHelper.getInstance(context).getBitmap(userInfoList.get(j).getNumber());
////            try {
////                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
////                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
////                File file = new File(dir, userInfoList.get(j).getNumber() + ".jpg");
////                FileOutputStream fo = new FileOutputStream(file);
////                fo.write(bytes.toByteArray());
////                fo.flush();
////                fo.close();
////            } catch (Exception e) {
////                LogUtil.e(DEBUG_TAG, e.getMessage());
////            }
////        }
////        try {
////            File file = new File(dirPath + "/人员列表.xlsx");
////            OutputStream out = new FileOutputStream(file);
////            workbook.write(out);
////            out.flush();
////            out.close();
////            Thread.sleep(10000);
////            importExportListener.success();
////        } catch (IOException | InterruptedException e) {
////            LogUtil.e(DEBUG_TAG, e.getMessage());
////            importExportListener.error(e.getMessage());
////        }
////    }
////
////    /**
////     * 导出考勤记录
////     * @param startDate 考勤记录查询起始时间
////     * @param endDate 考勤记录查询结束时间
////     * @param importExportListener 用于导出时弹窗的监听器
////     */
////    public static void exportAttendanceExcel(Long startDate, Long endDate, ImportExportListener importExportListener) {
////        if (!USBReceiver.mounted) {
////            importExportListener.error("未发现U盘，如已插入U盘，请重新插拔后再试");
////            return;
////        }
////
////        importExportListener.inProgress();
////
////        String sdcard = USBReceiver.mountPath;
////        if (!SharedPreferenceUtil.getInstance().getBoolean(IS_YS)) {
////            sdcard = "/mnt/media_rw/" + sdcard.substring(sdcard.lastIndexOf("/"));
////        }
////        File dir = new File (sdcard + "/考勤记录");
////        if (!dir.exists()) {
////            boolean isMk = dir.mkdirs();
////            LogUtil.e(DEBUG_TAG, " 创建文件夹：" + isMk);
////        }
////
////        Context context = MyApplication.getApp().getContext();
////        List<Attendance> attendanceList = DbHelper.getInstance(context).selectAttendanceListOrderByName(startDate, endDate);
////        WorkShift workShift = DbHelper.getInstance(context).selectWorkShift();
////
////        String[] tableHeaders = {"日期", "姓名", "编号", "上班时间", "下班时间", "温度"};
////
////        // 新建工作簿
////        Workbook workbook = new XSSFWorkbook();
////        Sheet sheet = workbook.createSheet("sheet1");
////        sheet.setDefaultColumnWidth(15);
////        sheet.setColumnWidth(3, 25 * 256);
////        sheet.setColumnWidth(4, 25 * 256);
////        // 打印时水平居中
////        sheet.setHorizontallyCenter(true);
////
////        // 单元格样式
////        CellStyle cellStyle = workbook.createCellStyle();
////        cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
////        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
////        //字体格式 
////        Font font = workbook.createFont();
////        font.setFontName("黑体");
////        font.setFontHeightInPoints((short) 14); //设置字体大小   
////        font.setBold(true);
////        cellStyle.setFont(font);
////
////        // 迟到早退：红色字体
////        CellStyle cellStyleRed = workbook.createCellStyle();
////        cellStyleRed.setAlignment(CellStyle.ALIGN_CENTER);
////        cellStyleRed.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
////        Font fontRed = workbook.createFont();
////        fontRed.setFontName("黑体");
////        fontRed.setFontHeightInPoints((short) 14);
////        fontRed.setBold(true);
////        fontRed.setColor(HSSFColor.RED.index);
////        cellStyleRed.setFont(fontRed);
////
////        // 第一行作为表头
////        Row row = sheet.createRow(0);
////
////        //创建表头
////        for (int i = 0; i < tableHeaders.length; i++) {
////            Cell cell = row.createCell(i);
////            cell.setCellValue(tableHeaders[i]);
////            cell.setCellStyle(cellStyle);
////        }
////
////        for (int j = 0; j < attendanceList.size(); j++) {
////            row = sheet.createRow(j + 1);
////            for (int k = 0; k <= tableHeaders.length - 1; k++) {
////                Cell cell = row.createCell(k);
////                Attendance attendance = attendanceList.get(j);
////                switch (k) {
////                    case 0:
////                        cell.setCellValue(DateUtils.getDateToString(attendance.getDate(), "yyyy-MM-dd"));
////                        cell.setCellStyle(cellStyle);
////                        break;
////                    case 1:
////                        cell.setCellValue(attendance.getName());
////                        cell.setCellStyle(cellStyle);
////                        break;
////                    case 2:
////                        cell.setCellValue(attendance.getNumber());
////                        cell.setCellStyle(cellStyle);
////                        break;
////                    case 3:
////                        cell.setCellValue(attendance.getClockInTime());
////                        if (workShift != null
////                                && attendance.getClockInTime() != null
////                                && DateUtils.compareTime(attendance.getClockInTime(), workShift.getStartWorkTime(), "HH:mm")) {
////                            cell.setCellStyle(cellStyleRed);
////                        }
////                        break;
////                    case 4:
////                        cell.setCellValue(attendance.getClockOutTime());
////                        if (workShift != null
////                                && attendance.getClockOutTime() != null
////                                && DateUtils.compareTime(workShift.getOffWorkTime(), attendance.getClockOutTime(), "HH:mm")) {
////                            cell.setCellStyle(cellStyleRed);
////                        }
////                        break;
////                    case 5:
////                        if (attendance.getTemperature() != null) {
////                            cell.setCellValue(attendance.getTemperature());
////                        }
////                        cell.setCellStyle(cellStyle);
////                        break;
////                }
////            }
////        }
////        try {
////            File file = new File(sdcard + "/考勤记录/考勤记录" + "_" +
////                    DateUtils.getDateToString(startDate, "yyyy-MM-dd") + "~" + DateUtils.getDateToString(endDate, "yyyy-MM-dd") + ".xlsx");
////            OutputStream out = new FileOutputStream(file);
////            workbook.write(out);
////            out.flush();
////            out.close();
////            Thread.sleep(4000);
////            importExportListener.success();
////        } catch (IOException | InterruptedException e) {
////            LogUtil.e(DEBUG_TAG, e.getMessage());
////            importExportListener.error(e.getMessage());
////        }
////    }
////
////    /**
////     * 导出通行记录
////     * @param importExportListener 用于导出时弹窗的监听器
////     */
////    public static void exportPassRecordExcel(Long startDate, Long endDate, ImportExportListener importExportListener) {
////        if (!USBReceiver.mounted) {
////            importExportListener.error("未发现U盘，如已插入U盘，请重新插拔后再试");
////            return;
////        }
////
////        importExportListener.inProgress();
////
////        String sdcard = USBReceiver.mountPath;
////        if (!SharedPreferenceUtil.getInstance().getBoolean(IS_YS)) {
////            sdcard = "/mnt/media_rw/" + sdcard.substring(sdcard.lastIndexOf("/"));
////        }
////        File dir = new File (sdcard + "/识别记录");
////        if (!dir.exists()) {
////            boolean isMk = dir.mkdirs();
////            LogUtil.e(DEBUG_TAG, " 创建文件夹：" + isMk);
////        }
////
////        Context context = MyApplication.getApp().getContext();
////        List<PassRecord> passRecordList = DbHelper.getInstance(context).selectPassRecord(1, 999999, startDate, endDate);
////        String[] tableHeaders = {"姓名", "编号", "温度", "记录时间"};
////
////        // 新建工作簿
////        Workbook workbook = new XSSFWorkbook();
////        Sheet sheet = workbook.createSheet("sheet1");
////        sheet.setDefaultColumnWidth(15);
////        sheet.setColumnWidth(3, 25 * 256);
////        sheet.setColumnWidth(4, 25 * 256);
////        // 打印时水平居中
////        sheet.setHorizontallyCenter(true);
////        // 单元格样式
////        CellStyle cellStyle = workbook.createCellStyle();
////        cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
////        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
////
////        //字体格式 
////        Font font = workbook.createFont();
////        font.setFontName("黑体");
////        font.setFontHeightInPoints((short) 14);//设置字体大小   
////        font.setBold(true);
////        cellStyle.setFont(font);
////
////        // 第一行作为表头
////        Row row = sheet.createRow(0);
////
////        //创建表头
////        for (int i = 0; i < tableHeaders.length; i++) {
////            Cell cell = row.createCell(i);
////            cell.setCellValue(tableHeaders[i]);
////            cell.setCellStyle(cellStyle);
////        }
////
////        for (int j = 0; j < passRecordList.size(); j++) {
////            row = sheet.createRow(j + 1);
////            PassRecord passRecord = passRecordList.get(j);
////            for (int k = 0; k <= tableHeaders.length - 1; k++) {
////                Cell cell = row.createCell(k);
////                switch (k) {
////                    case 0:
////                        cell.setCellValue(passRecord.getName());
////                        break;
////                    case 1:
////                        cell.setCellValue(passRecord.getNumber());
////                        break;
////                    case 2:
////                        if (passRecord.getTemperature() != null) {
////                            cell.setCellValue(passRecord.getTemperature());
////                        } else {
////                            cell.setCellValue("-");
////                        }
////                        break;
////                    case 3:
////                        if (passRecord.getRecordTime() != null) {
////                            String recordTime = DateUtils.getDateToString(passRecord.getRecordTime(), "yyyy-MM-dd HH:mm:ss");
////                            cell.setCellValue(recordTime);
////                        }
////                        break;
////                }
////                cell.setCellStyle(cellStyle);
////            }
//////            Bitmap bitmap = DbHelper.getInstance(context).getBitmap(userInfoList.get(j).getNumber());
//////            try {
//////                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//////                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//////                File file = new File(dir, userInfoList.get(j).getNumber() + ".jpg");
//////                FileOutputStream fo = new FileOutputStream(file);
//////                fo.write(bytes.toByteArray());
//////                fo.flush();
//////                fo.close();
//////            } catch (Exception e) {
//////                LogUtil.e(DEBUG_TAG, e.getMessage());
//////            }
////        }
////        try {
////            File file = new File(sdcard + "/识别记录/识别记录" + "_" +
////                    DateUtils.getDateToString(System.currentTimeMillis(), "yyyy-MM-dd") + ".xlsx");
////            OutputStream out = new FileOutputStream(file);
////            workbook.write(out);
////            out.flush();
////            out.close();
////            Thread.sleep(10000);
////            importExportListener.success();
////        } catch (IOException | InterruptedException e) {
////            LogUtil.e(DEBUG_TAG, e.getMessage());
////            importExportListener.error(e.getMessage());
////        }
////    }
////
////    public interface ImportExportListener {
////        // 导出进行中
////        void inProgress();
////        // 导出完成
////        void success();
////        // 导出失败
////        void error(String msg);
////    }
//
//}
