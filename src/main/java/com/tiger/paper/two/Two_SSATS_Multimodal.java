package com.tiger.paper.two;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author Tiger
 * @date 2021/4/24 15:45
 */
public class Two_SSATS_Multimodal {

    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 15;

    /**
     * Σ X^2 -- sphere  单峰函数
     *
     * @param xSet 多维 x 集合
     * @return
     */
    private double fSphere(List<Double> xSet) {
        double sum = 0;
        for (Double x : xSet) {
            sum += Math.pow(x, 2);
        }
        return packagingAccuracy(sum);
    }

    /**
     * Griewank 函数 多峰
     * @param xSet
     * @return
     */
    private double fGriewank(List<Double> xSet) {
        double part1 = 0;
        for (Double x : xSet) {
            part1 += Math.pow(x, 2) / 4000;
        }
        double part2 = 1;
        for (int i = 0; i < xSet.size(); i++) {
            part2 *= Math.cos(xSet.get(i) / Math.sqrt(i + 1));
        }
        return packagingAccuracy(part1 - part2 + 1);
    }

    private double packagingAccuracy(double x) {
        return BigDecimal.valueOf(x).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
    }

    private double randomNormalDistribution() {
        double u = 0.0d;
        double v = 0.0d;
        double w = 0.0d;
        double c = 0.0d;
        do {
            //获得两个（-1,1）的独立随机变量
            u = Math.random() * 2 - 1.0;
            v = Math.random() * 2 - 1.0;
            w = u * u + v * v;
        } while (w == 0.0 || w >= 1.0);
        //这里就是 Box-Muller转换
        c = Math.sqrt((-2 * Math.log(w)) / w);
        //返回2个标准正态分布的随机数，封装进一个数组返回
        //当然，因为这个函数运行较快，也可以扔掉一个
        //return [u*c,v*c];
        return packagingAccuracy(u * c);
    }
}
