package com.example.temprature;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.hcusbsdk.Interface.FStreamCallBack;
import com.hcusbsdk.Interface.USB_FRAME_INFO;
import com.hik.f2module.IFR_INFO;
import com.louisgeek.jstructlib.JavaStruct;
import com.louisgeek.jstructlib.StructException;

import java.nio.ByteOrder;
import java.util.Arrays;

public class TempStreamCallBack implements FStreamCallBack {
    private final Context context;

    public TempStreamCallBack(Context context) {
        this.context = context;
    }

    @Override
    public void invoke(int i, USB_FRAME_INFO usbFrameInfo) {
        if (usbFrameInfo == null) {
            return;
        }
        //数据拷贝
        byte[] byteArray = Arrays.copyOf(usbFrameInfo.pBuf, usbFrameInfo.dwBufSize);
        if (byteArray.length != Constants.f2ModuleVideoCodingTypeSize) {
            //直接返回，错误的内容可能导致触发自动校准
            return;
        }
        //写数据
        IFR_INFO.USB_THERMAL_STREAM_TEMP_YUV streamTempYuv = new IFR_INFO.USB_THERMAL_STREAM_TEMP_YUV();
        bufferToBean(streamTempYuv, byteArray);
        //读取温度信息并处理
        float min = streamTempYuv.ifrRealtimeTmOutcomeUploadInfo.fMinTmp;
        float max = streamTempYuv.ifrRealtimeTmOutcomeUploadInfo.fMaxTmp;
        //中心温临时存在 fAvrTmp 字段里
        float cen = streamTempYuv.ifrRealtimeTmOutcomeUploadInfo.fAvrTmp;
//        onTempCallback.invoke(min, cen, max)
        String msg = "min:" + min + " cen:" + cen + " max:" + max;
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        Log.d("TempStreamCallBack", msg);
    }

    private void bufferToBean(IFR_INFO.USB_THERMAL_STREAM_TEMP_YUV streamTempYuv, byte[] buffer) {
        try {
            JavaStruct.unpack(streamTempYuv, buffer, ByteOrder.LITTLE_ENDIAN);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }
}
