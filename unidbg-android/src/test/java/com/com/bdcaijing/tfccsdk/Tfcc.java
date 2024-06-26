package com.com.bdcaijing.tfccsdk;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;

public class Tfcc extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final DalvikModule dm;
    private final DvmObject TfccObj;

    public Tfcc (){
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.ss.android.auto")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/dongchedi_6_5_1.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        new AndroidModule(emulator, vm).register(memory);
        new JniGraphics(emulator, vm).register(memory);

        dm = vm.loadLibrary("cjtfcc", true);

        TfccObj = vm.resolveClass("com.bdcaijing.tfccsdk.Tfcc").newObject(null);
        dm.callJNI_OnLoad(emulator);
    }

    @Override
    public void setIntField(BaseVM vm, DvmObject<?> dvmObject, String signature, int value) {
        switch (signature){
            case "com/bdcaijing/tfccsdk/Tfcc->mErrorCode:I":{
//                return 0;
            }
        }
        super.setIntField(vm, dvmObject, signature, value);
    }

    public String tfccDecrypt(){
        try {
            FileInputStream fis = new FileInputStream("unidbg-android/src/test/resources/apk/dcd.txt");
            String str2 = IOUtils.toString(fis, "UTF-8");
            String s = TfccObj.callJniMethodObject(emulator, "tfccDecrypt(IILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                    17, 1, "14zRM+40n2UGVx0DlI7hqDFjsxGR6eJsnnxUME5ZDT8=",
                    str2
            ).getValue().toString();
            return s;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        Logger.getLogger(DalvikVM.class).setLevel(Level.DEBUG);
        Logger.getLogger(BaseVM.class).setLevel(Level.DEBUG);

        Tfcc tfcc = new Tfcc();
        String res = tfcc.tfccDecrypt();
        System.out.println(res);
    }

}
