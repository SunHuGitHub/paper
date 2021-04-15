package com.tiger.paper;

import com.alibaba.fastjson.JSONObject;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author Tiger
 * @date 2021/4/14 8:36
 */
@NoArgsConstructor
public class SSASA {

    /**
     * 种群数
     */
    private int speciesNum;
    /**
     * 最大迭代次数
     */
    private int iterations;
    /**
     * 生产者--producer
     */
    private int PD;
    /**
     * 意识到危险的麻雀数量
     */
    private int SD;
    /**
     * 生产者警戒阈值
     */
    private float ST;
    /**
     * 随机生产者警戒阈值
     */
    private float r2 = BigDecimal.valueOf(0.0f).floatValue();
    /**
     * 随机向量方向 1表示正方向 即正步长  -1表示负方向 即负步长
     */
    private int[] A = {1, -1};
    /**
     * 每次迭代，map里面的最优值 最差值都会更新
     */
    private Map<String, BigDecimal> updateMap;
    /**
     * 随机生成器
     */
    private final Random RANDOM = new Random();
    /**
     * 麻雀的坐标点
     */
    private List<Double> coordinatePoints;

    private List<MobileUser> mobileUsers;

    private EdgeSettings edgeSettings;

    /**
     * 任务集合，每个任务单位为（bits）
     */
    private Integer totalComputingDatas;

    private MobileUser mobileUser;
    private MobileUser mobileUserTemp;
    /**
     * 退火系数
     */
    private static double q = BigDecimal.valueOf(0.9d).doubleValue();
    /**
     * 初始温度
     */
    private static double t0 = BigDecimal.valueOf(100.0d).doubleValue();
    /**
     * 退火循环次数
     */
    private static final int LOOP = 100;

    public SSASA(int speciesNum, int iterations, float PDRatio, float SDRatio, float STRatio, MobileUser mobileUser, List<MobileUser> mobileUsers, EdgeSettings edgeSettings, Integer totalComputingDatas) {
        this.speciesNum = speciesNum;
        this.iterations = iterations;
        this.PD = (int) (speciesNum * PDRatio);
        this.SD = (int) (speciesNum * SDRatio);
        this.ST = BigDecimal.valueOf(STRatio).floatValue();
        this.coordinatePoints = new ArrayList<>(speciesNum + 1);
        this.mobileUsers = mobileUsers;
        this.edgeSettings = edgeSettings;
        this.mobileUser = mobileUser;
        this.mobileUserTemp = JSONObject.parseObject(JSONObject.toJSONString(mobileUser), MobileUser.class);
        this.totalComputingDatas = totalComputingDatas;
        /**
         * 初始化麻雀种群坐标点 这里index是[1,speciesNum] 因为考虑到公式中的i不能为0
         */
        coordinatePoints.add(null);
        double sparrowIndex;
        //任务数量 即可知多少维度
//        int taskSize = mobileUsers.get(0).getTotalComputingDatas().size();
        for (int i = 1; i <= this.speciesNum; i++) {
//            sparrowIndex = new ArrayList<>();
//            for (int k = 0; k < taskSize; k++) {
            //这里设置每个麻雀的坐标 最大 1 表示卸载 100% 0表示卸载 0% 即不卸载
            double idx = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
            while (idx > 0.8d || idx < 0.2d) {
                idx = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
            }
            sparrowIndex = idx;
//            }
            coordinatePoints.add(sparrowIndex);
        }
        //上面初始化完成后 初始计算下
        rankAndFindLocation();
    }

