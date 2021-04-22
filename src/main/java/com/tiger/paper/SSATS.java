package com.tiger.paper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tiger
 * @date 2021/4/22 8:04
 */
public class SSATS {
    /**
     * 产生多少混沌数值
     */
    private static final int CHAOTIC_SEQUENCE_NUMBER = 1000;
    /**
     * 混沌数值
     */
    private static final double[] CHAOTIC_VALUE = new double[CHAOTIC_SEQUENCE_NUMBER];
    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 6;
    /**
     * 种群大小
     */
    private static final int SPECIES_NUM = 100;
    /**
     * 经过 Cat混沌映射和反向学习后的 初始种群
     */
    private static List<Double> coordinatePoints;
    /**
     * 生产者警戒阈值
     */
    private static float ST;

    static {
        //cat map 产生混沌映射
        double[] temp = new double[CHAOTIC_SEQUENCE_NUMBER];
        CHAOTIC_VALUE[0] = BigDecimal.valueOf(Math.random()).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
        temp[0] = BigDecimal.valueOf(Math.random()).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
        for (int i = 1; i < CHAOTIC_SEQUENCE_NUMBER; i++) {
            CHAOTIC_VALUE[i] = BigDecimal.valueOf((CHAOTIC_VALUE[i - 1] + temp[i - 1]) % 1).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
            temp[i] = BigDecimal.valueOf((CHAOTIC_VALUE[i - 1] + 2 * temp[i - 1]) % 1).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
        }
        //-------------------------------------------------下面进行反向学习
        temp = new double[CHAOTIC_SEQUENCE_NUMBER];
        for (int i = 0; i < CHAOTIC_SEQUENCE_NUMBER; i++) {
            //x` = 1-x;
            temp[i] = BigDecimal.ONE.subtract(BigDecimal.valueOf(CHAOTIC_VALUE[i])).doubleValue();
        }
        //------------------------------------------------- 2*CHAOTIC_SEQUENCE_NUMBER个体中 找最优的前 SPECIES_NUM个体 作为初始种群
        coordinatePoints = Arrays.stream(CHAOTIC_VALUE).boxed().collect(Collectors.toList());
        coordinatePoints.addAll(Arrays.stream(temp).boxed().collect(Collectors.toList()));
        Collections.sort(coordinatePoints, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return f(o2) - f(o1) > 0 ? 1 : -1;
            }
        });
        //初始种群
        coordinatePoints = coordinatePoints.subList(0, SPECIES_NUM);
    }

    private static double f(double x) {
        return -Math.pow(x - 1, 2) + 2;
    }

    private void updateProducerPoint(double x) {

    }

    public static void main(String[] args) {

    }
}
