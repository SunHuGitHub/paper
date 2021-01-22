package com.tiger.paper;


import com.alibaba.fastjson.JSONObject;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author 孙小虎
 * @date 2021/01/14 - 14:42
 */
@NoArgsConstructor
public class SSA {

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
    private Map<String, Object> updateMap;
    /**
     * 随机生成器
     */
    private final Random RANDOM = new Random();
    /**
     * 麻雀的坐标点
     */
    private List<List<Double>> coordinatePoints;

    private List<MobileUser> mobileUsers;

    private EdgeSettings edgeSettings;


    public SSA(int speciesNum, int iterations, float PDRatio, float SDRatio, float STRatio, List<MobileUser> mobileUsers, EdgeSettings edgeSettings) {
        this.speciesNum = speciesNum;
        this.iterations = iterations;
        this.PD = (int) (speciesNum * PDRatio);
        this.SD = (int) (speciesNum * SDRatio);
        this.ST = BigDecimal.valueOf(STRatio).floatValue();
        this.coordinatePoints = new ArrayList<>(speciesNum + 1);
        this.mobileUsers = mobileUsers;
        this.edgeSettings = edgeSettings;
        /**
         * 初始化麻雀种群坐标点 这里index是[1,speciesNum] 因为考虑到公式中的i不能为0
         */
        coordinatePoints.add(null);
        List<Double> sparrowIndex;
        //任务数量 即可知多少维度
        int taskSize = mobileUsers.get(0).getTotalComputingDatas().size();
        for (int i = 1; i <= this.speciesNum; i++) {
            sparrowIndex = new ArrayList<>();
            for (int k = 0; k < taskSize; k++) {
                //这里设置每个麻雀的坐标 最大 1 表示卸载 100% 0表示卸载 0% 即不卸载
                sparrowIndex.add((RANDOM.nextInt(101) * 0.01));
            }
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
        List<Double> sparrowIndex = null;
        for (int i = 1; i <= PD; i++) {
            sparrowIndex = coordinatePoints.get(i);
            if (r2 < ST) {
                r = BigDecimal.valueOf(1 - Math.random()).doubleValue();
                for (int k = 0; k < sparrowIndex.size(); k++) {
                    sparrowIndex.add(k, BigDecimal.valueOf(sparrowIndex.get(k) * Math.exp((-i) / BigDecimal.valueOf((r * iterations)).doubleValue())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                }
            } else {
                //因为是1维 所以L为1
                for (int k = 0; k < sparrowIndex.size(); k++) {
                    sparrowIndex.add(k, BigDecimal.valueOf(sparrowIndex.get(k) + RANDOM.nextGaussian()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                }
            }
        }
    }

    /**
     * 更新追随者（追随者）的坐标 对应论文中的公式四
     */
    private void updateScroungerPoint() throws NumberFormatException {
        List<Double> pdMax;
        //麻雀坐标
        List<Double> sparrowIndex = null;
        for (int i = PD + 1; i <= speciesNum; i++) {
            sparrowIndex = coordinatePoints.get(i);
            if (i > speciesNum * 1.0 / 2) {
                for (int k = 0; k < sparrowIndex.size(); k++) {
                    sparrowIndex.add(k,BigDecimal.valueOf(RANDOM.nextGaussian() *
                            Math.exp(((((List<Double>) updateMap.get("globalMin")).get(k) - sparrowIndex.get(k)) / Math.pow(i, 2))))
                            .setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
                }
            } else {
                pdMax = (List<Double>) updateMap.get("pdMax");
                //一维情况下 A+ 要么是1 要么是-1  因为是1维 所以L为1
                map.put("x", BigDecimal.valueOf(pdMax.doubleValue() + BigDecimal.valueOf(Math.abs(map.get("x").doubleValue() - pdMax.doubleValue())).doubleValue() * BigDecimal.valueOf(A[Math.random() > 0.5 ? 1 : 0] * 1).doubleValue()));
            }
            coordinatePoints.set(i, map);
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
            Map<String, BigDecimal> map = coordinatePoints.get(sdIndex.get(i));
            BigDecimal x = map.get("x");
            BigDecimal f = f(x);
            BigDecimal fg = updateMap.get("fg");
            BigDecimal fw = updateMap.get("fw");
            BigDecimal globalMax = updateMap.get("globalMax");
            BigDecimal globalMin = updateMap.get("globalMin");
            if (f.doubleValue() > fg.doubleValue()) {
                //这里步长也是一个优化点。暂且还没优化
                //在寻优前期, 为了扩大在解空间整体的搜索范围, 加快寻优速度, 应该采用较大的步长因子；
                //在寻优后期, 搜索解趋于稳定, 为了使解的精度更高, 应该减小步长因子。
                //另外, 初始步长因子越小, 越容易陷入局部极值, 所以应给与较高的初始值, 如0.95
                map.put("x", BigDecimal.valueOf(globalMax.doubleValue() + BigDecimal.valueOf(RANDOM.nextGaussian() * BigDecimal.valueOf(Math.abs(x.doubleValue() - globalMax.doubleValue())).doubleValue()).doubleValue()));
                coordinatePoints.set(i, map);
            } else if (BigDecimal.valueOf(Math.abs(f.doubleValue() - fg.doubleValue())).doubleValue() < 1e-15) {
                map.put("x", BigDecimal.valueOf(x.doubleValue() + BigDecimal.valueOf((2 * Math.random() - 1)).doubleValue() * BigDecimal.valueOf((BigDecimal.valueOf(Math.abs(x.doubleValue() - globalMin.doubleValue())).doubleValue() / BigDecimal.valueOf((f.doubleValue() - fw.doubleValue() + 1e-50)).doubleValue())).doubleValue()));
                coordinatePoints.set(i, map);
            }
        }

    }

    /**
     * 找出生产者中最好的那个点  以及全局最差点，全局最优点，最优适应度，最差适应度
     */
    private void rankAndFindLocation() {
        List<Double> pdMax = coordinatePoints.get(1);
        List<Double> globalMin = pdMax;
        List<Double> globalMax = pdMax;
        List<Double> sparrowIndex;
        double f;
        double fg;
        double fw;
        for (int i = 2; i <= speciesNum; i++) {
            sparrowIndex = coordinatePoints.get(i);
            f = f(sparrowIndex);
            if (i <= PD && f(pdMax) < f) {
                pdMax = sparrowIndex;
            }
            if (f(globalMin) > f) {
                globalMin = sparrowIndex;
            }
            if (f(globalMax) < f) {
                globalMax = sparrowIndex;
            }
        }
        fg = f(globalMax);
        fw = f(globalMin);
        updateMap = new HashMap<>(16);
        //生产者最优的点
        updateMap.put("pdMax", pdMax);
        //全局最差点
        updateMap.put("globalMin", globalMin);
        //全局最优点
        updateMap.put("globalMax", globalMax);
        //最优的适度度
        updateMap.put("fg", fg);
        //最差的适度度
        updateMap.put("fw", fw);
    }

    /**
     * 适应度函数
     */
    private double f(List<Double> sparrowIndex) {
        MobileUser mobileUser = mobileUsers.get(0);
        //拿到用户的总任务集合
        List<Integer> totalComputingDatas = mobileUser.getTotalComputingDatas();
        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        //用户本地计算能力
        Float localComputingAbility = mobileUser.getLocalComputingAbility();
        //      本地执行时间    卸载时间      任务上传时间      上传数据大小          本地执行能耗          上传能量           trueTime = Math.max(localExeTime, offloadTime);
        double localExeTime, offloadTime, uplinkTime, uplinkComputingData, localExeEnergy = 0.0d, uplinkEnergy = 0.0d, trueTime = 0.0d;
        for (int i = 0; i < totalComputingDatas.size(); i++) {
            //上传数据大小
            uplinkComputingData = totalComputingDatas.get(i) * sparrowIndex.get(i);
            //本地执行时间
            localExeTime = BigDecimal.valueOf(((totalComputingDatas.get(i) - uplinkComputingData) * cyclesPerBit) / localComputingAbility).doubleValue();
            localExeEnergy += (totalComputingDatas.get(i) - uplinkComputingData) * cyclesPerBit * Math.pow(mobileUser.getLocalComputingAbility(), 2) * 1e-22;
            if (i != 0) {
                // i 等于 0 表示 一开始卸载任务    i != 0 表示用户处于移动状态中  所以要重新刷新上传速率
                reFreshUpdatingUplinkRate(mobileUser);
                uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getUpdatingUplinkRate()).doubleValue();
            } else {
                uplinkTime = BigDecimal.valueOf(uplinkComputingData / mobileUser.getInitUplinkRate()).doubleValue();
            }
            offloadTime = uplinkTime + BigDecimal.valueOf(uplinkComputingData * cyclesPerBit / edgeSettings.getMecComputingAbility()).doubleValue();
            uplinkEnergy += mobileUser.getTransPower() * uplinkTime;
            trueTime = BigDecimal.valueOf(trueTime + Math.max(localExeTime, offloadTime)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            mobileUser.setExecTime(trueTime);
        }
        return BigDecimal.valueOf(mobileUser.getAlpha() * trueTime + mobileUser.getBeta() * (localExeEnergy + uplinkEnergy)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @param mobileUser 当前移动用户
     * @return 刷新上传速率
     */
    private void reFreshUpdatingUplinkRate(MobileUser mobileUser) {
        double execTime = mobileUser.getExecTime();
        double sumW = BigDecimal.valueOf(0.0d).doubleValue();

        for (MobileUser user : mobileUsers) {
            if (mobileUser.getId().intValue() != user.getId().intValue()) {
                sumW += BigDecimal.valueOf(user.getTransPower() * BigDecimal.valueOf(Math.pow(user.getDistance(), -edgeSettings.getEta())).doubleValue()).doubleValue();
//                sumW += BigDecimal.valueOf(user.getTransPower() * BigDecimal.valueOf(Math.pow(calculateDistance(user, execTime), -edgeSettings.getEta())).doubleValue()).doubleValue();
            }
        }
        //根据用户的执行时间 计算出离基站的距离
        int distance = calculateDistance(mobileUser, execTime);

        mobileUser.getUplinkDistance().add(distance);

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
     * @param mobileUser 当前移动用户
     * @param execTime   执行时间
     * @return 计算离基站多远
     */
    private static int calculateDistance(MobileUser mobileUser, double execTime) {
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
        Map<String, Object> mobileMap = mobileConf.get(i);
        float startingPoint = Float.parseFloat(mobileMap.get("startingPoint").toString());
        float speed = Float.parseFloat(mobileMap.get("speed").toString());
        int degree = Integer.parseInt(mobileMap.get("degree").toString());
        double time = execTime - timeNum;
        return (int) Math.sqrt(Math.pow(startingPoint, 2) + Math.pow(speed * time, 2) - 2 * startingPoint * speed * time * Float.valueOf(df.format(Math.cos(Math.toRadians(degree)))));
    }


    public void calculate() {
        //麻雀算法迭代次数
        int t = 1;
        BigDecimal lastFg = updateMap.get("fg");
        System.out.println("初始最优点：" + updateMap.get("globalMax").doubleValue());
        System.out.println("初始最优适应度：" + lastFg.doubleValue());
        BigDecimal pdMax;
        BigDecimal temp;
        while (t <= iterations) {
            r2 = BigDecimal.valueOf(Math.random()).floatValue();
            updateProducerPoint();
            pdMax = coordinatePoints.get(1).get("x");
            for (int i = 2; i <= PD; i++) {
                temp = coordinatePoints.get(i).get("x");
                if (f(pdMax).doubleValue() < f(temp).doubleValue()) {
                    pdMax = temp;
                }
            }
            updateMap.put("pdMax", pdMax);
            try {
                updateScroungerPoint();
            } catch (NumberFormatException numberFormatException) {
                continue;
            }
            updateSDPoint();
            //更新完所有的位置后，重新算下最优值等
            rankAndFindLocation();
            t++;
        }
        System.out.println("迭代完成后最优点：" + updateMap.get("globalMax").doubleValue());
        System.out.println("迭代完成后最优适应度：" + updateMap.get("fg").doubleValue());
    }

}