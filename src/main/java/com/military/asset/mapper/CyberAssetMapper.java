package com.military.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.military.asset.entity.CyberAsset;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 网信资产Mapper接口
 * 作用：定义网信资产表（cyber_asset）的所有数据库操作
 * 与Service层关系：被CyberAssetServiceImpl调用，执行实际的SQL操作
 * 与XML关系：接口方法与CyberAssetMapper.xml中的SQL语句一一对应

 * 修改说明：移除 @Mapper 注解，由 @MapperScan 统一扫描
 * 原因：Spring Boot 3.x 中 @Mapper 注解与 @MapperScan 冲突，导致 factoryBeanObjectType 错误

 * 新增功能：
 * - selectAllExistingAssets(): 查询所有完整资产对象，用于导入时关键字段比较
 */
public interface CyberAssetMapper extends BaseMapper<CyberAsset> {

    /**
     * 查询所有已存在的资产ID
     * 用途：Excel导入时校验ID是否重复，防止主键冲突
     * 调用时机：监听器初始化时调用，加载数据库中已有的ID列表
     * @return 所有资产ID组成的列表（如["id1","id2"...]）
     */
    @Select("SELECT id FROM cyber_asset")
    List<String> selectAllExistingIds();

    /**
     * 批量插入网信资产
     * 用途：Excel导入时高效保存多条数据，比单条插入效率提升10倍以上
     * 事务保证：由Service层的@Transactional注解控制，确保要么全成功要么全失败
     * @param list 待插入的网信资产实体列表（从ExcelVO转换而来）
     */
    void insertBatch(@Param("list") List<CyberAsset> list);

    /**
     * 按上报单位和资产分类组合查询
     * 用途：支持前端多条件筛选，如"查询技术部的服务器资产"
     * 性能优化：数据库表中report_unit和asset_category字段需建立索引
     * @param reportUnit 上报单位（可选参数，为空则不筛选）
     * @param assetCategory 资产分类（可选参数，为空则不筛选）
     * @return 符合条件的网信资产列表
     */
    List<CyberAsset> selectByReportUnitAndCategory(
            @Param("reportUnit") String reportUnit,
            @Param("assetCategory") String assetCategory
    );

    /**
     * 按已用数量范围查询（网信资产特有功能）
     * 用途：统计资源利用情况，如"查询已用数量超过50的资产"
     * 业务场景：用于资产利用率分析报表
     * @param min 最小已用数量（包含）
     * @param max 最大已用数量（包含）
     * @return 符合数量范围的资产列表
     */
    List<CyberAsset> selectByUsedQuantityRange(
            @Param("min") Integer min,
            @Param("max") Integer max
    );

    // ============================ 新增方法 ============================

    /**
     * 查询所有已存在的网信资产（完整对象）

     * 新增用途：用于Excel导入时关键字段比较，而不仅仅是ID重复检查
     * 核心字段：包含所有业务字段，特别是上报单位、资产分类、资产名称、资产内容等关键字段

     * 网信资产特有字段：
     * - asset_content：资产内容（网信资产特有，参与关键字段比较）
     * - used_quantity：已用数量（网信资产特有，用于业务校验）

     * 性能考虑：
     * - 一次性查询所有数据，避免多次数据库交互
     * - 数据量较大时，考虑分批加载（当前设计适用于常规数据量）
     *
     * @return 所有网信资产完整对象列表
     */
    @Select("SELECT * FROM cyber_asset")
    List<CyberAsset> selectAllExistingAssets();
}