//package com.tiger.paper.two;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * @author Tiger
// * @date 2021/4/24 15:45
// */
//public class Two_SSATS_Multimodal {
//    /**
//     * 产生多少混沌数值
//     */
//    private static final int CHAOTIC_SEQUENCE_NUMBER = 300;
//    /**
//     * 控制小数点后几位的精度
//     */
//    private static final int PRECISION = 15;
//    /**
//     * 种群大小
//     */
//    private int speciesNum;
//    /**
//     * 最大迭代次数
//     */
//    private int iterations;
//    /**
//     * 发现者数量
//     */
//    private int PD;
//    /**
//     * 意识到危险的麻雀数量
//     */
//    private int SD;
//    /**
//     * 生产者警戒阈值
//     */
//    private double ST;
//    /**
//     * 混沌数值
//     */
//    private double[] chaoticValue;
//    /**
//     * 经过 Cat混沌映射和反向学习后的 初始种群
//     */
//    private List<Double> coordinatePoints;
//    /**
//     * 生产者坐标
//     */
//    private List<Double> pdPoints;
//    /**
//     * 跟随者坐标
//     */
//    private List<Double> scPoints;
//    /**
//     * 预警者坐标
//     */
//    private List<Double> sdPoints;
//    /**
//     * 随机生成器
//     */
//    private Random random;
//    /**
//     * 预警值
//     */
//    private double r2;
//    /**
//     * 自适应步长
//     */
//    private double ω;
//    /**
//     * speciesNum / 2
//     */
//    private int speciesNumDivideTwo;
//    /**
//     * 每次迭代，map里面的最优值 最差值都会更新
//     */
//    private Map<String, Double> updateMap;
//    /**
//     * 禁忌表长度
//     */
//    private int searchFields;
//    /**
//     * 禁忌表长度
//     */
//    private int tabooTableLength;
//    /**
//     * 禁忌表
//     */
//    private LinkedList<Double> tabooTable;
//
//    public Two_SSATS_Multimodal(int speciesNum, int iterations, double PDRatio, double SDRatio, double ST) {
//        this.speciesNum = speciesNum;
//        this.iterations = iterations;
//        this.PD = (int) (speciesNum * PDRatio);
//        this.SD = (int) (speciesNum * SDRatio);
//        this.random = new Random();
//        this.ST = ST;
//        this.pdPoints = new ArrayList<>(this.PD);
//        this.sdPoints = new ArrayList<>(this.SD);
//        this.r2 = Math.random();
//        this.speciesNumDivideTwo = speciesNum / 2;
//        this.updateMap = new HashMap<>(16);
//        this.searchFields = PD;
//        this.tabooTableLength = (int) Math.sqrt(speciesNum);
//        this.tabooTable = new LinkedList<>();
//        for (int i = 0; i < tabooTableLength; i++) {
//            tabooTable.add(-1d);
//        }
//        this.ω = BigDecimal.ONE.divide(BigDecimal.valueOf(1 + Math.exp(PDRatio * -10)), PRECISION, RoundingMode.HALF_UP).doubleValue();
//        //----------------------------------
//        chaoticValue = new double[CHAOTIC_SEQUENCE_NUMBER];
//        //cat map 产生混沌映射
//        double[] temp = new double[CHAOTIC_SEQUENCE_NUMBER];
//        chaoticValue[0] = BigDecimal.valueOf(Math.random()).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
//        temp[0] = BigDecimal.valueOf(Math.random()).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
//        for (int i = 1; i < CHAOTIC_SEQUENCE_NUMBER; i++) {
//            chaoticValue[i] = BigDecimal.valueOf((chaoticValue[i - 1] + temp[i - 1]) % 100).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
//            temp[i] = BigDecimal.valueOf((chaoticValue[i - 1] + 2 * temp[i - 1]) % 100).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
//        }
//        //-------------------------------------------------下面进行反向学习
//        temp = new double[CHAOTIC_SEQUENCE_NUMBER];
//        for (int i = 0; i < CHAOTIC_SEQUENCE_NUMBER; i++) {
//            //x` = 1-x;
//            temp[i] = BigDecimal.ONE.subtract(BigDecimal.valueOf(chaoticValue[i])).doubleValue();
//        }
//        //------------------------------------------------- 2*CHAOTIC_SEQUENCE_NUMBER个体中 找最优的前 SPECIES_NUM个体 作为初始种群
//        coordinatePoints = Arrays.stream(chaoticValue).boxed().collect(Collectors.toList());
//        coordinatePoints.addAll(Arrays.stream(temp).boxed().collect(Collectors.toList()));
//        Collections.sort(coordinatePoints, new Comparator<Double>() {
//            @Override
//            public int compare(Double o1, Double o2) {
//                return f(o2) - f(o1) > 0 ? 1 : -1;
//            }
//        });
//        //初始种群
//        coordinatePoints = coordinatePoints.subList(0, speciesNum);
//        //--------------------------------------------------------下面进行分离生产者、跟随者、预警者的位置，方便后续对它们更新
//        for (int i = 0; i < PD; i++) {
//            int pdIdx;
//            do {
//                pdIdx = random.nextInt(speciesNum);
//            } while (pdPoints.contains(coordinatePoints.get(pdIdx)));
//            pdPoints.add(coordinatePoints.get(pdIdx));
//        }
//        //跟随者
//        scPoints = new ArrayList<>(coordinatePoints);
//        scPoints.removeAll(pdPoints);
//        double idx;
//        while (scPoints.size() < speciesNum - PD) {
//            do {
//                idx = packagingAccuracy(Math.random());
//            }
//            while (idx > 0.8d || idx < 0.2d);
//            scPoints.add(idx);
//        }
//        //预警者
//        for (int i = 0; i < SD; i++) {
//            int sdIdx;
//            do {
//                sdIdx = random.nextInt(speciesNum);
//            } while (sdPoints.contains(coordinatePoints.get(sdIdx)));
//            sdPoints.add(coordinatePoints.get(sdIdx));
//        }
//    }
//
//    /**
//     * Σ X^2 -- sphere  单峰函数
//     *
//     * @param xSet 多维 x 集合
//     * @return
//     */
//    private double fSphere(List<Double> xSet) {
//        double sum = 0;
//        for (Double x : xSet) {
//            sum += Math.pow(x, 2);
//        }
//        return packagingAccuracy(sum);
//    }
//
//    /**
//     * Griewank 函数 多峰
//     *
//     * @param xSet
//     * @return
//     */
//    private double fGriewank(List<Double> xSet) {
//        double part1 = 0;
//        for (Double x : xSet) {
//            part1 += Math.pow(x, 2) / 4000;
//        }
//        double part2 = 1;
//        for (int i = 0; i < xSet.size(); i++) {
//            part2 *= Math.cos(xSet.get(i) / Math.sqrt(i + 1));
//        }
//        return packagingAccuracy(part1 - part2 + 1);
//    }
//
//    private double packagingAccuracy(double x) {
//        return BigDecimal.valueOf(x).setScale(PRECISION, RoundingMode.HALF_UP).doubleValue();
//    }
//
//    private double randomNormalDistribution() {
//        double u = 0.0d;
//        double v = 0.0d;
//        double w = 0.0d;
//        double c = 0.0d;
//        do {
//            //获得两个（-1,1）的独立随机变量
//            u = Math.random() * 2 - 1.0;
//            v = Math.random() * 2 - 1.0;
//            w = u * u + v * v;
//        } while (w == 0.0 || w >= 1.0);
//        //这里就是 Box-Muller转换
//        c = Math.sqrt((-2 * Math.log(w)) / w);
//        //返回2个标准正态分布的随机数，封装进一个数组返回
//        //当然，因为这个函数运行较快，也可以扔掉一个
//        //return [u*c,v*c];
//        return packagingAccuracy(u * c);
//    }
//
//
//}
