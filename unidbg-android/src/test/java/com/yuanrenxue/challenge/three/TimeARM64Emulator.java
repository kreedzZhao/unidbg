package com.yuanrenxue.challenge.three;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.BackendFactory;
import com.github.unidbg.arm.backend.HypervisorFactory;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidARM64Emulator;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class TimeARM64Emulator extends AndroidARM64Emulator {
    public TimeARM64Emulator(File executable) {
        super(executable.getName(),
                new File("target/rootfs"),
                Collections.<BackendFactory>singleton(new Unicorn2Factory(true)));
    }

    @Override
    protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
        return new TimeSyscallHandler(svcMemory);
    }
}
