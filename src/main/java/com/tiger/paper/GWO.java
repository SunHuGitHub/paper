package com.tiger.paper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Tiger
 * @date 2021/4/27 22:30
 */
public class GWO {
    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 6;
    /**
     * 初始种群
     */
    private List<Double> coordinatePoints;
    /**
     * 种群大小
     */
    private int speciesNum;
    /**
     * 最大迭代次数
     */
    private int iterations;

    private double alph;
    private double beta;
    private double seta;
    private double a;
    private double rand1;
    private double rand2;
    private double A;
    private double C;

    public GWO(int speciesNum, int iterations) {
        this.speciesNum = speciesNum;
        this.iterations = iterations;
        this.coordinatePoints = new ArrayList<>(speciesNum);
        double idx;
        for (int i = 1; i <= this.speciesNum; i++) {
            do {
                idx = packagingAccuracy(Math.random());
            }
            while (idx > 0.8d || idx < 0.2d);
            coordinatePoints.add(idx);
        }
    }

    private void rank() {
        List<Double> temp = new ArrayList<>(coordinatePoints);
        temp.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(f(o1), f(o2));
            }
        });
        this.alph = temp.get(0);
        this.beta = temp.get(1);
        this.seta = temp.get(2);
    }

    private double f(double x) {
        return packagingAccuracy(Math.pow(x - 0.8, 2) + 2);
    }

    private void calculate() {
        for (int i = 0; i < iterations; i++) {
            rank();
            a = 2 - (i / iterations);
            double A1;
            double C1;
            double A2;
            double C2;
            double A3;
            double C3;
            double X1;
            double X2;
            double X3;
            for (int z = 0; z < coordinatePoints.size(); z++) {

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A1 = 2 * a * rand1 - a;
                    C1 = 2 * rand2;
                    X1 = alph - A1 * Math.abs(C1 - coordinatePoints.get(i));
                } while (X1 < 0 || X1 > 1);

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A2 = 2 * a * rand1 - a;
                    C2 = 2 * rand2;
                    X2 = beta - A2 * Math.abs(C2 - coordinatePoints.get(i));
                } while (X2 < 0 || X2 > 1);

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A3 = 2 * a * rand1 - a;
                    C3 = 2 * rand2;
                    X3 = seta - A3 * Math.abs(C3 - coordinatePoints.get(i));
                } while (X3 < 0 || X3 > 1);
                coordinatePoints.set(i, (X1 + X2 + X3) / 3);
            }
        }
        rank();
        System.out.println(f(alph));
    }

    public static void main(String[] args) {
        new GWO(100, 100).calculate();
    }

    private double packagingAccuracy(double x) {
        return BigDecimal.valueOf(x).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
    }
}
