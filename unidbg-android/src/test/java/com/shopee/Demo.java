package com.shopee;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.ApplicationInfo;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.utils.TraceFunction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

public class Demo extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final Module module;

    public Demo() {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.shopee.ph")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/com.shopee.ph.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().addIOResolver(this);
        dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apks/libshpssdk.so"), true);

        module = dm.getModule();
        dm.callJNI_OnLoad(emulator);
    }

    public void selfDebug(long offset) {
        AtomicInteger num = new AtomicInteger();
        emulator.attach().addBreakPoint(dm.getModule().base + offset, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
            num.addAndGet(1);
            System.out.println("num: " + num);

            if (offset == 0x4044C){
                RegisterContext context = emulator.getContext();
                UnidbgPointer checkStr = context.getPointerArg(1);
                System.out.println("call "+Long.toHexString(offset));
                if (checkStr.getString(0).equals("x-sap-ri")){
                    return false;
                }
                Inspector.inspect(checkStr.getByteArray(0,32),"checkStr "+Long.toHexString(checkStr.peer));
            }

            return false;
        });
    }

    public void hook() {
        Debugger attach = emulator.attach();
        attach.addBreakPoint(module.findSymbolByName("memcpy").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext context = emulator.getContext();
                int len = context.getIntArg(2);
//                UnidbgPointer pointer1 = context.getPointerArg(0);
//                UnidbgPointer pointer2 = context.getPointerArg(1);
//                Inspector.inspect(pointer2.getByteArray(0,len),"src "+Long.toHexString(pointer1.peer)+" memcpy "+Long.toHexString(pointer2.peer));

                UnidbgPointer dest = context.getPointerArg(0);
                UnidbgPointer src = context.getPointerArg(1);
                int size = context.getIntArg(2);
                System.out.println("PC: "+context.getPCPointer()+" LR: "+context.getLRPointer()+" dest: "+dest+" src: "+src+" size: "+size);
//                Inspector.inspect(src.getByteArray(0, 0x30), "memcpy input");
                Inspector.inspect(src.getByteArray(0,size),"src "+Long.toHexString(src.peer)+" memcpy "+Long.toHexString(dest.peer));
                return true;
            }
        });
    }

    public void patch() {
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x9A208);
        byte[] bytes = {(byte) 0x1f, (byte) 0x20, (byte) 0x03, (byte) 0xD5}; // 1F 20 03 D5 异常位置
//        byte[] bytes = {(byte) 0xC0, (byte) 0x03, (byte) 0x5F, (byte) 0xD6}; // C0035FD6 异常位置
        pointer.write(bytes);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("resolve pathname=" + pathname + ", oflags=0x" + Integer.toHexString(oflags));
