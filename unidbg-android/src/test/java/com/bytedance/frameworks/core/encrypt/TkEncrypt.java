package com.bytedance.frameworks.core.encrypt;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class TkEncrypt extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private final Module module;

    public TkEncrypt()  {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.zhiliaoapp.musically")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/douyin/com.zhiliaoapp.musically_33.2.5.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

//        new AndroidModule(emulator, vm).register(memory);
//        new JniGraphics(emulator, vm).register(memory);

//        emulator.getBackend().registerEmuCountHook(10000);
//        emulator.getSyscallHandler().setVerbose(true);
//        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

//        vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/libPitayaObject.so"), false);
//        vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/libAndroidPitayaProxy.so"), false);
//        vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/libc++_shared.so"), false);


        //模块绑定的java层类
        DvmClass k = vm.resolveClass("ms/bd/o/k");
        DvmClass a0 = vm.resolveClass("ms/bd/o/a0",k);
        vm.resolveClass("com/bytedance/mobsec/metasec/ov/MS", a0);


        dm = vm.loadLibrary("metasec_ov", true);
        module = dm.getModule();
//        hook();
//        saveTrace();
        dm.callJNI_OnLoad(emulator);
    }

    public void hook(){
        // 0xe4fff4b0
//       emulator.traceWrite(0xe4fff450L, 0xe4fff450 + 0x50);
//       int offset = 0x13f604;
//       int offset = 0x555D0;
//       int offset = 0x13E274;  // svc
       int offset = 0x13E030;
        emulator.attach().addBreakPoint(dm.getModule().base + offset, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
//            RegisterContext context = emulator.getContext();
//            UnidbgPointer arg0 = context.getPointerArg(0);
//            long svcCode = arg0.toUIntPeer() - 0xe9;
//            System.out.println("svcCode = " + svcCode);

//            if (svcCode ) {
//                UnidbgPointer arg1 = context.getPointerArg(1);
//                Inspector.inspect(arg1.getByteArray(0,32),"checkStr "+Long.toHexString(arg1.peer));
//                return false;
//            }
            return false;
        });

    }

    public static void main(String[] args) {
        TkEncrypt dyEncrypt = new TkEncrypt();
    }

    public void saveTrace(){
        String dirPath = "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/";

//        TraceFunction traceFunction = new TraceFunction(emulator, module, "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/func.txt");
//        traceFunction.trace_function();

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
//        if (pathname.equals("/proc/self/exe")){
//            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/apks/douyin/exe"), pathname));
////            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
//        }
        return null;
    }
}
