package com.sogou.scoretools;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.context.Arm32RegisterContext;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScEncryptWall extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private final DvmClass ScEncryptWall;
    private Pointer buffer;


    ScEncryptWall() {

        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.sogou.activity.src")
//                .addBackendFactory(new Unicorn2Factory(true))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/sougou.apk")); // 创建Android虚拟机
        vm.setVerbose(true); // 设置是否打印Jni调用细节
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary("SCoreTools", true);
        dm.callJNI_OnLoad(emulator); // 手动执行JNI_OnLoad函数
        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块

        ScEncryptWall = vm.resolveClass("com.sogou.scoretools.ScEncryptWall");
        dm.callJNI_OnLoad(emulator);

        emulator.traceWrite(module.base + 0x3A0C0, module.base + 0x3A0C0);
    }

    public void native_init(){
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclass，直接填0，一般用不到。
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null); // context
        list.add(vm.addLocalObject(context));
        module.callFunction(emulator, 0x9565, list.toArray());
    };


    public void encryptDemo(){
        List<Object> list = new ArrayList<>(10);
        // arg1 env
        list.add(vm.getJNIEnv());
        // arg2 jobject/jclass 一般用不到 可以直接填0
        list.add(0);
        String str = "http://app.weixin.sogou.com/api/searchapp";
        String str2 = "type=2&ie=utf8&page=1&query=%E5%A5%8B%E9%A3%9E%E5%AE%89%E5%85%A8&select_count=1&tsn=1&usip=";
        String str3 = "lilac";
        list.add(vm.addLocalObject(new StringObject(vm,str)));
        list.add(vm.addLocalObject(new StringObject(vm,str2)));
        list.add(vm.addLocalObject(new StringObject(vm,str3)));
        Number number = module.callFunction(emulator,0x9ca1,list.toArray());
        String result = vm.getObject(number.intValue()).getValue().toString();
        System.out.println(result);
    }

    public static void main(String[] args) {
        ScEncryptWall scEncryptWall = new ScEncryptWall();
//        scEncryptWall.hookEncryptWallEncode();
//        scEncryptWall.inlineHookEncryptWallEncode();
//        scEncryptWall.hookByUnicorn();
        scEncryptWall.HookByConsoleDebugger();
        scEncryptWall.native_init();
        scEncryptWall.encryptDemo();
    }

    // console debugger
    public void HookByConsoleDebugger(){
        Debugger attach = emulator.attach();
//        attach.addBreakPoint(module.base + 0xA284);
//        attach.addBreakPoint(module.base + 0xA288);
//        attach.addBreakPoint(module.base + 0xB300);
        attach.addBreakPoint(module.base + 0xB372);
    }

    public void hookEncryptWallEncode(){
        HookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();
        hookZz.wrap(module.base + 0xA284 + 1, new WrapCallback<HookZzArm32RegisterContext>(){
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                System.out.println("HookZz hook EncryptWallEncode");
                UnidbgPointer input1 = ctx.getPointerArg(0);
                UnidbgPointer input2 = ctx.getPointerArg(1);
                UnidbgPointer input3 = ctx.getPointerArg(2);
                System.out.println("参数1："+input1.getString(0));
                System.out.println("参数2："+input2.getString(0));
                System.out.println("参数3："+input3.getString(0));
                buffer = ctx.getPointerArg(3);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                super.postCall(emulator, ctx, info);
                byte[] byteArray = buffer.getByteArray(0, 0x100);
                Inspector.inspect(byteArray, "EncryptWallEncode Output: ");
            }
        });
        hookZz.disable_arm_arm64_b_branch();
    }

    public void inlineHookEncryptWallEncode(){
        HookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();
        hookZz.instrument(module.base + 0xA284 + 1, new InstrumentCallback<Arm32RegisterContext>() {
            @Override
            public void dbiCall(Emulator<?> emulator, Arm32RegisterContext ctx, HookEntryInfo info) {
                System.out.println("HookZz inline hook EncryptWallEncode");
                Pointer input1 = ctx.getPointerArg(0);
                Pointer input2 = ctx.getPointerArg(1);
                Pointer input3 = ctx.getPointerArg(2);
                // getString的参数i代表index,即input[i:]
                System.out.println("参数1："+input1.getString(0));
                System.out.println("参数2："+input2.getString(0));
                System.out.println("参数3："+input3.getString(0));

                buffer = ctx.getPointerArg(3);
            }

        });
        hookZz.instrument(module.base + 0xA288 + 1, new InstrumentCallback<Arm32RegisterContext>() {
            @Override
            public void dbiCall(Emulator<?> emulator, Arm32RegisterContext ctx, HookEntryInfo info) {
                Inspector.inspect(buffer.getByteArray(0, 0x100), "EncryptWallEncode Inline Output: ");
            }
        });
        hookZz.disable_arm_arm64_b_branch();
    }

    public void hookByUnicorn(){
        emulator.getBackend().hook_add_new(new CodeHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                if(address == (module.base+0x9d24)){
                    System.out.println("Hook By Unicorn");
                    RegisterContext ctx = emulator.getContext();
                    Pointer input1 = ctx.getPointerArg(0);
                    Pointer input2 = ctx.getPointerArg(1);
                    Pointer input3 = ctx.getPointerArg(2);
                    // getString的参数i代表index,即input[i:]
                    System.out.println("参数1："+input1.getString(0));
                    System.out.println("参数2："+input2.getString(0));
                    System.out.println("参数3："+input3.getString(0));

                    buffer = ctx.getPointerArg(3);
                }
                if (address == (module.base+0x9d28)){
                    Inspector.inspect(buffer.getByteArray(0,0x100), "Unicorn hook EncryptWallEncode");
                }
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        }, module.base + 0x9d24, module.base + 0x9d28, null);
    }
}
