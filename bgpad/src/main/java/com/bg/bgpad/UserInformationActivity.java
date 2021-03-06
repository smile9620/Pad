package com.bg.bgpad;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.rtp.AudioGroup;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.TextView;

import com.bg.constant.Constant;
import com.bg.constant.DeviceName;
import com.bg.constant.InBodyBluetooth;
import com.bg.model.InBodyData;
import com.bg.model.User;
import com.bg.utils.FormatString;
import com.bg.utils.MyDialog;
import com.bg.utils.SetTitle;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UserInformationActivity extends BleActivityResult implements SetTitle.OnTitleBtClickListener {

    @BindView(R.id.title)
    View view;
    @BindView(R.id.image)
    ImageButton image;
    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.usernumber)
    EditText usernumber;
    @BindView(R.id.sex)
    RadioGroup sex;
    @BindView(R.id.boy)
    RadioButton boy;
    @BindView(R.id.girl)
    RadioButton girl;
    @BindView(R.id.birthday)
    EditText birthday;
    @BindView(R.id.age)
    TextView age;
    @BindView(R.id.height)
    EditText height;
    @BindView(R.id.test)
    Button test;
    @BindView(R.id.showheight)
    TableRow showheight;

    private List<User> users;
    private User sendUser;
    private boolean column;
    private boolean showdialog = true;
    private int selectsex = 0;
    private String photopath;
    public static UserInformationActivity instance;
    private Intent intent;
    private boolean user_exist;
    private boolean ble_enable = true;
    private String strBirth;
    public String sendData;
    private MyHandler handler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<UserInformationActivity> mActivity;

        public MyHandler(UserInformationActivity activity) {
            mActivity = new WeakReference<UserInformationActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            UserInformationActivity act = mActivity.get();
            if (act != null) {
                switch (msg.what) {
                    case 0:
                        act.getFocusable();
                        break;
                    case 1:
                        act.test.setClickable(true);
                        break;
                }
            }
        }
    }

    public UserInformationActivity() {
        instance = UserInformationActivity.this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_information);
        ButterKnife.bind(this);

        intent = getIntent();
        if (intent.getStringExtra("user_number") != null) {
            user_exist = true;
        }
        new SetTitle(this, view, new boolean[]{true, user_exist},
                "测试", new int[]{R.drawable.back_bt, R.drawable.delete_bt});
        column = intent.getBooleanExtra("column", true);
        if (!column) {
            showheight.setVisibility(View.VISIBLE);
        } else {
            showheight.setVisibility(View.INVISIBLE);
        }
        sex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (boy.isChecked()) {
                    selectsex = 1;  //性别 0 代表女生，1 代表男生
                } else {
                    selectsex = 0;
                }
            }
        });

        if (user_exist) {
            showdialog = false;
            usernumber.setFocusable(false);
            username.setFocusable(false);
            boy.setClickable(false);
            girl.setClickable(false);
            birthday.setClickable(false);
            image.setClickable(false);

            users = DataSupport.where(" user_number = ? ", intent.getStringExtra("user_number")).find(User.class);
            username.setText(users.get(0).getUser_name());
            usernumber.setText(users.get(0).getUser_number());
            if (users.get(0).getSex() == 1) { //性别 0 代表女生，1 代表男生
                boy.setChecked(true);
            } else {
                girl.setChecked(true);
            }
            birthday.setText(users.get(0).getBirthday());
            age.setText(String.valueOf(users.get(0).getAge()));
            if (users.get(0).getImage_path() != null) {
                File path1 = new File(getSDPath() + "/Image");
                if (path1.exists()) {
                    File file = new File(path1, users.get(0).getImage_path());
                    Uri imaUri = Uri.fromFile(file);
                    showImage(imaUri);
                }
            }
        }
    }

    @Override
    protected void updateState(boolean bool) {
        ble_enable = bool;
        if (!bool) {
            new MyDialog(this).setDialog("蓝牙已断开，请重新连接！", false, true, new MyDialog.DialogConfirm() {
                @Override
                public void dialogConfirm() {
                    finish();
                }
            }).show();
        }
    }

    private int time = 0;

    @Override
    protected void updateData(String str) {
        String[] datas = str.split(" ");
        if ((datas[0] + datas[1]).toString().equals(DeviceName.InBody_Head) &&
                datas[3].equals("13") && Integer.parseInt(datas[2], 16) == (datas.length - 3) &&
                datas[datas.length - 1].equals("FF")) {
            String req = FormatString.formateData(datas, new int[]{4, 4});
            if (req.equals("1")) {// 0 不可以发送数据， 1 可以发送数据
                if (sendData != null && sendUser != null) {
                    Intent intent = new Intent(this, InBodyTestReportActivity.class);
                    intent.putExtra("user", sendUser);
                    startActivityForResult(intent, Constant.TEST_REQ);
                }
            } else {
                handler.sendEmptyMessage(0);
            }
        } else {
            if (time < 3) {
                test();
                time++;
            }

        }
    }

    @Override
    public void leftBt(ImageButton left) {
        finishActivity();
    }

    @Override
    public void rightBt(ImageButton right) {
        final String number = intent.getStringExtra("user_number");
        final List<InBodyData> list = DataSupport.where("user_number = ?", number).find(InBodyData.class);
        new MyDialog(this).setDialog("是否删除该用户以及该用户下的" + list.size() + "条测试数据！", true, true, new MyDialog.DialogConfirm() {
            @Override
            public void dialogConfirm() {
                DataSupport.deleteAll(User.class, "user_number = ?", number);
                DataSupport.deleteAll(InBodyData.class, "user_number = ?", number);
                if (users.get(0).getImage_path() != null) {  //删除头像
                    deleteImage(users.get(0).getImage_path());
                }
                showToast(null,"删除成功！");
                finish();
            }
        }).show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @OnClick({R.id.image, R.id.test, R.id.birthday})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image:
                Permission();
                break;
            case R.id.birthday:
                showDialog(showdialog);
                break;
            case R.id.test:
                test.setClickable(false);
                handler.sendEmptyMessageDelayed(1, 3000);//按钮禁用3秒，防止重复点击
                String usernu = usernumber.getText().toString().trim();
                String userna = username.getText().toString().trim();
                String bir = birthday.getText().toString().trim();
                String heig = height.getText().toString().trim();
                sendData = checkUser(usernu, userna, bir, heig);
                if (sendData != null) {
                    sendUser = new User();
                    sendUser.setUser_number(usernu);
                    sendUser.setUser_name(userna);
                    sendUser.setBirthday(bir);
                    sendUser.setSex(selectsex);
                    if (photopath != null) {
                        sendUser.setImage_path(photopath);
                    }
                    if (!user_exist) {
                        final List<User> list = DataSupport.where("user_number = ?", usernu).find(User.class);
                        if (list.size() != 0) {
                            user_exist = true;
                            if (sendUser.getUser_name().equals(list.get(0).getUser_name()) &&
                                    sendUser.getSex() == list.get(0).getSex() &&
                                    sendUser.getBirthday().equals(list.get(0).getBirthday())) {
                                test();
                            } else {
                                new MyDialog(UserInformationActivity.this).setDialog("编号为：" + usernu +
                                        " 的用户本地已存在,是否进行覆盖？", true, true, new MyDialog.DialogConfirm() {
                                    @Override
                                    public void dialogConfirm() {
                                        ContentValues values = new ContentValues();
                                        values.put("user_name", sendUser.getUser_name());
                                        values.put("birthday", sendUser.getBirthday());
                                        values.put("sex", sendUser.getSex());
                                        DataSupport.update(User.class, values, list.get(0).getId());
                                        test();
                                    }
                                }).show();
                            }
                            if (sendUser.getImage_path() != null) {
                                ContentValues val = new ContentValues();
                                val.put("image_path", sendUser.getImage_path());
                                if (DataSupport.update(User.class, val, list.get(0).getId()) > -1) {
                                    deleteImage(list.get(0).getImage_path());
                                }
                            }
                        } else {
                            test();
                        }
                    } else {
                        test();
                    }
                }
                break;
        }
    }

    private void getFocusable() {
        new MyDialog(this).setDialog("下位机已存在该编号，请修改编号!", true, false, null).show();
        user_exist = false;

        username.setFocusable(true);
        username.setFocusableInTouchMode(true);
        username.requestFocus();

        boy.setClickable(true);
        girl.setClickable(true);
        birthday.setClickable(true);
        image.setClickable(true);

        usernumber.setFocusable(true);
        usernumber.setFocusableInTouchMode(true);
        usernumber.requestFocus();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private String checkUser(String number, String name, String birth, String he) {
        String result;
        if (number == null || number.isEmpty()) {
            usernumber.setHint("编号不能为空！");
            usernumber.setHintTextColor(Color.RED);
            return null;
        }

        if (name == null || name.isEmpty()) {
            username.setHint("姓名不能为空！");
            username.setHintTextColor(Color.RED);
            return null;
        }
        if (birth == null || birth.isEmpty()) {
            birthday.setHint("出生日期不能为空！");
            birthday.setHintTextColor(Color.RED);
            return null;
        }
        if (!column) {
            if (height.isCursorVisible()) {
                if (he == null || he.isEmpty()) {
                    height.setHint("身高不能为空！");
                    height.setHintTextColor(Color.RED);
                    return null;
                }
            }
        }

        int create = 0;//用户类别 1个字节  已有用户：0  新建：1
        int number_length = 0; //编号18个字节
        int name_length = 0;//姓名8个字节
        int he_length = 0;//身高3个字节
        if (!user_exist) {
            create = 1;
        }
        number_length = number.getBytes().length;//编号长度18字节
        try {
            name_length = name.getBytes("gbk").length;//姓名长度8字节
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        he_length = he.getBytes().length;//身高长度3字节

        if (number_length < 18) {
            for (int i = 0; i < 18 - number_length; i++) {
                number = number + " ";
            }
        }
        if (name_length < 8) {
            for (int i = 0; i < 8 - name_length; i++) {
                name = name + " ";
            }
        }
        if (!column) {
            if (he_length < 3) {
                for (int i = 0; i < 18 - name_length; i++) {
                    he = he + " ";
                }
            }
        } else {
            he = "   ";
        }

        String bi = birthday.getText().toString();
        String[] b = bi.split("-");
        strBirth = b[0] + b[1] + b[2];

        result = String.valueOf(create) + number + name + (boy.isChecked() ? 1 : 0) + strBirth +
                (column == false ? 0 : 1) + he;
        return result;
    }

    private Calendar cal = Calendar.getInstance();

    private void showDialog(Boolean bool) {
        if (bool) {

            final Date currentTime = new Date();
            new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    String mon = String.valueOf(month + 1);
                    if ((month + 1) < 10) {
                        mon = "0" + (month + 1);
                    }
                    String day = String.valueOf(dayOfMonth);
                    if (dayOfMonth < 10) {
                        day = "0" + dayOfMonth;
                    }
                    String data = year + "-" + mon + "-" + day;
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = null;
                    try {
                        date = formatter.parse(data);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (date.getTime() > currentTime.getTime()) {
                        showToast(null,"出生日期大于当前日期！");
                    } else {
                        birthday.setText(data);
                        DecimalFormat df = new DecimalFormat("0.0");
                        int cur_year = cal.get(Calendar.YEAR);
                        int cur_month = cal.get(Calendar.MONTH) + 1;
                        float ag = (cur_month - (month + 1)) / 12f + cur_year - year;
                        age.setText(String.valueOf(df.format(ag)));
                    }
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        }
    }

    private void Permission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                //申请CAMERA权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.
                        permission.CAMERA}, Constant.CAMERA_REQ);
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.
                        permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.PICTURE_REQ);
                return;
            }
        }

        File path1 = new File(getSDPath() + "/Image");
        if (!path1.exists()) {
            path1.mkdir();
        }
        if (photopath == null) {
            photopath = System.currentTimeMillis() + ".jpg";
        }
        File file = new File(path1, photopath);
        Constant.imageUri = Uri.fromFile(file);

