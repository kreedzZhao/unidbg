package com.meituan.android.common.mtguard;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.wrapper.DvmInteger;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NBridge extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NBridge;
    private final VM vm;

    private final Module module;
    public NBridge() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.sankuai.meituan")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/mt.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("mtguard", true);
        module = dm.getModule();
        NBridge = vm.resolveClass("com.meituan.android.common.mtguard.NBridge");
        dm.callJNI_OnLoad(emulator);

        // start trace
        String traceFile = "/home/kreedz/Documents/reverse/long/trace1.log";
        try {
            PrintStream traceStream = new PrintStream(new FileOutputStream(traceFile), true);
            emulator.traceCode(module.base,module.base+module.size).setRedirect(traceStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
        Number number = module.callFunction(emulator, 0x5a38d, list.toArray());
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
        list.add(111);
        DvmObject<?> obj = vm.resolveClass("java/lang/object").newObject(null);
        vm.addLocalObject(obj);
        ArrayObject myobject = new ArrayObject(obj);
        vm.addLocalObject(myobject);
        list.add(vm.addLocalObject(myobject));
        module.callFunction(emulator, 0x5a38d, list.toArray());
    };

    public static void main(String[] args) {
        NBridge nBridge = new NBridge();
        nBridge.main111();
        System.out.println(nBridge.mtMain());
        // 2448abf8d1cc87209245ba66fc375927
        // c21ffc63d2808d075ee18a7fd7b759ca
        // df4684330fedcd133e440c94c54e37ab
    }
}
