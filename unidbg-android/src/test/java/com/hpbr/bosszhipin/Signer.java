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
import java.util.Objects;

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

    public String nativeSignature() {
        String arg1 = "/api/zpCommon/batch/statisticclient_info=%7B%22version%22%3A%2210%22%2C%22os%22%3A%22Android%22%2C%22start_time%22%3A%221719492881182%22%2C%22resume_time%22%3A%221719492881182%22%2C%22channel%22%3A%220%22%2C%22model%22%3A%22google%7C%7CPixel+4%22%2C%22dzt%22%3A0%2C%22loc_per%22%3A0%2C%22uniqid%22%3A%2291f8d6d0-ad34-4e05-b455-6d9698187f60%22%2C%22oaid%22%3A%226a610784-dc28-482d-a73f-436038317926%22%2C%22oaid_honor%22%3A%226a610784-dc28-482d-a73f-436038317926%22%2C%22did%22%3A%22DUTUOBnnJdsQ7lZ0J-3SeFIIRlcP2teBHy6eRFVUVU9Cbm5KZHNRN2xaMEotM1NlRklJUmxjUDJ0ZUJIeTZlc2h1%22%2C%22tinker_id%22%3A%22Prod-arm64-v8a-release-12.100.1210010_0607-19-11-11%22%2C%22is_bg_req%22%3A0%2C%22network%22%3A%22wifi%22%2C%22operator%22%3A%22UNKNOWN%22%2C%22abi%22%3A1%7D&curidentity=0&data=%5B%7B%22action%22%3A%22msg_arrive%22%2C%22time%22%3A1719492887638%2C%22p2%22%3A%2299582182597633%22%2C%22p3%22%3A%22foreground%22%2C%22p4%22%3A%22mqtt%22%2C%22p6%22%3A%22null%22%7D%5D&req_time=1719492887640&uniqid=91f8d6d0-ad34-4e05-b455-6d9698187f60&v=12.100";
        String arg2 = "667deddc8660669adaa0148dbfef059a";
        String expectedRes = "V3.0d5d90e579c106a79ccd6e33c18e28c87";
        byte[] s = (byte[]) NativeApi.callStaticJniMethodObject(emulator,
                "nativeSignature([BLjava/lang/String;)[B",
                arg1.getBytes(StandardCharsets.UTF_8),
                arg2
        ).getValue();
        String realRes = new String(s);
        System.out.println("nativeEncodeRequest res: "+Objects.equals(realRes, expectedRes));
        return realRes;
    }

    public String nativeEncodeRequest(){
        String arg1 = "client_info=%7B%22version%22%3A%2210%22%2C%22os%22%3A%22Android%22%2C%22start_time%22%3A%221719492881182%22%2C%22resume_time%22%3A%221719492881182%22%2C%22channel%22%3A%220%22%2C%22model%22%3A%22google%7C%7CPixel+4%22%2C%22dzt%22%3A0%2C%22loc_per%22%3A0%2C%22uniqid%22%3A%2291f8d6d0-ad34-4e05-b455-6d9698187f60%22%2C%22oaid%22%3A%226a610784-dc28-482d-a73f-436038317926%22%2C%22oaid_honor%22%3A%226a610784-dc28-482d-a73f-436038317926%22%2C%22did%22%3A%22DUTUOBnnJdsQ7lZ0J-3SeFIIRlcP2teBHy6eRFVUVU9Cbm5KZHNRN2xaMEotM1NlRklJUmxjUDJ0ZUJIeTZlc2h1%22%2C%22tinker_id%22%3A%22Prod-arm64-v8a-release-12.100.1210010_0607-19-11-11%22%2C%22is_bg_req%22%3A0%2C%22network%22%3A%22wifi%22%2C%22operator%22%3A%22UNKNOWN%22%2C%22abi%22%3A1%7D&curidentity=0&data=%5B%7B%22action%22%3A%22msg_arrive%22%2C%22time%22%3A1719492887639%2C%22p2%22%3A%2299582182691841%22%2C%22p3%22%3A%22foreground%22%2C%22p4%22%3A%22mqtt%22%2C%22p8%22%3A%22%22%7D%2C%7B%22action%22%3A%22msg_arrive%22%2C%22time%22%3A1719492887642%2C%22p2%22%3A%2299582182851585%22%2C%22p3%22%3A%22foreground%22%2C%22p4%22%3A%22mqtt%22%2C%22p8%22%3A%22%22%7D%2C%7B%22action%22%3A%22msg_arrive%22%2C%22time%22%3A1719492887642%2C%22p2%22%3A%2299627300262912%22%2C%22p3%22%3A%22foreground%22%2C%22p4%22%3A%22mqtt%22%2C%22p8%22%3A%22%22%7D%5D&req_time=1719492887707&uniqid=91f8d6d0-ad34-4e05-b455-6d9698187f60&v=12.100";
        String arg2 = "667deddc8660669adaa0148dbfef059a";
        String expectedRes = "pkooSzZlRfHQKdDf4oHQZF1QlTQWos3fJdU-sPMBgOBWUUQcyxgbUcRpxETcsgs3NSO8akmUwYA7JJLpr7THFFejdIV6-sYjmMpwA3qqUgF-PZxYgjJ_ivBBK8VQQG9w-8EJLv34IPozgsAHUCnlAW1rwXciXSra6aGoBQFhLOjY0OMPsiC2ZCtcpUTnkSwbkXXW2GPdkFzcfX7afYw1n8VjTCRgIuK4dNXzLzrdFA5eiuqMvMtnjblbF2hyRecnBFqc5kVnVBfwf1b5VSGii4HsHkFV0DC9LVLJgBLojSSc6bquviNMMS6I-3DSltlbIsS_usW1a4FRw_Ad_u3Fw0Kk8V_S0Rea0E2_29DWCwhPouK7Rhy3nivS1xM7SLAn60p_xTGWxMaWw3xDEAPSyfqdlnq4JNP6KofA6Q36n1oxEXRj60NG08qcWEii61izAToLwZ-8ZcGjeNm62Rn94bKaVKHuEKLRdDMMxcu_NbH7revPZiVBn_2-dG1A3ZHambrATY1DzoGxbg4d7-BEk1CZHEEbqyXLEzjzbDI_n0yCWWPIfCcaekgQ1A82rTfy_jr0Tz7cbO_K9JrrUzsDo2M8FYPfCXZen6Nnzsd7C6CSKIFbv0yvpQvp-2CyWCV7wsYwX6nI4sWVCbWTI9ka19mKlCpdBBdXt56dTQ4c56lzA0DKT97FGywpJTaFB9WUM5iJ4bHy2Px93dtTO6bbLs8c9lPdbp7EvO0ZBmfAxpEDx9ch3CILAq41rqZID8fBlog18A9OS6H-SLKoh77FaOx8M_FtSSBCHlTYnpijfGqLvLtritbzCGYbyPUMEG0ChHQuFnuzE2kuhbOdZvTeyRV5i7n1bk5H_okbEbvcuxmPYM4hIB_YRGOM9lqaeOHORRYTuq3D9PXudjHcJPQ7l5wSVNcZ7wOH2AXNtdCGt_Z1wdlveD8qZEMkKHZWFgzImdMGIJBOn9ifKQi0hiUsipb95a_IfFUitQmujk4DQShQq2cQ_9S39Q~~";
        String realRes =  NativeApi.callStaticJniMethodObject(emulator,
                "nativeEncodeRequest([BLjava/lang/String;)Ljava/lang/String;",
                arg1.getBytes(StandardCharsets.UTF_8),
                arg2
        ).getValue().toString();
        System.out.println("nativeEncodeRequest res: "+Objects.equals(realRes, expectedRes));
        return realRes;
    }

    public static void main(String[] args) {
        Signer signer = new Signer();
        System.out.println(signer.nativeSignature());
        System.out.println(signer.nativeEncodeRequest());
    }
}
