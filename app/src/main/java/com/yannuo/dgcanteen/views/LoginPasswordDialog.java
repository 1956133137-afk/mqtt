package com.yannuo.dgcanteen.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.activitys.SettingActivity;
import com.yannuo.dgcanteen.util.ToastShowUtil;

import java.util.Calendar;

public class LoginPasswordDialog extends AppCompatActivity {
    private Intent intent;
    private AlertDialog dialog;
    private WindowManager.LayoutParams params;
    private EditText et;

    public void PasswordDialog(Context context, Display display){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        dialog = builder.setView(R.layout.dialog_login_password).create();
        dialog.show();

        params = dialog.getWindow().getAttributes();
        Point point = new Point();
        display.getSize(point);
        params.width = (int) (point.x * 0.3);
        params.height = (int) (point.y * 0.3);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        dialog.getWindow().setAttributes(params);

        et = dialog.getWindow().findViewById(R.id.input_password);
        et.addTextChangedListener(new HideTextWatcher(context,4));
    }

    private class HideTextWatcher implements TextWatcher {
        private int mMaxLength;
        private Context context;
        public HideTextWatcher(Context context,int maxLength) {
            this.context = context;
            this.mMaxLength = maxLength;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() == mMaxLength){
                if (editable.toString().equals(getCurrentTime())){
                    dialog.dismiss();
                    ToastShowUtil.show(this.context,"密码正确，正在跳转页面");
                    intent = new Intent(this.context, SettingActivity.class);
                    this.context.startActivity(intent);
                } else {
                    et.setText(null);
                    ToastShowUtil.show(this.context,"密码错误，请重新输入");
                }
            }
        }
    }
    private String getCurrentTime(){
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        StringBuilder builder = new StringBuilder();
        builder.append(month < 10 ? "0" + month : month);
        builder.append(day < 10 ? "0" + day : day);
        return builder.toString();
    }
}
