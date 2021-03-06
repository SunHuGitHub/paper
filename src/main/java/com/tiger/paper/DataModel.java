package com.tiger.paper;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Tiger
 * @date 2021/4/25 19:54
 */
@Data
@AllArgsConstructor
public class DataModel {
    /**
     * 设置excel表头名称
     */
    @ExcelProperty(value = "迭代次数", index = 0)
    private Integer taskNum;
    @ExcelProperty(value = "值", index = 1)
    private Double value;
    @ExcelProperty(value = "成本", index = 2)
    private Double cost;
    @ExcelProperty(value = "违反率", index = 3)
    private Double tvr;
    @ExcelProperty(value = "成本违反量", index = 4)
    private Double cv;
}
