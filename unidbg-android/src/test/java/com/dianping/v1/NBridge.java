package com.dianping.v1;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.SystemService;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;

import java.io.*;

public class NBridge extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final DvmObject SIUACollector;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;

    public NBridge()  {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.dianping.v1")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/dazhongdianping.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
        new JniGraphics(emulator, vm).register(memory);

        dm = vm.loadLibrary("mtguard", true);

        SIUACollector = vm.resolveClass("com/meituan/android/common/mtguard/NBridge$SIUACollector").newObject(null);
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "java/lang/StringBuilder->allocObject":{
                return ProxyDvmObject.createObject(vm, new StringBuilder());
            }
            case "java/io/BufferedReader->allocObject":{
                // BufferedReader 没有无参构造函数 等同于 vm.resolveClass("java/io/BufferedReader")
                return dvmClass.newObject(null);
            }
            case "java/io/InputStreamReader->allocObject":{
                return dvmClass.newObject(null);
            }
            case "java/io/FileInputStream->allocObject":{
                return dvmClass.newObject(null);
            }
        }
        return super.allocObject(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/StringBuilder-><init>()V":{
                return;
            }
            case "java/io/FileInputStream-><init>(Ljava/lang/String;)V":{
                String pathName = vaList.getObjectArg(0).getValue().toString();
                if (pathName.equals("/proc/cpuinfo")){
                    pathName = "unidbg-android/src/test/resources/apk/cpuinfo";
                }
                try {
                    fileInputStream = new FileInputStream(pathName);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            case "java/io/InputStreamReader-><init>(Ljava/io/InputStream;)V":{
                inputStreamReader = new InputStreamReader(fileInputStream);
                return;
            }
            case "java/io/BufferedReader-><init>(Ljava/io/Reader;)V":{
                bufferedReader = new BufferedReader(inputStreamReader);
                return;
            }
            case "java/io/BufferedReader->close()V":{
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return ;
            }
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getEnvironmentInfo()Ljava/lang/String;":{
                // 不能嵌套输入
//            return ProxyDvmObject.createObject(vm, SIUACollector.callJniMethodObject(
//                    emulator,
//                    "getEnvironmentInfoExtra()Ljava/lang/String;"
//            ).getValue().toString());
                return new StringObject(vm, "0|0|0|-|0|");
            }
            case "java/lang/StringBuilder->append(Ljava/lang/String;)Ljava/lang/StringBuilder;":{
                String arg1 = vaList.getObjectArg(0).getValue().toString();
                return ProxyDvmObject.createObject(vm, ((StringBuilder)(dvmObject.getValue())).append(arg1));
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isVPN()Ljava/lang/String;":{
                return new StringObject(vm, "0");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->brightness(Landroid/content/Context;)Ljava/lang/String;":{
                return new StringObject(vm, "0.8");
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->systemVolume(Landroid/content/Context;)Ljava/lang/String;":{
                return new StringObject(vm, "0");
            }
            case "java/lang/StringBuilder->toString()Ljava/lang/String;":{
                return new StringObject(vm, ((StringBuilder)dvmObject.getValue()).toString());
            }
//            case "android/app/Application->getSystemService(Ljava/lang/String;)Ljava/lang/Object;": {
//                StringObject serviceName = vaList.getObjectArg(0);
//                assert serviceName != null;
//                return new SystemService(vm, serviceName.getValue());
//            }
            case "android/content/Context->getApplicationContext()Landroid/content/Context;":{
                DvmClass context = vm.resolveClass("android.content.Context");
                DvmClass application =
                        vm.resolveClass("android/app/Application",context);
                DvmClass content = vm.resolveClass("android/content/Content", application);
                return content.newObject(signature);
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getCpuInfoType()Ljava/lang/String;":{
                return new StringObject(vm, "arm");
            }
            case "java/io/BufferedReader->readLine()Ljava/lang/String;":{
                try{
                    String line = bufferedReader.readLine();
                    if (line != null){
                        return new StringObject(vm, line);
                    }else{
                        return null;
                    }
                }catch (Exception e){

                }
            }
            case "java/lang/String->substring(I)Ljava/lang/String;":{
                return new StringObject(vm, dvmObject.getValue().toString().substring(vaList.getIntArg(0)));
            }
            case "java/lang/StringBuilder->append(I)Ljava/lang/StringBuilder;":{
                return ProxyDvmObject.createObject(vm, ((StringBuilder)dvmObject.getValue()).append(vaList.getIntArg(0)));
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isAccessibilityEnable()Z":{
                return false;
            }
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/String->valueOf(I)Ljava/lang/String;":{
                return new StringObject(vm, String.valueOf(vaList.getIntArg(0)));
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->uiAutomatorClickCount()I":{
                return 0;
            }
            case "java/lang/String->compareToIgnoreCase(Ljava/lang/String;)I":{
                String str = vaList.getObjectArg(0).getValue().toString();
                return dvmObject.getValue().toString().compareToIgnoreCase(str);
            }
            case "java/lang/String->lastIndexOf(I)I":{
                String str = dvmObject.getValue().toString();
                return str.lastIndexOf(vaList.getIntArg(0));
            }
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->mContext:Landroid/content/Context;":{
                return vm.resolveClass("android/content/Content").newObject(null);
            }
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    // getEnvironmentInfo
    public String getEnvironmentInfo(){
        String info = SIUACollector.callJniMethodObject(emulator, "getEnvironmentInfo()Ljava/lang/String;").getValue().toString();
        return info;
    }

    // getEnvironmentInfoExtra
    public String getEnvironmentInfoExtra(){
        return SIUACollector.callJniMethodObject(
                emulator,
                "getEnvironmentInfoExtra()Ljava/lang/String;"
        ).getValue().toString();
    }

    // getExternalEquipmentInfo
    public String getExternalEquipmentInfo(){
        return SIUACollector.callJniMethodObject(
                emulator,
                "getExternalEquipmentInfo()Ljava/lang/String;"
        ).getValue().toString();
    }

    // getHWEquipmentInfo
    public String getHWEquipmentInfo(){
        return SIUACollector.callJniMethodObject(
                emulator,
                "getHWProperty()Ljava/lang/String;"
        ).getValue().toString();
    }
    // getHWProperty
    public String getHWProperty(){
        return SIUACollector.callJniMethodObject(
                emulator,
                "getHWEquipmentInfo()Ljava/lang/String;"
        ).getValue().toString();
    }
    // getHWStatus
    public String getHWStatus(){
        return SIUACollector.callJniMethodObject(
                emulator,
                "getHWStatus()Ljava/lang/String;"
        ).getValue().toString();
    }

    public static void main(String[] args) {
        NBridge nBridge = new NBridge();
        System.out.println("getEnvironmentInfo: "+ nBridge.getEnvironmentInfo());
        System.out.println("getEnvironmentInfoExtra: "+ nBridge.getEnvironmentInfoExtra());
        System.out.println("getExternalEquipmentInfo: "+ nBridge.getExternalEquipmentInfo());
//        System.out.println("getHWEquipmentInfo: "+ nBridge.getHWEquipmentInfo());
//        System.out.println("getHWProperty: "+ nBridge.getHWProperty());
//        System.out.println("getHWStatus: "+ nBridge.getHWStatus());
    }

}
