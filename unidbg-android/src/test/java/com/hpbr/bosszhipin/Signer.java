package com.hpbr.bosszhipin;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.sina.oasis.SignUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Signer extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NativeApi;
    private final VM vm;

    public Signer() {
        // https://www.wandoujia.com/apps/6202222/history_v1124010
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.hpbr.bosszhipin")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/boss.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("yzwg", true);
        NativeApi = vm.resolveClass("com.twl.signer.YZWG");
        dm.callJNI_OnLoad(emulator);
    }

    public byte[] nativeSignature() {
        String arg1 = "/api/zpCommon/batch/statisticclient_info=%7B%22version%22%3A%2210%22%2C%22os%22%3A%22Android%22%2C%22start_time%22%3A%221719408872489%22%2C%22resume_time%22%3A%221719408872489%22%2C%22channel%22%3A%220%22%2C%22model%22%3A%22google%7C%7CPixel+4%22%2C%22dzt%22%3A0%2C%22loc_per%22%3A0%2C%22uniqid%22%3A%2291f8d6d0-ad34-4e05-b455-6d9698187f60%22%2C%22oaid%22%3A%226a610784-dc28-482d-a73f-436038317926%22%2C%22oaid_honor%22%3A%226a610784-dc28-482d-a73f-436038317926%22%2C%22did%22%3A%22DUTUOBnnJdsQ7lZ0J-3SeFIIRlcP2teBHy6eRFVUVU9Cbm5KZHNRN2xaMEotM1NlRklJUmxjUDJ0ZUJIeTZlc2h1%22%2C%22tinker_id%22%3A%22Prod-arm64-v8a-release-12.100.1210010_0607-19-11-11%22%2C%22is_bg_req%22%3A0%2C%22network%22%3A%22wifi%22%2C%22operator%22%3A%22UNKNOWN%22%2C%22abi%22%3A1%7D&curidentity=0&data=%5B%7B%22action%22%3A%22app-page-dwelltime%22%2C%22time%22%3A1719409615922%2C%22p%22%3A%221%22%2C%22p2%22%3A%2230175%22%7D%5D&req_time=1719409615925&uniqid=91f8d6d0-ad34-4e05-b455-6d9698187f60&v=12.100";
        String arg2 = "667deddc8660669adaa0148dbfef059a";
        byte[] s = (byte[]) NativeApi.callStaticJniMethodObject(emulator,
                "nativeSignature([BLjava/lang/String;)[B",
                arg1.getBytes(StandardCharsets.UTF_8),
                arg2
        ).getValue();
        return s;
    }

    public static void main(String[] args) {
        Signer signer = new Signer();
        byte[] res = signer.nativeSignature();
        System.out.println(new String(res));
    }
}
