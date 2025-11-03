package com.military.asset.entity;

import lombok.Data;

@Data
public class City {
    private String code;         // 城市ID（char(36)）
    private String name;         // 城市名称（如“南京市”）
    private String parentCode;   // 归属省份ID（关联Province.code）
    private Integer level;       // 行政等级（如2）
}