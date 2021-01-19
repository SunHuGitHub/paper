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
    private Map<String, String> updateMap;
    /**
     * 随机生成器
     */
    private final Random RANDOM = new Random();
    /**
     * 麻雀的坐标点
     */
    private List<List<Float>> coordinatePoints;

    private List<MobileUser> mobileUsers;


    public SSA(int speciesNum, int iterations, float PDRatio, float SDRatio, float STRatio, List<MobileUser> mobileUsers) {
        this.speciesNum = speciesNum;
        this.iterations = iterations;
        this.PD = (int) (speciesNum * PDRatio);
        this.SD = (int) (speciesNum * SDRatio);
        this.ST = BigDecimal.valueOf(STRatio).floatValue();
        this.coordinatePoints = new ArrayList<>(speciesNum + 1);
        this.mobileUsers = mobileUsers;
        /**
         * 初始化麻雀种群坐标点 这里index是[1,speciesNum] 因为考虑到公式中的i不能为0
         */
        coordinatePoints.add(null);
        List<Float> sparrowIndex;
        //任务数量 即可知多少维度
        int taskSize = mobileUser.getTotalComputingDatas().size();
        for (int i = 1; i <= this.speciesNum; i++) {
            sparrowIndex = new ArrayList<>();
            for (int k = 0; k < taskSize; k++) {
                //这里设置每个麻雀的坐标
                sparrowIndex.add((float) (RANDOM.nextInt(11) * 0.1));
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
        BigDecimal r;
        for (int i = 1; i <= PD; i++) {
            String sparrowIndex = coordinatePoints.get(i);
            if (r2 < ST) {
                r = BigDecimal.valueOf(1 - Math.random());
                map.put("x", BigDecimal.valueOf(map.get("x").doubleValue() * BigDecimal.valueOf(Math.exp((-i) / BigDecimal.valueOf((r.doubleValue() * iterations)).doubleValue())).doubleValue()));
            } else {
                //因为是1维 所以L为1
                map.put("x", BigDecimal.valueOf(map.get("x").doubleValue() + RANDOM.nextGaussian() * 1));
            }
            coordinatePoints.set(i, map);
        }
    }

    /**
     * 更新追随者（追随者）的坐标 对应论文中的公式四
     */
    private void updateScroungerPoint() throws NumberFormatException {
        BigDecimal pdMax;
        for (int i = PD + 1; i <= speciesNum; i++) {
            Map<String, BigDecimal> map = coordinatePoints.get(i);
            if (i > speciesNum * 1.0 / 2) {
                map.put("x", BigDecimal.valueOf(RANDOM.nextGaussian() * BigDecimal.valueOf(Math.exp((updateMap.get("globalMin").doubleValue() - map.get("x").doubleValue())) / Math.pow(i, 2)).doubleValue()));
            } else {
                pdMax = updateMap.get("pdMax");
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
        List<Float> pdMax = coordinatePoints.get(1);
        List<Float> globalMin = pdMax;
        List<Float> globalMax = pdMax;
        List<Float> sparrowIndex;
        BigDecimal f;
        BigDecimal fg;
        BigDecimal fw;
        for (int i = 2; i <= speciesNum; i++) {
            sparrowIndex = coordinatePoints.get(i);
            f = f(sparrowIndex);
            if (i <= PD && f(pdMax).doubleValue() < f.doubleValue()) {
                pdMax = sparrowIndex;
            }
            if (f(globalMin).doubleValue() > f.doubleValue()) {
                globalMin = sparrowIndex;
            }
            if (f(globalMax).doubleValue() < f.doubleValue()) {
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
     *
     * @param x1
     * @return
     */
    private double f(List<Float> sparrowIndex) {
        return localComputingTime(mobileUsers.get(0), sparrowIndex);
    }

    /**
     * @param mobileUser   移动用户
     * @param sparrowIndex 麻雀坐标
     * @return 本地处理时间
     */
    private double localComputingTime(MobileUser mobileUser, List<Float> sparrowIndex) {
        //拿到用户的总任务集合
        List<Integer> totalComputingDatas = mobileUser.getTotalComputingDatas();
        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        //用户本地计算能力
        Float localComputingAbility = mobileUser.getLocalComputingAbility();
        double sumTime = 0.0d;
        for (int i = 0; i < totalComputingDatas.size(); i++) {
            sumTime += BigDecimal.valueOf((totalComputingDatas.get(i) * sparrowIndex.get(i) * cyclesPerBit) / localComputingAbility).doubleValue();
        }
        return BigDecimal.valueOf(sumTime).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    //卸载时间 = 上传时间 + MEC执行时间
    private double offloadingTime(MobileUser mobileUser, List<Float> sparrowIndex) {
        //拿到用户的总任务集合
        List<Integer> totalComputingDatas = mobileUser.getTotalComputingDatas();
        //用户计算 1 bit数据所需CPU周期数
        Integer cyclesPerBit = mobileUser.getCyclesPerBit();
        double sumTime = 0.0d;
        for (int i = 0; i < totalComputingDatas.size(); i++) {
            float uplinkComputingData = totalComputingDatas.get(i) * (1 - sparrowIndex.get(i)) * cyclesPerBit;
        }
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