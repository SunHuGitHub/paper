package com.tiger.paper;

import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.bcel.internal.generic.NEW;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Tiger
 * @date 2021/4/12 21:43
 */
public class SA {
    /**
     * 初始温度
     */
    private double t0;
    /**
     * 结束温度
     */
    private double tEnd;

    /**
     * 退火系数
     */
    private double q;

    /**
     * 迭代次数
     */
    private long l;
    private List<MobileUser> mobileUsers;

    private EdgeSettings edgeSettings;
    private MobileUser mobileUser;
    private MobileUser mobileUserTemp;

    public SA(double t0, double tEnd, double q, long l, MobileUser mobileUser, List<MobileUser> mobileUsers, EdgeSettings edgeSettings, Integer totalComputingDatas) {
        this.t0 = t0;
        this.tEnd = tEnd;
        this.q = q;
        this.l = l;
        this.mobileUsers = mobileUsers;
        this.edgeSettings = edgeSettings;
        this.mobileUser = mobileUser;
        this.mobileUserTemp = JSONObject.parseObject(JSONObject.toJSONString(mobileUser), MobileUser.class);
        this.totalComputingDatas = totalComputingDatas;
    }

    /**
     * 任务集合，每个任务单位为（bits）
     */
    private Integer totalComputingDatas;


