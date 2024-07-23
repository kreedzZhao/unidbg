package com.meituan.android.common.mtguard;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class ShellBridge extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmClass ShellBridge;
    private final VM vm;

    private final Module module;
    public ShellBridge() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.dianping.v1")
                .build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/com.dianping.v1_11.20.13.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("mtguard", true);
        module = dm.getModule();
        ShellBridge = vm.resolveClass("com.meituan.android.common.mtguard.ShellBridge");
        dm.callJNI_OnLoad(emulator);

        // start trace
//        String traceFile = "/home/kreedz/Documents/reverse/long/trace1.log";
//        try {
//            PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile), true);
//            emulator.traceCode(module.base,module.base+module.size).setRedirect(traceStream);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("kreedz Path:"+pathname);
        return null;
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/Class->forName(Ljava/lang/String;)Ljava/lang/Class;":
//                System.out.println(vaList.getObjectArg(0).getValue().toString());
                // com.meituan.android.common.mtguard.MTGuard
                return vm.resolveClass("java/lang/Class");
            case "java/lang/Class->main2(I[Ljava/lang/Object;)Ljava/lang/Object;":
                int arg1 = vaList.getIntArg(0);
                switch (arg1) {
                    case 3:
//                        ArrayList<Object> list = new ArrayList<>(1);
//                        StringObject stringObject = new StringObject(vm, "/data/user/0/com.dianping.v1");
//                        vm.addLocalObject(stringObject);
//                        ArrayObject arr = new ArrayObject(stringObject);
//                        vm.addLocalObject(arr);
//                        list.add(vm.addLocalObject(arr));
                        return new StringObject(vm, "/data/user/0/com.dianping.v1");
                }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/Class->getClassLoader()Ljava/lang/ClassLoader;":
                return vm.resolveClass("java/lang/ClassLoader").newObject(null);
            case "java/lang/ClassLoader->loadClass(Ljava/lang/String;)Ljava/lang/Class;":
                System.out.println("loadClass: " + vaList.getObjectArg(0).getValue().toString());
                return vm.resolveClass("java/lang/Class");
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    public void main111(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        // 初始化为 1 正常跑为 66
        list.add(1);
        DvmObject<?> dvmObject = vm.resolveClass("java/lang/String").newObject(null);
//        StringObject dvmObject = new StringObject(vm, "s/m");
        vm.addLocalObject(dvmObject);
        ArrayObject arrayObject = new ArrayObject(dvmObject);
        vm.addLocalObject(arrayObject);
        list.add(vm.addLocalObject(arrayObject));
        module.callFunction(emulator, 0xf841, list.toArray());
    }

    public static void main(String[] args) {
        ShellBridge shellBridge = new ShellBridge();
        shellBridge.main111();
    }
}
