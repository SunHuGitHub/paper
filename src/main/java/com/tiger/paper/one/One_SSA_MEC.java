package com.tiger.paper.one;

import com.tiger.paper.EdgeSettings;
import com.tiger.paper.MobileUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tiger
 * @date 2021/4/22 8:04
 */
public class One_SSA_MEC {
    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 4;
    /**
     * cent/gigahertz   分/千兆赫 -> 6/1G
     */
    private static final int COST = 6;
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

    /**
     * 任务，每个任务单位为（bits）
     */
    private Long totalComputingDatas;

    private List<MobileUser> mobileUsers;

    private MobileUser mobileUser;

    private EdgeSettings edgeSettings;

    public One_SSA_MEC(int speciesNum, int iterations, double PDRatio, double SDRatio, double ST, MobileUser mobileUser, List<MobileUser> mobileUsers, EdgeSettings edgeSettings, Long totalComputingDatas) {
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
        this.mobileUsers = mobileUsers;
        this.edgeSettings = edgeSettings;
        this.mobileUser = mobileUser;
        this.totalComputingDatas = totalComputingDatas;
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
        while (scPoints.size() < speciesNum - PD) {
            do {
                idx = packagingAccuracy(Math.random());
            }
            while (idx > 0.8d || idx < 0.2d);
            scPoints.add(idx);
        }
        //预警者
        for (int i = 0; i < SD; i++) {
            int sdIdx;
            do {
                sdIdx = random.nextInt(speciesNum);
            } while (sdPoints.contains(coordinatePoints.get(sdIdx)));
            sdPoints.add(coordinatePoints.get(sdIdx));
        }
    }


//    private double f(double x) {
//        return packagingAccuracy(-Math.pow(x - 1, 2) + 2);
//    }

    private double fTime(double sparrowIndex) {

        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        //用户本地计算能力
        Float localComputingAbility = mobileUser.getLocalComputingAbility();
        //      本地执行时间      任务上传时间      上传数据大小
        double localExeTime, uplinkTime, uplinkComputingData;

        //上传数据大小
        uplinkComputingData = totalComputingDatas * sparrowIndex;
        //本地执行时间
        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

        double execTime = mobileUser.getExecTime();
        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
        mobileUser.setExecTime(localExeTime);
        reFreshUpdatingUplinkRate();
        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        double totalTime = localExeTime + uplinkTime + edgeExecTime;
        mobileUser.setExecTime(execTime);
        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);

