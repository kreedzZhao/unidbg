package com.sichuanol.cbgc.util;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import unicorn.ArmConst;

import java.io.File;

public class SignManager extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass signManager;
    private final VM vm;

    private final Module module;
    public SignManager() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
//                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.sichuanol.cbgc")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/川报观察7.2.1.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apk/libwtf.so"), true);
        module = dm.getModule();
        signManager = vm.resolveClass("com.sichuanol.cbgc.util.SignManager");
        dm.callJNI_OnLoad(emulator);

//        Debugger attach = emulator.attach(DebuggerType.CONSOLE);
//        attach.addBreakPoint(module.base + 0x00abf);

//        patchLog();
//        patchLog2();
        patchLog3();
    }

    // patch
    public void patchLog(){
        int patchCode = 0x46004600;
        emulator.getMemory().pointer(module.base + 0x00abE).setInt(0, patchCode);
    }

    public void patchLog2(){
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0x00abE);
        assert pointer != null;
        try(Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb)){
            KeystoneEncoded nop = keystone.assemble("nop");
            byte[] machineCode = nop.getMachineCode();
            pointer.write(0, machineCode, 0, machineCode.length);
            pointer.write(machineCode.length, machineCode, 0, machineCode.length);
        }
    }

    public void patchLog3(){
        emulator.attach().addBreakPoint(module.base + 0x00abE, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, address+5);
                return false;
            }
        });
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/sichuanol/cbgc/util/LogShutDown->getAppSign()Ljava/lang/String;":
                return new StringObject(vm, "0093CB6721DAF15D31CFBC9BBE3A2B79");
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public String getSign(){
        String str1 = "";
        String str2 = "";
        String str3 = "1628093856262";
        String getSign = signManager.callStaticJniMethodObject(
                emulator,
                "getSign",
                str1, str2, str3
        ).getValue().toString();
        System.out.println(getSign);
        return getSign;
    }

    public static void main(String[] args) {
        SignManager sm = new SignManager();
        sm.getSign();
    }
}
