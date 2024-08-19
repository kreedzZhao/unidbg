package com.cyberfend.cyfsecurity;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;

public class SensorDataBuilder extends AbstractJni {
    private final AndroidEmulator emulator;

    private final DvmClass sensorBuilder;
    private final VM vm;

    public SensorDataBuilder() {
        emulator = AndroidEmulatorBuilder
//                .for32Bit()
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.fedex.ida.android")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/FedEx_9.14.0.apks"));
        vm.setJni(this);
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
        // FedEx_9.14.0_apks.apk
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apk/libakamaibmp.so"), true);

        sensorBuilder = vm.resolveClass("com/cyberfend/cyfsecurity/SensorDataBuilder");
        dm.callJNI_OnLoad(emulator);
    }

    public static void main(String[] args) {
        SensorDataBuilder sb = new SensorDataBuilder();
    }
}
