package com.example.temprature;

public class Constants {
    public static final int PREVIEW_DUAL_SCALE = 2;

    public static final int PREVIEW_WIDTH = 192;
    public static final int PREVIEW_HEIGHT = 256;
    public static final int PREVIEW_HEIGHT_SDK = 520;


    //==========================================================
    public static final int F2_MODULE_FRAMERATE_25 = 25;
    public static final int F2_MODULE_FRAMERATE_50 = 50;

    //码流
    public static final int F2_MODULE_VIDEO_CODING_TYPE_8 = 8;
    public static final int F2_MODULE_VIDEO_CODING_TYPE_11 = 11;

    //码流8
    public static final int F2_MODULE_VIDEO_CODING_TYPE_8_SIZE = 201248;

    //截断的码流8
    public static final int F2_MODULE_VIDEO_CODING_TYPE_8_SIZE_CUT = 102944;

    //码流11
    public static final int F2_MODULE_VIDEO_CODING_TYPE_11_SIZE = 206392;

    //截断的码流11
    public static final int F2_MODULE_VIDEO_CODING_TYPE_11_SIZE_CUT = 105016;

    public static int f2ModuleFrameRate = F2_MODULE_FRAMERATE_50;
    public static int f2ModuleVideoCodingType = F2_MODULE_VIDEO_CODING_TYPE_8;
    public static int f2ModuleVideoCodingTypeSize = F2_MODULE_VIDEO_CODING_TYPE_8_SIZE_CUT;

    //是否是F2新固件
    public static boolean f2ModuleIsNewFirmware = false;
}
