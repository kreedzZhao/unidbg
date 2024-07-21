package com.yuanrenxue.challenge.three;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.mfw.tnative.AuthorizeHelper;

import java.io.File;

public class ChallengeThreeNativeLib extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass ChallengeThreeNativeLib;
    private final VM vm;

    private final Module module;

    public ChallengeThreeNativeLib() {
//        AndroidEmulatorBuilder builder = new AndroidEmulatorBuilder(false) {
//            public AndroidEmulator build() {
//                return new AndroidARMEmulator(processName, rootDir,
//                        backendFactories) {
//                    @Override
//                    protected UnixSyscallHandler<AndroidFileIO>
//                    createSyscallHandler(SvcMemory svcMemory) {
//                        return new TimeSyscallHandler(svcMemory);
//                    }
//                };
//            }
//        };
        // 注意這裡只有 32 位
//        emulator = builder.build();
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.yuanrenxue.challenge")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/yuanrenxue-challenge1048.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("three", true);
        module = dm.getModule();
        ChallengeThreeNativeLib = vm.resolveClass("com.yuanrenxue.challenge.three.ChallengeThreeNativeLib");
        dm.callJNI_OnLoad(emulator);
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
