package com.utils;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.ModuleListener;
import com.github.unidbg.arm.backend.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SearchData implements ModuleListener{
    public Module targetModule;
    public Emulator emulator;
    public byte[] data;
    public String moduleName;
    public int dataLength;
    public int intervals;
    public long blockcount = 0;
    public long startcount = 0;
    public List<long[]> activePlace = new ArrayList<>();;
    public SortedSet heapAddress = new TreeSet();
    public ArrayList<byte[]> memoryRange = new ArrayList<>();


    @Override
    public void onLoaded(Emulator<?> emulator, Module module) {
        if(module.name.equals(moduleName)){
            this.emulator = emulator;
            targetModule = module;
            hookReadAndWrite();
            hookBlock();
        }
    }


    public SearchData(String input, String moduleName, int intervals, long startcount){
        data = hexStringToByteArray(input);
        dataLength = data.length;
        this.moduleName = moduleName;
        this.intervals = intervals;
        this.startcount = startcount;
    };

    public SearchData(String input, String moduleName, int intervals){
        data = hexStringToByteArray(input);
        dataLength = data.length;
        this.moduleName = moduleName;
        this.intervals = intervals;
        this.startcount = 0;
    };


    public void setMemoryRange(int minSize) {
        memoryRange = new ArrayList<>();;
        // 整个栈空间
        long stackend = 0xC0000000L;
        long stackSize = 256L * emulator.getPageAlign();
        memoryRange.add(emulator.getBackend().mem_read(stackend - stackSize, stackSize));

        // 搜索除栈外活跃的内存块
        summaryRanges();
        for(long[] one: activePlace){
            try
            {
                long start = one[0];
                long size = one[1] - one[0] + 1;
                if(size > minSize){
                    byte[] memory = emulator.getBackend().mem_read(start, size);
                    memoryRange.add(memory);
                }

            }catch(Exception e)
            {

            }

        }
    }

    public void hookReadAndWrite(){
        emulator.getBackend().hook_add_new(new WriteHook() {
            @Override
            public void hook(Backend backend, long address, int size, long value, Object user) {
                for(int i=0;i<size;i++){
                    heapAddress.add(address+i);
                }
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        }, emulator.getMemory().MMAP_BASE, 0x7fffffff, null);


        emulator.getBackend().hook_add_new(new ReadHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                for(int i=0;i<size;i++){
                    heapAddress.add(address+i);
                }
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        }, emulator.getMemory().MMAP_BASE, 0x7fffffff, null);
    }

    public void summaryRanges(){
        ArrayList<Long> nums = new ArrayList<Long>(heapAddress);
        activePlace = new ArrayList<>();
        long start=nums.get(0);
        long end;
        long temp;
        int i=0;


        while(i<nums.size()){
            temp=nums.get(i);
            // 如果下一个触底
            if(i+1 == nums.size()){
                end = nums.get(i);
                activePlace.add(new long[]{start, end});
                //System.out.println("start:0x"+Long.toHexString(start)+"  end:0x"+Long.toHexString(end));
                break;
            }

            // 如果不连续
            if((temp+1) != nums.get(i+1)){
                end = temp;
                activePlace.add(new long[]{start, end});
                //System.out.println("start:0x"+Long.toHexString(start)+"  end:0x"+Long.toHexString(end));
                start = nums.get(i+1);
            }
            i++;
        }
    }

    public void hookBlock(){
        emulator.getBackend().hook_add_new(new BlockHook() {
            @Override
            public void hookBlock(Backend backend, long address, int size, Object user) {
                blockcount += 1;
                if(startcount==0){
                    if(blockcount%intervals == 0){
                        setMemoryRange(dataLength);
                        startSearch();
                    }
                }else {
                    if(blockcount >= startcount){
                        if(blockcount%intervals == 0){
                            setMemoryRange(dataLength);
                            startSearch();
                        }
                    }
                }

            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        }, targetModule.base, targetModule.size+ targetModule.base, null);

    }


    private void startSearch() {
        // search data
        for (int i = 0; i < memoryRange.size(); i++) {
            byte[] block = memoryRange.get(i);
            boolean find = AcontainB(block, data);
            if(find){
                System.out.println("find target at 0x"+Long.toHexString(blockcount-intervals)+" block");
                emulator.attach().debug();
                break;
            }
        }

    }


    //判断某个数组中是否包含另一个数组，数组为有序数组
    public boolean AcontainB(byte[] a, byte[] b){
        int p1 = 0;
        int p2 = 0;
        while (p1 < a.length){
            if(a[p1] == b[p2]){
                p2++;
                if(p2 == b.length){
                    return true;
                }
            }else {
                p2 = 0;
            }
            p1 ++;
        }
        return false;
    }

    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
