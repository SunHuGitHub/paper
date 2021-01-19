package com.tiger.paper;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author 孙小虎
 * @date 2021/1/18 - 22:18
 */
public class MECRunner {
    /**
     * 移动用户集合
     */
    private static List<MobileUser> mobileUsers;
    /**
     * 边缘服务器
     */
    private static EdgeSettings edgeSettings;
    /**
     * 任务数量
     */
    private static final int TASKNUM = 5;
    /**
     * 移动用户个数
     */
    private static final int USERNUM = 10;
    /**
     * 格式化小数点
     */
    private static DecimalFormat df;

    public static void main(String[] args) {

        //带宽 1MHZ  背景噪声 -100dbm MEC计算能力 5GHZ  路径衰落因子 4  边缘服务器基站范围
        edgeSettings = new EdgeSettings(3e6f, -100f, 5e9f, 4, 500);
        mobileUsers = new ArrayList<>();
        df = new DecimalFormat("0.00");
        //本模型是最小化每个移动用户的costFuntion 即 min costFuntion()
        //任务数据量 单位KB
        int[] taskDataSize = {300, 400, 500, 600};
        int[] cyclesPerBit = {1000, 1200, 1500};
        //本地计算能力 0.5GHZ  0.8GHZ  1GHZ
        float[] localComputingAbility = {0.5e9f, 0.8e9f, 1.0e9f};
        //传输功率 0.05w  0.08w  0.1w
        float[] transPower = {0.05f, 0.08f, 0.1f};
        //移动用户对时间的权重 最大为 10。已知时间权重，能耗权重就为 10 - X
        int[] alpha = {2, 5, 8};
        //离基站距离 假设小型基站范围500米  使用户随机在离基站100到400米之间
        int minDistance = edgeSettings.getSignalRange() - 400;
        int maxDistance = edgeSettings.getSignalRange() - 100;

        Random random = new Random();

        int alphaIndex = random.nextInt(alpha.length);
        //任务集合
        List<Integer> totalComputingDatas = new ArrayList<>();
        for (int i = 1; i <= TASKNUM; i++) {
            //1KB = 1024B = 1024 * 8 = 8192b
            totalComputingDatas.add(taskDataSize[random.nextInt(taskDataSize.length)] * 8192);
        }

        for (int i = 1; i <= USERNUM; i++) {
            //随机每个用户离基站的距离
            float distance = (random.nextInt(maxDistance - minDistance + 1) + minDistance) * 1.0f;
            mobileUsers.add(new MobileUser(i, totalComputingDatas,
                    distance,
                    cyclesPerBit[random.nextInt(cyclesPerBit.length)],
                    localComputingAbility[random.nextInt(localComputingAbility.length)],
                    transPower[random.nextInt(transPower.length)],
                    1.0f,
                    alpha[alphaIndex] * 0.1f,
                    (10 - alpha[alphaIndex]) * 0.1f,
                    (int) distance));

        }

        //初始化所有移动用户的上传速率
        initMobileUser();
//        //初始化移动用户的 Random Walk Model 即预定路线
//        initMobileConf();
//
//        double t = 2.12d;
//        mobileUsers.get(0).setExecTime(t);
//        reFreshMobileUplinkRate(mobileUsers.get(0));


    }

    /**
     * 初始化各个移动用户的上传速率
     */
    private static void initMobileUser() {
//        for (MobileUser mobileUser : mobileUsers) {
//            mobileUser.setUplinkRate(getUplinkRate(mobileUser));
//        }
        //这里只关心第一个用户
        MobileUser mobileUser = mobileUsers.get(0);
        mobileUser.setUplinkRate(getUplinkRate(mobileUser));
    }

    /**
     * @param mobileUser 当前移动用户
     * @return 一开始移动用户的上传速率（保留两位有效数字）
     */
    private static double getUplinkRate(MobileUser mobileUser) {
        double sumW = BigDecimal.valueOf(0.0d).doubleValue();
        for (MobileUser user : mobileUsers) {
            if (mobileUser.getId().intValue() != user.getId().intValue()) {
                sumW += BigDecimal.valueOf(user.getTransPower() * BigDecimal.valueOf(Math.pow(user.getDistance(), -edgeSettings.getEta())).doubleValue()).doubleValue();
            }
        }
        //上传速率公式为香农公式  见 上传速率公式.png
        if (edgeSettings.getBackgroundNoisePower() > 0) {
            //backgroundNoisePower为 W 时
            return BigDecimal.valueOf(edgeSettings.getBandwidth() *
                    BigDecimal.valueOf((BigDecimal.valueOf(Math.log(1 + (BigDecimal.valueOf(mobileUser.getTransPower() * BigDecimal.valueOf(Math.pow(mobileUser.getDistance(), -edgeSettings.getEta())).doubleValue())).doubleValue()
                            / BigDecimal.valueOf((edgeSettings.getBackgroundNoisePower() + sumW)).doubleValue())).doubleValue()
                            / BigDecimal.valueOf(Math.log(2)).doubleValue())).doubleValue()).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue();
        } else {
            //backgroundNoisePower 为 dbm 时
            return BigDecimal.valueOf(edgeSettings.getBandwidth() *
                    (BigDecimal.valueOf(Math.log(1 + (BigDecimal.valueOf(mobileUser.getTransPower() * BigDecimal.valueOf(Math.pow(mobileUser.getDistance(), -edgeSettings.getEta())).doubleValue()).doubleValue())
                            / BigDecimal.valueOf((BigDecimal.valueOf(Math.pow(10, edgeSettings.getBackgroundNoisePower() / 10.0)).doubleValue() / 1000 + sumW)).doubleValue())).doubleValue()
                            / BigDecimal.valueOf(Math.log(2)).doubleValue())).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
    }

    private static void SSA() {

    }
}
