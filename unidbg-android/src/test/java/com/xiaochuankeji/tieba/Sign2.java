package com.xiaochuankeji.tieba;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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

    public void e(){
        saveTrace();


        ArrayList<Object> objects = new ArrayList<>(3);
        objects.add(vm.getJNIEnv());
        objects.add(0);
        String input = "{\"filter\":\"all\",\"auto\":0,\"tab\":\"推荐\",\"refresh_count\":2,\"direction\":\"down\",\"c_types\":[1,2,11,15,16,17,52,53,40,50,41,70,22,25,27,88],\"sdk_ver\":{\"tt\":\"6.3.2.3\",\"tx\":\"4.600.1470\",\"mimo\":\"5.3.2\"},\"ad_wakeup\":1,\"h_ua\":\"Mozilla\\/5.0 (Linux; Android 14; Pixel 7 Build\\/AP2A.240905.003; wv) AppleWebKit\\/537.36 (KHTML, like Gecko) Version\\/4.0 Chrome\\/122.0.6225.0 Mobile Safari\\/537.36\",\"manufacturer\":\"Google\",\"h_av\":\"6.3.2\",\"h_dt\":0,\"h_os\":34,\"h_app\":\"zuiyou\",\"h_model\":\"Pixel 7\",\"h_did\":\"9747783039ae0a61\",\"h_nt\":1,\"h_ch\":\"vivo\",\"h_ts\":1735634510528,\"android_id\":\"9747783039ae0a61\",\"h_ids\":{},\"h_m\":308393297,\"token\":\"T8K4NdULsOQd4bfrbfQ0odQas80VeXL-PmyNdSlGGFhJZRF2WjX5KzvKDhAg896laIVmC0EmIBJ-_BCSkiHertB1S0g==\"}";
        objects.add(vm.addLocalObject(new ByteArray(vm, input.getBytes())));
        Number number = module.callFunction(emulator, 0x7667c, objects.toArray());
        byte[] value = (byte[]) vm.getObject(number.intValue()).getValue();
        System.out.println("hex: " + bytesToHex(value));
        // e4083916502105ede040f93ddde0d51d127eab2864cc15aaec89607fe5eab0696dc2f8db682581be451cac25c16c58af189ca9009d39567361c977b8c600b8a698756d2f9c7fcad4efc35d45270280d3f218b55aa36de6b6bd483951e27a6284fff118654b01cc929b5673f4f5832c1586ad2792d0bbfea48b2dd59e01ba8fa2ca5b9aefb11d7682439300edaa4e7390e6ee246cf1fad635bd521a72fb553ae6ec265aeaee06ba1e5d45424191756c8b44f1e0c2f74e68355c903209756f8a60668e640e497cfd370691ebce0260219ebb584fefb3c3b53e15b6d3b5617324fa215d7b9afcbf0585c01b12415aa3ebf2c181bcbf20d6988df832d35e1ac3bdbbf89c6480259db50fb1b51d7a96799294abb000cd8c2645851ee04f4f687a8b83598f0028815617534639555c97ddf779851ed10d1f6aaeb97c5bf96917bf55c0b348eb431d56fd3026c4b647acc7688c26715a1e18c1bcad461859307c51d84118f509336a2d050cb6abcae499e64519d7c36e4b328e23cbaed53c9c89d269d1939742587aad41b4855ae897c007a7d4a088dd669c05d05ae408d207ee4442804c5fe4468404aa25d49cdb5d31363ef571e6f02a4ecb24d05e0a7f318162119f6d678df35c809844dc02c97bfb88b4b9cfd4c9334c621f5a0f68a4fd303792a1f92653ed320df0f4afd781846633ed5c135e25f129de137c510474793898fff14622f2ad737d3fa7979aec731e2ac9d143b83da7dc301b4d44887fe783f5c21712567b86e10855aad1ed5101d45ae37e34ed6bbc9f62604909363a1e57423bc2fd28effafe7e035e3be807b9402cab96fb5613f181f88921ace8e9e1f646392d94bc735557649ff8492c7202ed01e13370b8385a1bddfd3f705a300e997a0d21cbc6ddaaf95796ef2d175266a6a50868f04b1af3f58c776eca0738120eb93dbc395ffdde52bd1b391a9bbf8308219a7d2044860e0fe2fbcbd709546fd0463cc54075ba3974eab60b85bc1dea84a154a1931d064b52e96e6b07039d16b567b9bc
        // 3708373737373737373737373737373750CF8A728AE29136B016E51A417B03211CE49D99407E90CB69A5EEED80EDBC32CACCD197A42FC41C638369E6CE48F38C9517376FBDF8A56ACBCAE0513BFBAE32394EEC44FBD1EF04503EF0B34CC7E819E3D22EC1394DE9F0A9341B51CCF56B50F6CC53335BE79A58E8181BA92381BD32D9B2E6EC3EB521714A1D2490242C2D9F82A5445CDB32C0351C1758A976C9342A532EC0E03F200AAFDE1A2CDF74FAA8B3714ED7602A4A936A6A8A11B95B3F959F2F80212616A7E9A0D00E2417D05B60085DC3848B33EF35DCDA4668A526D885B80C8BD5E479670231132BF9C30E546ABE00E0DB772727EE7C6E3E33B4A6F9519AC6B7C188D68C56AEB589D0C10226B7675D08B98DBBA9673873167E3B4C1BEA900706E625649DDF7F0F3B5CD7554B8E9B9E5EE2EEB2F6A71965235A867D5F3F370BB1C76CE8A82DEEE29B38D08A580EB89FB26D2DCD51E90856D7B9E30549BE8232D9A8BEB287D63F5DC83B5384F689305A0E19A68C3BE78600006C6A07EB2D5843A0F8401E3B4AFF1ED7E135C2AA799914FF818A2A3865D3C57E513A3F03F4F9215CDB10A70D4575A8D2100DDF17F5A9A658A6B3D2C95D1951F6D7CC6BF1B090E1A957806D8B7DA239ED43822B7D4BDC124B2211D8810C2CBEA73B1E21CD197A1CE802B8E49801025E470F469C8BDB325CC94B212C70815179569F3D12AA8ECADD8D1BDE041F63DECEDEC5D55B02AB419B08546F75D4013D4BAD3C37ADFD13AB90D1F54F87884FC9030A5B682B702C69F372716E9C2BA3C38EB93AF599267D2B58EFAA62DF91FD26F0E862B762FA0D12EA55745369E3A0B36A9AD9BC17BF21DCCAAF2EDB09BF5BF0BE53CFBD3D88BDDFA317A1AACB15864349FD90C85BCE209DA2F1430412960856AC22A1C56A767A8D62F8EF691D07FCDCA46C2493F289201CB6F21B84F734012A7813CF6D8BBADFEE3112283537D734619D946FDA5B9AEBE74E01379C9C012CB882E7F05FB4687F48CB9F5732C9BE3876DA97E853AEF3BCEB
    }

    public static void main(String[] args) {
        Sign2 sign2 = new Sign2();
        sign2.native_init();
        sign2.e();
    }
}
