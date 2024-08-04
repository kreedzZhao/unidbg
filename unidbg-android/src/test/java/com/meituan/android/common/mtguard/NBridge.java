package com.meituan.android.common.mtguard;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.SystemPropertyHook;
import com.github.unidbg.linux.android.SystemPropertyProvider;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.ClassLoader;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NBridge extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmClass NBridge;
    private final VM vm;

    private final Module module;
    public NBridge() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
//                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.sankuai.meituan")
                .build();
        Memory memory = emulator.getMemory();
        emulator.getSyscallHandler().addIOResolver(this);
        memory.setLibraryResolver(new AndroidResolver(23));

        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator);
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {
            @Override
            public String getProperty(String key) {
                //                    case "ro.build.id":
//                        return new StringObject(vm, "QD1A.190821.007");
//                    case "persist.sys.usb.config":
////                        return new StringObject(vm, "adb");
//                        return new StringObject(vm, "charging");
//                    case "sys.usb.config":
//                        return new StringObject(vm, "charging");
//                    case "sys.usb.state":
//                        return new StringObject(vm, "charging");
                System.out.println("kreedz getProperty: " + key);
                switch (key){
                    case "ro.build.user":
                        return "user";
                    case "ro.build.display.id":
                        return "QD1A.190821.007";
                    case "ro.build.host":
                        return "abfarm799";
//                    case "ro.serialno":
//                        return "99211FFAZ0088R";
//                    case "ro.product.manufacturer":
//                        return "Google";
//                    case "ro.product.brand":
//                        return "google";
//                    case "ro.product.model":
//                        return "Pixel 4";
                }
                return null;
            }
        });
        memory.addHookListener(systemPropertyHook);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/mt/mt.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("mtguard", true);
        module = dm.getModule();
        NBridge = vm.resolveClass("com.meituan.android.common.mtguard.NBridge");
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

    public String mtMain(){
        module.callFunction(emulator, 0x5a38d);


        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclazz，直接填0，一般用不到。
        list.add(203);
        StringObject input2_1 = new StringObject(vm, "9b69f861-e054-4bc4-9daf-d36ae205ed3e");
        ByteArray input2_2 = new ByteArray(vm, "GET /meishi/filter/v7/deal/select __reqTraceID=06609852-8046-49a2-8fd6-3cbe271e2556&abForRelevance=a&ab_arena_namechange=shiyanzu3&beacons=%5B%5D&cateId=1&ci=1&cityId=1&cold_launch=false&hasGroup=true&inner_jump=true&isLocal=0&is_preload=1&meishiHomePageEnterChannel=mt_app_home_page&msid=58f4ec5a49d34ecfa993477473a5b4f0a1720152699459932381720271621779&newStyle=e&offset=250&optimus_code=10&optimus_risk_level=71&poisBeforeInsert=&queryId=68256ab1-8d66-4040-9789-c4c298dc78ab&revisonStrategy=a&sessionClickedPois=&sessionImpressedPois=&silentRefresh=false&sort=defaults&userid=-1&utm_campaign=AgroupBgroupC0E303005519717216175931563050345678902967_a620934924_c23_e8712708736071571901Ghomepage_category2_1__a1__c-1024__gfood&utm_content=58f4ec5a49d34ecfa993477473a5b4f0a172015269945993238&utm_medium=android&utm_source=wandoujia&utm_term=1100090405&uuid=0000000000000428CDB68213F4B4388C20A910A13B758A172015270033710288&version_name=11.9.405".getBytes(StandardCharsets.UTF_8));
        DvmInteger input2_3 = DvmInteger.valueOf(vm, 2);
        vm.addLocalObject(input2_1);
        vm.addLocalObject(input2_2);
        vm.addLocalObject(input2_3);
        // 完整的参数2
        list.add(vm.addLocalObject(new ArrayObject(input2_1, input2_2, input2_3)));
        Number number = module.callFunction(emulator, 0x41bd, list.toArray());
        DvmObject[] value = ((ArrayObject) vm.getObject(number.intValue())).getValue();
        StringObject result = (StringObject) value[0];
        String res = result.getValue().toString();
        Inspector.inspect(res.getBytes(StandardCharsets.UTF_8), "result -> ");
        return res;
    }

    public void main111(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclazz，直接填0，一般用不到。
        list.add(1);
        DvmObject<?> obj = vm.resolveClass("java/lang/object").newObject(null);
        vm.addLocalObject(obj);
        ArrayObject myobject = new ArrayObject(obj);
        vm.addLocalObject(myobject);
        list.add(vm.addLocalObject(myobject));
        Number number = module.callFunction(emulator, 0x41bd, list.toArray());
        ArrayObject s = vm.getObject(number.intValue());
        String res = s.getValue()[0].getValue().toString();
        System.out.println("init res: "+res);
    };

    public static void main(String[] args) {
        NBridge nBridge = new NBridge();
        nBridge.main111();
        System.out.println(nBridge.mtMain());
        // 2448abf8d1cc87209245ba66fc375927
        // c21ffc63d2808d075ee18a7fd7b759ca
        // df4684330fedcd133e440c94c54e37ab
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/meituan/android/common/mtguard/NBridge->getClassLoader()Ljava/lang/ClassLoader;":
//                return new ClassLoader(vm, signature);
                return vm.resolveClass("java/lang/ClassLoader").newObject(null);
            case "java/lang/Class->main2(I[Ljava/lang/Object;)Ljava/lang/Object;":
                System.out.println("main2: "+vaList.getIntArg(0));
//                System.out.println(vaList.getIntArg(0));
//                return vm.resolveClass("java/lang/Object").newObject(null);
                int arg0 = vaList.getIntArg(0);
                switch (arg0){
                    case 1:
                        return new StringObject(vm, "com.sankuai.meituan");
                    case 4:
                        return new StringObject(vm, "ms_com.sankuai.meituan");
                    case 5:
                        return new StringObject(vm, "ppd_com.sankuai.meituan.xbt");
                    case 2:
                        return vm.resolveClass("android/app/ContextImpl").newObject(null);
                    case 6:
                        return new StringObject(vm, "5.1.9");
                    case 3:
                        return DvmInteger.valueOf(vm, 0);
                }
                throw new UnsupportedOperationException(signature + " -> " + arg0);
            case "java/lang/System->getProperty(Ljava/lang/String;)Ljava/lang/String;":
                String arg_0 = vaList.getObjectArg(0).getValue().toString();
                switch (arg_0){
                    case "http.proxyHost":
                        return null;
                    case "https.proxyHost":
                        return null;
                }
                throw new UnsupportedOperationException(arg_0);
            case "android/os/SystemProperties->get(Ljava/lang/String;)Ljava/lang/String;":
                String argStr0 = vaList.getObjectArg(0).getValue().toString();
                switch (argStr0){
                    case "ro.build.id":
                        return new StringObject(vm, "QD1A.190821.007");
                    case "persist.sys.usb.config":
//                        return new StringObject(vm, "adb");
                        return new StringObject(vm, "charging");
                    case "sys.usb.config":
                        return new StringObject(vm, "charging");
                    case "sys.usb.state":
                        return new StringObject(vm, "charging");
                }
                throw new UnsupportedOperationException(signature + " -> " + argStr0);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/ClassLoader->loadClass(Ljava/lang/String;)Ljava/lang/Class;":
                System.out.println("loadClass: " + vaList.getObjectArg(0).getValue().toString());
                return vm.resolveClass("java/lang/Class");
            case "android/app/ContextImpl->getPackageManager()Landroid/content/pm/PackageManager;":
                DvmClass clazz = vm.resolveClass("android/content/pm/PackageManager");
                return clazz.newObject(signature);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature){
            case "android/content/pm/ApplicationInfo->sourceDir:Ljava/lang/String;":
                return new StringObject(vm, "/data/app/com.sankuai.meituan-dinPQmJAjRqMIdGCOATtRg==/base.apk");
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature){
            case "android/os/Build->BRAND:Ljava/lang/String;":
                return new StringObject(vm, "google");
            case "android/os/Build->TYPE:Ljava/lang/String;":
                return new StringObject(vm, "user");
            case "android/os/Build->HARDWARE:Ljava/lang/String;":
                return new StringObject(vm, "flame");
            case "android/os/Build->MODEL:Ljava/lang/String;":
                return new StringObject(vm, "Pixel 4");
            case "android/os/Build->TAGS:Ljava/lang/String;":
                return new StringObject(vm, "release-keys");
            case "android/os/Build$VERSION->RELEASE:Ljava/lang/String;":
                return new StringObject(vm, "10");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/io/File->canRead()Z":
                return true;
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "java/io/File-><init>(Ljava/lang/String;)V":
                System.out.println("java.io.File: "+vaList.getObjectArg(0).getValue().toString());
                return vm.resolveClass("java/io/File").newObject(vaList.getObjectArg(0).getValue().toString());
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("kreedz path: "+pathname);
        if (pathname.contains("/sys/class/power_supply/battery/temp")){
            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/apk/mt/battery/temp"), pathname));
        }
        if (pathname.equals("/sys/class/power_supply/battery/voltage_now")){
            return FileResult.<AndroidFileIO>success(new ByteArrayFileIO(oflags, pathname, "4365937".getBytes()));
        }
//        if (pathname.contains("/data/data/com.sankuai.meituan/files/._mtg_mtdfp_up/.mini/hornCache")){
//            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/apk/mt/.mini/hornCache"), pathname));
//        }
        if (pathname.equals("/data/app/com.sankuai.meituan-dinPQmJAjRqMIdGCOATtRg==/base.apk")){
            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/apk/mt/mt.apk"), pathname));
        }
        return null;
    }
}
