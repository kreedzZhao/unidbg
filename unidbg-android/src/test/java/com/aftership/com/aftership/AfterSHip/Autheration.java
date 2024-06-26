package com.aftership.com.aftership.AfterSHip;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

public class Autheration extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final DvmClass SigEntity;

    public Autheration() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.aftership.AfterShip")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/aftership.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        dm = vm.loadLibrary("androidsig-lib", true);
        // 使用 libandroid 模块
        new AndroidModule(emulator, vm).register(memory);

        SigEntity = vm.resolveClass("com.automizely.sig.SigEntity");
        dm.callJNI_OnLoad(emulator);
    }

//    public String nativeGenerateSignature(){
//        SigEntity.callJniMethodObject(emulator, "")
//    }

    public static void main(String[] args) {
        Logger.getLogger(DalvikVM.class).setLevel(Level.DEBUG);
        Logger.getLogger(BaseVM.class).setLevel(Level.DEBUG);


        Autheration autheration = new Autheration();
//        autheration.callObjectMethod()
    }
}
