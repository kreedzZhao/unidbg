package com.aiqicha;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class Aqc extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final Module module;
    private final DvmClass NativeUtil;

    public Aqc() {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("com.baidu.xin.aiqicha")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/aqc.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().addIOResolver(this);
        dm = vm.loadLibrary("abymg", true);

        module = dm.getModule();
        NativeUtil = vm.resolveClass("com.baidu.abymg.nativeutil.NativeUtil");
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("resolve pathname=" + pathname + ", oflags=0x" + Integer.toHexString(oflags));
//        if (pathname.equals("/proc/self/cmdline")){
//            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)));
//        }
        return null;
    }

    public void hook(){
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module, 0x3634C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                return false;
            }
        });
    }

    public void genAbtkJNI(){
        hook();

        // public static native String genAbtkJNI(String str, String str2, String str3, String str4, String str5);
        String str="+JL8mwrC5DcSUL63boFScA==";
        String str2 = "";
        String str3 = "Udvq6umb9Bh8BQ0VKFLwR8kiUkXXxz3MX8PYG5u3xd9N7guEhGP86SDLbLddqAlR";
        String str4 = "";
        String str5 = "iB54zu4czbbVIQ0APdevNw==";
        String string = NativeUtil.callStaticJniMethodObject(
                emulator,
                "genAbtkJNI(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                new StringObject(vm, str),
                new StringObject(vm, str2),
                new StringObject(vm, str3),
                new StringObject(vm, str4),
                new StringObject(vm, str5)
        ).getValue().toString();
        System.out.println(string);
        // gOi5VHswhvPPlfYM02rnyt4HhLlV8Vy2c7I2Ie37YlCxICzFqthYK1QCR9HMYFYxffcSu3KuG3WfRSNmo4YAFn2ieCSSJwiYd0qO7mMvd5EDuOj6PtelwbxkzYuhvQ7gmhuUHGcFVvvWOoJ4Bms67YrDfkjj8VGE2gNF2cHMF+/w5E/Qw6APAPrqI7Mqvil2
    }

    public static void main(String[] args) {
        Aqc aqc = new Aqc();
        aqc.genAbtkJNI();
    }
}