    public double calculate() {
        return run(t0, tEnd, q, l, BigDecimal.valueOf(Math.random()).setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    /**
     * 主方法
     *
     * @param t0           初始温度
     * @param tEnd         结束温度
     * @param q            降温系数
     * @param l            迭代次数--退火速度
     * @param sparrowIndex 坐标
     */
    private double run(double t0, double tEnd, double q, long l, double sparrowIndex) {

        double t = t0;
        double df;
        double temp = sparrowIndex;
        while (t > tEnd) {
            //一直迭代
            for (long i = 0; i < l; i++) {
                double v1 = fTime(temp);
                double random;
                double val = temp;
                double v = temp;
                do {
                    random = BigDecimal.valueOf(Math.random() * 2 - 1).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    if (v + random < 1) {
                        val = v + random;
                    } else {
                        val = v - random;
                    }
                } while (val < 0 || val > 1);
//                mobileUser = JSONObject.parseObject(JSONObject.toJSONString(mobileUserTemp), MobileUser.class);
                double v2 = fTime(BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue());

                df = v2 - v1;
                // 这里是退火算法的精髓（以一定概率接受比现在差的，这样就跳出局部最优解趋于全局最优）
                // 新的路线比之前的要差（这里表现为新的路线长度比原先的长）
                // 专业术语：metropolis准则
                if (df >= 0) {
                    //表示接受新的移动
                    if (Math.exp((-df) / t) >= Math.random()) {
                        temp = BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    }
                } else {
                    temp = BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
                }
//                mobileUser = JSONObject.parseObject(JSONObject.toJSONString(mobileUserTemp), MobileUser.class);
            }
            t *= q;
        }
//        System.out.println(temp);
//        System.out.println(fTime(temp));
        return fTime(temp);
    }

//    private double fTime(double sparrowIndex) {
//        //拿到用户的总任务集合
////        List<Integer> totalComputingDatas = mobileUser.getTotalComputingDatas();
//        //用户计算 1 bit数据所需CPU周期数
//        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
//        //用户本地计算能力
//        Float localComputingAbility = mobileUser.getLocalComputingAbility();
//        //      本地执行时间    卸载时间      任务上传时间      上传数据大小          本地执行能耗          上传能量           trueTime = Math.max(localExeTime, offloadTime);
//        double localExeTime, offloadTime, uplinkTime, uplinkComputingData, localExeEnergy = 0.0d, uplinkEnergy = 0.0d, trueTime = 0.0d;
////        for (int i = 0; i < totalComputingDatas.size(); i++) {
//        //上传数据大小
//        uplinkComputingData = totalComputingDatas * sparrowIndex;
//        //本地执行时间
//        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
////        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
////        if (i != 0) {
//        // i 等于 0 表示 一开始卸载任务    i != 0 表示用户处于移动状态中  所以要重新刷新上传速率
////            reFreshUpdatingUplinkRate(mobileUser);
////            uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).doubleValue();
////        } else {
////        }
////        offloadTime = uplinkTime + BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).doubleValue();
////        trueTime = BigDecimal.valueOf(trueTime + Math.max(localExeTime, offloadTime)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double execTime = mobileUser.getExecTime();
//        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
//        mobileUser.setExecTime(localExeTime);
//
//        reFreshUpdatingUplinkRate();
//
//        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
////        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
//        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double totalTime = localExeTime + uplinkTime + edgeExecTime;
//        mobileUser.setExecTime(execTime);
//        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
////        }
//        return BigDecimal.valueOf(totalTime).setScale(2, RoundingMode.HALF_UP).doubleValue();
//    }

    //    private double fEnergy(double sparrowIndex) {
//        //拿到用户的总任务集合
////        List<Integer> totalComputingDatas = mobileUser.getTotalComputingDatas();
//        //用户计算 1 bit数据所需CPU周期数
//        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
//        //用户本地计算能力
//        Float localComputingAbility = mobileUser.getLocalComputingAbility();
//        //      本地执行时间    卸载时间      任务上传时间      上传数据大小          本地执行能耗          上传能量           trueTime = Math.max(localExeTime, offloadTime);
//        double localExeTime, offloadTime, uplinkTime, uplinkComputingData, localExeEnergy = 0.0d, uplinkEnergy = 0.0d, trueTime = 0.0d;
////        for (int i = 0; i < totalComputingDatas.size(); i++) {
//        //上传数据大小
//        uplinkComputingData = totalComputingDatas * sparrowIndex;
//        //本地执行时间
//        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
////        if (i != 0) {
//        // i 等于 0 表示 一开始卸载任务    i != 0 表示用户处于移动状态中  所以要重新刷新上传速率
////            reFreshUpdatingUplinkRate(mobileUser);
////            uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).doubleValue();
////        } else {
////        }
////        offloadTime = uplinkTime + BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).doubleValue();
////        trueTime = BigDecimal.valueOf(trueTime + Math.max(localExeTime, offloadTime)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double execTime = mobileUser.getExecTime();
//        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
//        mobileUser.setExecTime(localExeTime);
//
//        reFreshUpdatingUplinkRate();
//
//        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
////        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
////        double totalTime = localExeTime + uplinkTime + edgeExecTime;
//        mobileUser.setExecTime(execTime);
//        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
////        }
//        return BigDecimal.valueOf(localExeEnergy + uplinkEnergy).setScale(2, RoundingMode.HALF_UP).doubleValue();
//    }
    private double fTime(double sparrowIndex) {
        //拿到用户的总任务集合
//        List<Integer> totalComputingDatas = mobileUser.getTotalComputingDatas();
        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        //用户本地计算能力
        Float localComputingAbility = mobileUser.getLocalComputingAbility();
        //      本地执行时间    卸载时间      任务上传时间      上传数据大小          本地执行能耗          上传能量           trueTime = Math.max(localExeTime, offloadTime);
        double localExeTime, offloadTime, uplinkTime, uplinkComputingData, localExeEnergy = 0.0d, uplinkEnergy = 0.0d, trueTime = 0.0d;
//        for (int i = 0; i < totalComputingDatas.size(); i++) {
        //上传数据大小
        uplinkComputingData = totalComputingDatas * sparrowIndex;
        //本地执行时间
        localExeTime = BigDecimal.valueOf(((totalComputingDatas - uplinkComputingData) * cyclesPerBit) / localComputingAbility).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
//        if (i != 0) {
        // i 等于 0 表示 一开始卸载任务    i != 0 表示用户处于移动状态中  所以要重新刷新上传速率
//            reFreshUpdatingUplinkRate(mobileUser);
//            uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).doubleValue();
//        } else {
//        }
//        offloadTime = uplinkTime + BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).doubleValue();
//        trueTime = BigDecimal.valueOf(trueTime + Math.max(localExeTime, offloadTime)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        double execTime = mobileUser.getExecTime();
        Double updatingUplinkRate = mobileUser.getUpdatingUplinkRate();
        mobileUser.setExecTime(localExeTime);

        reFreshUpdatingUplinkRate();

        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        double totalTime = localExeTime + uplinkTime + edgeExecTime;
        mobileUser.setExecTime(execTime);
        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
//        }
        return BigDecimal.valueOf(totalTime).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * @return 刷新上传速率
     */
    private void reFreshUpdatingUplinkRate() {
        double execTime = mobileUser.getExecTime();
        double sumW = BigDecimal.valueOf(0.0d).doubleValue();

        for (MobileUser user : mobileUsers) {
//            if (mobileUser.getId().intValue() != user.getId().intValue()) {
            sumW += BigDecimal.valueOf(user.getTransPower() * BigDecimal.valueOf(Math.pow(user.getDistance(), -edgeSettings.getEta())).doubleValue()).doubleValue();
//                sumW += BigDecimal.valueOf(user.getTransPower() * BigDecimal.valueOf(Math.pow(calculateDistance(user, execTime), -edgeSettings.getEta())).doubleValue()).doubleValue();
//            }
        }
        //根据用户的执行时间 计算出离基站的距离
        int distance = calculateDistance(execTime);

//        mobileUser.getUplinkDistance().add(distance);

        //上传速率公式为香农公式  见 上传速率公式.png
        if (edgeSettings.getBackgroundNoisePower() > 0) {
            //backgroundNoisePower为 W 时
            mobileUser.setUpdatingUplinkRate(BigDecimal.valueOf(edgeSettings.getBandwidth() *
                    BigDecimal.valueOf((BigDecimal.valueOf(Math.log(1 + (BigDecimal.valueOf(mobileUser.getTransPower() * BigDecimal.valueOf(Math.pow(distance, -edgeSettings.getEta())).doubleValue())).doubleValue()
                            / BigDecimal.valueOf((edgeSettings.getBackgroundNoisePower() + sumW)).doubleValue())).doubleValue()
                            / BigDecimal.valueOf(Math.log(2)).doubleValue())).doubleValue()).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue());
        } else {
            //backgroundNoisePower 为 dbm 时
            mobileUser.setUpdatingUplinkRate(BigDecimal.valueOf(edgeSettings.getBandwidth() *
                    (BigDecimal.valueOf(Math.log(1 + (BigDecimal.valueOf(mobileUser.getTransPower() * BigDecimal.valueOf(Math.pow(distance, -edgeSettings.getEta())).doubleValue()).doubleValue())
                            / BigDecimal.valueOf((BigDecimal.valueOf(Math.pow(10, edgeSettings.getBackgroundNoisePower() / 10.0)).doubleValue() / 1000 + sumW)).doubleValue())).doubleValue()
                            / BigDecimal.valueOf(Math.log(2)).doubleValue())).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue());
        }
    }

    /**
     * @param execTime 执行时间
     * @return 计算离基站多远
     */
    private int calculateDistance(double execTime) {
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
        float startingPoint = Float.parseFloat(mobileMap.get("startingPoint").toString());
        float speed = Float.parseFloat(mobileMap.get("speed").toString());
        double time = execTime - timeNum;
        return (int) (startingPoint + sign * speed * time);
//        int degree = Integer.parseInt(mobileMap.get("degree").toString());
//        return (int) Math.sqrt(Math.pow(startingPoint, 2) + Math.pow(speed * time, 2) - 2 * startingPoint * speed * time * Double.valueOf(Math.cos(Math.toRadians(degree))));

    }
}
