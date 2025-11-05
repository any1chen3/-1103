package com.military.asset.controller;

import com.military.asset.entity.CyberAsset;
import com.military.asset.entity.DataContentAsset;
import com.military.asset.entity.Province;
import com.military.asset.entity.SoftwareAsset;
import com.military.asset.mapper.ProvinceMapper;
import com.military.asset.service.CyberAssetService;
import com.military.asset.service.DataContentAssetService;
import com.military.asset.service.SoftwareAssetService;
import com.military.asset.vo.ResultVO;
import com.military.asset.vo.stat.ProvinceMetricVO;
import com.military.asset.vo.stat.SoftwareAssetStatisticVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 三表统一CRUD控制器 + 首页控制器
 * 适配各表特有约束，统一返回ResultVO
 * 新增功能：首页欢迎页面，提供系统接口说明
 */
@RestController
@RequestMapping("/api/asset")
@Slf4j
@SuppressWarnings("unused") // 抑制IDE误报警告
public class AssetCrudController {

    private final SoftwareAssetService softwareService;
    private final CyberAssetService cyberService;
    private final DataContentAssetService dataService;
    private final ProvinceMapper provinceMapper;

    /**
     * 构造器注入
     */
    @Autowired
    public AssetCrudController(SoftwareAssetService softwareService,
                               CyberAssetService cyberService,
                               DataContentAssetService dataService,
                               ProvinceMapper provinceMapper) {
        this.softwareService = softwareService;
        this.cyberService = cyberService;
        this.dataService = dataService;
        this.provinceMapper = provinceMapper;
    }

    // ============================== 首页欢迎接口 ==============================

    /**
     * 系统首页欢迎接口
     * 访问路径：GET http://localhost:8080/

     * 作用：提供系统概览和所有可用接口的说明文档
     *
     * @return 系统欢迎信息和接口文档
     */
    @GetMapping("/")
    public ResultVO<String> home() {
        String welcomeMessage =
                "🚀 欢迎使用军工资产管理系统 🚀\n\n" +
                        "📊 系统概述：\n" +
                        "   本系统用于管理军工企业的三类核心资产：软件资产、网信资产、数据内容资产\n" +
                        "   支持Excel批量导入、CRUD操作、多条件组合查询等功能\n\n" +

                        "📋 可用接口列表：\n\n" +

                        "📥 Excel导入接口（POST请求，multipart/form-data格式）：\n" +
                        "   • 软件资产导入: /api/asset/import/software\n" +
                        "   • 网信资产导入: /api/asset/import/cyber\n" +
                        "   • 数据资产导入: /api/asset/import/data-content\n\n" +

                        "🔍 查询接口（GET请求）：\n" +
                        "   • 软件资产列表: /api/asset/software/list?reportUnit=xxx&assetCategory=xxx\n" +
                        "   • 软件资产取得方式统计: /api/asset/software/statistics/v2/acquisition\n" +
                        "   • 软件资产服务状态统计: /api/asset/software/statistics/v2/service-status\n" +
                        "   • 软件资产省份老化统计: /api/asset/software/statistics/v2/aging/province\n" +
                        "   • 软件资产升级判定: /api/asset/software/statistics/v2/aging/asset/{assetId}/upgrade-required\n" +
                        "   • 网信资产列表: /api/asset/cyber/list?reportUnit=xxx&assetCategory=xxx\n" +
                        "   • 数据资产列表: /api/asset/data/list?reportUnit=xxx&assetCategory=xxx\n" +
                        "   • 网信资产数量范围查询: /api/asset/cyber/quantity?min=10&max=50\n" +
                        "   • 数据资产开发工具查询: /api/asset/data/tool?developmentTool=MySQL\n\n" +
                        "   • 数据资产信息化程度（全部省份）: /api/asset/data/province/information-degree\n" +
                        "   • 数据资产国产化率（全部省份）: /api/asset/data/province/domestic-rate\n\n" +

                        "📝 详情查询接口（GET请求）：\n" +
                        "   • 软件资产详情: /api/asset/software/{id}\n" +
                        "   • 网信资产详情: /api/asset/cyber/{id}\n" +
                        "   • 数据资产详情: /api/asset/data/{id}\n\n" +

                        "➕ 新增接口（POST请求，JSON格式）：\n" +
                        "   • 新增软件资产: /api/asset/software\n" +
                        "   • 新增网信资产: /api/asset/cyber\n" +
                        "   • 新增数据资产: /api/asset/data\n\n" +

                        "✏️ 修改接口（PUT请求，JSON格式）：\n" +
                        "   • 修改软件资产: /api/asset/software\n" +
                        "   • 修改网信资产: /api/asset/cyber\n" +
                        "   • 修改数据资产: /api/asset/data\n\n" +

                        "🗑️ 删除接口（DELETE请求）：\n" +
                        "   • 删除软件资产: /api/asset/software/{id}\n" +
                        "   • 删除网信资产: /api/asset/cyber/{id}\n" +
                        "   • 删除数据资产: /api/asset/data/{id}\n\n" +

                        "💡 使用说明：\n" +
                        "   1. 所有CRUD接口返回统一格式：{code:200, message:\"成功\", data:...}\n" +
                        "   2. Excel导入支持.xlsx和.xls格式\n" +
                        "   3. 日期格式：YYYY-MM-DD（如：2025-10-09）\n" +
                        "   4. 金额字段支持小数，保留2位小数\n\n" +

                        "🔧 技术栈：\n" +
                        "   • 后端：Spring Boot 3.2.0 + MyBatis-Plus 3.5.4\n" +
                        "   • 数据库：MySQL 8.0\n" +
                        "   • Excel解析：EasyExcel 3.3.2\n" +
                        "   • 构建工具：Maven\n\n" +

                        "📞 如有问题，请联系系统管理员";

        return ResultVO.success(welcomeMessage, "系统首页加载成功");
    }

