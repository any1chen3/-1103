package com.military.asset.controller;

import com.military.asset.service.impl.ReportUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 上报单位省份自动填充接口
 */
@RestController
@RequestMapping("/api/report-unit")
@Tag(name = "ReportUnitController", description = "上报单位相关接口")
public class ReportUnitController {

    @Resource
    private ReportUnitService reportUnitService;

    /**
     * 执行省份自动填充任务
     * 功能：批量处理report_unit表中province为空的数据，按省市县区关键字匹配并填充省份，未匹配则填“其他”
     */
    @PostMapping("/auto-fill-province")
    @Operation(summary = "自动填充省份信息", description = "处理province为空的上报单位，按行政区划匹配省份")
    public ResponseEntity<String> autoFillProvince() {
        try {
            // 调用Service层执行填充逻辑
            int processedCount = reportUnitService.batchFillProvince();
            return ResponseEntity.ok("自动填充完成，共处理 " + processedCount + " 条记录");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("自动填充失败：" + e.getMessage());
        }
    }
}