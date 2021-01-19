package com.tiger.paper;

import lombok.Getter;

import java.util.Random;

/**
 * @author 孙小虎
 * @date 2020/8/23 - 1:00
 */
@Getter
public class EdgeSettings {
    /**
     * 边缘服务器ID
     */
    private Integer id;
    /**
     * 带宽（MHZ）--10MHZ
     */
    private Float bandwidth;
    /**
     * 背景噪声功率（dbm 或者 W）
     */
    private Float backgroundNoisePower;
    /**
     * MEC计算能力（cycles/sec）
     */
    private Float mecComputingAbility;
    /**
     * 路径衰落因子
     */
    private Integer eta;
    /**
     * 边缘服务器基站范围
     */
    private Integer signalRange;

    public EdgeSettings(Float bandwidth, Float backgroundNoisePower, Float mecComputingAbility, Integer eta, Integer signalRange) {
        this.bandwidth = bandwidth;
        this.backgroundNoisePower = backgroundNoisePower;
        this.mecComputingAbility = mecComputingAbility;
        this.eta = eta;
        this.signalRange = signalRange;
    }
}
