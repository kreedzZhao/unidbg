package com.cloudy.linglingbang.model.request.retrofit2;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPoint;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class CheckCodeUtil extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmClass checkCodeUtil;
    private final VM vm;

    private final Module module;

    private final Memory memory;

    public CheckCodeUtil() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
//                .for64Bit()
//                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.cloudy.linglingbang")
                .build();
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/五菱汽车_V8.2.6.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        //  绑定重定向
        emulator.getSyscallHandler().addIOResolver(this);
        DalvikModule dm = vm.loadLibrary("encrypt", true);
        module = dm.getModule();
        checkCodeUtil = vm.resolveClass("com.cloudy.linglingbang.model.request.retrofit2.CheckCodeUtil");
        dm.callJNI_OnLoad(emulator);

        Debugger dbg = emulator.attach(DebuggerType.CONSOLE);
        // 检测逻辑
//        dbg.addBreakPoint(module.base + 0x160E0 + 1);
//        dbg.addBreakPoint(module.base + 0x161b5);
//        dbg.addBreakPoint(module.base + 0x5BCE + 1);

        // 函数中看到 aes 的流程，但是是全局变量，看下实际跳转的地址
//        dbg.addBreakPoint(module.base + 0x15ED4 + 1); // 跳转到 aes_encrypt1 0x5CF5
        // EncryptOneBlock
        // MOV             R12, R4
        // POP.W           {R4,LR}
        // BX              R12
//        dbg.addBreakPoint(module.base + 0x5AF6 + 1); // 0x5139

        // state 地址
