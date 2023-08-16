package com.tv.danmaku.bili;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class Sign extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final DvmClass LibBili;

    public Sign()  {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("tv.danmaku.bili")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/bilibili.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getBackend().registerEmuCountHook(10000);
        emulator.getSyscallHandler().setVerbose(true);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

        emulator.getSyscallHandler().addIOResolver(this);
        dm = vm.loadLibrary("bili", true);

        LibBili = vm.resolveClass("com.bilibili.nativelibrary.LibBili");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("resolve pathname=" + pathname + ", oflags=0x" + Integer.toHexString(oflags));
        if (pathname.equals("/proc/self/cmdline")){
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
        }
        return null;
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        // java/util/Map->isEmpty()Z
        if ("java/util/Map->isEmpty()Z".equals(signature)) {
            return dvmObject.getValue() == null || ((TreeMap<?, ?>) dvmObject.getValue()).isEmpty();
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature){
            case "java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;":{
                TreeMap<?, ?> map = (TreeMap<?, ?>) dvmObject.getValue();
                Object key = varArg.getObjectArg(0).getValue();
                return ProxyDvmObject.createObject(vm, map.get(key));
            }
            case "java/util/Map->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;": {
                TreeMap<Object, Object> map = (TreeMap<Object, Object>) dvmObject.getValue();
                Object key = varArg.getObjectArg(0).getValue();
                Object value = varArg.getObjectArg(1).getValue();
                return ProxyDvmObject.createObject(vm, map.put(key, value));
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        // com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;
        switch (signature){
            case "com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;":{
                Map map = (Map) varArg.getObjectArg(0).getValue();
                return new StringObject(vm, SignedQuery.r(map));
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature){
            case "com/bilibili/nativelibrary/SignedQuery-><init>(Ljava/lang/String;Ljava/lang/String;)V":{
                String arg1 = varArg.getObjectArg(0).getValue().toString();
                String arg2 = varArg.getObjectArg(1).getValue().toString();
                return vm.resolveClass("com.bilibili.nativelibrary.SignedQuery").newObject(new SignedQuery(arg1, arg2));
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    public String callS(){
        TreeMap<String, String> map = new TreeMap<>();
        map.put("build", "6180500");
        map.put("mobi_app", "android");
        map.put("channel", "shenma069");
        map.put("appkey", "1d8b6e7d45233436");
        map.put("s_locale", "zh_CN");
        String ret = LibBili.callStaticJniMethodObject(
                emulator,
                "s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;",
                ProxyDvmObject.createObject(vm, map)
        ).getValue().toString();
        return ret;
    }

    public static void main(String[] args) {
        Sign sign = new Sign();
        String res = sign.callS();
        System.out.println("res: "+res);
    }
}
