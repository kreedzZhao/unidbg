package cn.xiaochuankeji.tieba;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class ZuiYou extends AbstractJni {

    private final AndroidEmulator emulator;
    private final DvmClass NetCrypto;
    private final VM vm;

    private final Module module;
    public ZuiYou() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("cn.xiaochuankeji.tieba")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/right573.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("net_crypto", true);
        module = dm.getModule();
        NetCrypto = vm.resolveClass("com.izuiyou.network.NetCrypto");
        dm.callJNI_OnLoad(emulator);
    }

    public void native_init(){
        NetCrypto.callStaticJniMethod(emulator, "native_init()V");
    }

    public String sign(){
        String arg1 = "https://api.izuiyou.com/index/recommendxx";
        String arg2 = "r0ysue";
        return NetCrypto.callStaticJniMethodObject(
                emulator,
                "sign(Ljava/lang/String;[B)Ljava/lang/String;",
        arg1, arg2.getBytes(StandardCharsets.UTF_8)).getValue().toString();
    }

    public void hook65540() {
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.wrap(module.base + 0x65540 + 1, new WrapCallback<HookZzArm32RegisterContext>(){
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Inspector.inspect(ctx.getR0Pointer().getByteArray(0, 0x10), "Arg1");
                System.out.println(ctx.getR1Long());
                Inspector.inspect(ctx.getR2Pointer().getByteArray(0, 0x10), "Arg3");
                ctx.push(ctx.getR2Pointer());
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer output = ctx.pop();
                Inspector.inspect(output.getByteArray(0, 0x10), "Arg3 after function");
            }
        });
    }

    public static void main(String[] args) {
        ZuiYou zuiyou = new ZuiYou();
        zuiyou.hook65540();
        zuiyou.native_init();
        System.out.println(zuiyou.sign());
    }
}
