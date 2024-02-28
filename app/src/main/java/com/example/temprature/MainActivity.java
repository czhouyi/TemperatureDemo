package com.example.temprature;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.temprature.databinding.ActivityMainBinding;
import com.hcusbsdk.Interface.JavaInterface;
import com.hcusbsdk.Interface.USB_DEVICE_INFO;
import com.hcusbsdk.Interface.USB_SYSTEM_DEVICE_INFO;
import com.hik.f2module.F2UsbModuleHelper;
import com.hik.modulelib.UsbModuleInfo;
import com.hik.modulelib.manager.UsbActionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private F2UsbModuleHelper f2UsbModuleHelper;
    private static final String TAG = "MainActivity1";
    private static final String F2_FPGA_ID_01 = "0953560101"; //测温 P20Max 测温6.9
    private static final String F2_FPGA_ID_02 = "0953560102"; //商用 E20 红色小镜头 商用6.9
    private static final String F2_FPGA_ID_03 = "0953560103"; //商用 E20Plus 红色大镜头 商用9.7
    private static final String F2_FPGA_ID_04 = "0953560104"; //测温 P20
    private static final String F2_FPGA_ID_05 = "0953560105"; //测温 P20R
    private static final Set<String> F2_FPGA_ID_List_Viewer = new HashSet<>();
    private static final Set<String> F2_FPGA_ID_List_Sight = new HashSet<>();

    static {
        F2_FPGA_ID_List_Viewer.add(F2_FPGA_ID_01);
        F2_FPGA_ID_List_Viewer.add(F2_FPGA_ID_04);
        F2_FPGA_ID_List_Viewer.add(F2_FPGA_ID_05);
        F2_FPGA_ID_List_Sight.add(F2_FPGA_ID_02);
        F2_FPGA_ID_List_Sight.add(F2_FPGA_ID_03);
    }

    TextView textView;
    List<String> tips = new ArrayList<>();
    private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";

    private void appendTip(String tip) {
        tips.add(tip);
        textView.setText(String.join("\n", tips));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.mainText);
        Button btnGetTemp = (Button) findViewById(R.id.btn_get_temp);
        btnGetTemp.setOnClickListener(view -> {
            requestUsbPermission();
//            openUsbDevice();
        });
        f2UsbModuleHelper = F2UsbModuleHelper.INSTANCE;
        tryGetUsbPermission();

        appendTip("App打开");
        UsbActionManager usbActionManager = new UsbActionManager(getLifecycle(), action -> {
            if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                appendTip("插入USB设备");
                Log.d(TAG, "插入USB设备");
//                tryGetUsbPermission();
//                openUsbDevice();
            } else if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                f2UsbModuleHelper.stopStreamPreview();
                appendTip("拔出USB设备");
                Log.d(TAG, "拔出USB设备");
            }
            return null;
        });
    }

    private void tryGetUsbPermission() {
        tips.clear();
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionActionReceiver, filter);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        appendTip("设备数：" + usbManager.getDeviceList().size());
        for (UsbDevice usbDevice: usbManager.getDeviceList().values()) {
            if (usbManager.hasPermission(usbDevice)) {
                appendTip("有权限");
                openUsbDevice();
            } else {
                appendTip("无权限");
                usbManager.requestPermission(usbDevice, pendingIntent);
            }
        }

    }

    private void requestUsbPermission() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        appendTip("设备数：" + usbManager.getDeviceList().size());
        for (UsbDevice usbDevice: usbManager.getDeviceList().values()) {
            if (usbManager.hasPermission(usbDevice)) {
                appendTip("有权限");
                openUsbDevice();
            } else {
                appendTip("无权限");
                usbManager.requestPermission(usbDevice, pendingIntent);
            }
        }

    }

    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if(null != usbDevice){
                            openUsbDevice();
                        }
                    }
                    else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