//        if (pathname.equals("/proc/self/cmdline")){
//            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
//        }
        return null;
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "android/app/ActivityThread->getApplication()Landroid/app/Application;":
                return vm.resolveClass(
                        "android/app/Application",
                        vm.resolveClass("android/content/ContextWrapper",
                                vm.resolveClass("android/content/Context")
                        )
                ).newObject(signature);
            case "android/content/Context->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;":
                String pref = vaList.getObjectArg(0).getValue().toString();
                System.out.println("[debugger] getSharedPreferences arg0: "+pref);
                return vm.resolveClass("android/content/SharedPreferences").newObject(pref);
            case "android/content/SharedPreferences->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":
                String sPref = dvmObject.getValue().toString();
                if (sPref.equals("SPHelper_sp_main")){
                    String key = vaList.getObjectArg(0).getValue().toString();
                    switch (key) {
                        case "E1YASQpPEEUQWR1CCUwVVVVw":
                            return new StringObject(vm, "f0VMRgEAAAAIAAAAAGKYAQAAAAACAAAAJAAAADdhZmJiMzU1KmJjYTQqM2EyYio+NzE3KjUwZDY2M2Y2PzQxNQMAAAAIAAAAAAAEFAAAAAA=");
                    }
                }
            case "android/app/Application->getPackageName()Ljava/lang/String;":
                return new StringObject(vm, "com.shopee.ph");
            case "android/content/pm/ApplicationInfo->loadLabel(Landroid/content/pm/PackageManager;)Ljava/lang/CharSequence;":
                // Learning
                CharSequence b = "Shopee";
                return ProxyDvmObject.createObject(vm, b);
            case "android/content/pm/PackageManager->getInstallerPackageName(Ljava/lang/String;)Ljava/lang/String;":
                return new StringObject(vm, "com.shopee.ph");
            case "android/content/SharedPreferences->edit()Landroid/content/SharedPreferences$Editor;":
                return vm.resolveClass("android/content/SharedPreferences$Editor").newObject(signature);
            case "android/content/SharedPreferences$Editor->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;":
                String arg0 = vaList.getObjectArg(0).getValue().toString();
                String arg1 = vaList.getObjectArg(1).getValue().toString();
                System.out.println("[debugger] getSharedPreferences putString: ("+arg0+", "+arg1+")");
                return dvmObject;
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public long getLongField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "android/content/pm/PackageInfo->firstInstallTime:J":
                return 1732349660249L;
            case "android/content/pm/PackageInfo->lastUpdateTime:J":{
                return 1732349661249L;
            }
        }
        return super.getLongField(vm, dvmObject, signature);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "android/content/pm/PackageInfo->applicationInfo:Landroid/content/pm/ApplicationInfo;":
                return new ApplicationInfo(vm);
            case "android/content/pm/ApplicationInfo->sourceDir:Ljava/lang/String;":
                return new StringObject(vm, "/data/app/com.shopee.ph-6AYcVt33bWXGT_JAdpxbeQ==/");
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "android/content/pm/ApplicationInfo->flags:I":
                // 检查app是否开启调试
                return 1048576;
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "android/content/SharedPreferences$Editor->commit()Z":
                return true;
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/shopee/shpssdk/wvvvuwwu->vuwuuuvv(ILjava/lang/Object;)Ljava/lang/Object;":
                int intArg = vaList.getIntArg(0);
                System.out.println("[debugger] vuwuuuvv: intArg=" + intArg);
                switch (intArg) {
                    case 74:
                        // 参与计算了 只影响最长的
                        return new StringObject(vm, "oTSflMz92sjaiteHWrTbCA==|ZcaOB3RDBDZ45yuQO417seuqaFkyOokc0bEPSbb4EEOEWgsDQ8mELI4+L7dozfSMKhz8XKeONFtbuVR8YZ6h+cQJr6Sy6A7R9n7jAkbl|hApTLtwZfU6Gp8wb|08|1");
                }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public void saveTrace(){

//        TraceFunction traceFunction = new TraceFunction(emulator, module, "unidbg-android/src/test/java/com/shopee/func.txt");
//        traceFunction.trace_function();

        String traceFile = "unidbg-android/src/test/java/com/shopee/CrackMeTracetraceCode.txt";
        PrintStream traceStream = null;
        try {
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        emulator.traceCode(module.base,module.base+module.size).setRedirect(traceStream);
    }

    public void callSign() {
        // 0x995dc
        DvmClass RequestCryptUtils = vm.resolveClass("com/shopee/shpssdk/wvvvuwwu");

        byte[] bytes = "https://mall.shopee.ph/api/v4/pages/bottom_tab_bar".getBytes();
        ByteArray arr1 = new ByteArray(vm, bytes);

        byte[] bytes2 = "{\"img_size\":\"3.0x\",\"latitude\":\"\",\"location\":\"[]\",\"longitude\":\"\",\"new_arrival_reddot_last_dismissed_ts\":0,\"feed_reddots\":[{\"timestamp\":0,\"noti_code\":28}],\"client_feature_meta\":{\"is_live_and_video_merged_tab_supported\":false},\"video_reddot_last_dismissed_ts\":0,\"view_count\":[{\"count\":1,\"source\":0,\"tab_name\":\"Live\"}]}".getBytes();
        ByteArray arr2 = new ByteArray(vm, bytes2);

        // params1 0x126DB260
        myTraceWrite(0x126ee800+0x300, 1);
//        saveTrace();
//        selfDebug(0x4044C);
//        selfDebug(0x083e44);
        patch();
//        hook();
        StringObject result = RequestCryptUtils.callStaticJniMethodObject(emulator, "vuwuuwvw([B[B)Ljava/lang/String;", arr1, arr2);
//        StringObject result = RequestCryptUtils.callStaticJniMethodObject(emulator, "vuwuuwvw([B[B)Ljava/lang/String;", arr1,null);
        System.out.println(result.toString());
        // {"1a9ec9b9": "zlug+XF2Nwvjy/venQJClQYEmb4=",
        // "4b3e0f91": "08WkwBHxzv4eq3CAsG4hwUOfsu4=",
        // "9bbcf962": "J+AkNdUssUTsskOpXQ1t/PFn4eruB/oNMiDsjfrMquzEmNel4+S0MRJyzrpw8lPPtv63CFlsywq1cDNlW9kQLqJeTsegbvOcMZoxhfFPkfzmTwcXjtR4IVNGsnlrepxNTxBEx73fYXEwwAUUg+WypWvPAOIoWffNEUQmafBZxvooKbFI56jiSwSE5wPawXVLlbcoNr7vXzUo+wvd3ZVOztT3fgkZdPD/jVcy7dEFxj//blNWTScwEkHOZrkpbczOjCGkaLMnUnZGSHF4WEmzpUuA+fkTDGkiU4WoTdxT6gNpaCsT74SrC3F9/J9jNunVfKqp4LIcFMAMvJLK4IIeieZp5nyyT+dTsD93W52k3NaYs5sMPfG4fzmO0c5gTLyqV4qNm6cV28GWmkGM5VsLTcoMc38JT8cMFzJYKz1vnvZKovw2yJX3mGE9+kza0irBNskbGMXm2JnGgcHl+7d88gSswVl5mE7CG+2tdgYOgBklcp0iWtWvNZ0Om0qClEQgKtQtZLRxc89r+B5fzApBDC3KZTDcbrHoh14n6jvHB/UvMvDJGF5kF4XWBXtND6nwBrlwYOW3DrhFUuTjPQxUqoExBtIi9ez6coxhfSqjR6LbwFmEySp+VaXOLiDK8bFG7d/Z4CgvkGG7LpIEUyniXzaJ/8O=",
        // "c7fff146": "3eLK1JknKLMHueeIIkcKRsshsUT=",
        // "x-sap-ri": "f5a32f66000102030405061701090a0b0c0d0e0f101112131415"}
    }

    private void myTraceWrite(long startAddr, int size) {
        emulator.traceWrite(startAddr, startAddr+size);
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.callSign();
    }
}
