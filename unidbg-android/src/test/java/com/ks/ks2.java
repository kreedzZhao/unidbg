package com.ks;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.AssetManager;
import com.github.unidbg.linux.android.dvm.api.ServiceManager;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmBoolean;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;
import com.utils.MemoryScan;
import com.utils.SearchData;
import com.utils.TraceFunction;
import unicorn.Arm64Const;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ks2 extends AbstractJni implements IOResolver {
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open:" + pathname);
        return null;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    ks2() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .setProcessName("com.smile.gifmaker")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));

        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/ks/ks11.420.30984.apk"));
        // 设置JNI
        vm.setJni(this);
        // 打印日志
        vm.setVerbose(true);
        new JniGraphics(emulator, vm).register(memory);
        new AndroidModule(emulator, vm).register(memory);
        emulator.getSyscallHandler().addIOResolver(this);   //重定向io
        // 加载目标SO
        DalvikModule dm = vm.loadLibrary("kwsgmain", true);
        //获取本SO模块的句柄,后续需要用它
        module = dm.getModule();
        // 调用JNI OnLoad
        dm.callJNI_OnLoad(emulator);
  }

    ;

    public void callByAddress() {
        List<Object> list = new ArrayList<>(4);
        list.add(vm.getJNIEnv()); // 第⼀个参数是env
        DvmObject<?> thiz = vm.resolveClass("com/kuaishou/android/security/internal/dispatch/JNICLibrary").newObject(null);
        list.add(vm.addLocalObject(thiz)); // 第⼆个参数，实例⽅法是jobject，静态⽅法是jclass，直接填0，⼀般⽤不到。
        DvmObject<?> context = vm.resolveClass("com/yxcorp/gifshow/App").newObject(null); // context
        vm.addLocalObject(context);
        list.add(10412); //参数1
        StringObject appkey = new StringObject(vm, "d7b7d042-d4f2-4012-be60-d97ff2429c17"); // SO⽂件有校验
        vm.addLocalObject(appkey);
        DvmInteger intergetobj = DvmInteger.valueOf(vm, 0);
        vm.addLocalObject(intergetobj);
        list.add(vm.addLocalObject(new ArrayObject(intergetobj, appkey, intergetobj, intergetobj, context, intergetobj, intergetobj)));
        // 直接通过地址调⽤
        Number numbers = module.callFunction(emulator, 0x41680, list.toArray());
        System.out.println("numbers:" + numbers);
        DvmObject<?> object = vm.getObject(numbers.intValue());
        String result = (String) object.getValue();
        System.out.println("result:" + result);
    }

    private void myHook() {
//        emulator.traceWrite(0xE4FFF329L, 0xE4FFF329L + 24);
//        emulator.traceWrite(0x124e4eb0L, 0x124e4eb0 + 24);
        emulator.traceWrite(0xE4FFF5E0L, 0xE4FFF5E0 + 24);
    }

    ;

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/yxcorp/gifshow/App->getPackageCodePath()Ljava/lang/String;": {
                return new StringObject(vm, "/data/app/com.smile.gifmaker-q14Fo0PSb77vTIOM1-iEqQ==/base.apk");
            }
            case "com/yxcorp/gifshow/App->getAssets()Landroid/content/res/AssetManager;": {
                return new AssetManager(vm, signature);
            }
            case "com/yxcorp/gifshow/App->getPackageName()Ljava/lang/String;": {
                return new StringObject(vm, "com.smile.gifmaker");
            }
            case "com/yxcorp/gifshow/App->getPackageManager()Landroid/content/pm/PackageManager;": {
                DvmClass clazz = vm.resolveClass("android/content/pm/PackageManager");
                return clazz.newObject(signature);
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callBooleanMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/Boolean->booleanValue()Z":
                DvmBoolean dvmBoolean = (DvmBoolean) dvmObject;
                return dvmBoolean.getValue();
        }
        return super.callBooleanMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/kuaishou/android/security/internal/common/ExceptionProxy->getProcessName(Landroid/content/Context;)Ljava/lang/String;":
                return new StringObject(vm, "com.smile.gifmaker");
            case "com/meituan/android/common/mtguard/NBridge->getSecName()Ljava/lang/String;":
                return new StringObject(vm, "ppd_com.sankuai.meituan.xbt");
            case "com/meituan/android/common/mtguard/NBridge->getAppContext()Landroid/content/Context;":
                return vm.resolveClass("android/content/Context").newObject(null);
            case "com/meituan/android/common/mtguard/NBridge->getMtgVN()Ljava/lang/String;":
                return new StringObject(vm, "4.4.7.3");
            case "com/meituan/android/common/mtguard/NBridge->getDfpId()Ljava/lang/String;":
                return new StringObject(vm, "");
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/kuaishou/android/security/internal/common/ExceptionProxy->nativeReport(ILjava/lang/String;)V": {
                return;
            }
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    public void hook(long offset) {
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.base + offset, new BreakPointCallback() {
            int num = 0;

            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                num += 1;
                System.out.println("num次:" + num);

                RegisterContext context = emulator.getContext();
                UnidbgPointer R1 = context.getPointerArg(1);

//                Inspector.inspect(context.getPointerArg(0).getByteArray(0, 0x60), "AES");
//                Inspector.inspect(context.getPointerArg(3).getByteArray(0, 0x60), "0xDB94 [3]");

//                emulator.attach().addBreakPoint(context.getLRPointer().peer, new BreakPointCallback() {
//                    @Override
//                    public boolean onHit(Emulator<?> emulator, long address) {
////                        emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
//                        Inspector.inspect(R1.getByteArray(0, 0x20), "sha256");
//                        return true;
//                    }
//                });


                if (num == 30) {
                    return false;
                }
                return false;
            }
        });

