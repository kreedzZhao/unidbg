package com.bytedance.frameworks.core.encrypt;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;

import java.io.*;

public class DyEncrypt extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private final Module module;

    public DyEncrypt()  {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.ss.android.ugc.aweme")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/douyin/douyin_28_9_0.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
        new JniGraphics(emulator, vm).register(memory);

        emulator.getBackend().registerEmuCountHook(10000);
        emulator.getSyscallHandler().setVerbose(true);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

//        vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/arm64-v8a/libc.so"), true);
        DvmClass h = vm.resolveClass("ms/bd/c/l");
        DvmClass a0 = vm.resolveClass("ms/bd/c/j0",h);
        vm.resolveClass("com/bytedance/mobsec/metasec/ml/MS",a0);

        dm = vm.loadLibrary("metasec_ml", true);
        module = dm.getModule();
        saveTrace();
//        vm.addNotFoundClass("com/bytedance/mobsec/metasec/ml/MS");
        dm.callJNI_OnLoad(emulator);
    }

    public static void main(String[] args) {
        DyEncrypt dyEncrypt = new DyEncrypt();
    }

    public void saveTrace(){
        String dirPath = "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/";

//        String traceFile = dirPath + "trace.txt";
//        PrintStream traceStream = null;
//        try {
//            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        //核心 trace 开启代码，也可以自己指定函数地址和偏移量
//        emulator.traceCode(module.base, module.base + module.size).setRedirect(traceStream);
        emulator.traceCode(module.base, module.base + module.size);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open:" + pathname);
        return null;
    }
}
