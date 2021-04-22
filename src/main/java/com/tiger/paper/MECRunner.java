package com.tiger.paper;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author 孙小虎
 * @date 2021/1/18 - 22:18
 */
public class MECRunner {
    /**
     * 移动用户集合
     */
    private static List<MobileUser> mobileUsers = new ArrayList<>();
    /**
     * 边缘服务器
     */
    private static EdgeSettings edgeSettings;
    /**
     * 任务数量
     */
    private static int TASKNUM = 1000;
    /**
     * 移动用户个数
     */
    private static final int USERNUM = 5;
    /**
     * 格式化小数点
     */
    private static DecimalFormat df;
    private static SSA ssa;
    private static SA sa;
    private static SSASA ssasa;
    private static List<Integer> taskCollec = new ArrayList<>(TASKNUM);

    static {
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(new FileInputStream(new File("E:\\paper\\src\\main\\java\\com\\tiger\\paper\\data.txt")), "utf-8");
//            isr = new InputStreamReader(new FileInputStream(new File("D:\\work_sapce_IDEA\\paper\\src\\main\\java\\com\\tiger\\paper\\data.txt")), "utf-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                taskCollec.add(Integer.parseInt(s) * 8192);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        //带宽 1MHZ  背景噪声 -100dbm MEC计算能力 5GHZ  路径衰落因子 4  边缘服务器基站范围
        edgeSettings = new EdgeSettings(20e6f, -250f, 4e9f, 2, 500);
//        mobileUsers = new ArrayList<>();
        df = new DecimalFormat("0.00");
        //本模型是最小化每个移动用户的costFuntion 即 min costFuntion()
        //任务数据量 单位KB
        int[] taskDataSize = {1500};
        int[] cyclesPerBit = {1200};
        //本地计算能力 0.5GHZ  0.8GHZ  1GHZ
        float[] localComputingAbility = {1.5e9f, 1.8e9f, 2e9f};
        //传输功率 0.05w  0.08w  0.1w
        float[] transPower = {0.8f, 1f};
        //移动用户对时间的权重 最大为 10。已知时间权重，能耗权重就为 10 - X
        int[] alpha = {2, 5, 8};
        //离基站距离 假设小型基站范围500米  使用户随机在离基站100到400米之间
        int minDistance = edgeSettings.getSignalRange() - 400;
        int maxDistance = edgeSettings.getSignalRange() - 200;

        Random random = new Random();

        int alphaIndex = random.nextInt(alpha.length);
        //任务集合
//        List<Integer> totalComputingDatas = new ArrayList<>();
//        for (int i = 1; i <= 1; i++) {
//            //1KB = 1024B = 1024 * 8 = 8192b
//            totalComputingDatas.add(taskDataSize[random.nextInt(taskDataSize.length)] * 8192);
//        }

        for (int i = 1; i <= USERNUM; i++) {
            //随机每个用户离基站的距离
            float distance = (random.nextInt(maxDistance - minDistance + 1) + minDistance) * 1.0f;
            mobileUsers.add(new MobileUser(i, null,
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
        //初始化移动用户的 Random Walk Model 即预定路线
        initMobileConf();
        List<Double> ssaRes = Collections.synchronizedList(new ArrayList<>(TASKNUM));
        List<Double> saRes = Collections.synchronizedList(new ArrayList<>(TASKNUM));
        List<Double> ssasaRes = Collections.synchronizedList(new ArrayList<>(TASKNUM));

        MobileUser mobileUser = mobileUsers.get(0);
        mobileUsers.remove(0);
//        for (int i = 0; i < TASKNUM; i++) {
//            totalComputingDatas = taskDataSize[random.nextInt(taskDataSize.length)] * 8192;
//            ssa = new SSA(200, 500, 0.3f, 0.2f, 0.8f, new ArrayList<>(mobileUsers), edgeSettings, totalComputingDatas);
//            ssaRes.add(ssa.calculate());
//            sa = new SA(1000d, 1d, 0.9d, 500, new ArrayList<>(mobileUsers), edgeSettings, totalComputingDatas);
//            saRes.add(sa.calculate());
//        }
        for (int b = 0; b < 8; b++) {
            for (int j = 0; j < 8; j++) {
//            ExecutorService ssaThreadPool = Executors.newFixedThreadPool(7);
//            CountDownLatch ssaCountDown = new CountDownLatch(TASKNUM);
                ExecutorService saThreadPool = Executors.newFixedThreadPool(4);
                CountDownLatch saCountdown = new CountDownLatch(TASKNUM);
//            ExecutorService ssasaThreadPool = Executors.newFixedThreadPool(8);
//            CountDownLatch ssasaCountdown = new CountDownLatch(TASKNUM);
                for (int i = 0; i < TASKNUM; i++) {
                    int finalI = i;
//                ssaThreadPool.execute(() -> {
//                    ssa = new SSA(100, 1000, 0.2f, 0.1f, 0.8f, JSONObject.parseObject(JSONObject.toJSONString(mobileUser), MobileUser.class), mobileUsers, edgeSettings, taskCollec.get(finalI));
//                    ssaRes.add(ssa.calculate());
//                    ssaCountDown.countDown();
//                });
//
                    saThreadPool.execute(() -> {
                        sa = new SA(1000d, 1d, 0.9d, 500, JSONObject.parseObject(JSONObject.toJSONString(mobileUser), MobileUser.class), mobileUsers, edgeSettings, taskCollec.get(finalI));
                        saRes.add(sa.calculate());
                        saCountdown.countDown();
                    });
//                ssasaThreadPool.execute(() -> {
//                    ssasa = new SSASA(100, 1000, 0.2f, 0.1f, 0.8f, JSONObject.parseObject(JSONObject.toJSONString(mobileUser), MobileUser.class), mobileUsers, edgeSettings, taskCollec.get(finalI));
//                    ssasaRes.add(ssasa.calculate());
//                    ssasaCountdown.countDown();
//                });
                }
//            ssaCountDown.await();
//            ssaThreadPool.shutdown();
                saCountdown.await();
                saThreadPool.shutdown();
//            ssasaCountdown.await();
//            ssasaThreadPool.shutdown();
                double sum = 0;
//            for (Double ssaRe : ssaRes) {
//                sum += ssaRe;
//            }
//            System.out.println("ssa：" + TASKNUM + "：" + BigDecimal.valueOf(sum / TASKNUM).setScale(2, RoundingMode.HALF_UP).doubleValue());
//            sum = 0;
                for (Double saRe : saRes) {
                    sum += saRe;
                }
                System.out.println("sa：" + TASKNUM + "：" + BigDecimal.valueOf(sum / TASKNUM).setScale(2, RoundingMode.HALF_UP).doubleValue());
//            for (Double ssasaRe : ssasaRes) {
//                sum += ssasaRe;
//            }
//            System.out.println("ssasa：" + TASKNUM + "：" + BigDecimal.valueOf(sum / TASKNUM).setScale(2, RoundingMode.HALF_UP).doubleValue());
                TASKNUM = TASKNUM + 1000;
//            ssaRes.clear();
                saRes.clear();
//            ssasaRes.clear();
            }
            TASKNUM = 1000;
        }
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
        mobileUser.setInitUplinkRate(getUplinkRate(mobileUser));
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

    /**
     * Random Walk Model
     * 初始化移动用户的移动模型
     */
    private static void initMobileConf() {
        Random random = new Random();
        //这里只移动第一个用户 其他用户考虑静止
        MobileUser user = mobileUsers.get(0);
        float v;
        float t;
        for (int i = 0; i < 5; i++) {
            HashMap<String, Object> mobileConf = new HashMap<>();
            v = user.getSpeed()[random.nextInt(user.getSpeed().length)];
            t = user.getTimeInterval()[random.nextInt(user.getTimeInterval().length)];
            Float preDistance = user.getDistance();
            mobileConf.put("startingPoint", preDistance);
            mobileConf.put("speed", v);
            mobileConf.put("time", t);
            user.getMobileConf().add(mobileConf);
            //将原先距离修改为新的移动点的距离
            int sign = 1;
            if ((i & 1) != 0) {
                sign = -1;
            }
            int newDistance = (int) (preDistance + sign * t * v);
            user.setDistance(((float) newDistance));
        }
//        for (int i = 0; i < 5; i++) {
//            int newDistance;
//            //0 - 360° 之间随机
//            int degree;
//            float v = user.getSpeed()[random.nextInt(user.getSpeed().length)];
//            float t = user.getTimeInterval()[random.nextInt(user.getTimeInterval().length)];
//            double step = v * t;
//            HashMap<String, Object> mobileConf = new HashMap<>();
//            do {
//                degree = random.nextInt(361);
//                if (degree == 0 || degree == 360) {
//                    newDistance = (int) (user.getDistance() + step);
//                } else if (degree == 180) {
//                    newDistance = (int) Math.abs(user.getDistance() - step);
//                } else {
//                    if (degree < 180) {
//                        newDistance = (int) Math.sqrt(Math.pow(user.getDistance(), 2) + Math.pow(step, 2) - 2 * user.getDistance() * step * Float.valueOf(df.format(Math.cos(Math.toRadians(180 - degree)))));
//                        mobileConf.put("degree", 180 - degree);
//                    } else {
//                        newDistance = (int) Math.sqrt(Math.pow(user.getDistance(), 2) + Math.pow(step, 2) - 2 * user.getDistance() * step * Float.valueOf(df.format(Math.cos(Math.toRadians(degree - 180)))));
//                        mobileConf.put("degree", degree - 180);
//                    }
//                }
//            } while (newDistance > edgeSettings.getSignalRange());
//
//            //这里说明移动后距离没超过MEC的信号范围
//            mobileConf.put("startingPoint", user.getDistance());
//            mobileConf.put("speed", v);
//            mobileConf.put("time", t);
//
//            user.getMobileConf().add(mobileConf);
//            //将原先距离修改为新的移动点的距离
//            user.setDistance(((float) newDistance));
//        }
    }
}
