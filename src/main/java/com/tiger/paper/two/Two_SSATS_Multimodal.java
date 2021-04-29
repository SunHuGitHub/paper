package com.tiger.paper.two;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


/**
 * @author Tiger
 * @date 2021/4/24 15:45
 */
public class Two_SSATS_Multimodal {
    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 30;
    /**
     * 维度
     */
    private static final int DIMENSION = 30;
    private static final int UPPER_BOUND = 1;
    private static final int LOWER_BOUND = -1;
    /**
     * 产生多少混沌数值
     */
    private static final int CHAOTIC_SEQUENCE_NUMBER = 300;

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
     * 混沌数值
     */
    private double[] chaoticValue;
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
     * 自适应步长
     */
    private double ω;
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
    /**
     * 领域搜索长度
     */
    private int searchFields;
    /**
     * 禁忌表长度
     */
    private int tabooTableLength;
    /**
     * 禁忌表
     */
    private LinkedList<List<Double>> tabooTable;

    /*
    *         //----------------------------------
        chaoticValue = new double[CHAOTIC_SEQUENCE_NUMBER];
        //cat map 产生混沌映射
        double[] temp = new double[CHAOTIC_SEQUENCE_NUMBER];
        chaoticValue[0] = BigDecimal.valueOf(Math.random()).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
        temp[0] = BigDecimal.valueOf(Math.random()).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
        for (int i = 1; i < CHAOTIC_SEQUENCE_NUMBER; i++) {
            chaoticValue[i] = BigDecimal.valueOf((chaoticValue[i - 1] + temp[i - 1]) % 100).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
            temp[i] = BigDecimal.valueOf((chaoticValue[i - 1] + 2 * temp[i - 1]) % 100).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
        }
        //-------------------------------------------------下面进行反向学习
        temp = new double[CHAOTIC_SEQUENCE_NUMBER];
        for (int i = 0; i < CHAOTIC_SEQUENCE_NUMBER; i++) {
            //x` = 1-x;
            temp[i] = BigDecimal.ONE.subtract(BigDecimal.valueOf(chaoticValue[i])).doubleValue();
        }
        //------------------------------------------------- 2*CHAOTIC_SEQUENCE_NUMBER个体中 找最优的前 SPECIES_NUM个体 作为初始种群
        coordinatePoints = Arrays.stream(chaoticValue).boxed().collect(Collectors.toList());
        coordinatePoints.addAll(Arrays.stream(temp).boxed().collect(Collectors.toList()));
        Collections.sort(coordinatePoints, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return f(o2) - f(o1) > 0 ? 1 : -1;
            }
        });
        //初始种群
        coordinatePoints = coordinatePoints.subList(0, speciesNum);
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
        double idx;
        while (scPoints.size() < speciesNum - PD) {
            do {
                idx = packagingAccuracy(Math.random());
            }
            while (idx > 0.8d || idx < 0.2d);
            scPoints.add(idx);
        }
    *
    * */


