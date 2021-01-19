package com.tiger.paper;


import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author 孙小虎
 * @date 2021/01/14 - 14:42
 */
public class SSA_SA {

    /**
     * 种群数
     */
    private static final int SPECIES_NUM = 200;
    /**
     * 最大迭代次数
     */
    private static final int ITERATIONS = 200;
    /**
     * 生产者--producer
     */
    private static final int PD = (int) (SPECIES_NUM * 0.2f);
    /**
     * 意识到危险的麻雀数量
     */
    private static final int SD = (int) (SPECIES_NUM * 0.1f);
    /**
     * 生产者警戒阈值
     */
    private static final float ST = BigDecimal.valueOf(0.8f).floatValue();
    /**
     * 随机生产者警戒阈值
     */
    private static float r2 = BigDecimal.valueOf(0.0f).floatValue();
    /**
     * 随机向量方向 1表示正方向 即正步长  -1表示负方向 即负步长
     */
    private static final int[] A = {1, -1};
    /**
     * 每次迭代，map里面的最优值 最差值都会更新
     */
    private static Map<String, BigDecimal> updateMap = null;
    /**
     * 随机生成器
     */
    private static final Random RANDOM = new Random();
    /**
     * 麻雀的坐标点
     */
    private static List<Map<String, BigDecimal>> coordinatePoints = new ArrayList<>(SPECIES_NUM + 1);
    /**
     * 退火系数
     */
    private static double q = BigDecimal.valueOf(0.95d).doubleValue();
    /**
     * 初始温度
     */
    private static double t0 = BigDecimal.valueOf(1000.0d).doubleValue();
    /**
     * 退火循环次数
     */
    private static final int LOOP = 100;
    /**
     * 初始化坐标范围
     */
    private static final int INITRANGE = 300;
    /**
     * 初始化坐标小数范围
     */
    private static final double DECIMALRANGE = 0.01d;


    /**
     * 初始化麻雀种群坐标点 这里index是[1,SPECIES_NUM] 因为考虑到公式中的i不能为0
     */
    static {
        HashMap<String, BigDecimal> map = null;
        coordinatePoints.add(map);
        for (int i = 1; i <= SPECIES_NUM; i++) {
            map = new HashMap<>(16);
            //这里设置麻雀初始范围 取2代表麻雀范围为[-1,1]
            map.put("x", BigDecimal.valueOf((RANDOM.nextInt() % INITRANGE) * DECIMALRANGE));
            coordinatePoints.add(map);
        }
        //上面初始化完成后 初始计算下
        rankAndFindLocation();
    }


    /**
     * 更新生产者的坐标 对应论文中的公式三
     */
    private static void updateProducerPoint() {
        BigDecimal r;
        for (int i = 1; i <= PD; i++) {
            Map<String, BigDecimal> map = coordinatePoints.get(i);
            if (r2 < ST) {
                r = BigDecimal.valueOf(1 - Math.random());
                map.put("x", BigDecimal.valueOf(map.get("x").doubleValue() * BigDecimal.valueOf(Math.exp((-i) / BigDecimal.valueOf((r.doubleValue() * ITERATIONS)).doubleValue())).doubleValue()));
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
    private static void updateScroungerPoint() throws NumberFormatException {
        BigDecimal pdMax;
        for (int i = PD + 1; i <= SPECIES_NUM; i++) {
            Map<String, BigDecimal> map = coordinatePoints.get(i);
            if (i > SPECIES_NUM * 1.0 / 2) {
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
    private static void updateSDPoint() {
        ArrayList<Integer> sdIndex = new ArrayList<>(SD);
        //随机选择SD个 麻雀坐标索引
        for (int i = 1; i <= SD; i++) {
            //RANDOM.nextInt(SPECIES_NUM) 范围为[0,SPECIES_NUM) 但是coordinatePoints里咱们0号位置是空的 所以后面+1
            // 范围就是[1,SPECIES_NUM+1) 即 [1,SPECIES_NUM]
            int index = RANDOM.nextInt(SPECIES_NUM) + 1;
            while (sdIndex.contains(index)) {
                index = RANDOM.nextInt(SPECIES_NUM) + 1;
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
    private static void rankAndFindLocation() {
        BigDecimal pdMax = coordinatePoints.get(1).get("x");
        BigDecimal globalMin = pdMax;
        BigDecimal globalMax = pdMax;
        BigDecimal temp;
        BigDecimal f;
        BigDecimal fg;
        BigDecimal fw;
        for (int i = 2; i <= SPECIES_NUM; i++) {
            temp = coordinatePoints.get(i).get("x");
            f = f(temp);
            if (i <= PD && f(pdMax).doubleValue() < f.doubleValue()) {
                pdMax = temp;
            }
            if (f(globalMin).doubleValue() > f.doubleValue()) {
                globalMin = temp;
            }
            if (f(globalMax).doubleValue() < f.doubleValue()) {
                globalMax = temp;
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
    private static BigDecimal f(BigDecimal x1) {
        return BigDecimal.valueOf(BigDecimal.valueOf(-Math.pow(x1.doubleValue() - 1, 2)).doubleValue() + 9);
    }


    public static void main(String[] args) {
        //麻雀算法迭代次数
        int t = 1;
        //退火循环次数
        int l = 1;
        //退火次数
        int count = 0;
        BigDecimal lastFg = updateMap.get("fg");
        System.out.println("初始最优点：" + updateMap.get("globalMax").doubleValue());
        System.out.println("初始最优适应度：" + lastFg.doubleValue());
        BigDecimal pdMax;
        BigDecimal temp;
        double df;
        //保存前一次的坐标位置
        List<Map<String, BigDecimal>> backUpCoordinatePoints;
        HashMap<String, BigDecimal> backUpUpdateMap;
        while (t <= ITERATIONS) {
            do {
                backUpCoordinatePoints = JSONObject.parseObject(JSONObject.toJSONString(coordinatePoints), ArrayList.class);
                backUpUpdateMap = JSONObject.parseObject(JSONObject.toJSONString(updateMap), HashMap.class);
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
                //模拟退火思想
                df = lastFg.doubleValue() - updateMap.get("fg").doubleValue();
                // > 0 表示当前迭代的适应度值比上一次的小 即当前迭代是较差的解
                if (df > 0) {
                    // < 表示不接受这个较差的解  >= 表示接受  此概率受到温度参数的影响, 其值的大小随温度的下降而逐渐减小，使得算法在前期有较大概率跳出局部极值, 而在后期又能具有较高的收敛速度
                    if (BigDecimal.valueOf(Math.exp(BigDecimal.valueOf((-df) / t0).doubleValue())).doubleValue() < Math.random()) {
                        //不接收较差的解 所以回滚之前的坐标值
                        coordinatePoints = backUpCoordinatePoints;
                        updateMap = backUpUpdateMap;
                        count++;
                    }
                }
                lastFg = updateMap.get("fg");
                l++;
            } while (l <= LOOP);
            t0 *= q;
            t++;
        }
        System.out.println("退火次数：" + count);
        System.out.println("迭代完成后最优点：" + updateMap.get("globalMax").doubleValue());
        System.out.println("迭代完成后最优适应度：" + updateMap.get("fg").doubleValue());
    }
}