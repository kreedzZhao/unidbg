package com.yuanrenxue.challenge.three;

import capstone.Arm64_const;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.*;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.mfw.tnative.AuthorizeHelper;
import unicorn.Arm64Const;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class ChallengeThreeNativeLib extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass ChallengeThreeNativeLib;
    private final VM vm;

    private final Module module;
//    private final File executable
    public ChallengeThreeNativeLib() {
//        executable = new File("unidbg-android/src/test/resources/apk/yuanrenxue-challenge1048.apk");
//        emulator = new TimeARM64Emulator(executable);

        emulator = AndroidEmulatorBuilder
                .for64Bit()
//                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.yuanrenxue.challenge")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/yuanrenxue-challenge1048.apk"));
        vm.setJni(this);
        vm.setVerbose(true);


//        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apk/libthree.so"), true);
        DalvikModule dm = vm.loadLibrary("three", true);
        module = dm.getModule();
        ChallengeThreeNativeLib = vm.resolveClass("com.yuanrenxue.challenge.three.ChallengeThreeNativeLib");
        dm.callJNI_OnLoad(emulator);

        hookTime(module);

        Debugger attach = emulator.attach();
//        // 这个函数是明文初始化，默认输入是 3
//        attach.addBreakPoint(module.base + 0x1F7c4);
        // padding 之后第一次使用
//        attach.addBreakPoint(module.base + 0x1F278);

//        emulator.traceWrite(module.base + 0x359000, module.base + 0x359040);

        String traceFile = "yrx03.txt";
        PrintStream traceStream = null;
        try {
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
            emulator.traceCode(module.base, module.base+module.size).setRedirect(traceStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // hook time
    public void hookTime(Module module){
        // 由于是在函数中，所以在 jni_onload 之后 hook 都可以
        emulator.attach().addBreakPoint(module.base + 0x1F94C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_X0, 0);
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_PC, address+4);
                return true;
            }
        });
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "android/os/Looper->myLooper()Landroid/os/Looper;":
                return vm.resolveClass("android/os/Looper").newObject(signature);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public byte[] sign() {
        return (byte[]) ChallengeThreeNativeLib.newObject(null)
                .callJniMethodObject(emulator, "sign(I)[B", 3)
                .getValue();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(String[] args) {
        ChallengeThreeNativeLib three = new ChallengeThreeNativeLib();
        byte[] sign = three.sign();
        System.out.println(bytesToHex(sign));
    }
}
