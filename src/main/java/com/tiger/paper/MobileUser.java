package com.tiger.paper;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 孙小虎
 * @date 2020/8/22 - 23:19
 * 移动用户
 */

@Getter
@ToString
public class MobileUser {
    /**
     * 移动用户ID
     */
    private Integer id;

    /**
     * 任务集合，每个任务单位为（bits）
     */
    private List<Integer> totalComputingDatas;
    /**
     * 距离基站距离（米）
     */
    @Setter
    private Float distance;
    /**
     * 计算 1 bit数据所需CPU周期数（cycles）
     */
    private Integer cyclesPerBit;

    /**
     * 本地CPU每秒周期数度量（cycles/sec）即本地计算能力
     */
    private Float localComputingAbility;

    /**
     * 传输功率（W）
     */
    private Float transPower;

    /**
     * 移动用户效用函数的系数
     */
    private Float gamma;
    /**
     * 移动用户对时间的权重 0-1
     */
    private Float alpha;
    /**
     * 移动用户对能耗的权重 0-1
     */
    private Float beta;

    /**
     * 上传速率
     */
    @Setter
    private Double uplinkRate;

    /**
     * 上传任务时距离MEC的距离 第一次就是初始化的  之后每一次更改 都是在reFreshMobileUplinkRate方法里设置
     */
    private List<Integer> uplinkDistance = new ArrayList<>(8);

    /**
     * 记录每移动一次的速度，角度，起始点等
     */
    private List<Map<String, Object>> mobileConf = new ArrayList<>(8);

    /**
     * 移动用户的速度
     */
    @Setter
    private float[] speed = {0.5f, 1.0f, 1.5f};

    /**
     * 移动用户移动的时间间隔
     */
    @Setter
    private float[] timeInterval = {1.0f, 2.0f, 3.0f};

    /**
     * 任务执行时间（它是一个累计值，每执行一个任务，这里时间会累加）
     */
    @Setter
    private double execTime;

    public MobileUser(Integer id, List<Integer> totalComputingDatas, Float distance, Integer cyclesPerBit, Float localComputingAbility, Float transPower, Float gamma, Float alpha, Float beta, Integer uplinkDistance) {
        this.id = id;
        this.totalComputingDatas = totalComputingDatas;
        this.distance = distance;
        this.cyclesPerBit = cyclesPerBit;
        this.localComputingAbility = localComputingAbility;
        this.transPower = transPower;
        this.gamma = gamma;
        this.alpha = alpha;
        this.beta = beta;
        this.uplinkDistance.add(uplinkDistance);
    }
}