//                        Toast.makeText(context, String.valueOf("Permission denied for device" + usbDevice), Toast.LENGTH_LONG).show();
                        appendTip("Permission denied for device" + usbDevice);
                    }
                }
            }
        }
    };

    private void openUsbDevice() {
        tips.clear();
        try {
            boolean init = f2UsbModuleHelper.USB_Init();
            if (!init) {
                appendTip("init失败");
                return;
            } else {
                appendTip("init成功");
            }
            int deviceCount = f2UsbModuleHelper.USB_GetDeviceCount(getApplicationContext());
//            deviceCount = f2UsbModuleHelper.USB_GetDeviceCount(getApplicationContext());
            appendTip("设备数量：" + deviceCount);
//            USB_DEVICE_INFO[] deviceInfoArray = new USB_DEVICE_INFO[JavaInterface.MAX_DEVICE_NUM];
//            JavaInterface.getInstance().USB_EnumDevices(deviceCount, deviceInfoArray);
//            appendTip(deviceInfoArray[0].szSerialNumber);
            boolean result = f2UsbModuleHelper.openUsbDevice(getApplicationContext());
            if (result) {
                Log.d(TAG, "F2模组打开成功");
                appendTip("F2模组打开成功");

                UsbModuleInfo usbModuleInfo = f2UsbModuleHelper.USB_GetSystemDeviceInfo();
                if (usbModuleInfo != null) {
                    appendTip("获取F2模组信息成功");
                    Log.d(TAG, "获取F2模组信息成功");
                    Log.d(TAG, "deviceType: ${usbModuleInfo.deviceType}");
                    Log.d(TAG, "serialNumber: ${usbModuleInfo.serialNumber}");
                    Log.d(TAG, "deviceName: ${usbModuleInfo.deviceName}");
                    Log.d(TAG, "devType: ${usbModuleInfo.devType}");
                    dealParam(usbModuleInfo);
                    //开始预览
                    f2UsbModuleHelper.startStreamPreview(
                            new TempStreamCallBack(getApplicationContext()),
                            new Size(Constants.PREVIEW_WIDTH, Constants.PREVIEW_HEIGHT_SDK),
                            Constants.f2ModuleFrameRate, Constants.f2ModuleVideoCodingType);
                } else {
                    appendTip("获取F2模组信息失败");
                    Log.d(TAG, "获取F2模组信息失败");
                }
            } else {
                appendTip("F2模组打开失败");
                Log.d(TAG, "F2模组打开失败");
            }
        } catch (Throwable e) {
            StackTraceElement[] steArr = e.getStackTrace();
            String msg = e.getMessage();
            for (StackTraceElement ste : steArr) {
                msg += "\n" + ste.getClassName() + "#" + ste.getMethodName() + ":" + ste.getLineNumber();
            }
            appendTip("发生了错误：" + msg);
        }
    }

    private void dealParam(UsbModuleInfo usbModuleInfo) {
        UsbModuleVersionBean usbModuleVersion = convertUsbModuleVersion(usbModuleInfo.getFirmwareVersion(), usbModuleInfo.getHardwareVersion());
        if (usbModuleVersion == null) {
            return;
        }
        if (F2_FPGA_ID_List_Sight.contains(usbModuleVersion.moduleId)) {
            //商用模组
            Constants.f2ModuleFrameRate = Constants.F2_MODULE_FRAMERATE_50;
            Log.e(TAG, "checkAppCanWorkWithTheDevice: f2ModuleFrameRate = 50");
            //兼容固件版本
            if (Integer.parseInt(usbModuleVersion.buildDate) <= 20220922) {
                //是以前的旧固件,采用截断的码流8,如 20220922 这个版本的固件
                Constants.f2ModuleVideoCodingType = Constants.F2_MODULE_VIDEO_CODING_TYPE_8;
                Constants.f2ModuleVideoCodingTypeSize = Constants.F2_MODULE_VIDEO_CODING_TYPE_8_SIZE_CUT;
                Constants.f2ModuleIsNewFirmware = false;
                Log.e(TAG, "checkAppCanWorkWithTheDevice: 是以前的旧固件-商用模组");
            } else {
                //是新固件，采用完整码流8
                Constants.f2ModuleVideoCodingType = Constants.F2_MODULE_VIDEO_CODING_TYPE_8;
                Constants.f2ModuleVideoCodingTypeSize = Constants.F2_MODULE_VIDEO_CODING_TYPE_8_SIZE;
                Constants.f2ModuleIsNewFirmware = true;
                Log.e(TAG, "checkAppCanWorkWithTheDevice: 是新固件-商用模组");
            }
        } else if (F2_FPGA_ID_List_Viewer.contains(usbModuleVersion.moduleId)) {
            //测温模组
            Constants.f2ModuleFrameRate = Constants.F2_MODULE_FRAMERATE_25;
            Log.e(TAG, "checkAppCanWorkWithTheDevice: f2ModuleFrameRate = 25");
            //兼容固件版本
//            if (usbModuleVersion.buildDate.toInt() <= 20221118) {
            if (Integer.parseInt(usbModuleVersion.buildDate) <= 20221121) {
                //是以前的旧固件,采用截断的码流8,如 20221118 这个版本的固件
                Constants.f2ModuleVideoCodingType = Constants.F2_MODULE_VIDEO_CODING_TYPE_8;
                Constants.f2ModuleVideoCodingTypeSize = Constants.F2_MODULE_VIDEO_CODING_TYPE_8_SIZE_CUT;
                Constants.f2ModuleIsNewFirmware = false;
                Log.e(TAG, "checkAppCanWorkWithTheDevice: 是以前的旧固件");
            } else {
                //是新固件，采用码流11
                Constants.f2ModuleVideoCodingType = Constants.F2_MODULE_VIDEO_CODING_TYPE_11;
                Constants.f2ModuleVideoCodingTypeSize = Constants.F2_MODULE_VIDEO_CODING_TYPE_11_SIZE;
                Constants.f2ModuleIsNewFirmware = true;
                Log.e(TAG, "checkAppCanWorkWithTheDevice: 是新固件");
            }
        }
    }

    private UsbModuleVersionBean convertUsbModuleVersion(String firmwareVersionDesc, String fpgaVersionDesc) {
        //APP_020003_20220824
        String[] firmwareVersionDescList = firmwareVersionDesc.split("_");
        if (firmwareVersionDescList.length < 3) {
            Log.e(TAG, "convertUsbModuleVersion: firmwareVersionDesc 版本号格式有误");
            return null;
        }
        //APP
        String firmwarePrefix = firmwareVersionDescList[0];
        //020003
        String firmwareVersion = firmwareVersionDescList[1];
        //20220824
        String buildDate = firmwareVersionDescList[2];
        //
        //FPGA_032101_0953560101
        String[] fpgaVersionDescList = fpgaVersionDesc.split("_");
        if (fpgaVersionDescList.length < 3) {
            Log.e(TAG, "convertUsbModuleVersion: fpgaVersionDesc 版本号格式有误");
            return null;
        }
        //FPGA
        String fpgaPrefix = fpgaVersionDescList[0];
        //032101
        String fpgaVersion = fpgaVersionDescList[1];
        //0953560101
        String moduleId = fpgaVersionDescList[2];
        //
        UsbModuleVersionBean usbModuleVersion = new UsbModuleVersionBean();
        usbModuleVersion.firmwareVersion = firmwareVersion;
        usbModuleVersion.fpgaVersion = fpgaVersion;
        usbModuleVersion.buildDate = buildDate;
        usbModuleVersion.moduleId = moduleId;
        return usbModuleVersion;
    }

    class UsbModuleVersionBean {
        String firmwareVersion;
        String fpgaVersion;
        String buildDate;
        String moduleId;
    }

    private void openUsbDevice1() {
        tips.clear();
        try {
            CheckPermission();
            CheckNetworkPermission();
            CheckPermission(android.Manifest.permission.CAMERA);
            //初始化USBSDK
            if (JavaInterface.getInstance().USB_Init()) {
                appendTip("USB_Init Success!");
                Log.i("[USBDemo]", "USB_Init Success!");
            } else {
                appendTip("USB_Init Failed!");
                Log.e("[USBDemo]", "USB_Init Failed!");
                Toast.makeText(this, "USB_Init Failed!", Toast.LENGTH_SHORT).show();
                return;
            }

            int m_dwDevCount = JavaInterface.getInstance().USB_GetDeviceCount(this);
            m_dwDevCount = JavaInterface.getInstance().USB_GetDeviceCount(this);
            appendTip("USB_GetDeviceCount Device count is :" + m_dwDevCount);
            if (m_dwDevCount > 0) {
                Log.i("[USBDemo]", "USB_GetDeviceCount Device count is :" + m_dwDevCount);
            } else if(m_dwDevCount == 0) {
                Log.i("[USBDemo]", "USB_GetDeviceCount Device count is :" + m_dwDevCount);
                return;
            } else {
                Log.e("[USBDemo]", "USB_GetDeviceCount failed! error:" + JavaInterface.getInstance().USB_GetLastError());
                return;
            }
            //获取设备信息
            USB_DEVICE_INFO[] deviceInfoArray = new USB_DEVICE_INFO[JavaInterface.MAX_DEVICE_NUM];
            if (JavaInterface.getInstance().USB_EnumDevices(m_dwDevCount, deviceInfoArray)) {
                //打印设备信息
                for (int i = 0; i < m_dwDevCount; i++) {
                    appendTip("USB_EnumDevices Device info is szSerialNumber:" + deviceInfoArray[i].szSerialNumber);
                    Log.i("[USBDemo]", "USB_EnumDevices Device info is dwIndex:" + deviceInfoArray[i].dwIndex +
                            " dwVID:" + deviceInfoArray[i].dwVID +
                            " dwPID:" + deviceInfoArray[i].dwPID +
                            " szManufacturer:" + deviceInfoArray[i].szManufacturer +
                            " szDeviceName:" + deviceInfoArray[i].szDeviceName +
                            " szSerialNumber:" + deviceInfoArray[i].szSerialNumber +
                            " byHaveAudio:" + deviceInfoArray[i].byHaveAudio);
                }
            } else {
                appendTip("USB_EnumDevices failed! error:" + JavaInterface.getInstance().USB_GetLastError());
                Log.e("[USBDemo]", "USB_EnumDevices failed! error:" + JavaInterface.getInstance().USB_GetLastError());
                Toast.makeText(this, "USB_EnumDevices failed! error:" + JavaInterface.getInstance().USB_GetLastError(), Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (Throwable e) {
            StackTraceElement[] steArr = e.getStackTrace();
            String msg = e.getMessage();
            for (StackTraceElement ste : steArr) {
                msg += "\n" + ste.getClassName() + "#" + ste.getMethodName() + ":" + ste.getLineNumber();
            }
            appendTip("发生了错误：" + msg);
        }
    }
    //读写文件权限动态申请
    public void CheckPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            // 判断是否有这个权限，是返回PackageManager.PERMISSION_GRANTED，否则是PERMISSION_DENIED
            // 这里我们要给应用授权所以是!= PackageManager.PERMISSION_GRANTED
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("[USBDemo]", "未获得读写权限");
                // 如果应用之前请求过此权限但用户拒绝了请求,且没有选择"不再提醒"选项 (后显示对话框解释为啥要这个权限)，此方法将返回 true。
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    Log.i("[USBDemo]", "用户永久拒绝权限申请");
                }
                else
                {
                    Log.i("[USBDemo]", "申请权限");
                    // requestPermissions以标准对话框形式请求权限。123是识别码（任意设置的整型），用来识别权限。应用无法配置或更改此对话框。
                    //当应用请求权限时，系统将向用户显示一个对话框。当用户响应时，系统将调用应用的 onRequestPermissionsResult() 方法，向其传递用户响应。您的应用必须替换该方法，以了解是否已获得相应权限。回调会将您传递的相同请求代码传递给 requestPermissions()。
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }
            }
            Log.i("[USBDemo]", "已获得读写权限");
        } else {
            Log.i("[USBDemo]", "无需动态申请");
        }
    }
    public void CheckPermission(String sPermission)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            // 判断是否有这个权限，是返回PackageManager.PERMISSION_GRANTED，否则是PERMISSION_DENIED
            // 这里我们要给应用授权所以是!= PackageManager.PERMISSION_GRANTED
            if (ContextCompat.checkSelfPermission(this, sPermission) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("[USBDemo]", "未获得权限");
                // 如果应用之前请求过此权限但用户拒绝了请求,且没有选择"不再提醒"选项 (后显示对话框解释为啥要这个权限)，此方法将返回 true。
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, sPermission))
                {
                    Log.i("[USBDemo]", "用户永久拒绝权限申请");
                }
                else
                {
                    Log.i("[USBDemo]", "申请权限");
                    // requestPermissions以标准对话框形式请求权限。123是识别码（任意设置的整型），用来识别权限。应用无法配置或更改此对话框。
                    //当应用请求权限时，系统将向用户显示一个对话框。当用户响应时，系统将调用应用的 onRequestPermissionsResult() 方法，向其传递用户响应。您的应用必须替换该方法，以了解是否已获得相应权限。回调会将您传递的相同请求代码传递给 requestPermissions()。
                    ActivityCompat.requestPermissions(this, new String[]{sPermission}, 100);
                }
            }
            Log.i("[USBDemo]", "已获得权限");
        } else {
            Log.i("[USBDemo]", "无需动态申请");
        }
    }

    //网络权限动态申请
    public void CheckNetworkPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            // 判断是否有这个权限，是返回PackageManager.PERMISSION_GRANTED，否则是PERMISSION_DENIED
            // 这里我们要给应用授权所以是!= PackageManager.PERMISSION_GRANTED
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("[USBDemo]", "未获得网络权限");
                // 如果应用之前请求过此权限但用户拒绝了请求,且没有选择"不再提醒"选项 (后显示对话框解释为啥要这个权限)，此方法将返回 true。
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.INTERNET))
                {
                    Log.i("[USBDemo]", "用户永久拒绝权限申请");
                }
                else
                {
                    Log.i("[USBDemo]", "申请权限");
                    // requestPermissions以标准对话框形式请求权限。123是识别码（任意设置的整型），用来识别权限。应用无法配置或更改此对话框。
                    //当应用请求权限时，系统将向用户显示一个对话框。当用户响应时，系统将调用应用的 onRequestPermissionsResult() 方法，向其传递用户响应。您的应用必须替换该方法，以了解是否已获得相应权限。回调会将您传递的相同请求代码传递给 requestPermissions()。
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET}, 100);
                }
            }
            Log.i("[USBDemo]", "已获得网络权限");
        } else {
            Log.i("[USBDemo]", "无需动态申请");
        }
    }

}