//        Intent intent = new Intent(UserInformationActivity.this, CameraActivity.class);
//        startActivityForResult(intent, Constant.CAMERA_STA_USERINFO);

        Intent in = new Intent("android.media.action.IMAGE_CAPTURE");
        Uri photoUri = Uri.fromFile(new File(file.getPath()));
        in.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(in, Constant.CAMERA_STA_USERINFO);
    }

    private void finishActivity() {

        if (!user_exist && photopath != null) {
            deleteImage(photopath);
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case Constant.CAMERA_REQ:
                case Constant.PICTURE_REQ:
                    Permission();
                    break;
            }
        } else {
            showToast(null,"请在应用管理中打开访问权限！");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showImage(Uri uri) {
        Bitmap bitmap;
        try {
            // 读取uri所在的图片
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Matrix matrix = new Matrix(); //抗锯齿
            matrix.postScale(0.2f, 0.2f);
            Bitmap temp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            image.setBackground(new BitmapDrawable(this.getResources(), temp));
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        } catch (Exception e) {
            photopath = null;
            e.printStackTrace();
        }
    }

    private void test() {
        if (ble_enable) { //蓝牙可用时，才能写数据
//          writeData("34122219870611503X诸葛亮  1200006110180", new byte[]{(byte) 0xEA, (byte) 0x52, (byte) 0x29, (byte) 0x22, (byte) 0xFF});
            writeData(sendData, new byte[]{(byte) 0xEA, (byte) 0x52, (byte) 0x2A, (byte) 0x22, (byte) 0xFF});
        } else {
            showToast(null,"蓝牙已断开，无法测试！");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constant.CAMERA_STA_USERINFO:
                    showImage(Constant.imageUri);
                    break;
                case Constant.TEST_REQ:
                    test();
                    break;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishActivity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeMessages(0);
    }
}