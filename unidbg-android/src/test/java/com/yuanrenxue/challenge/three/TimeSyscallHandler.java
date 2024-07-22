package com.yuanrenxue.challenge.three;

import com.github.unidbg.Emulator;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.ARM64SyscallHandler;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.struct.TimeVal32;
import com.github.unidbg.unix.struct.TimeZone;
import com.sun.jna.Pointer;
import unicorn.ArmConst;

import java.util.Calendar;

public class TimeSyscallHandler extends ARM64SyscallHandler {
    public TimeSyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    @Override
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        switch (NR) {
            case 78:
                // gettimeofday
                mygettimeofday(emulator);
                return true;
            case 263:
                // clock_gettime
                myclock_gettime(emulator);
                return true;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }


    private void mygettimeofday(Emulator<?> emulator) {
        Pointer tv = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
        Pointer tz = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
        emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, mygettimeofday(tv, tz));
    };

    private int mygettimeofday(Pointer tv, Pointer tz) {
        long currentTimeMillis = System.currentTimeMillis();

        long tv_sec = currentTimeMillis / 1000;
        long tv_usec = (currentTimeMillis % 1000) * 1000;

        TimeVal32 timeVal = new TimeVal32(tv);
        timeVal.tv_sec = (int) tv_sec;
        timeVal.tv_usec = (int) tv_usec;
        timeVal.pack();

        if (tz != null) {
            Calendar calendar = Calendar.getInstance();
            int tz_minuteswest = -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
            TimeZone timeZone = new TimeZone(tz);
            timeZone.tz_minuteswest = tz_minuteswest;
            timeZone.tz_dsttime = 0;
            timeZone.pack();
        }
        return 0;
    }

    private static final int CLOCK_REALTIME = 0;
    private static final int CLOCK_MONOTONIC = 1;
    private static final int CLOCK_THREAD_CPUTIME_ID = 3;
    private static final int CLOCK_MONOTONIC_RAW = 4;
    private static final int CLOCK_MONOTONIC_COARSE = 6;
    private static final int CLOCK_BOOTTIME = 7;
    private final long nanoTime = System.nanoTime();

    private int myclock_gettime(Emulator<?> emulator) {
        int clk_id = emulator.getBackend().reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer tp = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
        long offset = clk_id == CLOCK_REALTIME ? System.currentTimeMillis() * 1000000L : System.nanoTime() - nanoTime;
        long tv_sec = offset / 1000000000L;
        long tv_nsec = offset % 1000000000L;

        switch (clk_id) {
            case CLOCK_REALTIME:
            case CLOCK_MONOTONIC:
            case CLOCK_MONOTONIC_RAW:
            case CLOCK_MONOTONIC_COARSE:
            case CLOCK_BOOTTIME:
                tp.setInt(0, (int) tv_sec);
                tp.setInt(4, (int) tv_nsec);
                return 0;
            case CLOCK_THREAD_CPUTIME_ID:
                tp.setInt(0, 0);
                tp.setInt(4, 1);
                return 0;
        }
        throw new UnsupportedOperationException("clk_id=" + clk_id);
    }
}