package com.military.asset.mapper;

import com.military.asset.entity.District;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

// 区县Mapper
@Mapper
public interface DistrictMapper {
    // 根据区县名称查询（用于匹配“区”或“县”）
    List<District> selectByName(String name); // 可能有重名区县，返回列表

    // 新增：查询所有区县
    List<District> selectAll();
}
