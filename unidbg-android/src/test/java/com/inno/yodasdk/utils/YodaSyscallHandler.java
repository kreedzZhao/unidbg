package com.inno.yodasdk.utils;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.context.EditableArm32RegisterContext;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.DumpFileIO;
import com.github.unidbg.memory.SvcMemory;
import com.sun.jna.Pointer;

import java.util.concurrent.ThreadLocalRandom;

public class YodaSyscallHandler extends ARM32SyscallHandler {

    public YodaSyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    @Override
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        switch (NR){
            case 190:
                vfork(emulator);
                return true;
            case 114:
                wait4(emulator);
                return true;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }

    private void vfork(Emulator<?> emulator) {
        EditableArm32RegisterContext context = (EditableArm32RegisterContext) emulator.getContext();
        int childPid = emulator.getPid() + ThreadLocalRandom.current().nextInt(256);
        int r0 = childPid;
        System.out.println("vfork pid=" + r0);
        context.setR0(r0);
    }

    private void wait4(Emulator emulator) {
        EditableArm32RegisterContext context = (EditableArm32RegisterContext) emulator.getContext();
        int pid = context.getR0Int();
        Pointer wstatus = context.getR1Pointer();
        int options = context.getR2Int();
        Pointer rusage = context.getR3Pointer();
        System.out.println("wait4 pid=" + pid + ", wstatus=" + wstatus + ", options=0x" + Integer.toHexString(options) + ", rusage=" + rusage);
    }

    protected int pipe2(Emulator<?> emulator) {
        EditableArm32RegisterContext context = (EditableArm32RegisterContext) emulator.getContext();
        Pointer pipefd = context.getPointerArg(0);
        int flags = context.getIntArg(1);
        int write = getMinFd();
        this.fdMap.put(write, new DumpFileIO(write));
        int read = getMinFd();
        // stdout中写入popen command 应该返回的结果
//        String stdout = "Linux localhost 4.9.186-perf-gd3d6708 #1 SMP PREEMPT Wed Nov 4 01:05:59 CST 2020 aarch64\n";
        // stdout中写入popen command 应该返回的结果
        String command = emulator.get("popen_command");
        String stdout = "";
        switch (command){
            case "uname -a":{
                stdout = "Linux localhost 4.9.186-perf-gd3d6708 #1 SMP PREEMPT Wed Nov 4 01:05:59 CST 2020 aarch64\n";
            }
            break;
            case "cd /system/bin && ls -l":{
                stdout = "total 25152\n" +
                        "-rwxr-xr-x 1 root   shell     128688 2009-01-01 08:00 abb\n" +
                        "lrwxr-xr-x 1 root   shell          6 2009-01-01 08:00 acpi -> toybox\n" +
                        "-rwxr-xr-x 1 root   shell      30240 2009-01-01 08:00 adbd\n" +
                        "-rwxr-xr-x 1 root   shell        207 2009-01-01 08:00 am\n" +
                        "-rwxr-xr-x 1 root   shell     456104 2009-01-01 08:00 apexd\n" +
                        "lrwxr-xr-x 1 root   shell         13 2009-01-01 08:00 app_process -> app_process64\n" +
                        "-rwxr-xr-x 1 root   shell      25212 2009-01-01 08:00 app_process32\n";
            }
            break;
            case "stat /root":{
                stdout = "stat: '/root': No such file or directory\n";
            }
            break;
            default:
                System.out.println("command do not match!");
        }
        this.fdMap.put(read, new ByteArrayFileIO(0, "pipe2_read_side", stdout.getBytes()));
        pipefd.setInt(0, read);
        pipefd.setInt(4, write);
        System.out.println("pipe2 pipefd=" + pipefd + ", flags=0x" + flags + ", read=" + read + ", write=" + write + ", stdout=" + stdout);
        context.setR0(0);
        return 0;
    }
}