//        long memcpy = module.findSymbolByName("memcpy").getAddress();
//        debugger.addBreakPoint(memcpy, new BreakPointCallback() {
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                RegisterContext context = emulator.getContext();
//                UnidbgPointer dest = context.getPointerArg(0);
//                UnidbgPointer src = context.getPointerArg(1);
//                int size = context.getIntByReg(2);
//                System.out.println("PC: "+context.getPCPointer()+" LR: "+context.getLRPointer()+" dest: "+dest+" src: "+src+" size: "+size);
//                Inspector.inspect(src.getByteArray(0, 0x30), "memcpy input");
//                return true;
//            }
//        });
    }

    public void saveTrace() {
        String dirPath = "unidbg-android/src/test/java/com/ks/";

        String outputPath = dirPath + "memorys.bin";
        new MemoryScan(emulator, outputPath);

//        TraceFunction traceFunction = new TraceFunction(emulator, module, dirPath + "func.txt");
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
    }

    public static int randint(int min,int max){
        Random rand = new Random();
        return rand.nextInt((max-min)+1)+min;
    }

    public void dfa() {
        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.base + 0x26A14, new BreakPointCallback() {
            int num = 0;

            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                num += 1;
                String hexString = "79616e677275687561";
                int length = hexString.length()/2;
                MemoryBlock fakeInputBlock = emulator.getMemory().malloc(length, true);
//                byte[] byteArray = DatatypeConverter.parseHexBinary(hexString);
                byte[] bytes = new BigInteger(hexString, 16).toByteArray();
                // 处理 BigInteger 可能产生的前导 0
                if (bytes[0] == 0) {
                    bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
                }
                fakeInputBlock.getPointer().write(bytes);

                RegisterContext context = emulator.getContext();
                // 修改X1为指向新字符串的新指针
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_X1,fakeInputBlock.getPointer().peer);
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_X2, 0x9);
                emulator.attach().addBreakPoint(context.getLRPointer().peer, new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
                        return true;
                    }
                });
                return true;
            }
        });

        debugger.addBreakPoint(module.base+0x25938, new BreakPointCallback() {
            UnidbgPointer pointer;
            RegisterContext context = emulator.getContext();

            int num = 1;

            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                pointer = context.getPointerArg(0);
                if(num%9==0){
                    pointer.setByte(randint(0,15),(byte) randint(0,0xff));
                }
                num+=1;
                return true;
            }
        });

        debugger.addBreakPoint(module.base + 0x2636C, new BreakPointCallback() {
            UnidbgPointer pointer;
            RegisterContext context = emulator.getContext();

            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                UnidbgPointer R1 = context.getPointerArg(1);
                emulator.attach().addBreakPoint(context.getLRPointer().peer, new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
//                        emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
                        Inspector.inspect(R1.getByteArray(0, 0x10), "AES");
                        return true;
                    }
                });
                return true;
            }
        });
    }

    public String get_NS_sig3() throws FileNotFoundException {
        System.out.println("_NS_sig3 start");
//        saveTrace();
//        myHook();
//        hook(0x45590);
//        hook(0x2636C);
//        hook(0x25938);
//        hook(0x259A0);
        dfa();

//        emulator.traceWrite(0xe4fff5f0L, 0xe4fff5f0L + 24);
//        emulator.traceWrite(0x124e4e40, 0x124e4e40 + 0x30);
//        emulator.traceWrite(0x124d3300, 0x124d3300 + 0x30);
//        emulator.traceWrite(0x124d8580, 0x124d8580 + 0x20);

//        hook(0xdc78); // memcpy src: 0x124d326f

        List<Object> list = new ArrayList<>(10);
        list.add(vm.getJNIEnv()); // 第⼀个参数是env
        DvmObject<?> thiz = vm.resolveClass("com/kuaishou/android/security/internal/dispatch/JNICLibrary").newObject(null);
        list.add(vm.addLocalObject(thiz)); // 第⼆个参数，实例⽅法是jobject，静态⽅法是jclass，直接填0，⼀般⽤不到。
        DvmObject<?> context = vm.resolveClass("com/yxcorp/gifshow/App").newObject(null); // context
        vm.addLocalObject(context);
        list.add(10418); //参数1
        StringObject urlObj = new StringObject(vm, "yangruhua");
        vm.addLocalObject(urlObj);
        ArrayObject arrayObject = new ArrayObject(urlObj);
        StringObject appkey = new StringObject(vm, "d7b7d042-d4f2-4012-be60-d97ff2429c17");
        vm.addLocalObject(appkey);
        DvmInteger intergetobj = DvmInteger.valueOf(vm, -1);
        vm.addLocalObject(intergetobj);
        DvmBoolean boolobj = DvmBoolean.valueOf(vm, false);
        vm.addLocalObject(boolobj);
        StringObject appkey2 = new StringObject(vm, "7e46b28a-8c93-4940-8238-4c60e64e3c81");
        vm.addLocalObject(appkey2);
        list.add(vm.addLocalObject(new ArrayObject(arrayObject, appkey, intergetobj, boolobj, context, null, boolobj, appkey2)));
        Number numbers = module.callFunction(emulator, 0x41680, list.toArray());
        System.out.println("numbers:" + numbers);
        DvmObject<?> object = vm.getObject(numbers.intValue());
        String result = (String) object.getValue();
        System.out.println("result:" + result);
        return result;
    }

    public static void main(String[] args) throws FileNotFoundException {
        ks2 ks = new ks2();
        ks.callByAddress();
        ks.get_NS_sig3();
    }
}