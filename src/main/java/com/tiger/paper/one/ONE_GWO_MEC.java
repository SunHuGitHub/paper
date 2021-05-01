package com.tiger.paper.one;

import com.tiger.paper.EdgeSettings;
import com.tiger.paper.MobileUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author Tiger
 * @date 2021/4/27 22:30
 */
public class ONE_GWO_MEC {
    /**
     * 控制小数点后几位的精度
     */
    private static final int PRECISION = 4;
    /**
     * cent/gigahertz   分/千兆赫 -> 6/1G
     */
    private static final int COST = 6;
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
    /**
     * 任务，每个任务单位为（bits）
     */
    private Long totalComputingDatas;

    private List<MobileUser> mobileUsers;

    private MobileUser mobileUser;

    private EdgeSettings edgeSettings;

    public ONE_GWO_MEC(int speciesNum, int iterations, MobileUser mobileUser, List<MobileUser> mobileUsers, EdgeSettings edgeSettings, Long totalComputingDatas) {
        this.speciesNum = speciesNum;
        this.iterations = iterations;
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
    }

    private void rank() {
        List<Double> temp = new ArrayList<>(coordinatePoints);
        temp.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(fEnergy(o1), fEnergy(o2));
            }
        });
        this.alph = temp.get(0);
        this.beta = temp.get(1);
        this.seta = temp.get(2);
    }

//    private double f(double x) {
//        return packagingAccuracy(Math.pow(x - 0.8, 2) + 2);
//    }

    public double calculate() {
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
                    X1 = alph - A1 * Math.abs(C1 - coordinatePoints.get(z));
                } while (X1 < 0 || X1 > 1);

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A2 = 2 * a * rand1 - a;
                    C2 = 2 * rand2;
                    X2 = beta - A2 * Math.abs(C2 - coordinatePoints.get(z));
                } while (X2 < 0 || X2 > 1);

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A3 = 2 * a * rand1 - a;
                    C3 = 2 * rand2;
                    X3 = seta - A3 * Math.abs(C3 - coordinatePoints.get(z));
                } while (X3 < 0 || X3 > 1);
                coordinatePoints.set(z, (X1 + X2 + X3) / 3);
            }
        }
        rank();
        return fEnergy(alph);
    }
    public Map<String,Double> calculateMap() {
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
                    X1 = alph - A1 * Math.abs(C1 - coordinatePoints.get(z));
                } while (X1 < 0 || X1 > 1);

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A2 = 2 * a * rand1 - a;
                    C2 = 2 * rand2;
                    X2 = beta - A2 * Math.abs(C2 - coordinatePoints.get(z));
                } while (X2 < 0 || X2 > 1);

                do {
                    rand1 = Math.random();
                    rand2 = Math.random();
                    A3 = 2 * a * rand1 - a;
                    C3 = 2 * rand2;
                    X3 = seta - A3 * Math.abs(C3 - coordinatePoints.get(z));
                } while (X3 < 0 || X3 > 1);
                coordinatePoints.set(z, (X1 + X2 + X3) / 3);
            }
        }
        rank();
        Map<String, Double> res = new HashMap<>();
        res.put("res", fEnergy(alph));
        res.put("cost", packagingAccuracy(((alph * totalComputingDatas * mobileUser.getCyclesPerBit()) / edgeSettings.getMecComputingAbility()) * (edgeSettings.getMecComputingAbility() / 1e9) * COST));
        return res;
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

    private double fEnergy(double sparrowIndex) {

        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        //用户本地计算能力
        Float localComputingAbility = mobileUser.getLocalComputingAbility();
        //      本地执行时间        任务上传时间      上传数据大小          本地执行能耗          上传能量
        double localExeTime, uplinkTime, uplinkComputingData, localExeEnergy = 0.0d, uplinkEnergy = 0.0d;
//        for (int i = 0; i < totalComputingDatas.size(); i++) {
        //上传数据大小
        uplinkComputingData = totalComputingDatas * sparrowIndex;
        //本地执行时间
        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
        double execTime = mobileUser.getExecTime();
        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
        mobileUser.setExecTime(localExeTime);

        reFreshUpdatingUplinkRate();

        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
        mobileUser.setExecTime(execTime);
        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
        return packagingAccuracy(localExeEnergy + uplinkEnergy + uplinkComputingData * cyclesPerBit * Math.pow(edgeSettings.getMecComputingAbility(), 2) * 1e-22);
    }

//    private double fTime(double sparrowIndex) {
//
//        //用户计算 1 bit数据所需CPU周期数
//        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
//        //用户本地计算能力
//        Float localComputingAbility = mobileUser.getLocalComputingAbility();
//        //      本地执行时间      任务上传时间      上传数据大小
//        double localExeTime, uplinkTime, uplinkComputingData;
//
//        //上传数据大小
//        uplinkComputingData = totalComputingDatas * sparrowIndex;
//        //本地执行时间
//        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//
//        double execTime = mobileUser.getExecTime();
//        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
//        mobileUser.setExecTime(localExeTime);
//        reFreshUpdatingUplinkRate();
//        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//
//        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double totalTime = localExeTime + uplinkTime + edgeExecTime;
//        mobileUser.setExecTime(execTime);
//        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
//
//        return packagingAccuracy(totalTime);
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
