package com.mfw.tnative;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AuthorizeHelper extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass AuthorizeHelper;
    private final VM vm;

    private final Module module;
    public AuthorizeHelper() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.mfw.roadbook")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/mafengwo_ziyouxing.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("mfw", true);
        module = dm.getModule();
        AuthorizeHelper = vm.resolveClass("com.mfw.tnative.AuthorizeHelper");
        dm.callJNI_OnLoad(emulator);
    }

    public String xPreAuthencode(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclazz，直接填0，一般用不到。
        Object custom = null;
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(custom);// context
        list.add(vm.addLocalObject(context));
        list.add(vm.addLocalObject(new StringObject(vm, "r0ysue")));
        list.add(vm.addLocalObject(new StringObject(vm, "com.mfw.roadbook")));

        Number number = module.callFunction(emulator, 0x2e301, list.toArray());
        String result = vm.getObject(number.intValue()).getValue().toString();
        return result;
    }

//    public String xPreAuthencode(){
//        Object custom = null;
//        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(custom);// context
//        String arg2 = "r0ysue";
//        String arg3 = "com.mfw.roadbook";
//
////        ArrayList<Object> objects = new ArrayList<>();
////        objects.add(arg1);
////        objects.add(arg2);
////        objects.add(arg3);
//
//        String xPreAuthencode = AuthorizeHelper.callJniMethodObject(
//                emulator,
//        "xPreAuthencode(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
//                        context, arg2, arg3)
//                .getValue().toString();
//        return xPreAuthencode;
//    }

    public static void main(String[] args) {
        AuthorizeHelper authorizeHelper = new AuthorizeHelper();
        String res = authorizeHelper.xPreAuthencode();
        System.out.println(res);

    }
}
