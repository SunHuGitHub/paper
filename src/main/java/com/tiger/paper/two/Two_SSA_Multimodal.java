package com.tiger.paper.two;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tiger
 * @date 2021/4/24 15:45
 */
public class Two_SSA_Multimodal {
    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 15;
    /**
     * 维度
     */
    private static final int DIMENSION = 3;
    private static final int UPPER_BOUND = 10;
    private static final int LOWER_BOUND = -10;
    /**
     * 种群大小
     */
    private int speciesNum;
    /**
     * 最大迭代次数
     */
    private int iterations;
    /**
     * 发现者数量
     */
    private int PD;
    /**
     * 意识到危险的麻雀数量
     */
    private int SD;
    /**
     * 生产者警戒阈值
     */
    private double ST;
    /**
     * 初始种群
     */
    private List<List<Double>> coordinatePoints;
    /**
     * 生产者坐标
     */
    private List<List<Double>> pdPoints;
    /**
     * 跟随者坐标
     */
    private List<List<Double>> scPoints;
    /**
     * 预警者坐标
     */
    private List<List<Double>> sdPoints;
    /**
     * 随机生成器
     */
    private Random random;
    /**
     * 预警值
     */
    private double r2;
    /**
     * speciesNum / 2
     */
    private int speciesNumDivideTwo;
    /**
     * 生产者最优点，全局最优点，全局最差点
     */
    private Map<String, List<Double>> updateMap;
    /**
     * 全局最优值，最差值
     */
    private Map<String, Double> updateValue;

    public Two_SSA_Multimodal(int speciesNum, int iterations, double PDRatio, double SDRatio, double ST) {
        this.speciesNum = speciesNum;
        this.iterations = iterations;
        this.PD = (int) (speciesNum * PDRatio);
        this.SD = (int) (speciesNum * SDRatio);
        this.random = new Random();
        this.ST = ST;
        this.pdPoints = new ArrayList<>(this.PD);
        this.sdPoints = new ArrayList<>(this.SD);
        this.r2 = Math.random();
        this.speciesNumDivideTwo = speciesNum / 2;
        this.coordinatePoints = new ArrayList<>(speciesNum);
        this.updateMap = new HashMap<>(16);
        this.updateValue = new HashMap<>(16);
        List<Double> temp;
        for (int i = 1; i <= this.speciesNum; i++) {
            temp = new ArrayList<>(DIMENSION);
            for (int z = 0; z < DIMENSION; z++) {
                temp.add(makeRandom(UPPER_BOUND, LOWER_BOUND, PRECISION));
            }
            coordinatePoints.add(temp);
        }
        //--------------------------------------------------------下面进行分离生产者、跟随者、预警者的位置，方便后续对它们更新
        for (int i = 0; i < PD; i++) {
            int pdIdx;
            do {
                pdIdx = random.nextInt(speciesNum);
            } while (pdPoints.contains(coordinatePoints.get(pdIdx)));
            pdPoints.add(coordinatePoints.get(pdIdx));
        }
        //跟随者
        scPoints = new ArrayList<>(coordinatePoints);
        scPoints.removeAll(pdPoints);
    }

    /**
     * 生成指定范围，指定小数位数的随机数
     *
     * @param max   最大值
     * @param min   最小值
     * @param scale 小数位数
     * @return
     */
    double makeRandom(float max, float min, int scale) {
        return BigDecimal.valueOf(Math.random() * (max - min) + min).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }


    private void updateProducerPoint() {
        //麻雀坐标
        List<Double> sparrowIndex;
        double sparrowJDimension;
        for (int i = 0; i < PD; i++) {
            sparrowIndex = pdPoints.get(i);
            if (r2 < ST) {
                //更新各个维度的坐标
                for (int j = 0; j < DIMENSION; j++) {
                    do {
                        sparrowJDimension = sparrowIndex.get(j) * Math.exp(-(i / (iterations * Math.random())));
                    } while (sparrowJDimension < 0 || sparrowJDimension > 1);
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            } else {
                for (int j = 0; j < DIMENSION; j++) {
                    do {
                        sparrowJDimension = sparrowIndex.get(j) + randomNormalDistribution();
                    } while (sparrowJDimension < 0 || sparrowJDimension > 1);
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            }
        }
    }

    /**
     * 找出生产者中最好的那个点  以及全局最差点，全局最优点，最优适应度，最差适应度
     */
    private void rankAndFindLocation() {

        List<List<Double>> pdPointsTemp = new ArrayList<>(pdPoints);
        List<List<Double>> scPointsTemp = new ArrayList<>(scPoints);

        pdPointsTemp.sort(new Comparator<List<Double>>() {
            @Override
            public int compare(List<Double> o1, List<Double> o2) {
                return Double.compare(fSphere(o1), fSphere(o2));
            }
        });
        scPointsTemp.sort(new Comparator<List<Double>>() {
            @Override
            public int compare(List<Double> o1, List<Double> o2) {
                return Double.compare(fSphere(o1), fSphere(o2));
            }
        });

        List<Double> pdMax = pdPointsTemp.get(0);
        List<Double> pdmin = pdPointsTemp.get(PD - 1);
        List<Double> scMax = scPointsTemp.get(0);
        List<Double> scmin = scPointsTemp.get(speciesNum - PD - 1);

        double fpdMax = fSphere(pdMax);
        double fpdmin = fSphere(pdmin);
        double fscMax = fSphere(scMax);
        double fscmin = fSphere(scmin);
        //生产者最优的点
        updateMap.put("pdMax", new ArrayList<>(pdMax));

        if (fpdMax < fscMax) {
            //全局最优点
            updateMap.put("globalMax", new ArrayList<>(pdMax));
            //最优的适度度
            updateValue.put("fg", packagingAccuracy(fpdMax));
        } else {
            updateMap.put("globalMax", new ArrayList<>(scMax));
            updateValue.put("fg", packagingAccuracy(fscMax));
        }
        if (fpdmin < fscmin) {
            //全局最差点
            updateMap.put("globalMin", new ArrayList<>(scmin));
            //最差的适度度
            updateValue.put("fw", packagingAccuracy(fscmin));
        } else {
            updateMap.put("globalMin", new ArrayList<>(pdmin));
            updateValue.put("fw", packagingAccuracy(fpdmin));
        }
    }

    public static void main(String[] args) {
        new Two_SSA_Multimodal(10, 500, 0.8d, 0.1d, 0.8d);
    }

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
     *
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

    /**
     * 包装精度
     *
     * @param x
     * @return
     */
    private double packagingAccuracy(double x) {
        return BigDecimal.valueOf(x).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 正太分布随机数
     *
     * @return
     */
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
