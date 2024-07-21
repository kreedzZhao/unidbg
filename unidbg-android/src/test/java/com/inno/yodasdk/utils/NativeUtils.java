package com.inno.yodasdk.utils;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.AndroidElfLoader;
import com.github.unidbg.linux.AndroidSyscallHandler;
import com.github.unidbg.linux.android.*;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.sun.jna.Pointer;

import java.io.File;

public class NativeUtils extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private final DvmClass NativeUtils;
    private Pointer buffer;


    NativeUtils() {
        AndroidEmulatorBuilder builder = new AndroidEmulatorBuilder(false) {
            @Override
            public AndroidEmulator build() {
                return new AndroidARMEmulator(processName, rootDir,
                        backendFactories) {
                    @Override
                    protected UnixSyscallHandler<AndroidFileIO>
                    createSyscallHandler(SvcMemory svcMemory) {
                        return new YodaSyscallHandler(svcMemory);
                    }
                };
            }
        };
        emulator = builder
                .setProcessName("com.inno.yodasdk.utils")
                .setRootDir(new File("target/rootfs")).build();
//        emulator = AndroidEmulatorBuilder.for32Bit()
//                .setProcessName("com.inno.yodasdk.utils")
////                .addBackendFactory(new Unicorn2Factory(true))
//                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        // key code
        emulator.getSyscallHandler().addIOResolver(this);
        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator);
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {
            @Override
            public String getProperty(String key) {
                System.out.println("kreedz: " + key);
                switch (key){
                    case "ro.serialno":
                        return "99211FFAZ0088R";
                    case "ro.product.manufacturer":
                        return "Google";
                    case "ro.product.brand":
                        return "google";
                    case "ro.product.model":
                        return "Pixel 4";
                }
                return null;
            }
        });
        memory.addHookListener(systemPropertyHook);


        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/qtt_new.apk")); // 创建Android虚拟机
        vm.setVerbose(true); // 设置是否打印Jni调用细节
        vm.setJni(this);

        // hook open
        DalvikModule dmLibc = vm.loadLibrary(new File("unidbg-android/src/main/resources/android/sdk23/lib/libc.so"), true);
        Module libcModule = dmLibc.getModule();
        long popen = libcModule.findSymbolByName("popen").getAddress();
        emulator.attach().addBreakPoint(popen, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext context = emulator.getContext();
                String string = context.getPointerArg(0).getString(0);
                System.out.println("kreedz: popen command "+string);
                emulator.set("popen_command", string);
                return true;
            }
        });

        // init init_array call after hooking
        DalvikModule dm = vm.loadLibrary("yoda", true);
        dm.callJNI_OnLoad(emulator); // 手动执行JNI_OnLoad函数
        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块

        NativeUtils = vm.resolveClass("com.inno.yodasdk.utils.NativeUtils");
        dm.callJNI_OnLoad(emulator);



//        emulator.traceWrite(module.base + 0x3A0C0, module.base + 0x3A0C0);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
//        emulator.attach().debug();
        System.out.println("kreedz: " + pathname);
        return null;
    }

    public byte[] bulkWark(){
        byte[] value = (byte[]) NativeUtils.callStaticJniMethodObject(
                emulator,
                "bulwark(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)[B",
                "{\"instance\":\"com.inno.yodasdk.info.Infos@103af2e\",\"app_version\":\"3.10.48.000.0714.1521\",\"sensor_count\":\"35\",\"mac\":\"DA:28:C4:45:4F:ED\",\"platform\":\"android\",\"manufacturer\":\"Google\",\"scene\":\"qtt_login\",\"sid\":\"73585f0c-9e9a-45d0-9df5-06cc48bc1186\",\"cpu_model\":\"AArch64 Processor rev 14 (aarch64) ,8,2841600\",\"sdk_version\":\"1.0.7.210128\",\"model\":\"Pixel 4\",\"screen_size\":\"1080,2280,2.75\",\"brand\":\"google\",\"adb\":\"1\",\"gyro\":\"0.0,0.01,0.98\",\"hardware\":\"flame\",\"ext\":\"{\\\"login_way\\\":\\\"2\\\"}\",\"screen_scale\":\"5.7\",\"os_version\":\"29\",\"sim_state\":\"1\",\"screen_brightness\":\"91\",\"volume\":\"5,0,0,8,6\",\"boot_time\":\"1721491758994\",\"wifi_name\":\"<unknown ssid>\",\"tk\":\"ACE5Vh1vlxWgkOBYnvWR-QkgsdOs2iV1JGs0NzUxNDk1MDg5NTIyNQ\",\"charge_state\":\"1\",\"package_name\":\"com.jifen.qukan\",\"wifi_mac\":\"02:00:00:00:00:00\",\"apps_count\":\"4,253\",\"android_id\":\"6110f3ab3b4860cf\",\"cid\":\"47514950895225\"}",
                "dubo",
                "1721530925"
        ).getValue();
        return value;
    }

    public static void main(String[] args) {
        NativeUtils nativeUtils = new NativeUtils();
        byte[] bytes = nativeUtils.bulkWark();
        System.out.println(bytes);
    }
}
