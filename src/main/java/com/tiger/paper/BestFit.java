package com.tiger.paper;

import java.util.ArrayList;

public class BestFit {
    public static void main(String[] args) {
        double[] doubles=new double[]{1,3,5,546,8};
        String[] strings=new String[]{"!","2","4","sdf","ss"};
        System.out.println(findBest(doubles,strings));
    }
    public static String findBest(double[] value, String[] names){
        ArrayList<Block> blocks=new ArrayList<>();
        for (int i=0;i<value.length;i++){
            blocks.add(new Block(names[i],value[i]));
        }
        blocks.sort((a1,a2)->Double.compare(a2.getValue(),a1.getValue()));
        return blocks.get(0).getName();
    }
}