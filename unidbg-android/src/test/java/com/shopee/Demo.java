package com.shopee;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Demo extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;

    public Demo()  {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.shopee.ph")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/shopee.xapk"));
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().addIOResolver(this);
        dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apks/libshpssdk.so"), true);

        dm.callJNI_OnLoad(emulator);

        // 0xfffe0134
        emulator.attach().addBreakPoint(dm.getModule().base + 0x96dd0, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
            return false;
        });
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("resolve pathname=" + pathname + ", oflags=0x" + Integer.toHexString(oflags));
//        if (pathname.equals("/proc/self/cmdline")){
//            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
//        }
        return null;
    }

    public void callSign(){
        // 0x995dc
        DvmClass RequestCryptUtils = vm.resolveClass("com/shopee/shpssdk/wvvvuwwu");


//        byte[] bytes = "https://mall.shopee.ph/api/v4/pages/bottom_tab_bar".getBytes();
        byte[] bytes = "https://mall.shopee.ph/api/v4/pages/bottom_tab_bar".getBytes();
        ByteArray arr1 = new ByteArray(vm,bytes);

        byte[] bytes2 = "{\"img_size\":\"3.0x\",\"latitude\":\"\",\"location\":\"[]\",\"longitude\":\"\",\"new_arrival_reddot_last_dismissed_ts\":0,\"feed_reddots\":[{\"timestamp\":0,\"noti_code\":28}],\"client_feature_meta\":{\"is_live_and_video_merged_tab_supported\":false},\"video_reddot_last_dismissed_ts\":0,\"view_count\":[{\"count\":1,\"source\":0,\"tab_name\":\"Live\"}]}".getBytes();
        ByteArray arr2 = new ByteArray(vm,bytes2);

//        emulator.traceCode(module.base,module.base+module.size);
        StringObject result = RequestCryptUtils.callStaticJniMethodObject(emulator, "vuwuuwvw([B[B)Ljava/lang/String;", arr1,arr2);
//        StringObject result = RequestCryptUtils.callStaticJniMethodObject(emulator, "vuwuuwvw([B[B)Ljava/lang/String;", arr1,null);
        System.out.println(result.toString());
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.callSign();
    }
}
