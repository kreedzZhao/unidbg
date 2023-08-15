package com.xiaochuankeji.tieba;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Sign extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;

    private final DalvikModule dm;
    private final DvmClass NetCrypto;

    public Sign(){
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("cn.xiaochuankeji.tieba")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/right573.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        dm = vm.loadLibrary("net_crypto", true);
        // 使用 libandroid 模块
        new AndroidModule(emulator, vm).register(memory);

        NetCrypto = vm.resolveClass("com.izuiyou.network.NetCrypto");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "cn/xiaochuankeji/tieba/AppController->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
            case "cn/xiaochuankeji/tieba/AppController->getPackageName()Ljava/lang/String;":{
                String packageName = vm.getPackageName();
                if (packageName != null) {
                    return new StringObject(vm, packageName);
                }
                break;
            }
            case "cn/xiaochuankeji/tieba/AppController->getClass()Ljava/lang/Class;":{
                return dvmObject.getObjectType();
            }
            case "java/lang/Class->getSimpleName()Ljava/lang/String;":{
                String className = ((DvmClass) dvmObject).getClassName();
                String[] name = className.split("/");
                return new StringObject(vm, name[name.length - 1]);
            }
            case "cn/xiaochuankeji/tieba/AppController->getFilesDir()Ljava/io/File;":{
                return vm.resolveClass("java/io/File").newObject("/data/data/cn.xiaochuankeji.tieba/files");
            }
            case "java/io/File->getAbsolutePath()Ljava/lang/String;":{
                return new StringObject(vm, dvmObject.getValue().toString());
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Debug->isDebuggerConnected()Z":{
                return false;
            }
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Process->myPid()I":{
                return emulator.getPid();
            }
        }
        return super.callStaticIntMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;": {
                DvmObject<?> context = vm.resolveClass("cn/xiaochuankeji/tieba/AppController").newObject(null);
                return context;
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public String getProtocolKey(){
        String res = NetCrypto.callStaticJniMethodObject(emulator, "getProtocolKey()Ljava/lang/String;").getValue().toString();
        return res;
    }

    public String sign(){
        String res = NetCrypto.callStaticJniMethodObject(emulator, "sign(Ljava/lang/String;[B)Ljava/lang/String;",
                "Hello World", "V ME 50".getBytes(StandardCharsets.UTF_8)).getValue().toString();
        return res;
    }

    public static void main(String[] args) {
        Sign sign = new Sign();
        String s = sign.sign();
        System.out.println("res: "+ s);
    }
}
