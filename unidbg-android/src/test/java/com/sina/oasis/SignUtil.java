package com.sina.oasis;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class SignUtil extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NativeApi;
    private final VM vm;

    public SignUtil() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.sina.oasis")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/lvzhou.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("oasiscore", true);
        NativeApi = vm.resolveClass("com.weibo.xvideo.NativeApi");
        dm.callJNI_OnLoad(emulator);
    }

    public String calls() {
        String arg1 = "aid=01A-khBWIm48A079Pz_DMW6PyZR8uyTumcCNm4e8awxyC2ANU.&cfrom=28B5295010&cuid=5999578300&noncestr=46274W9279Hr1X49A5X058z7ZVz024&platform=ANDROID&timestamp=1621437643609&ua=Xiaomi-MIX2S__oasis__3.5.8__Android__Android10&version=3.5.8&vid=1019013594003&wm=20004_90024";
        Boolean arg2 = false;
        String s = NativeApi.newObject(null).callJniMethodObject(emulator,
                "s([BZ)Ljava/lang/String;",
                arg1.getBytes(StandardCharsets.UTF_8),
                arg2
        ).getValue().toString();
        return s;
    }

    public static void main(String[] args) {
        SignUtil signUtil = new SignUtil();
        String res = signUtil.calls();
        System.out.println(res);
    }
}
