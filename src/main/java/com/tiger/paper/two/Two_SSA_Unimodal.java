package com.tiger.paper.two;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author Tiger
 * @date 2021/4/22 8:04
 */
public class Two_SSA_Unimodal {

    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 15;
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
    private List<Double> coordinatePoints;
    /**
     * 生产者坐标
     */
    private List<Double> pdPoints;
    /**
     * 跟随者坐标
     */
    private List<Double> scPoints;
    /**
     * 预警者坐标
     */
    private List<Double> sdPoints;
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
     * 每次迭代，map里面的最优值 最差值都会更新
     */
    private Map<String, Double> updateMap;


    public Two_SSA_Unimodal(int speciesNum, int iterations, double PDRatio, double SDRatio, double ST) {
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
        this.updateMap = new HashMap<>(16);
        this.coordinatePoints = new ArrayList<>(speciesNum);
        double idx;
        for (int i = 1; i <= this.speciesNum; i++) {
            do {
                idx = packagingAccuracy(Math.random());
            }
            while (idx > 0.8d || idx < 0.2d);
            coordinatePoints.add(idx);
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

        //预警者
        for (int i = 0; i < SD; i++) {
            int sdIdx;
            do {
                sdIdx = random.nextInt(speciesNum);
            } while (sdPoints.contains(coordinatePoints.get(sdIdx)));
            sdPoints.add(coordinatePoints.get(sdIdx));
        }
    }


    private double f(double x) {
        return packagingAccuracy(-Math.pow(x - 1, 2) + 2);
    }

    private void updateProducerPoint() {
        //麻雀坐标
        double sparrowIndex;

        for (int i = 0; i < PD; i++) {
            if (r2 < ST) {
                do {
                    sparrowIndex = pdPoints.get(i) * Math.exp(-(i / (iterations * Math.random())));
                }
                while (sparrowIndex < 0 || sparrowIndex > 1);
            } else {
                do {
                    sparrowIndex = pdPoints.get(i) + randomNormalDistribution();
                }
                while (sparrowIndex < 0 || sparrowIndex > 1);
            }
            pdPoints.set(i, packagingAccuracy(sparrowIndex));
        }
    }

    private void updateScroungerPoint() {
        double globalMin = updateMap.get("globalMin");
        double sparrowIndex;
        double temp;
        double pdMax = updateMap.get("pdMax");
        for (int i = 0; i < scPoints.size(); i++) {
            sparrowIndex = scPoints.get(i);
            temp = sparrowIndex;
            if (i > speciesNumDivideTwo) {
                do {
                    sparrowIndex = packagingAccuracy(randomNormalDistribution() * Math.exp((globalMin - temp) / Math.pow(i, 2)));
                } while (sparrowIndex < 0 || sparrowIndex > 1);
            } else {
                do {
                    sparrowIndex = pdMax + Math.abs(temp - pdMax) * (Math.random() > 0.5 ? 1 : -1);
                } while (sparrowIndex < 0 || sparrowIndex > 1);
            }
            scPoints.set(i, packagingAccuracy(sparrowIndex));
        }
    }

    private void updateSDPoint() {
        double sparrowIndex;
        double f;
        double fg = updateMap.get("fg");
        double fw = updateMap.get("fw");
        double globalMax = updateMap.get("globalMax");
        double globalMin = updateMap.get("globalMin");
        double temp;
        for (int i = 0; i < sdPoints.size(); i++) {
            sparrowIndex = sdPoints.get(i);
            f = f(sparrowIndex);
            temp = sparrowIndex;
            if (f > fg) {
                do {
                    sparrowIndex = globalMax + randomNormalDistribution() * Math.abs(globalMax - temp);
                } while (sparrowIndex < 0 || sparrowIndex > 1);
            } else if (Math.abs(f - fg) <= 1e-10) {
                double abs = Math.abs(temp - globalMin);
                double v = fw - f + 1e-18;
                double v1 = abs / v;
                while (Math.abs(v1) > 1) {
                    v1 /= 10;
                }
                do {
                    sparrowIndex = temp + (Math.random() * 2 - 1) * v1;
                } while (sparrowIndex < 0 || sparrowIndex > 1);
            }
            sdPoints.set(i, packagingAccuracy(sparrowIndex));
        }
    }

    /**
     * 找出生产者中最好的那个点  以及全局最差点，全局最优点，最优适应度，最差适应度
     */
    private void rankAndFindLocation() {
        List<Double> pdPointsTemp = new ArrayList<>(pdPoints);
        List<Double> scPointsTemp = new ArrayList<>(scPoints);

        pdPointsTemp.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return f(o2) - f(o1) > 0 ? 1 : -1;
            }
        });
        scPointsTemp.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return f(o2) - f(o1) > 0 ? 1 : -1;
            }
        });

        Double pdMax = pdPointsTemp.get(0);
        Double pdmin = pdPointsTemp.get(PD - 1);
        Double scMax = scPointsTemp.get(0);
        Double scmin = scPointsTemp.get(speciesNum - PD - 1);

        double fpdMax = f(pdMax);
        double fpdmin = f(pdmin);
        double fscMax = f(scMax);
        double fscmin = f(scmin);
        //生产者最优的点
        updateMap.put("pdMax", packagingAccuracy(pdMax));
        Double historicalBest = updateMap.getOrDefault("historicalBest", -1d);
        updateMap.put("historicalBest", f(pdMax) > f(historicalBest) ? packagingAccuracy(pdMax) : packagingAccuracy(historicalBest));
        if (fpdMax > fscMax) {
            //全局最优点
            updateMap.put("globalMax", packagingAccuracy(pdMax));
            //最优的适度度
            updateMap.put("fg", packagingAccuracy(fpdMax));
        } else {
            updateMap.put("globalMax", packagingAccuracy(scMax));
            updateMap.put("fg", packagingAccuracy(fscMax));
        }
        if (fpdmin > fscmin) {
            //全局最差点
            updateMap.put("globalMin", packagingAccuracy(scmin));
            //最差的适度度
            updateMap.put("fw", packagingAccuracy(fscmin));
        } else {
            updateMap.put("globalMin", packagingAccuracy(pdmin));
            updateMap.put("fw", packagingAccuracy(fpdmin));
        }
    }

    private void updateProduceMap() {
        List<Double> pdPointsTemp = new ArrayList<>(pdPoints);
        pdPointsTemp.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return f(o2) - f(o1) > 0 ? 1 : -1;
            }
        });
        double pdMax = pdPointsTemp.get(0);
        updateMap.put("pdMax", packagingAccuracy(pdMax));
        Double historicalBest = updateMap.getOrDefault("historicalBest", -1d);
        updateMap.put("historicalBest", f(pdMax) > f(historicalBest) ? packagingAccuracy(pdMax) : packagingAccuracy(historicalBest));
    }

    private void calculate() {
        for (int i = 1; i <= iterations; i++) {
            r2 = Math.random();
            //更新发现者坐标
            updateProducerPoint();
            //更新updateMap中最优的发现者坐标
            rankAndFindLocation();
            //更新追随者坐标
            updateScroungerPoint();
            //更新预警者坐标
            updateSDPoint();

            rankAndFindLocation();
        }
        System.out.println(updateMap.get("globalMax"));
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            Two_SSA_Unimodal ssats = new Two_SSA_Unimodal(100, 1000, 0.2, 0.1, 0.8);
            ssats.calculate();
        }
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
