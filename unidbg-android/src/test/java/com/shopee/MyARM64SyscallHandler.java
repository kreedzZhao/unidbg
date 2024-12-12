package com.shopee;

import com.github.unidbg.Emulator;
import com.github.unidbg.linux.ARM64SyscallHandler;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.spi.SyscallHandler;

public class MyARM64SyscallHandler extends ARM64SyscallHandler {
    public MyARM64SyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    @Override
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        switch (NR) {
            case 0x10a:
                break;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }
}
