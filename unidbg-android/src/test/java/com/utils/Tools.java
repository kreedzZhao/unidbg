package com.utils;

import java.util.Random;

public class Tools {
    public static int randint(int min,int max){
        Random rand = new Random();
        return rand.nextInt((max-min)+1)+min;
    }
}
