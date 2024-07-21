package com.com.xunmeng.pinduoduo.secure;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public class DeviceNative extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private final DvmClass DeviceNative;

    private final boolean logging;

    DeviceNative(boolean logging) {
        this.logging = logging;

        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.xunmeng.pinduoduo")
//                .addBackendFactory(new Unicorn2Factory(true))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/pdd_57200.apk")); // 创建Android虚拟机
        vm.setVerbose(logging); // 设置是否打印Jni调用细节
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary("pdd_secure", true); // 加载libttEncrypt.so到unicorn虚拟内存，加载成功以后会默认调用init_array等函数
        dm.callJNI_OnLoad(emulator); // 手动执行JNI_OnLoad函数
        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块

        DeviceNative = vm.resolveClass("com.com.xunmeng.pinduoduo.secure.DeviceNative");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V": {
                return;
            }
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    public String callInfo2() {
        DvmObject<?> dvmObject = vm.resolveClass("android.content.Context").newObject(null);

        ArrayList<Object> objects = new ArrayList<>(10);
        objects.add(vm.getJNIEnv());
        objects.add(0);
        objects.add(vm.addLocalObject(dvmObject));
        objects.add(0x17AD420321AL);
        Number number = module.callFunction(emulator, 0xe3d5,
                objects.toArray());
        String result = vm.getObject(number.intValue()).getValue().toString();
//        String result = this.DeviceNative.callStaticJniMethodObject(
//                emulator,
//                "info2(Landroid/content/Context;J)Ljava/lang/String;",
//                dvmObject,
//                720447129257L
//        ).getValue().toString();
        return result;
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/content/Context->checkSelfPermission(Ljava/lang/String;)I": {
//                System.out.println("checkSelfPermission --->>> "+varArg.getObjectArg(0).toString());
                return -1;
            }
            case "android/telephony/TelephonyManager->getSimState()I": {
                return 1;
            }
            case "android/telephony/TelephonyManager->getNetworkType()I": {
                return 13;
            }
            case "android/telephony/TelephonyManager->getDataState()I": {
                return 0;
            }
            case "android/telephony/TelephonyManager->getDataActivity()I": {
                return 4;
            }
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case ("android/provider/Settings$Secure->ANDROID_ID:Ljava/lang/String;"): {
//                String arg0 = varArg.getObjectArg(0).getValue().toString();
//                if(arg0.equals("ANDROID_ID"))
                return new StringObject(vm, "android_id");
            }
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case ("android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;"): {
                return new StringObject(vm, "");
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case ("android/os/Debug->isDebuggerConnected()Z"): {
                return false;
            }
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case ("java/lang/Throwable-><init>()V"): {
                return vm.resolveClass("java.lang.Throwable").newObject(new Throwable());
            }
            case ("java/io/ByteArrayOutputStream-><init>()V"): {
                return vm.resolveClass("java/io/ByteArrayOutputStream").newObject(new ByteArrayOutputStream());
            }
            case ("java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V"): {
                ByteArrayOutputStream baos = (ByteArrayOutputStream)varArg.getObjectArg(0).getValue();
                try {
                    return vm.resolveClass("java/util/zip/GZIPOutputStream").newObject(new GZIPOutputStream(baos));
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
                byte[] gos = (byte[]) varArg.getObjectArg(0).getValue();
                try {
                    ((GZIPOutputStream)dvmObject.getValue()).write(gos);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            case "java/util/zip/GZIPOutputStream->finish()V":{
                try {
                    ((GZIPOutputStream)dvmObject.getValue()).finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            case "java/util/zip/GZIPOutputStream->close()V":{
                try {
                    ((GZIPOutputStream)dvmObject.getValue()).close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case ("android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;"): {
                return vm.resolveClass("android/telephony/TelephonyManager").newObject(signature);
            }
            case ("android/telephony/TelephonyManager->getSimOperatorName()Ljava/lang/String;"): {
                return new StringObject(vm, "中国联通");
            }
            case ("android/telephony/TelephonyManager->getSimCountryIso()Ljava/lang/String;"): {
                return new StringObject(vm, "cn");
            }
            case ("android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;"): {
                return new StringObject(vm, "46001");
            }
            case ("android/telephony/TelephonyManager->getNetworkOperatorName()Ljava/lang/String;"): {
                return new StringObject(vm, "中国联通");
            }
            case ("android/telephony/TelephonyManager->getNetworkCountryIso()Ljava/lang/String;"): {
                return new StringObject(vm, "cn");
            }
            case ("android/content/Context->getContentResolver()Landroid/content/ContentResolver;"): {
                return vm.resolveClass("android.content.ContentResolver").newObject(signature);
            }
            case ("java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;"): {
                StackTraceElement[] elements = {
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.secure.DeviceNative", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.secure.SecureNative", "", "", 0),
                        new StackTraceElement("com.xunmeng.pinduoduo.secure.s", "", "", 0),
                        new StackTraceElement("com.aimi.android.common.http.a", "", "", 0),
                        new StackTraceElement("com.aimi.android.common.http.j", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.k", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.PQuic.Interceptor", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.g", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.config.i$c", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),

                        new
                                StackTraceElement("com.xunmeng.pinduoduo.basekit.http.manager.b$4", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.o", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.e", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.b", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.a", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.m", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.c", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.internal.interceptor.j", ""
                                , "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.internal.b.g", "", "", 0),
                        new StackTraceElement("okhttp3.RealCall", "", "", 0),
                        new
                                StackTraceElement("com.aimi.android.common.http.unity.UnityCallFactory$a", "", "",
                                0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b.a", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.a.a", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b.b", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.a.a", "", "", 0),
                        new StackTraceElement("1", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.g", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.g$a", "", "", 0),
                        new
                                StackTraceElement("com.xunmeng.pinduoduo.arch.quickcall.a.b", "", "", 0),
                        new
                                StackTraceElement("java.util.concurrent.ThreadPoolExecutor", "", "", 0),
                        new
                                StackTraceElement("java.util.concurrent.ThreadPoolExecutor$Worker", "", "", 0),
                        new StackTraceElement("java.lang.Thread", "", "", 0),
                };
                DvmObject[] objs = new DvmObject[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    objs[i] =
                            vm.resolveClass("java/lang/StackTraceElement").newObject(elements[i]);
                }
                return new ArrayObject(objs);
            }
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;": {
                StackTraceElement element = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, element.getClassName());
            }
            case "java/io/ByteArrayOutputStream->toByteArray()[B": {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) dvmObject.getValue();
                byte[] byteArray = baos.toByteArray();
                return new ByteArray(vm, byteArray);
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":{
                // TODO: replace handle
                StringObject str = (StringObject) dvmObject;
                StringObject s1 = vaList.getObjectArg(0);
                StringObject s2 = vaList.getObjectArg(1);
                assert s1 != null;
                assert s2 != null;
                return new StringObject(vm, str.getValue().replaceAll(s1.getValue(),
                        s2.getValue()));
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    public static void main(String[] args) {
        DeviceNative deviceNative = new DeviceNative(true);
        String result = deviceNative.callInfo2();
        Inspector.inspect(result.getBytes(StandardCharsets.UTF_8), "result -> ");
        System.out.println(result);
        // 91346b37e02e9d44d1a44f74ab601d88
        // 32cf925ec31b42d563be6170cef8be02
    }
}
