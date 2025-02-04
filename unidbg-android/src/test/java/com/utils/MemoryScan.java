package com.utils;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.ReadHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.backend.WriteHook;
import com.github.unidbg.arm.context.RegisterContext;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;


public class MemoryScan {
    private final Emulator<?> emulator;
    private final String outputPath;
    private final List<byte[]> cache;
    private final int cacheSize;
    private final RegisterContext registerContext;
    private final int windowSize;

    public MemoryScan(Emulator<?> emulator, String outputPath) {
        this.emulator = emulator;
        this.outputPath = outputPath;
        this.cacheSize = 1024 * 1024; // 1 MB
        this.cache = new ArrayList<>();
        this.registerContext = emulator.getContext();
        this.windowSize = 0x80;
        hookReadAndWrite();
    }

    public MemoryScan(Emulator<?> emulator, String outputPath, int size) {
        this.emulator = emulator;
        this.outputPath = outputPath;
        this.cacheSize = 1024 * 1024; // 1 MB
        this.cache = new ArrayList<>();
        this.registerContext = emulator.getContext();
        this.windowSize = size;
        hookReadAndWrite();
    }


    private void hookReadAndWrite() {
        WriteHook writeHook = new WriteHook() {
            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }

            @Override
            public void hook(Backend backend, long address, int size, long value, Object user) {
                byte[] headerData = getHeaderData(address);
                byte[] readBytes;

                try {
                    readBytes = emulator.getBackend().mem_read(address - (windowSize / 2), windowSize);
                } catch (Exception e) {
                    readBytes = new byte[16];
                }

                byte[] newData = new byte[headerData.length + readBytes.length];
                System.arraycopy(headerData, 0, newData, 0, headerData.length);
                System.arraycopy(readBytes, 0, newData, headerData.length, readBytes.length);

                writeToFile(newData);
            }
        };

        ReadHook readHook = new ReadHook() {
            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }

            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                byte[] headerData = getHeaderData(address);
                byte[] readBytes;

                try {
                    readBytes = emulator.getBackend().mem_read(address - (windowSize / 2), windowSize);
                } catch (Exception e) {
                    readBytes = new byte[16];
                }

                byte[] newData = new byte[headerData.length + readBytes.length];
                System.arraycopy(headerData, 0, newData, 0, headerData.length);
                System.arraycopy(readBytes, 0, newData, headerData.length, readBytes.length);

                writeToFile(newData);
            }
        };

        emulator.getBackend().hook_add_new(writeHook, 1, 0, null);
        emulator.getBackend().hook_add_new(readHook, 1, 0, null);
    }

    private byte[] getHeaderData(long address) {
        StringBuilder pcString = new StringBuilder(registerContext.getPCPointer().toString());
        while (pcString.length() % 16 != 0) {
            pcString.append(' ');
        }
        String addressString = "dataloc:" + Long.toHexString(address);

        ByteBuffer header = ByteBuffer.allocate(16 + pcString.length() + addressString.length());
        header.put(new byte[16]); // 填充16个字节的0
        header.put(pcString.toString().getBytes());
        header.put(addressString.getBytes());

        return header.array();
    }

    private void writeToFile(byte[] readBytes) {
        cache.add(readBytes);

        // 当缓存达到设定的大小时，将数据写入文件
        if (getCacheSize() >= cacheSize) {
            flushCacheToFile();
        }
    }

    private void flushCacheToFile() {
        Path outputPath = Paths.get(this.outputPath);
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            for (byte[] bytes : cache) {
                outputStream.write(bytes);
            }
            cache.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getCacheSize() {
        ToIntFunction<byte[]> lengthFunction = bytes -> bytes.length;

        int sum = 0;
        for (byte[] bytes : cache) {
            sum += lengthFunction.applyAsInt(bytes);
        }
        return sum;
    }

}