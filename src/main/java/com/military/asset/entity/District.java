package com.military.asset.entity;

import lombok.Data;

@Data
public class District {
    private String code;         // 区县ID（char(36)）
    private String name;         // 区县名称（如“江宁区”）
    private String parentCode;   // 归属城市ID（关联City.code）
    private Integer level;       // 行政等级（如3）
    private String province;     // 归属省份ID（直接关联Province.code，优化查询）
}