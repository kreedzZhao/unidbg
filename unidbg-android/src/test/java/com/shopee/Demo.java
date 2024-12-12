package com.shopee;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.DynarmicFactory;
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
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

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

    public void selfDebug() {
        // 0xfffe0134
        emulator.attach().addBreakPoint(dm.getModule().base + 0x9A208, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
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
                UnidbgPointer pointer1 = context.getPointerArg(0);
                UnidbgPointer pointer2 = context.getPointerArg(1);
                Inspector.inspect(pointer2.getByteArray(0,len),"src "+Long.toHexString(pointer1.peer)+" memcpy "+Long.toHexString(pointer2.peer));
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
                return new StringObject(vm, "/data/app/~~TNhZXjbYEVaQ2at1MsSVMA==/com.shopee.ph-HZr14DpuHxk6SIasCzTZIQ==");
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
        String traceFile = "/Users/kz.zhao/Documents/reverse/native/unidbg/unidbg-android/src/test/java/com/shopee/CrackMeTracetraceCode.txt";
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

        saveTrace();
//        selfDebug();
        patch();
        hook();
        StringObject result = RequestCryptUtils.callStaticJniMethodObject(emulator, "vuwuuwvw([B[B)Ljava/lang/String;", arr1, arr2);
//        StringObject result = RequestCryptUtils.callStaticJniMethodObject(emulator, "vuwuuwvw([B[B)Ljava/lang/String;", arr1,null);
        System.out.println(result.toString());
        // {"1a9ec9b9": "zlug+XF2Nwvjy/venQJClQYEmb4=",
        // "4b3e0f91": "hvLRCZaawpVyScGBsFOhhjOosjT=",
        // "9bbcf962": "kx6r9dUssUTsskOpdfDWTkTJxbf1Z3mNyReTwnzzu10fNbJuezee7myKrTyQSxvbrn50fUBFhPgsqtcZxtwnO7/PcN2OPU5+deBSKFh3Oiw2pxcA5tFlRwebTf4J+RZwoSINw1HVQA+PrBK4tD9eAR3RNxEUf594aQWEvEVlqrNTmt//SJ0VZNHcwfQeNCy6nzR12riUeMcW3nAougLYz68GYxD1SVLqcf9LAbvwuTu08+Q81akHEIVhPMQ8sj2qjmIpbtysegDw7RJ+bQ7yz1qx8CdOnT6N5T/n+UWuZ68i9Vo5DqAaVdp2bdAzYzApMSXMLJuEazuZDJpAvhutet8gWKZysKzSzDU2X1xL/yDmG3M5QdpJMs4GBm8rte80+oRlDg36wq+NUI5pf+OPvyYkrvKFih8F5DSp01QfT28LskpHdvZnnxMkV6fZVL3pRinkv8KYkSoLfY5QBkT299YlR8h2r66j57TajopnLZFxmEzBk5FZhl3+UPCc+Jt+akOIf9RIit8al26gFPuA16zOstIycx2T3zscbqrr2DZxQXOBQmOFEKX5pnXquQU6iB3YJ/js77I1s1GTukcvXiUCaaPu0jwVBvjKGVVLwB3D8TKRd6jH6ZXgKthuZdoB9PV2eecfUcyGiza+OiDlLkfcjXt=",
        // "c7fff146": "Q/clH7jEZPj//oqAxtOOMkshsUT=",
        // "x-sap-ri": "b2060000000102030405061701090a0b0c0d0e0f101112131415"}
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.callSign();
    }
}
