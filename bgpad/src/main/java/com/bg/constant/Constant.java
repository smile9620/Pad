package com.bg.constant;

import android.net.Uri;
import android.os.Environment;

import com.bg.bgpad.BluetoothLeService;

/**
 * Created by zjy on 2017-03-06.
 */

public class Constant {

    public final static int CAMERA_STA_USERINFO = 0;   // 开启相机
    public final static int CAMERA_REQ = 1;   // 相机权限
    public final static int PICTURE_REQ = 2;  // 相册权限
    public final static int BT_REQ = 3; // 蓝牙权限
    public final static int QR_REQ  = 4; //开启二维码
    public final static int EXPORT_REQ  = 5; //导出数据
    public final static int TEST_REQ  = 6; //导出数据
    public final static int REQUEST_ACCESS_LOCATION = 7; //蓝牙权限
    public final static int REQUEST_GRANTED_ACCESS = 8; //相机权限
    public static boolean mScanning = true; //蓝牙是否在搜索中
    public static String mDeviceAddress; // 蓝牙地址
    public static BluetoothLeService mBluetoothLeService = null;
    public static Uri imageUri = null;
}
