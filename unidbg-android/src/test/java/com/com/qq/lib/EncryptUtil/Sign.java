package com.com.qq.lib.EncryptUtil;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

public class Sign extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;

    private final DalvikModule dm;
    private final DvmClass EncryptUtil;

    public Sign(){
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("vip.prrga.vxqlly")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/vip.prrga.vxqlly.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        dm = vm.loadLibrary("sojm", true);
        // 使用 libandroid 模块
        new AndroidModule(emulator, vm).register(memory);

        EncryptUtil = vm.resolveClass("com.qq.lib.EncryptUtil");
        dm.callJNI_OnLoad(emulator);
    }

    public String encrypt(){
        String str1 = "{\"system_build_id\":\"a1000\",\"system_iid\":\"b5e62950fd4e8f9741869010cc250ce5\",\"system_app_type\":\"local\",\"new_player\":\"fx\",\"system_oauth_id\":\"815d51faff95af4ea229cb721f3874f8\",\"size\":\"50\",\"app_status\":\"1D51DEF1A193F45DA099399AA5720BD43D9C3623:2\",\"system_version\":\"5.6.1\",\"system_build_aff\":\"\",\"bundle_id\":\"vip.prrga.vxqlly\",\"page\":\"1\",\"keyword\":\"警花张津瑜\",\"system_oauth_type\":\"android\",\"system_token\":\"72B2629B04B6CF0B058BD9648524E1822D53EA1C47BAAE5E6B780EE380F4B592E069D1C1CBA609849ED1CC02F4C50B925D8FDFF90F65B037504C7EC9315ED8E30659D83EF3AFE6B40841AA5D3FF42E2BFA0218AA40D4F56F0B3E7B759520DBD0EB018FF229\"}";
        String str2 = "BwcnBzRjN2U/MmZhYjRmND4xPjI+NWQwZWU0YmI2MWQ3YjAzKw8cEywsIS4BIg==";
        return EncryptUtil.callStaticJniMethodObject(emulator, "encrypt(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                str1, str2).getValue().toString();
    }

    public static void main(String[] args) {
        Logger.getLogger(DalvikVM.class).setLevel(Level.DEBUG);
        Logger.getLogger(BaseVM.class).setLevel(Level.DEBUG);

        Sign sign = new Sign();
        String encrypt = sign.encrypt();
        System.out.println(encrypt);
    }
}
