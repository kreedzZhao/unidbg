package com.bytedance.frameworks.core.encrypt;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;
import com.utils.TraceFunction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DyEncrypt extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private final Module module;

    public DyEncrypt()  {
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
        DvmClass a0 = vm.resolveClass("ms/bd/c/i0",h);
        vm.resolveClass("com/bytedance/mobsec/metasec/ml/MS",a0);

        dm = vm.loadLibrary("metasec_ml", true);
        module = dm.getModule();
//        hook();
//        saveTrace();
        dm.callJNI_OnLoad(emulator);
    }

    public void hook(){
        // 0xe4fff4b0
//       emulator.traceWrite(0xe4fff450L, 0xe4fff450 + 0x50);
//       int offset = 0x13f604;
//       int offset = 0x555D0;
//       int offset = 0x13E274;  // svc
       int offset = 0x13E030;
        emulator.attach().addBreakPoint(dm.getModule().base + offset, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
//            RegisterContext context = emulator.getContext();
//            UnidbgPointer arg0 = context.getPointerArg(0);
//            long svcCode = arg0.toUIntPeer() - 0xe9;
//            System.out.println("svcCode = " + svcCode);

//            if (svcCode ) {
//                UnidbgPointer arg1 = context.getPointerArg(1);
//                Inspector.inspect(arg1.getByteArray(0,32),"checkStr "+Long.toHexString(arg1.peer));
//                return false;
//            }
            return false;
        });

    }

    public void callFunc(String url, String headers){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.addLocalObject(new StringObject(vm, url)));
        list.add(vm.addLocalObject(new StringObject(vm, headers)));
        Number number = module.callFunction(emulator, 0x9E098, list.toArray());
        System.out.println(vm.getObject(number.intValue()).toString());
    }

    public Number callX(int arg1, int arg2, long arg3, String arg4, Object arg5){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(arg1);
        list.add(arg2);
        list.add(arg3);
        if(arg4==null){
            list.add(null);
        }else {
            list.add(vm.addLocalObject(new StringObject(vm, arg4)));
        }
        if (arg5 != null) {
            list.add(vm.addGlobalObject(ProxyDvmObject.createObject(vm, arg5)));
        } else {
            list.add(0);
        }
        Number number = module.callFunction(emulator, 0x1c5f40, list.toArray());
        return number;
    };

    public void callInit(){
        // com.ss.android.ugc.aweme.app.host.AwemeHostApplication
        Number number = callX(50331651, 0, 495051302224L, null, null);
        Object value = number.intValue();
        System.out.println("================"+value);

        Number number1 = callX(33554436, 0, 495588013296L, "", vm.resolveClass("com/ss/android/ugc/aweme/app/host/AwemeHostApplication").newObject(null));
        Object res2 = vm.getObject(number1.intValue());
        System.out.println("================"+res2);
    }

    public static void main(String[] args) {
        DyEncrypt dyEncrypt = new DyEncrypt();
        System.out.println("================");
//        dyEncrypt.callInit();
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
                break;
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public void saveTrace(){
        String dirPath = "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/";

//        TraceFunction traceFunction = new TraceFunction(emulator, module, "unidbg-android/src/test/java/com/bytedance/frameworks/core/encrypt/func.txt");
//        traceFunction.trace_function();

//        String traceFile = dirPath + "trace.txt";
//        PrintStream traceStream = null;
//        try {
//            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        //核心 trace 开启代码，也可以自己指定函数地址和偏移量
//        emulator.traceCode(module.base, module.base + module.size).setRedirect(traceStream);
        emulator.traceCode(module.base, module.base + module.size);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open:" + pathname);
        if (pathname.equals("/proc/self/exe")){
            return FileResult.success(new SimpleFileIO(oflags, new File("unidbg-android/src/test/resources/apks/douyin/exe"), pathname));
//            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
        }
        return null;
    }
}
