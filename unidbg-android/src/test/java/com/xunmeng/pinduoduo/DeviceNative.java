package com.xunmeng.pinduoduo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.SystemService;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class DeviceNative extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final DvmClass deviceNative;
    private GZIPOutputStream gzipOutputStream;
    private ByteArrayOutputStream byteArrayOutputStream;

    public DeviceNative() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.xunmeng.pinduoduo")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/pdd.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
        new JniGraphics(emulator, vm).register(memory);

        dm = vm.loadLibrary("pdd_secure", true);

        deviceNative = vm.resolveClass("com.xunmeng.pinduoduo.secure.DeviceNative");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V":{
                return;
            }
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callStaticIntMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "android/content/Context->checkSelfPermission(Ljava/lang/String;)I":{
                return -1;
            }
        }
        return super.callStaticIntMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        // TODO @kreedz 网络相关的检测
        switch (signature){
            case "android/content/Context->checkSelfPermission(Ljava/lang/String;)I":{
                return -1;
            }
            case "android/telephony/TelephonyManager->getSimState()I":{
                return 1;
            }
            case "android/telephony/TelephonyManager->getNetworkType()I":{
                return 13; // 4G
            }
            case "android/telephony/TelephonyManager->getDataState()I":{
                return 2;
            }
            case "android/telephony/TelephonyManager->getDataActivity()I":{
                return 3; // DATA_ACTIVITY_INOUT
            }
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/xunmeng/pinduoduo/secure/EU->gad()Ljava/lang/String;":{
                // com.xunmeng.pinduoduo.secure.EU.gad(Native Method)
                //	com.xunmeng.pinduoduo.secure.DeviceNative.info3(Native Method)
                //	com.xunmeng.pinduoduo.secure.SecureNative.deviceInfo3(Pdd:22)
                return new StringObject(vm, "8d17a4ac1ab6f30a");
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "android/os/Debug->isDebuggerConnected()Z":{
                return false;
            }
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;":{
                StringObject serviceName = varArg.getObjectArg(0);
                System.out.println("serviceName: " + serviceName.getValue().toString());
                assert serviceName != null;
                return new SystemService(vm, serviceName.getValue());
            }
            // 运营商品牌
            case "android/telephony/TelephonyManager->getSimOperatorName()Ljava/lang/String;":{
                return new StringObject(vm, "中国移动");
            }
            case "android/telephony/TelephonyManager->getSimCountryIso()Ljava/lang/String;":{
                return new StringObject(vm, "cn");
            }
            case "android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;":{
                return new StringObject(vm, "46002");
            }
            case "android/telephony/TelephonyManager->getNetworkOperatorName()Ljava/lang/String;":{
                return new StringObject(vm, "中国移动");
            }
            case "android/telephony/TelephonyManager->getNetworkCountryIso()Ljava/lang/String;":{
                return new StringObject(vm, "cn");
            }
            case "java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;":{
                StackTraceElement[] elements = { new StackTraceElement("com.xunmeng.pinduoduo.secure.DeviceNative","","",0), new StackTraceElement("com.xunmeng.pinduoduo.secure.SecureNative","","",0), new StackTraceElement("com.xunmeng.pinduoduo.secure.s","","",0), new StackTraceElement("com.aimi.android.common.http.a","","",0), new StackTraceElement("com.aimi.android.common.http.j","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.k","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.PQuic Interceptor","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.g","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.config.i$c","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.xunmeng.pinduoduo.basekit.http.manager.b$4","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.o","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.e","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.b","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.a","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.m","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.c","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.j","" ,"",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.internal.b.g","","",0), new StackTraceElement("okhttp3.RealCall","","",0), new StackTraceElement("com.aimi.android.common.http.unity.UnityCallFactory$a","","", 0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b.a","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.a.a","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b.b","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.a.a","","",0), new StackTraceElement("1","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.g","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.g$a","","",0), new StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b","","",0), new StackTraceElement("java.util.concurrent.ThreadPoolExecutor","","",0), new StackTraceElement("java.util.concurrent.ThreadPoolExecutor$Worker","","",0), new StackTraceElement("java.lang.Thread","","",0), };
                DvmObject[] objs = new DvmObject[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    objs[i] =
                            vm.resolveClass("java/lang/StackTraceElement").newObject(elements[i]);
                }
                return new ArrayObject(objs);
            }
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;":{
                StackTraceElement element = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, element.getClassName());
            }
            case "java/io/ByteArrayOutputStream->toByteArray()[B":{
                byteArrayOutputStream = (ByteArrayOutputStream)
                        dvmObject.getValue();
                byte[] result = byteArrayOutputStream.toByteArray();
                return new ByteArray(vm, result);
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":{
                String arg1 = vaList.getObjectArg(0).getValue().toString();
                String arg2 = vaList.getObjectArg(1).getValue().toString();
                String str = dvmObject.getValue().toString();
                return new StringObject(vm, str.replaceAll(arg1, arg2));
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "java/lang/Throwable-><init>()V":{
                return vm.resolveClass("java/lang/Throwable").newObject(new Throwable());
            }
            case "java/io/ByteArrayOutputStream-><init>()V":{
                return vm.resolveClass("java/io/ByteArrayOutputStream").newObject(new ByteArrayOutputStream());
            }
            case "java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V":{
                try {
                    return vm.resolveClass("java/util/zip/GZIPOutputStream").newObject(new GZIPOutputStream((ByteArrayOutputStream) varArg.getObjectArg(0).getValue()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/zip/GZIPOutputStream->write([B)V":{
                gzipOutputStream = (GZIPOutputStream)
                        dvmObject.getValue();
                byte[] input = (byte[]) varArg.getObjectArg(0).getValue();
                try {
                    gzipOutputStream.write(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            case "java/util/zip/GZIPOutputStream->finish()V":{
                try {
                    gzipOutputStream.finish();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            case "java/util/zip/GZIPOutputStream->close()V":{
                try {
                    gzipOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }

    public String getInfo2() {
        DvmObject<?> context =
                vm.resolveClass("android/content/Context").newObject(null);// context
        String res = deviceNative.callStaticJniMethodObject(
                emulator,
                "info2(Landroid/content/Context;J)Ljava/lang/String;",
                context,
                1692354667887L
        ).getValue().toString();
        return res;
    }

    public String getInfo3() {
        DvmObject<?> context =
                vm.resolveClass("android/content/Context").newObject(null);// context
        String res = deviceNative.callStaticJniMethodObject(
                emulator,
                "info3(Landroid/content/Context;JLjava/lang/String;)Ljava/lang/String;",
                context,
                1692454421013L,
                "8hfeBBSw"
        ).getValue().toString();
        return res;
    }


    public static void main(String[] args) {
        DeviceNative deviceNative = new DeviceNative();
        String info2 = deviceNative.getInfo2();
        System.out.println("info2: " + info2);
        String info3 = deviceNative.getInfo3();
        System.out.println("info3: " + info3);
    }
}