    /**
     * 更新生产者的坐标 对应论文中的公式三
     */
    private void updateProducerPoint() {
        //(0,1]之间的随机值
        double r;
        //麻雀坐标
        double sparrowIndex;
        for (int i = 1; i <= PD; i++) {
            sparrowIndex = coordinatePoints.get(i);
            if (r2 < ST) {
                r = BigDecimal.valueOf(1 - Math.random()).doubleValue();
                sparrowIndex = BigDecimal.valueOf(sparrowIndex * Math.exp((-i) / BigDecimal.valueOf((r * iterations)).doubleValue())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                while (sparrowIndex < 0 || sparrowIndex > 1) {
//                    r = BigDecimal.valueOf(1 - Math.random()).doubleValue();
//                    sparrowIndex = BigDecimal.valueOf(sparrowIndex * Math.exp((-i) / BigDecimal.valueOf((r * iterations)).doubleValue())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    sparrowIndex = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            } else {
                //因为是1维 所以L为1
                sparrowIndex = BigDecimal.valueOf(sparrowIndex + RANDOM.nextGaussian()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                while (sparrowIndex < 0 || sparrowIndex > 1) {
//                    sparrowIndex = BigDecimal.valueOf(sparrowIndex + RANDOM.nextGaussian()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    sparrowIndex = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            }
            coordinatePoints.set(i, sparrowIndex);
        }
    }

    public void setTotalComputingDatas(Integer totalComputingDatas) {
        this.totalComputingDatas = totalComputingDatas;
    }

    /**
     * 更新追随者（追随者）的坐标 对应论文中的公式四
     */
    private void updateScroungerPoint() throws NumberFormatException {
        BigDecimal pdMax;
        //麻雀坐标
        double sparrowIndex;
        for (int i = PD + 1; i <= speciesNum; i++) {
            sparrowIndex = coordinatePoints.get(i);
            pdMax = updateMap.get("pdMax");
            if (i > speciesNum * 1.0 / 2) {

                sparrowIndex = BigDecimal.valueOf(RANDOM.nextGaussian() *
                        Math.exp((((updateMap.get("globalMin").doubleValue()) - sparrowIndex) / Math.pow(i, 2))))
                        .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                while (sparrowIndex < 0 || sparrowIndex > 1) {
//                    sparrowIndex = BigDecimal.valueOf(RANDOM.nextGaussian() *
//                            Math.exp((((updateMap.get("globalMin").doubleValue()) - sparrowIndex) / Math.pow(i, 2))))
//                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    sparrowIndex = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            } else {
                //一维情况下 A+ 要么是1 要么是-1  因为是1维 所以L为1
                sparrowIndex = BigDecimal.valueOf(pdMax.doubleValue() + BigDecimal.valueOf(Math.abs(sparrowIndex - pdMax.doubleValue())).doubleValue() * BigDecimal.valueOf(A[Math.random() > 0.5 ? 1 : 0] * 1).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                while (sparrowIndex < 0 || sparrowIndex > 1) {
//                    sparrowIndex = BigDecimal.valueOf(pdMax.doubleValue() + BigDecimal.valueOf(Math.abs(sparrowIndex - pdMax.doubleValue())).doubleValue() * BigDecimal.valueOf(A[Math.random() > 0.5 ? 1 : 0] * 1).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    sparrowIndex = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            }
            coordinatePoints.set(i, sparrowIndex);
        }
    }

    /**
     * 随机选择SD个个体进行预警行为 对应论文中的公式五
     */
    private void updateSDPoint() {
        ArrayList<Integer> sdIndex = new ArrayList<>(SD);
        //随机选择SD个 麻雀坐标索引
        for (int i = 1; i <= SD; i++) {
            //RANDOM.nextInt(speciesNum) 范围为[0,speciesNum) 但是coordinatePoints里咱们0号位置是空的 所以后面+1
            // 范围就是[1,speciesNum+1) 即 [1,speciesNum]
            int index = RANDOM.nextInt(speciesNum) + 1;
            while (sdIndex.contains(index)) {
                index = RANDOM.nextInt(speciesNum) + 1;
            }
            sdIndex.add(index);
        }
        for (int i = 0; i < sdIndex.size(); i++) {
            double sparrowIndex = coordinatePoints.get(sdIndex.get(i));
            double f = fEnergy(sparrowIndex);
            BigDecimal fg = updateMap.get("fg");
            BigDecimal fw = updateMap.get("fw");
            BigDecimal globalMax = updateMap.get("globalMax");
            BigDecimal globalMin = updateMap.get("globalMin");
            if (f > fg.doubleValue()) {
                //这里步长也是一个优化点。暂且还没优化
                //在寻优前期, 为了扩大在解空间整体的搜索范围, 加快寻优速度, 应该采用较大的步长因子；
                //在寻优后期, 搜索解趋于稳定, 为了使解的精度更高, 应该减小步长因子。
                //另外, 初始步长因子越小, 越容易陷入局部极值, 所以应给与较高的初始值, 如0.95
                sparrowIndex = BigDecimal.valueOf(globalMax.doubleValue() + BigDecimal.valueOf(RANDOM.nextGaussian() * BigDecimal.valueOf(Math.abs(sparrowIndex - globalMax.doubleValue())).doubleValue()).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                while (sparrowIndex < 0 || sparrowIndex > 1) {
//                    sparrowIndex = BigDecimal.valueOf(globalMax.doubleValue() + BigDecimal.valueOf(RANDOM.nextGaussian() * BigDecimal.valueOf(Math.abs(sparrowIndex - globalMax.doubleValue())).doubleValue()).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    sparrowIndex = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            } else if (BigDecimal.valueOf(Math.abs(f - fg.doubleValue())).doubleValue() < 1e-4) {
                sparrowIndex = BigDecimal.valueOf(sparrowIndex + BigDecimal.valueOf((2 * Math.random() - 1)).doubleValue() * BigDecimal.valueOf((BigDecimal.valueOf(Math.abs(sparrowIndex - globalMin.doubleValue())).doubleValue() / BigDecimal.valueOf((f - fw.doubleValue() + 1e-4)).doubleValue())).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                while (sparrowIndex < 0 || sparrowIndex > 1) {
//                    sparrowIndex = BigDecimal.valueOf(sparrowIndex + BigDecimal.valueOf((2 * Math.random() - 1)).doubleValue() * BigDecimal.valueOf((BigDecimal.valueOf(Math.abs(sparrowIndex - globalMin.doubleValue())).doubleValue() / BigDecimal.valueOf((f - fw.doubleValue() + 1e-4)).doubleValue())).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    sparrowIndex = BigDecimal.valueOf((RANDOM.nextInt(101) * 0.01)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            }
            coordinatePoints.set(i, sparrowIndex);
        }

    }

    /**
     * 找出生产者中最好的那个点  以及全局最差点，全局最优点，最优适应度，最差适应度
     */
    private void rankAndFindLocation() {
        double pdMax = coordinatePoints.get(1);
        double globalMin = pdMax;
        double globalMax = pdMax;
        double sparrowIndex;
        double f;
        double fg;
        double fw;
        for (int i = 2; i <= speciesNum; i++) {
            sparrowIndex = coordinatePoints.get(i);
            f = fEnergy(sparrowIndex);
            if (i <= PD && fEnergy(pdMax) < f) {
                pdMax = sparrowIndex;
            }
            if (fEnergy(globalMin) < f) {
                globalMin = sparrowIndex;
            }
            if (fEnergy(globalMax) > f) {
                globalMax = sparrowIndex;
            }
        }
        fg = fEnergy(globalMax);
        fw = fEnergy(globalMin);
        updateMap = new HashMap<>(16);
        //生产者最优的点
        updateMap.put("pdMax", BigDecimal.valueOf(pdMax));
        //全局最差点
        updateMap.put("globalMin", BigDecimal.valueOf(globalMin));
        //全局最优点
        updateMap.put("globalMax", BigDecimal.valueOf(globalMax));
        //最优的适度度
        updateMap.put("fg", BigDecimal.valueOf(fg));
        //最差的适度度
        updateMap.put("fw", BigDecimal.valueOf(fw));
    }

    /**
     * 适应度函数
     */
//    private double f(double sparrowIndex) {
//        MobileUser mobileUser = mobileUsers.get(0);
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
//        mobileUser.setExecTime(localExeTime);
//        reFreshUpdatingUplinkRate(mobileUser);
//        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
//        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double totalTime = localExeTime + uplinkTime + edgeExecTime;
////        }
//        return BigDecimal.valueOf(mobileUser.getAlpha() * totalTime + mobileUser.getBeta() * (localExeEnergy + uplinkEnergy)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//    }
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
//        reFreshUpdatingUplinkRate();
//        uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
////        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
//        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double totalTime = localExeTime + uplinkTime + edgeExecTime;
//        mobileUser.setExecTime(execTime);
//        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
////        }
//        return BigDecimal.valueOf(totalTime).setScale(2, RoundingMode.HALF_UP).doubleValue();
//    }

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
            ;
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
        double time = execTime - timeNum;
        float startingPoint = Float.parseFloat(mobileMap.get("startingPoint").toString());
        float speed = Float.parseFloat(mobileMap.get("speed").toString());
        return (int) (startingPoint + sign * speed * time);
//        int degree = Integer.parseInt(mobileMap.get("degree").toString());
//        return (int) Math.sqrt(Math.pow(startingPoint, 2) + Math.pow(speed * time, 2) - 2 * startingPoint * speed * time * Double.valueOf(Math.cos(Math.toRadians(degree))));
    }


    public double calculate() {
        //麻雀算法迭代次数
        double t0Temp = t0;
        int t = 1;
        double lastFg;
//        System.out.println("初始最优点：" + (updateMap.get("globalMax")));
//        System.out.println("初始最优适应度：" + lastFg);
        double pdMax;
        double temp;
        double df;
        int l = 1;
        List<Double> coordinatePointsLast;
//        HashMap<String, BigDecimal> updateMapLast;
        List<Double> SDTemp;
        while (t <= iterations) {
            do {
                lastFg = updateMap.get("fg").doubleValue();
                SDTemp = new ArrayList<>();
                for (int i = PD + 1; i <= speciesNum; i++) {
                    SDTemp.add(coordinatePoints.get(i));
                }
//                coordinatePointsLast = JSONObject.parseArray(JSONObject.toJSONString(coordinatePoints), Double.class);
//                updateMapLast = JSONObject.parseObject(JSONObject.toJSONString(updateMap), new HashMap<String, BigDecimal>().getClass());
                r2 = BigDecimal.valueOf(Math.random()).floatValue();
                updateProducerPoint();
                pdMax = coordinatePoints.get(1);
                for (int i = 2; i <= PD; i++) {
                    temp = coordinatePoints.get(i);
                    if (fEnergy(pdMax) > fEnergy(temp)) {
                        pdMax = temp;
                    }
                }
                updateMap.put("pdMax", BigDecimal.valueOf(pdMax));
                try {
                    updateScroungerPoint();
                } catch (NumberFormatException numberFormatException) {
                    continue;
                }
                updateSDPoint();
                //更新完所有的位置后，重新算下最优值等
                rankAndFindLocation();
                //模拟退火思想
                df = updateMap.get("fg").doubleValue() - lastFg;
                // > 0 表示当前迭代的适应度值比上一次的大 即当前迭代是较差的解
                if (df > 0) {
                    // < 表示不接受这个较差的解  >= 表示接受  此概率受到温度参数的影响, 其值的大小随温度的下降而逐渐减小，使得算法在前期有较大概率跳出局部极值, 而在后期又能具有较高的收敛速度
                    if (Math.exp((-df) / t0) < Math.random()) {
                        //不接收较差的解 所以回滚之前的坐标值
                        for (int i = PD + 1; i <= speciesNum; i++) {
                            coordinatePoints.set(i, SDTemp.get(i - 1 - PD));
                        }
//                        coordinatePoints = coordinatePointsLast;
                        rankAndFindLocation();
//                        updateMap = updateMapLast;
                    }
                }
//                l++;
                t0 *= q;
            } while (t0 >= 1d);
            t0 = t0Temp;
            t++;
        }
//        System.out.println("迭代完成后最优点：" + updateMap.get("globalMax"));
//        System.out.println("迭代完成后最优适应度：" + updateMap.get("fg"));
        return updateMap.get("fg").doubleValue();
    }

    private double fEnergy(double sparrowIndex) {
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
        localExeEnergy = (totalComputingDatas - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
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
        uplinkEnergy = mobileUser.getTransPower() * uplinkTime;
//        double edgeExecTime = BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//        double totalTime = localExeTime + uplinkTime + edgeExecTime;
        mobileUser.setExecTime(execTime);
        mobileUser.setUpdatingUplinkRate(updatingUplinkRate);
//        }
        return BigDecimal.valueOf(localExeEnergy + uplinkEnergy).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