        return packagingAccuracy(totalTime);
    }


    /**
     * @return 刷新上传速率
     */
    private void reFreshUpdatingUplinkRate() {
        double execTime = mobileUser.getExecTime();
        double sumW = BigDecimal.valueOf(0.0d).doubleValue();

        for (MobileUser user : mobileUsers) {
            sumW += BigDecimal.valueOf(user.getTransPower() * BigDecimal.valueOf(Math.pow(user.getDistance(), -edgeSettings.getEta())).doubleValue()).doubleValue();
        }
        //根据用户的执行时间 计算出离基站的距离
        double distance = calculateDistance(execTime);

        //上传速率公式为香农公式  见 上传速率公式.png
        if (edgeSettings.getBackgroundNoisePower() > 0) {
            //backgroundNoisePower为 W 时
            mobileUser.setUpdatingUplinkRate(packagingAccuracy(edgeSettings.getBandwidth() *
                    (Math.log(1 + (mobileUser.getTransPower() * Math.pow(distance, -edgeSettings.getEta()))
                            / (edgeSettings.getBackgroundNoisePower() + sumW))
                            / Math.log(2))));
        } else {
            //backgroundNoisePower 为 dbm 时
            mobileUser.setUpdatingUplinkRate(packagingAccuracy(edgeSettings.getBandwidth() *
                    (Math.log(1 + (mobileUser.getTransPower() * Math.pow(distance, -edgeSettings.getEta()))
                            / (Math.pow(10, edgeSettings.getBackgroundNoisePower() / 10.0) / 1000 + sumW))
                            / Math.log(2))));
        }

    }

    /**
     * @param execTime 执行时间
     * @return 计算离基站多远
     */
    private double calculateDistance(double execTime) {
        List<Map<String, Object>> mobileConf = mobileUser.getMobileConf();
        double timeNum = 0.0d;
        int i = 0;
        //找到 execTime 所在哪个区间 这样就能算出 这时其他移动用户离基站距离 以及 功率
        for (; i < mobileConf.size(); i++) {
            double time = Double.parseDouble(mobileConf.get(i).get("time").toString());
            if (time + timeNum < execTime) {
                timeNum += time;
            } else {
                break;
            }
        }
        Map<String, Object> mobileMap;
        if (i == 5) {
            mobileMap = mobileConf.get(i - 1);
        } else {
            mobileMap = mobileConf.get(i);
        }
        int sign = 1;
        if ((i & 1) != 0) {
            sign = -1;
        }
        double time = execTime - timeNum;
        double startingPoint = Double.parseDouble(mobileMap.get("startingPoint").toString());
        double speed = Double.parseDouble(mobileMap.get("speed").toString());
        return packagingAccuracy(startingPoint + sign * speed * time);
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
            f = fTime(sparrowIndex);
            temp = sparrowIndex;
            if (f > fg) {
                do {
                    sparrowIndex = globalMax + randomNormalDistribution() * Math.abs(globalMax - temp);
                } while (sparrowIndex < 0 || sparrowIndex > 1);
            } else if (Math.abs(f - fg) <= 1e-4) {
                double abs = packagingAccuracy(Math.abs(temp - globalMin));
                double v = packagingAccuracy(fw - f + 1e-4);
                double v1 = packagingAccuracy(abs / v);
                while (Math.abs(v1) > 1) {
                    v1 /= 10;
                }
                int cnt = 1;
                do {
                    if (cnt > 50) {
                        break;
                    }
                    sparrowIndex = temp + (Math.random() * 2 - 1) * v1;
                    cnt++;
                } while (sparrowIndex < 0 || sparrowIndex > 1);
                if (cnt > 50) {
                    sparrowIndex = updateMap.get("globalMax");
                }
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
                return Double.compare(fTime(o1), fTime(o2));
            }
        });
        scPointsTemp.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(fTime(o1), fTime(o2));
            }
        });

        Double pdMax = pdPointsTemp.get(0);
        Double pdmin = pdPointsTemp.get(PD - 1);
        Double scMax = scPointsTemp.get(0);
        Double scmin = scPointsTemp.get(speciesNum - PD - 1);

        double fpdMax = fTime(pdMax);
        double fpdmin = fTime(pdmin);
        double fscMax = fTime(scMax);
        double fscmin = fTime(scmin);
        //生产者最优的点
        updateMap.put("pdMax", packagingAccuracy(pdMax));
        Double historicalBest = updateMap.getOrDefault("historicalBest", -1d);
        updateMap.put("historicalBest", fTime(pdMax) < fTime(historicalBest) ? packagingAccuracy(pdMax) : packagingAccuracy(historicalBest));
        if (fpdMax < fscMax) {
            //全局最优点
            updateMap.put("globalMax", packagingAccuracy(pdMax));
            //最优的适度度
            updateMap.put("fg", packagingAccuracy(fpdMax));
        } else {
            updateMap.put("globalMax", packagingAccuracy(scMax));
            updateMap.put("fg", packagingAccuracy(fscMax));
        }
        if (fpdmin < fscmin) {
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
                return fTime(o1) - fTime(o2) > 0 ? 1 : -1;
            }
        });
        double pdMax = pdPointsTemp.get(0);
        updateMap.put("pdMax", packagingAccuracy(pdMax));
        Double historicalBest = updateMap.getOrDefault("historicalBest", -1d);
        updateMap.put("historicalBest", fTime(pdMax) < fTime(historicalBest) ? packagingAccuracy(pdMax) : packagingAccuracy(historicalBest));
    }

    public double calculate() {
        for (int i = 1; i <= iterations; i++) {
            r2 = Math.random();
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
        }
        return updateMap.get("fg");
    }

    public double calculateSLA() {
        for (int i = 1; i <= iterations; i++) {
            r2 = Math.random();
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
        }
        double res = 0d;
        double fg = updateMap.get("fg");
        if (fg > packagingAccuracy((totalComputingDatas * mobileUser.getCyclesPerBit() / mobileUser.getLocalComputingAbility() * 0.68))) {
            res = 1d;
        }
        return res;
    }

    public Map<String, Double> calculateMap() {
        for (int i = 1; i <= iterations; i++) {
            r2 = Math.random();
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
        }
        Map<String, Double> res = new HashMap<>();
        res.put("res", updateMap.get("fg"));
        res.put("cost", packagingAccuracy(((updateMap.get("globalMax") * totalComputingDatas * mobileUser.getCyclesPerBit()) / edgeSettings.getMecComputingAbility()) * (edgeSettings.getMecComputingAbility() / 1e9) * COST));
        return res;
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

//    private double fEnergy(double sparrowIndex) {
//
//        //用户计算 1 bit数据所需CPU周期数
//        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
//        //用户本地计算能力
//        Float localComputingAbility = mobileUser.getLocalComputingAbility();
//        //      本地执行时间        任务上传时间      上传数据大小          本地执行能耗          上传能量
//        double localExeTime, uplinkTime, uplinkComputingData, localExeEnergy = 0.0d, uplinkEnergy = 0.0d;
////        for (int i = 0; i < totalComputingDatas.size(); i++) {
//        //上传数据大小
//        uplinkComputingData = totalComputingDatas * sparrowIndex;
//        //本地执行时间
//        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
//        double execTime = mobileUser.getExecTime();
//        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
//        mobileUser.setExecTime(localExeTime);
//
//        reFreshUpdatingUplinkRate();
//
//        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
//        mobileUser.setExecTime(execTime);
//        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
//        return packagingAccuracy(localExeEnergy + uplinkEnergy + uplinkComputingData * cyclesPerBit * Math.pow(edgeSettings.getMecComputingAbility(), 2) * 1e-22);
//    }

    /**
     * 同时考虑时间和能耗
     *
     * @param sparrowIndex
     * @return
     */
    private double fCost(double sparrowIndex) {
        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        //用户本地计算能力
        Float localComputingAbility = mobileUser.getLocalComputingAbility();
        //    本地执行时间    卸载时间      任务上传时间      上传数据大小          本地执行能耗          上传能量
        double localExeTime, uplinkTime, uplinkComputingData, localExeEnergy, uplinkEnergy;
        //上传数据大小
        uplinkComputingData = totalComputingDatas * sparrowIndex;
        //本地执行时间
        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(PRECISION, BigDecimal.ROUND_HALF_UP).doubleValue();
        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
        mobileUser.setExecTime(localExeTime);
        double execTime = mobileUser.getExecTime();
        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
        reFreshUpdatingUplinkRate();
        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(PRECISION, BigDecimal.ROUND_HALF_UP).doubleValue();
        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
        double totalTime = localExeTime + uplinkTime;
        mobileUser.setExecTime(execTime);
        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);

        double p1 = mobileUser.getAlpha() * (totalTime / (totalComputingDatas * cyclesPerBit / localComputingAbility));
        double p2 = mobileUser.getBeta() * ((localExeEnergy + uplinkEnergy) / (totalComputingDatas * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22));
        return packagingAccuracy(p1 + p2);
    }
}
