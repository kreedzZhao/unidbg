package com.xiaochuankeji.tieba;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Sign2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;

    private final DalvikModule dm;
    private final DvmClass aaa;
    private final Module module;

    public Sign2(){
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("cn.xiaochuankeji.tieba")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/03-zuiyou.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
//        dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apks/libav_lite.so"), true);
        dm = vm.loadLibrary("av_lite", true);
        module = dm.getModule();
        // 使用 libandroid 模块
        new AndroidModule(emulator, vm).register(memory);

        aaa = vm.resolveClass("com.qq.a.a.a.a.a");
        dm.callJNI_OnLoad(emulator);
    }

    public void native_init(){
        // 0x7660c
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclass，直接填0，一般用不到。
        module.callFunction(emulator, 0x7660c, list.toArray());
    };

    public static void main(String[] args) {
        Sign2 sign2 = new Sign2();
        sign2.native_init();
    }
}
