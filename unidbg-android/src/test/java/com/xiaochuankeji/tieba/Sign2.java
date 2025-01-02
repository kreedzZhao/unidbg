package com.xiaochuankeji.tieba;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Sign2 extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;

    private final DalvikModule dm;
    private final DvmClass aaa;
    private final Module module;

    public Sign2(){
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName("cn.xiaochuankeji.tieba")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/03-zuiyou.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
//        dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/apks/libav_lite.so"), true);
        dm = vm.loadLibrary("av_lite", true);
        module = dm.getModule();
        // 使用 libandroid 模块
        new AndroidModule(emulator, vm).register(memory);

        aaa = vm.resolveClass("com.qq.a.a.a.a.a");
        dm.callJNI_OnLoad(emulator);
    }

    public void native_init(){
        // 0x7660c
        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第一个参数是env
        list.add(0); // 第二个参数，实例方法是jobject，静态方法是jclass，直接填0，一般用不到。
        module.callFunction(emulator, 0x7660c, list.toArray());
    };

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            // Convert each byte to a two-digit hex string
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                // Append a leading zero if the hex string is a single character
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase(); // Convert to uppercase if needed
    }

    public void saveTrace(){
        // unidbg-android/src/test/java/com/xiaochuankeji/tieba/Sign.java
        String traceFile = "unidbg-android/src/test/java/com/xiaochuankeji/tieba/aes.txt";
        PrintStream traceStream = null;
        try {
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        emulator.traceCode(module.base,module.base+module.size).setRedirect(traceStream);
    }

    public void hook() {
        Debugger attach = emulator.attach();
        attach.addBreakPoint(module.findSymbolByName("memcpy").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext context = emulator.getContext();
                int len = context.getIntArg(2);
//                UnidbgPointer pointer1 = context.getPointerArg(0);
//                UnidbgPointer pointer2 = context.getPointerArg(1);
//                Inspector.inspect(pointer2.getByteArray(0,len),"src "+Long.toHexString(pointer1.peer)+" memcpy "+Long.toHexString(pointer2.peer));

                UnidbgPointer dest = context.getPointerArg(0);
                UnidbgPointer src = context.getPointerArg(1);
                int size = context.getIntArg(2);
                System.out.println("PC: "+context.getPCPointer()+" LR: "+context.getLRPointer()+" dest: "+dest+" src: "+src+" size: "+size);
//                Inspector.inspect(src.getByteArray(0, 0x30), "memcpy input");
                Inspector.inspect(src.getByteArray(0,size),"src "+Long.toHexString(src.peer)+" memcpy "+Long.toHexString(dest.peer));
                return true;
            }
        });
    }

    public void selfDebug(long offset) {
        AtomicInteger num = new AtomicInteger();
        emulator.attach().addBreakPoint(dm.getModule().base + offset, (emu, address) -> {
            System.out.println("Hit breakpoint: 0x" + Long.toHexString(address));
            num.addAndGet(1);
            System.out.println("num: " + num);

            if (offset == 0x78124){
                RegisterContext context = emulator.getContext();
                UnidbgPointer pointer1 = context.getPointerArg(0);
                UnidbgPointer pointer2 = context.getPointerArg(1);
//                Inspector.inspect(pointer2.getByteArray(0,len),"src "+Long.toHexString(pointer1.peer)+" memcpy "+Long.toHexString(pointer2.peer));

                if(num.get() == 36){
                    return false;
                }
            }

            return false;
        });
    }

    public void myTraceWrite(){
        emulator.traceWrite(0x1260900eL, 0x1260900e+1);
    }

    public byte[] e(){
//        saveTrace();
//        hook();
//        myTraceWrite();
        // 0x94180 0x94198
        // 0x9458C free
        // 0x9C348
//        selfDebug(0x94180);
//        selfDebug(0x94198);
//        selfDebug(0x9458C);
//        selfDebug(0x9C348);
        // key: 37373737373737373737373737373737
        // iv: 370E3737373737373737373737373737

        ArrayList<Object> objects = new ArrayList<>(3);
        objects.add(vm.getJNIEnv());
        objects.add(0);
        String input = "{\"h_av\":\"6.3.2\",\"h_dt\":0,\"h_os\":33,\"h_app\":\"zuiyou\",\"h_model\":\"Pixel 4\",\"h_did\":\"151bf99ec379c294\",\"h_nt\":1,\"h_ch\":\"vivo\",\"h_ts\":1735721096687,\"android_id\":\"151bf99ec379c294\",\"h_ids\":{},\"h_m\":308383928,\"token\":\"T4K5NIx5u90VuhjSBD4yz0nIQ9SR44KnP4hzNDeSRpc_uoXjkUKd9qsIyDcpMq8NWVekxwfNgGgi_R9eb-9i3sm_jbw==\"}";
//        String input = "kreedz";
        objects.add(vm.addLocalObject(new ByteArray(vm, input.getBytes())));
        Number number = module.callFunction(emulator, 0x7667c, objects.toArray());
        byte[] value = (byte[]) vm.getObject(number.intValue()).getValue();
        System.out.println("hex: " + bytesToHex(value));
        return value;
        // 370A37373737373737373737373737373E5E850681D7A48F641E562D8A90E978
        //                                 3e5e850681d7a48f641e562d8a90e978ad64b96c06bc404faee633422cca5733

        // 37373737373737373737373737373737
        // 37083737373737373737373737373737
        // 370E3737373737373737373737373737E242FAFC71B02511DB027B1D7A3FF06F644ECD40306A0769B47D8074FE8741D88E6FDC406B1FC4972B0A658FF70F2E19078A8B04B63E829BB5419CD1713101E94C326F6D76AE74E5CB4F39DE24E13CC4F704176E90659FF4DBBE860D7654042D4167F73400428A24BAF214F1217B3CFBF90EA20B41404632A295936B0C1B211329B9C5A93868E5B1807E9DD80E204A65E7CF648DE0300BDBF9F86CE468F5BC851FF8FA42287D4FD03FC23A3442F28D4FC2D7C623A7E03FC9C3422659BC5FB29395B71D91D35CF7CDFC7ED8E2397E24B30FA19C348D0C3DB73BD814FB3054C174B86E41725B31FC1BD0D6B3834D39093F31F69B88857F6D91D3A9B5323F47EDA0E448BF8E57AC9D323564E781E8D9D48CCA4158FB98B79AC28B054CB294D97B182F24DB1EB3CC912C730AAF200CA21FB075B08AB9C33AFEFEF54DA5157543FD39
        //                                 e242fafc71b02511db027b1d7a3ff06f644ecd40306a0769b47d8074fe8741d88e6fdc406b1fc4972b0a658ff70f2e19078a8b04b63e829bb5419cd1713101e94c326f6d76ae74e5cb4f39de24e13cc4f704176e90659ff4dbbe860d7654042d4167f73400428a24baf214f1217b3cfbf90ea20b41404632a295936b0c1b211329b9c5a93868e5b1807e9dd80e204a65e7cf648de0300bdbf9f86ce468f5bc851ff8fa42287d4fd03fc23a3442f28d4fc2d7c623a7e03fc9c3422659bc5fb29395b71d91d35cf7cdfc7ed8e2397e24b30fa19c348d0c3db73bd814fb3054c174b86e41725b31fc1bd0d6b3834d39093f31f69b88857f6d91d3a9b5323f47eda0e448bf8e57ac9d323564e781e8d9d48cca4158fb98b79ac28b054cb294d97b182f24db1eb3cc912c730aaf200ca21fb075b08ab9c33afefef54da5157543fd39fb3212e85e46757ca768cdc0ac605687

    }

    public void s(byte[] input){
//        myTraceWrite();
        // 生成位置 0x782c0 func: 0x78124 bt: 0x07753c
//        hook();
        selfDebug(0x78124);
//        selfDebug(0x07753c);
        // func: 0x7734C
        // 查看 ida 推测 0x79330
//        selfDebug(0x79330);
//        selfDebug(0xA06F0);
        selfDebug(0x9D600);

        ArrayList<Object> objects = new ArrayList<>(3);
        objects.add(vm.getJNIEnv());
        objects.add(0);
        objects.add(vm.addLocalObject(new StringObject(vm, "kreedz")));
        objects.add(vm.addLocalObject(new ByteArray(vm, input)));
        Number number = module.callFunction(emulator, 0x7734c, objects.toArray());
        String value = (String) vm.getObject(number.intValue()).getValue();
        System.out.println("hex: " + value);
        // v2-a4573e0fe5525f8f0d389051fc3af39a
    }

    public void hook_zz(){
        HookZz instance = HookZz.getInstance(emulator);
//        instance.replace();
    }

    public static void main(String[] args) {
        Sign2 sign2 = new Sign2();
        sign2.native_init();
        byte[] aesRes = sign2.e();
        sign2.s(aesRes);
    }
}