    // ============================== 软件资产CRUD ==============================

    @GetMapping("/software/{id}")
    public ResultVO<SoftwareAsset> getSoftware(@PathVariable String id) {
        try {
            SoftwareAsset asset = softwareService.getById(id);
            return ResultVO.success(asset, "查询软件资产详情成功");
        } catch (RuntimeException e) {
            log.error("查询软件资产失败，ID：{}", id, e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/software/list")
    public ResultVO<List<SoftwareAsset>> listSoftware(
            @RequestParam(required = false) String reportUnit,
            @RequestParam(required = false) String assetCategory) {
        try {
            List<SoftwareAsset> list = softwareService.listByReportUnitAndCategory(reportUnit, assetCategory);
            return ResultVO.success(list, "查询软件资产列表成功（共" + list.size() + "条）");
        } catch (Exception e) {
            log.error("查询软件资产列表失败", e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/software/statistics")
    public ResultVO<List<SoftwareAssetStatisticVO>> statisticSoftware() {
        try {
            List<SoftwareAssetStatisticVO> statistics = softwareService.statisticsByReportUnit();
            return ResultVO.success(statistics, "查询软件资产统计成功（共" + statistics.size() + "条）");
        } catch (Exception e) {
            log.error("统计软件资产取得方式与服务状态失败", e);
            return ResultVO.fail("统计失败：" + e.getMessage());
        }
    }


    @PostMapping("/software")
    public ResultVO<Void> addSoftware(@RequestBody SoftwareAsset asset) {
        try {
            softwareService.add(asset);
            return ResultVO.success("新增软件资产成功，ID：" + asset.getId());
        } catch (RuntimeException e) {
            log.error("新增软件资产失败，ID：{}", asset.getId(), e);
            return ResultVO.fail("新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/software")
    public ResultVO<Void> updateSoftware(@RequestBody SoftwareAsset asset) {
        try {
            softwareService.update(asset);
            return ResultVO.success("修改软件资产成功，ID：" + asset.getId());
        } catch (RuntimeException e) {
            log.error("修改软件资产失败，ID：{}", asset.getId(), e);
            return ResultVO.fail("修改失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/software/{id}")
    public ResultVO<Void> deleteSoftware(@PathVariable String id) {
        try {
            softwareService.remove(id);
            return ResultVO.success("删除软件资产成功，ID：" + id);
        } catch (RuntimeException e) {
            log.error("删除软件资产失败，ID：{}", id, e);
            return ResultVO.fail("删除失败：" + e.getMessage());
        }
    }

    // ============================== 网信资产CRUD ==============================

    @GetMapping("/cyber/{id}")
    public ResultVO<CyberAsset> getCyber(@PathVariable String id) {
        try {
            CyberAsset asset = cyberService.getById(id);
            return ResultVO.success(asset, "查询网信资产详情成功");
        } catch (RuntimeException e) {
            log.error("查询网信资产失败，ID：{}", id, e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/cyber/list")
    public ResultVO<List<CyberAsset>> listCyber(
            @RequestParam(required = false) String reportUnit,
            @RequestParam(required = false) String assetCategory) {
        try {
            List<CyberAsset> list = cyberService.listByReportUnitAndCategory(reportUnit, assetCategory);
            return ResultVO.success(list, "查询网信资产列表成功（共" + list.size() + "条）");
        } catch (Exception e) {
            log.error("查询网信资产列表失败", e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/cyber/quantity")
    public ResultVO<List<CyberAsset>> listCyberByQuantity(
            @RequestParam Integer min,
            @RequestParam Integer max) {
        try {
            List<CyberAsset> list = cyberService.listByUsedQuantityRange(min, max);
            return ResultVO.success(list, "查询网信资产数量范围成功（共" + list.size() + "条）");
        } catch (Exception e) {
            log.error("查询网信资产数量范围失败，min：{}，max：{}", min, max, e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/cyber")
    public ResultVO<Void> addCyber(@RequestBody CyberAsset asset) {
        try {
            cyberService.add(asset);
            return ResultVO.success("新增网信资产成功，ID：" + asset.getId());
        } catch (RuntimeException e) {
            log.error("新增网信资产失败，ID：{}", asset.getId(), e);
            return ResultVO.fail("新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/cyber")
    public ResultVO<Void> updateCyber(@RequestBody CyberAsset asset) {
        try {
            cyberService.update(asset);
            return ResultVO.success("修改网信资产成功，ID：" + asset.getId());
        } catch (RuntimeException e) {
            log.error("修改网信资产失败，ID：{}", asset.getId(), e);
            return ResultVO.fail("修改失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/cyber/{id}")
    public ResultVO<Void> deleteCyber(@PathVariable String id) {
        try {
            cyberService.remove(id);
            return ResultVO.success("删除网信资产成功，ID：" + id);
        } catch (RuntimeException e) {
            log.error("删除网信资产失败，ID：{}", id, e);
            return ResultVO.fail("删除失败：" + e.getMessage());
        }
    }

    // ============================== 数据内容资产CRUD ==============================

    @GetMapping("/data/{id}")
    public ResultVO<DataContentAsset> getData(@PathVariable String id) {
        try {
            DataContentAsset asset = dataService.getById(id);
            return ResultVO.success(asset, "查询数据资产详情成功");
        } catch (RuntimeException e) {
            log.error("查询数据资产失败，ID：{}", id, e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/data/list")
    public ResultVO<List<DataContentAsset>> listData(
            @RequestParam(required = false) String reportUnit,
            @RequestParam(required = false) String assetCategory) {
        try {
            List<DataContentAsset> list = dataService.listByReportUnitAndCategory(reportUnit, assetCategory);
            return ResultVO.success(list, "查询数据资产列表成功（共" + list.size() + "条）");
        } catch (Exception e) {
            log.error("查询数据资产列表失败", e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/data/tool")
    public ResultVO<List<DataContentAsset>> listDataByTool(@RequestParam String developmentTool) {
        try {
            List<DataContentAsset> list = dataService.listByDevelopmentTool(developmentTool);
            return ResultVO.success(list, "按开发工具查询成功（共" + list.size() + "条）");
        } catch (Exception e) {
            log.error("按开发工具查询数据资产失败，工具：{}", developmentTool, e);
            return ResultVO.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/data/province/information-degree")
    public ResultVO<List<ProvinceMetricVO>> calculateInformationDegree() {
        try {
            List<ProvinceMetricVO> metrics = buildProvinceMetrics(dataService::calculateProvinceInformationDegree);
            return ResultVO.success(metrics, "各省份信息化程度计算成功");
        } catch (RuntimeException e) {
            log.error("各省份信息化程度批量计算失败", e);
            return ResultVO.fail("计算失败：" + e.getMessage());
        }
    }

    @GetMapping("/data/province/domestic-rate")
    public ResultVO<List<ProvinceMetricVO>> calculateDomesticRate() {
        try {
            List<ProvinceMetricVO> metrics = buildProvinceMetrics(dataService::calculateProvinceDomesticRate);
            return ResultVO.success(metrics, "各省份国产化率计算成功");
        } catch (RuntimeException e) {
            log.error("各省份国产化率批量计算失败", e);
            return ResultVO.fail("计算失败：" + e.getMessage());
        }
    }

    private List<ProvinceMetricVO> buildProvinceMetrics(Function<String, BigDecimal> calculator) {
        List<Province> provinces = provinceMapper.selectAll();
        if (Objects.isNull(provinces) || provinces.isEmpty()) {
            log.warn("省份表未查询到数据，返回空列表");
            return Collections.emptyList();
        }

        List<ProvinceMetricVO> metrics = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            if (province == null || province.getName() == null) {
                continue;
            }
            BigDecimal value = calculator.apply(province.getName());
            metrics.add(new ProvinceMetricVO(province.getCode(), province.getName(), value));
        }
        return metrics;
    }

    @PostMapping("/data")
    public ResultVO<Void> addData(@RequestBody DataContentAsset asset) {
        try {
            dataService.add(asset);
            return ResultVO.success("新增数据资产成功，ID：" + asset.getId());
        } catch (RuntimeException e) {
            log.error("新增数据资产失败，ID：{}", asset.getId(), e);
            return ResultVO.fail("新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/data")
    public ResultVO<Void> updateData(@RequestBody DataContentAsset asset) {
        try {
            dataService.update(asset);
            return ResultVO.success("修改数据资产成功，ID：" + asset.getId());
        } catch (RuntimeException e) {
            log.error("修改数据资产失败，ID：{}", asset.getId(), e);
            return ResultVO.fail("修改失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/data/{id}")
    public ResultVO<Void> deleteData(@PathVariable String id) {
        try {
            dataService.remove(id);
            return ResultVO.success("删除数据资产成功，ID：" + id);
        } catch (RuntimeException e) {
            log.error("删除数据资产失败，ID：{}", id, e);
            return ResultVO.fail("删除失败：" + e.getMessage());
        }
    }
}