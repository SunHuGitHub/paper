package com.tiger.paper;

import lombok.Data;

@Data
public class Block  {
    private String name;
    private double value;

    public Block(){}

    public Block(String name, double value) {
        this.name = name;
        this.value = value;
    }
}