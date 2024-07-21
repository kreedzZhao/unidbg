package ctrip.android.security;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class SecurityUtil extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmClass SecurityUtils;
    private final VM vm;

    private final Module module;
    public SecurityUtil() {
        // 注意這裡只有 32 位
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("com.cloudy.linglingbang")
                .build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/apk/xc 8-38-2.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("scmain", true);
        module = dm.getModule();
        SecurityUtils = vm.resolveClass("ctrip.android.security.SecurityUtil");
        dm.callJNI_OnLoad(emulator);
    }

    public String simpleSign(){
        String arg0 = "ab37d55565c74e8505668768781eb91a";
        String arg1 = "getdata";
        String result = SecurityUtils.newObject(null).callJniMethodObject(
                emulator,
                "simpleSign([BLjava/lang/String;)Ljava/lang/String;",
                arg0.getBytes(StandardCharsets.UTF_8), arg1
        ).getValue().toString();
        return result;
    }

    public static void main(String[] args) {
        // [INFO]-[xc]: SecurityUtil.simpleSign is called: bArr=ab37d55565c74e8505668768781eb91a, str=getdata
        // [INFO]-[xc]: SecurityUtil.simpleSign result=E8355D5441842220D8001DB12EAB0ED56C9E8A6610A0E0DF986FED0E11CCDEEE5E991884D6E
        SecurityUtil securityUtils = new SecurityUtil();
        System.out.println(securityUtils.simpleSign());
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("Access pathname: " + pathname);
        if (("proc/"+emulator.getPid()+"/status").equals(pathname)){
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, ("Name:\tip.android.view\n" +
                    "Umask:\t0077\n" +
                    "State:\tS (sleeping)\n" +
                    "Tgid:\t" + emulator.getPid() + "\n" +
                    "Ngid:\t0\n" +
                    "Pid:\t" + emulator.getPid() + "\n" +
                    "PPid:\t835\n" +
                    "TracerPid:\t0\n" +
                    "Uid:\t10242\t10242\t10242\t10242\n" +
                    "Gid:\t10242\t10242\t10242\t10242\n" +
                    "FDSize:\t512\n" +
                    "Groups:\t3002 3003 9997 20242 50242 \n" +
                    "VmPeak:\t 2997252 kB\n" +
                    "VmSize:\t 2766204 kB\n" +
                    "VmLck:\t       0 kB\n" +
                    "VmPin:\t       0 kB\n" +
                    "VmHWM:\t  850400 kB\n" +
                    "VmRSS:\t  658828 kB\n" +
                    "RssAnon:\t  366848 kB\n" +
                    "RssFile:\t  285072 kB\n" +
                    "RssShmem:\t    6908 kB\n" +
                    "VmData:\t 1717928 kB\n" +
                    "VmStk:\t    8192 kB\n" +
                    "VmExe:\t      20 kB\n" +
                    "VmLib:\t  169484 kB\n" +
                    "VmPTE:\t    3076 kB\n" +
                    "VmPMD:\t      16 kB\n" +
                    "VmSwap:\t    1604 kB\n" +
                    "Threads:\t169\n" +
                    "SigQ:\t1/20889\n" +
                    "SigPnd:\t0000000000000000\n" +
                    "ShdPnd:\t0000000000000000\n" +
                    "SigBlk:\t0000000080001204\n" +
                    "SigIgn:\t0000002000000001\n" +
                    "SigCgt:\t0000000e400096fc\n" +
                    "CapInh:\t0000000000000000\n" +
                    "CapPrm:\t0000000000000000\n" +
                    "CapEff:\t0000000000000000\n" +
                    "CapBnd:\t0000000000000000\n" +
                    "CapAmb:\t0000000000000000\n" +
                    "NoNewPrivs:\t0\n" +
                    "Seccomp:\t2\n" +
                    "Speculation_Store_Bypass:\tunknown\n" +
                    "Cpus_allowed:\t03\n" +
                    "Cpus_allowed_list:\t0-1\n" +
                    "Mems_allowed:\t1\n" +
                    "Mems_allowed_list:\t0\n" +
                    "voluntary_ctxt_switches:\t35273\n" +
                    "nonvoluntary_ctxt_switches:\t15338").getBytes()));
        }
        if (("proc/"+emulator.getPid()+"/cmdline").equals(pathname)){
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "ctrip.android.view".getBytes()));
//            return FileResult.success(new SimpleFileIO(oflags, new File("/home/kreedz/Documents/reverse/long/files/cmdline"), pathname));
        }
        return null;
    }
}
