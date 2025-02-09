package com.bytedance.frameworks.core.encrypt;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmLong;
import com.github.unidbg.linux.file.DirectoryFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.utils.MemoryScan;
import com.utils.TraceFunction;
import unicorn.Arm64Const;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.utils.Tools.randint;

public class DyEncrypt extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final DvmClass initClass;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private final Module module;

    private boolean isFirst = true;
    private String appVersionName = "27.9.0";

    public DyEncrypt() {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.ss.android.ugc.aweme")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/douyin/douyin_27.9.0.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
//        new JniGraphics(emulator, vm).register(memory);

//        emulator.getBackend().registerEmuCountHook(10000);
//        emulator.getSyscallHandler().setVerbose(true);
//        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

        // com.bytedance.mobsec.metasec.ml.MS ms.bd.c.e0 ms.bd.c.l
        // 27.9 com.bytedance.mobsec.metasec.ml.MS ms.bd.c.i0 ms.bd.c.l
        DvmClass h = vm.resolveClass("ms/bd/c/l");
        DvmClass a0 = vm.resolveClass("ms/bd/c/i0", h);
        vm.resolveClass("com/bytedance/mobsec/metasec/ml/MS", a0);

        dm = vm.loadLibrary("metasec_ml", true);
        module = dm.getModule();

//        saveTrace();
        dm.callJNI_OnLoad(emulator);

        initClass = vm.resolveClass("ms.bd.c.l");
    }

    public void hook(long offset) {
        // XA: 0xe8bb4 向上追 0x0a5724 0x0a1020
//       emulator.traceWrite(0xe4fff450L, 0xe4fff450 + 0x50);
//        int offset = 0xB6CB8;
        AtomicInteger callCount = new AtomicInteger();
        emulator.attach().addBreakPoint(dm.getModule().base + offset, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
            RegisterContext context = emulator.getContext();
            UnidbgPointer output = context.getPointerArg(2);
            UnidbgPointer input = context.getPointerArg(1);
            Inspector.inspect(input.getByteArray(0, 0x10), "AES_INPUT");

            emulator.attach().addBreakPoint(context.getLRPointer().peer, new BreakPointCallback() {
                @Override
                public boolean onHit(Emulator<?> emulator, long address) {
//                        emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
                    Inspector.inspect(output.getByteArray(0, 0x20), "AES_OUTPUT");
                    return true;
                }
            });

            callCount.addAndGet(1);
            System.out.println("call count: " + callCount.get());
//            if (callCount.get() == 4) {
//                return false;
//            }

            return true;
        });

    }

    public void dfa() {
        // 0xDEFE8 0xDEFEC
        emulator.attach().addBreakPoint(dm.getModule().base + 0xDEFE8, new BreakPointCallback() {

            int callCount = 0;
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                callCount++;
                System.out.println("call count: " + callCount);
                if (callCount == 4) {
                    // x16=0xbb359fbe x5=0x60d5e7a7 x17=0xcfed6121 x1=0x39367f43
                    int newValue = 0xbb359fbe+randint(1, 50);
//                    emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W0 + 16, 0x56359fbe);
//                    emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W0 + 17, 0xcf996121);
//                    emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W0 + 5, 0x60d5e722);
                    emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W0 + 1, 0x39367f22);
//                    emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W0 + 16, newValue);
                    System.out.println("change x16 to： " + Integer.toHexString(newValue));
                    return true;
                }
                return true;
            }
        });
    }

    public void callFunc(String url, String headers) {
        String dirPath = "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/";

//        TraceFunction traceFunction = new TraceFunction(emulator, module, "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/func.txt");
//        traceFunction.trace_function();

//        String outputPath = dirPath + "memorys.bin";
//        new MemoryScan(emulator, outputPath);

//        emulator.traceWrite(0x126d8000, 0x126d8000+0x160);
        hook(0xDEDD4);
            dfa();
//        hook(0xB7F44);
//        hook(0xDF60C);
//        hook(0xB7F44);
//        hook(0xDEFEC);
//        hook(0xB6CB8);
//        hook(0x963E8);
//        hook(0xD5784);
//        saveTrace();

//        List<Object> list = new ArrayList<>(10);
//        list.add(vm.addLocalObject(new StringObject(vm, url)));
//        list.add(vm.addLocalObject(new StringObject(vm, headers)));
        module.callFunction(emulator, 0x9E098, url, headers);
//        System.out.println(vm.getObject(number.intValue()).toString());
        String string = emulator.getContext().getPointerArg(0).getString(0);
        System.out.println(string);
    }

    public Number callX(int arg1, int arg2, long arg3, String arg4, Object arg5) {
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(arg1);
        list.add(arg2);
        list.add(arg3);
        if (arg4 == null) {
            list.add(null);
        } else {
            list.add(vm.addLocalObject(new StringObject(vm, arg4)));
        }
        if (arg5 != null) {
            list.add(vm.addGlobalObject(ProxyDvmObject.createObject(vm, arg5)));
        } else {
            list.add(0);
        }
        Number number = module.callFunction(emulator, 0xcb6f4, list.toArray());
        return number;
    }

    ;

    public void callInit() {
        /**
         * ms.bd.c.l.a
         * 调用 java 函数，直接就可以解决 init 的问题，而 navive 的并不能
         2025-02-04 16:58:01.132 21749-21866 XStealth                com.ss.android.ugc.aweme             I  param: 16777219 0 0 null com.ss.android.ugc.aweme.app.host.AwemeHostApplication@feeaa6a
         2025-02-04 20:38:39.765 31644-31818 Kreedz->                com.ss.android.ugc.aweme             E  int1: 67108865, int2: 0, long: 0, str: ["1128","","","bo95dJizD1WFcV03zOuLzN5Pn1sFtVa3szqiVQmflMJTNW0p0Kpqfw8D4i0zUlfrou4kuYt\/i0521YRygM83dwv\/wn3DD+TMJF+QFzW9wb8Qq2\/1B4jPMbObrDNdyMMukpAYqy1fLWtbLGVIPxsFsZegwQy5lsRX9h49PH\/Qx8MwgYvWvH7ZTFLV28LwTWZiljQyBPaBE+TsyumEu0Y+JRkeidHFEYcVs0yRoa+xC004hugQhdPupIt6dBiWA4phsB3fNJZjFTAKGE1lPB4gzt6Qf+FmlgZBbRvT8zekxTV2HZ5dUvSutB2\/0QpbHKAvWL4DRA==","v04.05.02.01-bugfix","","","","","","0","-1","810",[],["tk_key","douyin"]], obj: null
         */
        DvmObject<?> dvmObject1 = initClass.callStaticJniMethodObject(
                emulator, "a(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                16777219, 0, 0, null, vm.resolveClass("com.ss.android.ugc.aweme.app.host.AwemeHostApplication").newObject(null)
        );
        System.out.println("1 call: " + dvmObject1);

        // 这里结果为 true，但是直接调用是 false
        String config = "[\"1128\",\"\",\"\",\"bo95dJizD1WFcV03zOuLzN5Pn1sFtVa3szqiVQmflMJTNW0p0Kpqfw8D4i0zUlfrou4kuYt\\/i0521YRygM83dwv\\/wn3DD+TMJF+QFzW9wb8Qq2\\/1B4jPMbObrDNdyMMukpAYqy1fLWtbLGVIPxsFsZegwQy5lsRX9h49PH\\/Qx8MwgYvWvH7ZTFLV28LwTWZiljQyBPaBE+TsyumEu0Y+JRkeidHFEYcVs0yRoa+xC004hugQhdPupIt6dBiWA4phsB3fNJZjFTAKGE1lPB4gzt6Qf+FmlgZBbRvT8zekxTV2HZ5dUvSutB2\\/0QpbHKAvWL4DRA==\",\"v04.05.02.01-bugfix\",\"\",\"\",\"\",\"\",\"\",\"0\",\"-1\",\"810\",[],[\"tk_key\",\"douyin\"]]";
        DvmObject<?> dvmObject2 = initClass.callStaticJniMethodObject(
                emulator, "a(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                67108865, 0, 0, config, null
        );
        System.out.println("2 call: " + dvmObject2.getValue());

        DvmObject<?> dvmObject3 = initClass.callStaticJniMethodObject(
                emulator, "a(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                67108866, 0, 0, "1128", null
        );
        System.out.println("3 call: " + dvmObject3.getValue());

        initClass.callStaticJniMethodObject(
                emulator, "a(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                50331651, 0, 491663158480L, null, null
        );
        initClass.callStaticJniMethodObject(
                emulator, "a(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                33554436, 0, dvmObject3.getValue(), "", vm.resolveClass("com.ss.android.ugc.aweme.app.host.AwemeHostApplication").newObject(null)
        );
        initClass.callStaticJniMethodObject(
                emulator, "a(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                33554433, 0, dvmObject3.getValue(), "luckydog_init", vm.resolveClass("com.ss.android.ugc.aweme.app.host.AwemeHostApplication").newObject(null)
        );


//        Number number1 = callX(16777219, 0, 0, null, vm.resolveClass("com.ss.android.ugc.aweme.app.host.AwemeHostApplication").newObject(null));
//        Object res2 = vm.getObject(number1.intValue());
//        System.out.println("================"+res2);
    }

    public static void main(String[] args) {
        DyEncrypt dyEncrypt = new DyEncrypt();
        System.out.println("================");
        dyEncrypt.callInit();
        String url = "https://api5-normal-m-hj.amemv.com/aweme/v1/danmaku/get_v2/?item_id=7455654726131125531&start_time=0&has_total=true&total=24&danmaku_density=-1&authentication_token=MS4wLjAAAAAA4ERx43gGq5TtENTNBWYWDnKLaupOYt1mqm6Dtls-Sn7W30ZWjN7OddN5mnL5cNo4vivYWW7UNwK8bn8_jc1KOtpb3_wzvpFlTIhOCLw7sT7aHjxYfUuCuU_8v9BH9-12UFeft0CTWSbJD4DBkHisX1AKqfwbh-06Ou0HhBp8e6D02IcG2pZgi-8wBkMCdRZMfoZicyMMF_Nz0llzvzBWciS8q_Nchsb9e91I61kwj-sgbHnGFRaNrqI-XDUOR2lx&duration=11000&is_lvideo=false&iid=2197275370075360&device_id=3886113653835577&ac=wifi&channel=douyinweb1_64&aid=1128&app_name=aweme&version_code=270900&version_name=27.9.0&device_platform=android&os=android&ssmix=a&device_type=Pixel+4&device_brand=google&language=en&os_api=33&os_version=13&manifest_version_code=270901&resolution=1080*2214&dpi=440&update_version_code=27909900&_rticket=1737532667129&first_launch_timestamp=1737530973&last_deeplink_update_version_code=0&cpu_support64=true&host_abi=arm64-v8a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&need_personal_recommend=1&is_android_pad=0&is_android_fold=0&ts=1737532666&cdid=b4ac1f58-4cfc-4dff-ac51-ec490896d19a";
        String headers = ("cookie\r\n" +
                "passport_csrf_token=ae6d4b4926767ee65f8facd4095d7356; passport_csrf_token_default=ae6d4b4926767ee65f8facd4095d7356; odin_tt=f8d61a33546caa5bb0b270d58a0d13133871483aaf017e5ddd813ce794f9a0780386e13bae5f84adefff80b9316239254fc154176e1acf7d2562bce055310c33e27428ecfd4919f507a04c1f17ee72cc\r\n" +
                "x-tt-dt\r\n" +
                "AAAQQEFEMC67WVNGAQJUQL4XDERYMAXKV7P5PRXEL6VLLXNEP2AD64MPFGTZPCQ5TLQZOLECRX7BB4YZZLNP53BIOFTAW7F7NX7OXFODFGRTRSKAQDDM2N5TSWW5Y\r\n" +
                "activity_now_client\r\n" +
                "1737532668582\r\n" +
                "x-ss-req-ticket\r\n" +
                "1737532667130\r\n" +
                "sdk-version\r\n" +
                "2\r\n" +
                "passport-sdk-version\r\n" +
                "203183\r\n" +
                "x-vc-bdturing-sdk-version\r\n" +
                "3.7.0.cn\r\n" +
                "x-tt-store-region\r\n" +
                "cn-gd\r\n" +
                "x-tt-store-region-src\r\n" +
                "did\r\n" +
                "x-tt-request-tag\r\n" +
                "s=1;p=0\r\n" +
                "x-ss-dp\r\n" +
                "1128\r\n" +
                "x-tt-trace-id\r\n" +
                "00-8d04769a0ddce6657e4c7398f6630468-8d04769a0ddce665-01\r\n" +
                "user-agent\r\n" +
                "com.ss.android.ugc.aweme/270901 (Linux; U; Android 13; en_US; Pixel 4; Build/TP1A.220624.014; Cronet/TTNetVersion:eb99db8f 2023-11-08 QuicVersion:43f5661a 2023-09-26)\r\n" +
                "accept-encoding\r\n" +
                "gzip, deflate, br");
        dyEncrypt.callFunc(url, headers);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/bytedance/mobsec/metasec/ml/MS->b(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;":
                // hook ms.bd.c.l.b 函数
                int i0 = vaList.getIntArg(0);
                System.out.println("i0======:" + i0);
                switch (i0) {
                    case 16777250:
                        if (isFirst) {
                            isFirst = false;
                            return new StringObject(vm, "80dd151582403f5a");
                        } else {
                            return new StringObject(vm, "869e6c75bd49da823be35a48efd4195f52e51cf9ccecff7ee5254d6ff4732309");
                        }
                    case 16777233:
                        return new StringObject(vm, appVersionName);
                    case 65539:
                        return new StringObject(vm, "/data/user/0/com.ss.android.ugc.gweme/files/.msdata");
                    case 33554433:
                    case 33554434:
                        return new StringObject(vm, "true");
                }
                break;
            case "java/lang/Thread->currentThread()Ljava/lang/Thread;":
                return vm.resolveClass("java/lang/Thread").newObject(vm);
            case "java/lang/Boolean->valueOf(Z)Ljava/lang/Boolean;":
                DvmObject<?> objectArg = vaList.getObjectArg(0);
                if (objectArg == null) {
                    return vm.resolveClass("java/lang/Boolean").newObject(Boolean.valueOf(null)); // TODO: 强行定位 true
                }
                System.out.println("Boolean.valueOf: " + objectArg);
            case "java/lang/Long->valueOf(J)Ljava/lang/Long;":
                long longArg = vaList.getLongArg(0);
                System.out.println("Long.valueOf: " + longArg);
                return DvmLong.valueOf(vm, longArg);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/Thread->getStackTrace()[Ljava/lang/StackTraceElement;":
                DvmObject<?>[] a = {
                        vm.resolveClass("java/lang/StackTraceElement").newObject("dalvik.system.VMStack"),
                        vm.resolveClass("java/lang/StackTraceElement").newObject("java.lang.Thread"),
                        vm.resolveClass("java/lang/StackTraceElement").newObject("java.lang.Thread"),
                };
                return new ArrayObject(a);
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;":
                return new StringObject(vm, dvmObject.toString());
            case "java/lang/StackTraceElement->getMethodName()Ljava/lang/String;":
                return new StringObject(vm, "getStackTrace");
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    public void saveTrace() {
        String dirPath = "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/";

//        TraceFunction traceFunction = new TraceFunction(emulator, module, "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/func.txt");
//        traceFunction.trace_function();

        String traceFile = dirPath + "trace.txt";
        PrintStream traceStream = null;
        try {
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //核心 trace 开启代码，也可以自己指定函数地址和偏移量
        emulator.traceCode(module.base, module.base + module.size).setRedirect(traceStream);
//        emulator.traceCode(module.base, module.base + module.size);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
//        if (pathname.equals("/proc/self/exe")){
//            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/apks/douyin/exe"), pathname));
////            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
//        }
        if (pathname.equals("/data/user/0/com.ss.android.ugc.gweme/files/.msdata/mssdk/ml/")) {
            return FileResult.success(new DirectoryFileIO(
                    oflags, "unidbg-android/src/test/resources/apks/douyin/mssdk/ml", new File("unidbg-android/src/test/resources/apks/douyin/mssdk/ml")
            ));
        }
        if (pathname.contains(".msdata/mssdk/ml/")) {
            File file = new File(pathname.replace("/data/user/0/com.ss.android.ugc.gweme/files/.msdata/mssdk/ml/",
                    "unidbg-android/src/test/resources/apks/douyin/mssdk/ml"));
            if (file.exists()) {
                return FileResult.success(new SimpleFileIO(
                        oflags, file, pathname
                ));
            }
        }

        System.out.println("file open:" + pathname);
        return null;
    }
}