    public Two_SSATS_Multimodal(int speciesNum, int iterations, double PDRatio, double SDRatio, double ST) {
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
        this.searchFields = PD;
        this.tabooTableLength = (int) Math.sqrt(speciesNum);
        this.tabooTable = new LinkedList<>();
        for (int i = 0; i < tabooTableLength; i++) {
            tabooTable.add(new ArrayList<>());
        }
        this.ω = BigDecimal.ONE.divide(BigDecimal.valueOf(1 + Math.exp(PDRatio * -10)), PRECISION, RoundingMode.HALF_UP).doubleValue();
        this.r2 = Math.random();

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
        List<List<Double>> lists = new ArrayList<>(coordinatePoints);
        lists.sort(new Comparator<List<Double>>() {
            @Override
            public int compare(List<Double> o1, List<Double> o2) {
                return Double.compare(fSphere(o1), fSphere(o2));
            }
        });
        pdPoints = lists.subList(0, PD);
        //跟随者
        scPoints = new ArrayList<>(coordinatePoints);
        scPoints.removeAll(pdPoints);
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
                        sparrowJDimension = sparrowIndex.get(j) * (1 + ω * randomNormalDistribution());
                    } while (sparrowJDimension < LOWER_BOUND || sparrowJDimension > UPPER_BOUND);
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            } else {
                for (int j = 0; j < DIMENSION; j++) {
                    do {
                        sparrowJDimension = sparrowIndex.get(j) + randomNormalDistribution();
                    } while (sparrowJDimension < LOWER_BOUND || sparrowJDimension > UPPER_BOUND);
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
        //历史最优点
        List<Double> historicalBest = updateMap.getOrDefault("historicalBest", new ArrayList<>());
        updateMap.put("historicalBest", fSphere(pdMax) < fSphere(historicalBest) ? new ArrayList<>(pdMax) : new ArrayList<>(historicalBest));
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

    public double calculate() {
        double lastf = 0;
        for (int i = 1; i <= iterations; i++) {
            r2 = Math.random();
            ω = BigDecimal.ONE.divide(BigDecimal.valueOf(1 + Math.exp(2 * (BigDecimal.valueOf(2L * i / 1000).doubleValue() - 1))), PRECISION, RoundingMode.HALF_UP).doubleValue();
            //更新发现者坐标
            updateProducerPoint();
//            System.out.println("更新发现者坐标");
            //更新最优坐标1
            rankAndFindLocation();
//            System.out.println("更新最优坐标1");
            //更新追随者坐标
            updateScroungerPoint();
//            System.out.println("更新追随者坐标");
            //更新最优坐标2
            rankAndFindLocation();
//            System.out.println("更新最优坐标2");
            //更新预警者坐标
            updateSDPoint();
//            System.out.println("更新预警者坐标");
            //更新最优坐标3
            rankAndFindLocation();
//            System.out.println("更新最优坐标3");

            //禁忌搜索判断
            if (i != 1) {
                List<Double> pdMax = updateMap.get("pdMax");
                List<Double> historicalBest = updateMap.get("historicalBest");
                double fpdMax = fSphere(pdMax);
                double newPdIdx;
                int n = 1;
                //禁忌搜索判断条件
                if (Math.abs(lastf - fpdMax) / fpdMax <= 0.2) {
                    List<Double> tempIdx = new ArrayList<>(pdMax);
                    double ftempIdx = fpdMax;
                    while (n <= searchFields) {
                        List<Double> temp = new ArrayList<>(pdMax);
                        for (int z = 0; z < DIMENSION; z++) {
                            do {
                                newPdIdx = packagingAccuracy(pdMax.get(z) + (Math.random() > 0.5 ? 1 : -1) * ω * (historicalBest.get(z) - pdMax.get(z)));
                            } while (newPdIdx < 0 || newPdIdx > 1);
                            temp.set(z, newPdIdx);
                        }
                        if (!contains(temp)) {
                            if (fSphere(temp) < ftempIdx) {
                                tempIdx = temp;
                                ftempIdx = fSphere(temp);
                            }
                            n++;
                        }
                    }
                    modifyUpdateMap(tempIdx);
                }
            }
            List<Double> pdMax = updateMap.get("pdMax");
            liftAndAddTaboo(pdMax);
            lastf = fSphere(pdMax);
        }
        return updateValue.get("fg");
    }

    private boolean contains(List<Double> idx) {
        for (List<Double> doubles : tabooTable) {
            for (int i = 0; i < DIMENSION; i++) {
                if (!doubles.get(i).equals(idx.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void liftAndAddTaboo(List<Double> pdMax) {
        tabooTable.pollFirst();
        tabooTable.offerLast(pdMax);
    }

    private void modifyUpdateMap(List<Double> tempIdx) {
        double f = fSphere(tempIdx);
        List<Double> pdMax = updateMap.get("pdMax");
        List<Double> historicalBest = updateMap.get("historicalBest");
        double fpdMax = fSphere(pdMax);
        if (f < fpdMax) {
            updateMap.put("pdMax", tempIdx);
            pdPoints.set(pdPoints.indexOf(pdMax), tempIdx);
        }
        if (f < fSphere(historicalBest)) {
            updateMap.put("historicalBest", tempIdx);
        }
        if (f < updateValue.get("fg")) {
            updateValue.put("fg", f);
            updateMap.put("globalMax", tempIdx);
        }
        if (f > updateValue.get("fw")) {
            updateValue.put("fw", f);
            updateMap.put("globalMin", tempIdx);
        }
    }

    private void updateSDPoint() {
        List<Double> sparrowIndex;
        double f;
        double fg = updateValue.get("fg");
        double fw = updateValue.get("fw");
        List<Double> globalMax = updateMap.get("globalMax");
        List<Double> globalMin = updateMap.get("globalMin");
        double sparrowJDimension = 0;
        Set<Integer> hashSet = new HashSet<>();
        int idx;
        for (int i = 0; i < SD; i++) {
            do {
                idx = random.nextInt(speciesNum);
            } while (!hashSet.add(idx));
            sparrowIndex = coordinatePoints.get(idx);
            f = fSphere(sparrowIndex);

            if (f > fg) {
                for (int j = 0; j < DIMENSION; j++) {
                    do {
                        sparrowJDimension = globalMax.get(j) + randomNormalDistribution() * Math.abs(globalMax.get(j) - sparrowIndex.get(j));
                    } while (sparrowJDimension < LOWER_BOUND || sparrowJDimension > UPPER_BOUND);
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            } else if (Math.abs(f - fg) <= 1e-14) {
                for (int j = 0; j < DIMENSION; j++) {
                    double abs = Math.abs(sparrowIndex.get(j) - globalMin.get(j));
                    double v = fw - f + 1e-14;
                    double v1 = abs / v;
//                    while (Math.abs(v1) > 1) {
//                        v1 /= 10;
//                    }
                    int cnt = 1;
                    do {
                        if (cnt > 50) {
                            break;
                        }
                        sparrowJDimension = sparrowIndex.get(j) + (Math.random() * 2 - 1) * v1;
                        cnt++;
                    } while (sparrowJDimension < LOWER_BOUND || sparrowJDimension > UPPER_BOUND);
                    if (cnt > 50) {
                        sparrowJDimension = updateMap.get("globalMax").get(j);
                    }
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            }
        }
    }

    private void updateScroungerPoint() {
        List<Double> globalMin = updateMap.get("globalMin");
        List<Double> sparrowIndex;
        double sparrowJDimension;
        List<Double> pdMax = updateMap.get("pdMax");
        for (int i = 0; i < speciesNum - PD; i++) {
            sparrowIndex = scPoints.get(i);

            if (i > speciesNumDivideTwo) {
                for (int j = 0; j < DIMENSION; j++) {
                    do {
                        sparrowJDimension = randomNormalDistribution() * Math.exp((globalMin.get(j) - sparrowIndex.get(j)) / Math.pow(i, 2));
                    } while (sparrowJDimension < LOWER_BOUND || sparrowJDimension > UPPER_BOUND);
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            } else {
                for (int j = 0; j < DIMENSION; j++) {
                    do {
                        sparrowJDimension = pdMax.get(j) + Math.abs(sparrowIndex.get(j) - pdMax.get(j)) * (Math.random() > 0.5 ? 1 : -1);
                    } while (sparrowJDimension < LOWER_BOUND || sparrowJDimension > UPPER_BOUND);
                    sparrowIndex.set(j, packagingAccuracy(sparrowJDimension));
                }
            }
        }
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

    private double packagingAccuracy(double x) {
        return BigDecimal.valueOf(x).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
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

    /**
     * 生成
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
