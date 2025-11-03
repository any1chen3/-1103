package com.military.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.asset.entity.DataContentAsset;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据内容资产Mapper接口
 * 作用：定义数据内容资产表（data_content_asset）的所有数据库操作
 * 特点：包含数据资产特有方法（如按开发工具查询）
 * 调用链：DataContentAssetServiceImpl → 本接口 → DataContentAssetMapper.xml → 数据库

 * 修改说明：移除 @Mapper 注解，由 @MapperScan 统一扫描
 * 原因：Spring Boot 3.x 中 @Mapper 注解与 @MapperScan 冲突，导致 factoryBeanObjectType 错误

 * 新增功能：
 * - selectAllExistingAssets(): 查询所有完整资产对象，用于导入时关键字段比较
 */
public interface DataContentAssetMapper extends BaseMapper<DataContentAsset> {

    /**
     * 查询所有已存在的资产ID
     * 用途：Excel导入时校验ID唯一性，防止重复入库
     * @return 资产ID列表
     */
    List<String> selectAllExistingIds();

    /**
     * 批量插入数据内容资产
     * 用途：Excel导入时高效保存数据，适合一次性导入上千条记录
     * @param list 数据内容资产实体列表
     */
    void insertBatch(@Param("list") List<DataContentAsset> list);

    /**
     * 按上报单位和资产分类组合查询
     * 用途：前端多条件筛选数据资产
     * @param reportUnit 上报单位（可选）
     * @param assetCategory 资产分类（可选）
     * @return 符合条件的资产列表
     */
    List<DataContentAsset> selectByReportUnitAndCategory(
            @Param("reportUnit") String reportUnit,
            @Param("assetCategory") String assetCategory
    );

    /**
     * 按开发工具筛选（数据资产特有功能）
     * 用途：查询使用特定工具开发的数据资产，如"查询所有用MySQL开发的资产"
     * 业务价值：用于统计不同开发工具的使用频率
     * @param developmentTool 开发工具名称（如"MySQL"、"Oracle"）
     * @return 符合条件的资产列表
     */
    List<DataContentAsset> selectByDevelopmentTool(@Param("developmentTool") String developmentTool);

    // ============================ 新增方法 ============================

    /**
     * 查询所有已存在的数据内容资产（完整对象）

     * 新增用途：用于Excel导入时关键字段比较，而不仅仅是ID重复检查
     * 核心字段：包含所有业务字段，特别是上报单位、资产分类、资产名称等关键字段

     * 性能考虑：
     * - 一次性查询所有数据，避免多次数据库交互
     * - 数据量较大时，考虑分批加载（当前设计适用于常规数据量）
     *
     * @return 所有数据内容资产完整对象列表
     */
    @Select("SELECT * FROM data_content_asset")
    List<DataContentAsset> selectAllExistingAssets();
}