package com.military.asset.mapper;

import com.military.asset.entity.City;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

// 城市Mapper
@Mapper
public interface CityMapper {
    // 根据城市名称查询城市（用于匹配“市”）
    List<City> selectByName(String name); // 可能有重名城市，返回列表

    // 新增：查询所有城市
    List<City> selectAll();
}
