package com.bytedance.frameworks.core.encrypt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.android.dvm.wrapper.DvmBoolean;
import com.github.unidbg.linux.android.dvm.wrapper.DvmLong;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import unicorn.Arm64Const;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DyRh extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private static long finalLong;  // 最终调用的入参3

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("open file:" + pathname);
        return null;
    }

    DyRh(){
        // 创建模拟器实例
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.ss.android.ugc.aweme").build();
        emulator.getSyscallHandler().addIOResolver(this);
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apks/douyin/douyin_31_9.apk"));
        // 设置JNI
        vm.setJni(this);
        // 打印日志
        vm.setVerbose(true);
        new AndroidModule(emulator, vm).register(memory);
        vm.resolveClass("com/bytedance/mobsec/metasec/ml/MS",vm.resolveClass("ms/bd/c/g0",vm.resolveClass("ms/bd/c/m")));
        // 加载目标SO
        DalvikModule dm = vm.loadLibrary("metasec_ml", true);
        //获取本SO模块的句柄,后续需要用它
        module = dm.getModule();
        // 调用JNI OnLoad
        dm.callJNI_OnLoad(emulator);
    };
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
    public void callInit() throws InterruptedException {
//        callX(16777217,0,0L,"d8ddc3", new byte[]{59, 55, 4});
//        callX(16777217,0,0L,"274640", new byte[]{109, 56, 67});
//        callX(16777217,0,0L,"3e7259", new byte[]{33, 104, 73, 8, 8, 55, 36, 65, 98, 99, 44, 100, 65, 8, 30, 58, 62, 65, 114, 44, 22, 83, 106, 67, 30, 7, 62, 77, 114});
//        callX(16777217,0,0L,"3e7259", new byte[]{33, 104, 73, 8, 8, 55, 36, 65, 98, 99, 44, 100, 65, 8, 30, 58, 62, 65, 114, 44, 22, 83, 106, 67, 30, 7, 62, 77, 114});
//        callX(16777217,0,0L,"3e7259", new byte[]{33, 104, 73, 8, 8, 55, 36, 65, 98, 99, 44, 100, 65, 8, 30, 58, 62, 65, 114, 44, 22, 83, 106, 67, 30, 7, 62, 77, 114});
//        callX(16777217,0,0L,"81940c", hexStringToByteArray("2a3c470e0d6d2f156c6527304f0e09663a1d7f6b3b38044300663e5e7b60223e454e0660340226570d18674f017d2f1f7a513d3a4653"));
        callX(16777219,0,0L,"", vm.resolveClass("com/ss/android/ugc/aweme/app/host/AwemeHostApplication").newObject(null));
//        Thread.sleep(2000); // 执行上面的后出现 SDK not init, crashing...
        System.out.println("==================2");


        // DoLazyInit
        Number number2 = callX(67108865,0,0L,"[\"1128\",\"\",\"\",\"bo95dJizD1WFcV03zOuLzN5Pn1sFtVa3szqiVQmflMJTNW0p0Kpqfw8D4i0zUlfrou4kuYt\\/i0521YRygM83dwv\\/wn3DD+TMJF+QFzW9wb8Qq2\\/1B4jPMbObrDNdyMMukpAYqy1fLWtbLGVIPxsFsZegwQy5lsRX9h49PH\\/Qx8MwgYvWvH7ZTFLV28LwTWZiljQyBPaBE+TsyumEu0Y+JRkeidHFEYcVs0yRoa+xC004hugQhdPupIt6dBiWA4phsB3fNJZjFTAKGE1lPB4gzt6Qf+FmlgZBbRvT8zekxTV2HZ5dUvSutB2\\/0QpbHKAvWL4DRA==\",\"v04.07.00\",\"\",\"\",\"\",\"\",\"\",\"0\",\"-1\",\"810\",\"\",\"0\",[],[\"tk_key\",\"douyin\"]]", null);
        String res2 = vm.getObject(number2.intValue()).toString();
        System.out.println("================"+res2);
    }
    public void callFun(){
        String arg1 = "https://api5-normal-lq.amemv.com/aweme/v2/comment/list/?aweme_id=7431546314183331111&cursor=0&count=13&insert_ids&address_book_access=2&gps_access=2&forward_page_type=1&channel_id=0&city=320500&hotsoon_filtered_count=0&hotsoon_has_more=0&follower_count=0&is_familiar=0&page_source=0&is_fold_list=false&user_avatar_shrink=96_96&aweme_author=MS4wLjABAAAAN-GJk044PEW1TUph29y3AF1wkGwrQW8OVP9EhTT3vIw&item_type=0&comment_aggregation=0&top_query_word=%E4%B8%89%E4%BA%9A%E6%8E%89%E5%85%A5%E4%B8%8B%E6%B0%B4%E9%81%93%E7%94%B7%E5%AD%A9%E5%B0%B8%E4%BD%93%E6%89%BE%E5%88%B0&is_preload=0&authentication_token=MS4wLjAAAAAAhIVOT1hE4Pl7hiJBDy88e-Jc8NnysiNFu6Ql-JpnX9zNvZS-03hmVGKeNZsnclFmMNYlsGnJtfs5IFlut5NM-GDR-z37mogvxYd22If6DR2yo59at1pNUCWD02cHi81NwnqHTk7thn6LDPiGSscc_cOA6lPcSVdjODAQnTWCFysoUhjrT28yGyuoy6nsiiwQUeMgX7Sy7mDfHgMB8qOxLMTQPa3MUYgs40eEkT00vuMEyweGPijBukPm45ARQ72Q&use_url_optimize=0&current_l1_comment_count=0&service_id=0&group_id=0&comment_scene=0&hotspot_id&comment_count=435&medium_shrink=304_407&use_light_optimize=0&friend_interaction=0&klink_egdi=AAIjIhKgRfgbXTHgKchV4e3ePxtHe63HPIZwNTKOXtOCnXWIKBce8HI3&iid=1141714941142316&device_id=2016947781502964&ac=wifi&channel=douyin-ls-sm-and-02&aid=1128&app_name=aweme&version_code=310900&version_name=31.9.0&device_platform=android&os=android&ssmix=a&device_type=Pixel+4+XL&device_brand=google&language=zh&os_api=29&os_version=10&manifest_version_code=310901&resolution=1440*2984&dpi=560&update_version_code=31909900&_rticket=1730389123900&first_launch_timestamp=1730383001&last_deeplink_update_version_code=0&cpu_support64=true&host_abi=arm64-v8a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&is_preinstall=0&need_personal_recommend=1&is_android_pad=0&is_android_fold=0&ts=1730389113&cdid=f445306e-20bd-411d-8266-a6d2539c7d1b";
        String headerStr = "{\"x-ss-stub\": \"910a39f0ce3f4d580bf7278262fbe163\"}"; // 10 910a39f0ce3f4d580bf7278262fbe163
        Number number = callX(50331649, 0,finalLong,arg1,getArr(headerStr));
        String res = vm.getObject(number.intValue()).toString();
        JSONArray jsonArray = JSON.parseArray(res);
        Map<String, String> keyValueMap = new HashMap<>();
        for (int i = 0; i < jsonArray.size(); i += 2) {
            String key = jsonArray.getString(i);
            String value = jsonArray.getString(i + 1);
            keyValueMap.put(key, value);
        }
        for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());

        }
    }

    public static void main(String[] args) throws InterruptedException {
        DyRh demo = new DyRh();
        demo.callInit();
        demo.callFun();
    }













    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
    public ArrayObject getArr(String headerStr){
        JSONObject jsonOb = JSON.parseObject(headerStr);
        List<String> resultList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : jsonOb.entrySet()) {
            resultList.add(entry.getKey());
            resultList.add(entry.getValue().toString());
        }
        // 将List转换为字符串数组
        String[] resultArray = resultList.toArray(new String[0]);
        ArrayObject arr = ArrayObject.newStringArray(vm, resultArray);
        return arr;
    }
    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature){
            case "com/bytedance/mobsec/metasec/ml/MS->b(IIJLjava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;":{
                // hook ms.bd.c.l.b函数
                int i0 = vaList.getIntArg(0);
                System.out.println("i0======:" + i0);
                switch (i0){
                    case 65539:{
                        return new StringObject(vm, "/data/user/0/com.ss.android.ugc.aweme/files/.msdata");
                    }
                    case 268435470:{
                        return DvmLong.valueOf(vm,0);
                    }
                    case 16777250:{
                        String args5 = (String) vaList.getObjectArg(4).getValue();
                        // frida hook的有多个,根据unidbg中获取的选择
                        System.out.println("args5===:" + args5);
                        switch (args5)  {
                            case "1128-0-167774bf518c11948aa0784351ccf5a9":{
                                // 暂时
                                return null;
                            }
                            case "1128-0-sdi":{
                                // 暂时
                                return new StringObject(vm, "e751102f284fcab81a9c0730681cff833364b0708389d1413406e63606ba656d7b1b28be43810a9f");
                            }
                        }
                    }
                    case 33554433:{
                        return DvmBoolean.valueOf(vm, true);
                    }
                    case 33554434:{
                        // frida hook可以发现
                        finalLong = vaList.getLongArg(2);
                        System.out.println("finalLong==:" + finalLong);
                        return DvmBoolean.valueOf(vm, true);
                    }
                    case 16777233:{
                        return new StringObject(vm, "31.9.0");
                    }
                }
            }
            case "java/lang/Boolean->valueOf(Z)Ljava/lang/Boolean;":{
                int value = vaList.getIntArg(0);
                if (value!=0){
                    return DvmBoolean.valueOf(vm, true);
                }
                else {
                    return DvmBoolean.valueOf(vm, false);
                }
            }
            case "java/lang/Thread->currentThread()Ljava/lang/Thread;":{
                return vm.resolveClass("java/lang/Thread").newObject(Thread.currentThread());
            }
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }
    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/Thread->getStackTrace()[Ljava/lang/StackTraceElement;":{
                StackTraceElement[] elements = ((Thread) dvmObject.getValue()).getStackTrace();
                DvmObject<?>[] objs = new DvmObject<?>[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    objs[i] = vm.resolveClass("java/lang/StackTraceElement").newObject(elements[i]);
                }
                return new ArrayObject(objs);
            }
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;":{
                StackTraceElement stackTraceElement = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, stackTraceElement.getClassName());
            }
            case "java/lang/StackTraceElement->getMethodName()Ljava/lang/String;":{
                StackTraceElement stackTraceElement = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, stackTraceElement.getMethodName());
            }
            case "java/lang/Boolean->getBytes(Ljava/lang/String;)[B":{

            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }
    @Override
    public long callLongMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature){
            case "java/lang/Long->longValue()J":{
                Long longObject = (Long) dvmObject.getValue();
                return longObject.longValue();
            }
        }
        return super.callLongMethodV(vm, dvmObject, signature, vaList);
    }
}