//        dbg.addBreakPoint(module.base + 0x515A + 1); // 0xbffff468
//        dbg.addBreakPoint(module.base + 0x5165 ); // 0xbffff468


        patchDetect();
    }

    public void patchDetect() {
//        UnidbgPointer pointer = UnidbgPointer.pointer(emulator,module.base + 0x160E2);
//        byte[] code = new byte[]{(byte) 0x20, (byte) 0xB1};//直接用硬编码改原so的代码：  4FF00109
//        pointer.write(code);

        int patchCode = 0x20B1;
        emulator.getMemory().pointer(module.base + 0x160E2 + 1).setInt(0, patchCode);

//        emulator.attach().addBreakPoint(module.base + 0x160E2, new BreakPointCallback() {
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, address+13);
//                return true;
//            }
//        });

        // 修改加密入参
        emulator.attach().addBreakPoint(module.base + 0x5CF5, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                String fakeInput = "hello";
                int len = fakeInput.length();
                MemoryBlock fakeInputBlock = emulator.getMemory().malloc(len, true);
                fakeInputBlock.getPointer().write(fakeInput.getBytes(StandardCharsets.UTF_8));
                // 修改 r1 地址为这个字符串地址
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, fakeInputBlock.getPointer().peer);
                return true;
            }
        });

        // 查看输出结果
        emulator.attach().addBreakPoint(module.base + 0x5139, new BreakPointCallback() {
            RegisterContext context = emulator.getContext();
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                // 这里查看 lr 位置的返回值
                emulator.attach().addBreakPoint(context.getLRPointer().peer, new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
                        // m0xbffff51c 0x5b19 断点可以看到 R2->R6 所以就是 R6
                        /*
                        第一次也就是正确的
                        57 B0 D6 0B 18 73 AD 7D E3 AA 2F 5C 1E 4B 3F F6
                        57 B0 F4 0B 18 8E AD 7D D9 AA 2F 5C 1E 4B 3F ED
                        6B B0 D6 0B 18 73 AD E6 E3 AA 81 5C 1E 06 3F F6
                        57 46 D6 0B B4 73 AD 7D E3 AA 2F 4D 1E 4B FF F6
                        FA B0 D6 0B 18 73 AD A1 E3 AA D8 5C 1E 74 3F F6
                        57 B0 D6 E5 18 73 8D 7D E3 16 2F 5C 23 4B 3F F6
                        57 FC D6 0B 6A 73 AD 7D E3 AA 2F BE 1E 4B 00 F6
                        57 B0 65 0B 18 AE AD 7D 04 AA 2F 5C 1E 4B 3F 28
                        57 B0 D6 3D 18 73 13 7D E3 8F 2F 5C 02 4B 3F F6
                        57 B0 04 0B 18 DC AD 7D 77 AA 2F 5C 1E 4B 3F 85
                        5D B0 D6 0B 18 73 AD 2F E3 AA D6 5C 1E AC 3F F6
                        57 B0 D6 C9 18 73 C9 7D E3 E5 2F 5C 80 4B 3F F6
                         */
                        return false;
                    }
                });
                return true;
            }
        });

        // state dfa 攻击
        // if ( i == 9 )
        //        break; 其实在这个函数随机位置这里
        emulator.attach().addBreakPoint(module.base+0x5195, new BreakPointCallback() {
            int round = 0;
            UnidbgPointer statePointer = memory.pointer(0xbffff468);
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                round += 1;
                System.out.println("round:"+round);
                if (round % 9 == 0){
                    statePointer.setByte(randInt(0, 3), (byte) randInt(0, 0xff)); // TODO
                    return true;
                }
                return true;//返回true 就不会在控制台断住
            }

            private long randInt(int min, int max) {
                Random random = new Random();
                return random.nextInt((max - min) + 1) + min;
            }
        });
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("kreedz Path:" + pathname);
        return null;
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "android/app/ActivityThread->currentActivityThread()Landroid/app/ActivityThread;":
                return vm.resolveClass("android/app/ActivityThread").newObject(null);
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/app/ActivityThread->getSystemContext()Landroid/app/ContextImpl;":
                return vm.resolveClass("android/app/ContextImpl").newObject(null);
            case "android/app/ContextImpl->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "android/os/Build->MODEL:Ljava/lang/String;":
                return new StringObject(vm, "Pixel 4");
            case "android/os/Build->MANUFACTURER:Ljava/lang/String;":
                return new StringObject(vm, "Google");
            case "android/os/Build$VERSION->SDK:Ljava/lang/String;":
                return new StringObject(vm, "33");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    public String decrypt(String input) {
        ArrayList<Object> list = new ArrayList<>(5);
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(vm.addLocalObject(new StringObject(vm, input)));
        Number number = module.callFunction(emulator, 0x160B5, list.toArray());
        String res = vm.getObject(number.intValue()).getValue().toString();
        System.out.println("decrypt res: " + res);
        return res;
    }

    public String encrypt() {
        // MC7jek3TNYIfE+liRct8qHWDwJHa1NBEwg5Ofv9GRRcy5J2/5XTo8AI34EDdrJ4pLSmhI+oh0+tTAKl2g3l0xVZ0+sZH/r1mklAq7Pn2caezaBB3TTuxjGsTmLVZBGHMz7hw0sBOJVn34F0mSZrwTilyVu6W1+ZtZos5M9VQ02JGkkb/mSIo1+13APv+aEmhk9uvO6u+EkaSEczu8KeF48A==
        String input = "sd=0";
        ArrayList<Object> list = new ArrayList<>(5);
        list.add(vm.getJNIEnv());
        list.add(0);
        list.add(vm.addLocalObject(new StringObject(vm, input)));
        list.add(2);
        list.add(vm.addLocalObject(new StringObject(vm, "1724507256601")));
        Number number = module.callFunction(emulator, 0x13481, list.toArray());
        String res = vm.getObject(number.intValue()).getValue().toString();
        System.out.println("encrypt res: " + res);
        return res;
    }

    public static void main(String[] args) {
        CheckCodeUtil cu = new CheckCodeUtil();
//        // 真实返回数据
//        // sd=0&ostype=ios&imei=00&mac=00:00:00:00:00:00&model=Pixel 4&sdk=33&serviceTime=1724507256601&mod=Google&checkcode=a28c730f02f248372b64303dfa15a479
//        String input = "MC7jek3TNYIfE+liRct8qHWDwJHa1NBEwg5Ofv9GRRcy5J2/5XTo8AI34EDdrJ4pLSmhI+oh0+tTAKl2g3l0xVZ0+sZH/r1mklAq7Pn2caezaBB3TTuxjGsTmLVZBGHMz7hw0sBOJVn34F0mSZrwTilyVu6W1+ZtZos5M9VQ02JGkkb/mSIo1+13APv+aEmhk9uvO6u+EkaSEczu8KeF48A==";
//        cu.decrypt(input);
//        // encrypt 生成
//        // sd=0&ostype=ios&imei=00&mac=00:00:00:00:00:00&model=Pixel 4&sdk=33&serviceTime=1724507256601&mod=Google&checkcode=f434d066a83ffdbfe68921c782cfd589
//        input = "MC7jek3TNYIfE+liRct8qHWDwJHa1NBEwg5Ofv9GRRcy5J2/5XTo8AI34EDdrJ4pLSmhI+oh0+tTAKl2g3l0xVZ0+sZH/r1mklAq7Pn2caezaBB3TTuxjGsTmLVZBGHMz7hw0sBOJVn34F0mSZrwTijVjT+p5ZnuyCG7q99wfqfMLFmLXUNcCi8IX7BeVd3MWdfQB1AhlOSg+CQh1keh1SA==";
//        cu.decrypt(input);
        // 修改为 hello
        String encryptRes = "MV7DWCxhzrX3jqi9cHks/9lew1gsYc61946ovXB5LP/ZXsNYLGHOtfeOqL1weSz/2V7DWCxhzrX3jqi9cHks/9lew1gsYc61946ovXB5LP/ZXsNYLGHOtfeOqL1weSz/2V7DWCxhzrX3jqi9cHks/9lew1gsYc61946ovXB5LP/ZXsNYLGHOtfeOqL1weSz/2V7DWCxhzrX3jqi9cHks/9g==";
        cu.decrypt(encryptRes);
        cu.encrypt();
    }
}
