package com.tujia.gundam;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Gundam extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass gundam;
    private final VM vm;

    private final Module module;

    public Gundam() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
//                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.tujia.hotel")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/途家民宿.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("tujia_encrypt", true);
        module = dm.getModule();
        gundam = vm.resolveClass("com.tujia.gundam.Gundam");
        dm.callJNI_OnLoad(emulator);

//        Debugger attach = emulator.attach(DebuggerType.CONSOLE);
//        attach.addBreakPoint(module.base + 0x00abf);

//        patchLog();
//        patchLog2();
//        patchLog3();
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/tujia/hotel/TuJiaApplication->getInstance()Lcom/tujia/hotel/TuJiaApplication;":
                return vm.resolveClass("com/tujia/hotel/TuJiaApplication").newObject(null);
            case "java/security/MessageDigest->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;":
                StringObject type = varArg.getObjectArg(0);
                System.out.println(type);
                String name = "";
                if ("\"SHA1\"".equals(type.toString())) {
                    name = "SHA1";
                } else {
                    name = type.toString();
                    System.out.println("else name: " + name);
                }
                try {
                    return vm.resolveClass("java/security/MessageDigest").newObject(MessageDigest.getInstance(name));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
//                return vm.resolveClass("java/security/MessageDigest").newObject(null);
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "com/tujia/hotel/TuJiaApplication->getPackageName()Ljava/lang/String;":
                return new StringObject(vm, "com.tujia.hotel");
            case "com/tujia/hotel/TuJiaApplication->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            case "android/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;":
                return vm.resolveClass("android/content/pm/PackageInfo").newObject(null);
            case "java/security/MessageDigest->digest([B)[B":
                MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
                byte[] input = (byte[]) varArg.getObjectArg(0).getValue();
                byte[] result = messageDigest.digest(input);
                return new ByteArray(vm, result);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    public String encrypt() {
        String str1 = "";
        vm.addLocalObject(new StringObject(vm, str1));
        String str2 = "Mozilla/5.0 (Linux; Android 13; Pixel 4 Build/TP1A.221005.002.B2; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/127.0.6533.103 Mobile Safari/537.36 tujia(hotel/261/261 mNet/wifi loc/en_US)";
        vm.addLocalObject(new StringObject(vm, str2));
        String str3 = "LON=null;LAT=null;CID=-1;LAC=-1;UID=72fb73ed-79da-3733-9cf1-7b8c1fc99fb1;OSVersion=13;AppVersion=261_261;DevType=2;DevModel=Pixel 4;Manufacturer=Google;;TJM=0;VersionName=8.39.0;";
        vm.addLocalObject(new StringObject(vm, str3));
        String str4 = "{\"code\":null,\"parameter\":{\"coordType\":0,\"latitude\":\"null\",\"longitude\":\"null\"},\"client\":{\"abTest\":{},\"abTests\":{},\"adTest\":{\"m1\":\"GOOGLE unknown\",\"m2\":\"3344512559811278d0dd84aa326e70d0\",\"m3\":\"armeabi-v7a\",\"m4\":\"armeabi\",\"m5\":\"100\",\"m6\":\"1\",\"m7\":\"2\"},\"api_level\":261,\"appFP\":\"qA/Ch2zqjORBz90YV34sUT2BZeSprYQM36vm5mqaOoIOjB6ZYKB+NiREzjJiHw7NCNxhI2Okj8O0m7oPKnzVz4+tkyi1M0xyOn+fCq9nutVcmXaGCNQ+ezdbzGln0MiZtRw5HTom6AsS6B5e9AAcccx+vvemnHcUjjlzUntTlOmBa/uC1HGCqoeAmdyBGBcKNgaJykmZ0UczkTZpV2hDjwWPLiiHVMP9FI+3HNwzFXOEZc8F4CjJ9S1KRTtEPJ2K/hGoywuOEnEOauBaa5UU7mrsOlXgAoLBWq3H1+cEziz/T86A6uW0oC+mJHwOLnP8HKN0q2Fu3rTcKZ+Prbs/dcBHaWJi1C1tHZFza2O+1gUQTgvg+Kq57BvE6IjEhveT\",\"appId\":\"com.tujia.hotel\",\"appVersion\":\"261_261\",\"appVersionUpdate\":\"rtag-20210826-105800-yinfengg_1\",\"batteryStatus\":\"charging\",\"buildTag\":\"rtag-20210826-105800-yinfengg_1\",\"buildVersion\":\"8.39.0\",\"ccid\":\"51742142491264577656\",\"channelCode\":\"qq\",\"crnVersion\":\"267\",\"devModel\":\"Pixel 4\",\"devToken\":\"\",\"devType\":2,\"dtt\":\"\",\"electricity\":\"43\",\"flutterPkgId\":\"292\",\"gps\":null,\"kaTest\":{\"k1\":\"1_1_2\",\"k2\":\"flame\",\"k3\":\"abfarm-release-rbe-64-2004-0084\",\"k4\":\"TP1A.221005.002.B2\",\"k5\":\"google/flame/flame:13/TP1A.221005.002.B2/9382335:user/release-keys\",\"k6\":\"flame\",\"k7\":\"TP1A.221005.002.B2\"},\"latitude\":null,\"locale\":\"en-US\",\"longitude\":null,\"networkType\":\"1\",\"osVersion\":\"13\",\"platform\":\"1\",\"salt\":\"YM1A2GgAYQTAwxZANMzxGjQEZYDzkxZNYA10TAMIkATzxAYMMA03DAMMgAWTxAOOYA2w2DUEZQGTjzZMMYh3zThYhUjjlzZMNNh1jWJgEFDT5kOOMYlzDzNIRgGziyNNNZ5xmTYY\",\"screenInfo\":\"\",\"sessionId\":\"72fb73ed-79da-3733-9cf1-7b8c1fc99fb1_1723985269671\",\"tId\":\"24081817401421413083\",\"tbTest\":{\"j1\":\"unknown\",\"j2\":\"flame\",\"j3\":\"Pixel 4\",\"j4\":\"Google\",\"j5\":\"google\",\"j6\":\"c2f2-0.5-8906123\",\"j7\":\"flame\",\"j8\":\"2.1.0  (ART)\"},\"traceid\":\"1723985279581_1723985275497_1723985274214\",\"uID\":\"72fb73ed-79da-3733-9cf1-7b8c1fc99fb1\",\"version\":\"261\",\"wifi\":null,\"wifimac\":\"lzG8kvOS04Nkdiv9W55SwfyKNm+dT0iI83v6ZqLYIwI=\"},\"psid\":\"12e4dc36-243a-49a7-94d6-f85981433880\",\"type\":null,\"user\":null,\"usid\":null}";
        vm.addLocalObject(new StringObject(vm, str4));
        int i = 2010;
        long l = 1723985276L;
        String s = gundam.callStaticJniMethodObject(
                emulator,
                "encrypt(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IJ)Ljava/lang/String;"
                , str1, str2, str3, str4, i, l
        ).getValue().toString();
        return s;
    }

    public static void main(String[] args) {
        Gundam g = new Gundam();
        // fd5a533cc5b42e30ff3cf65998cb851fd9384e60
        String res = g.encrypt();
        System.out.println("res -> " + res);
    }

}
