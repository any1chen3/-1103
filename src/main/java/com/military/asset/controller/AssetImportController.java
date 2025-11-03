package com.military.asset.controller;

import com.alibaba.excel.EasyExcel;
import com.military.asset.service.SoftwareAssetService;
import com.military.asset.service.CyberAssetService;
import com.military.asset.service.DataContentAssetService;
import com.military.asset.listener.SoftwareAssetExcelListener;
import com.military.asset.listener.CyberAssetExcelListener;
import com.military.asset.listener.DataContentAssetExcelListener;
import com.military.asset.service.impl.ReportUnitService;
import com.military.asset.vo.ExcelErrorVO;
import com.military.asset.vo.ImportResult;
import com.military.asset.vo.excel.SoftwareAssetExcelVO;
import com.military.asset.vo.excel.CyberAssetExcelVO;
import com.military.asset.vo.excel.DataContentAssetExcelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import jakarta.servlet.http.HttpServletResponse;



import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产导入控制器 - 新逻辑版本（无限制记录数量 + 使用现有模板文件）

 * 核心特性：
 * 1. 支持大规模数据导入（10万+行数据）
 * 2. 三个表的导入方法都使用新的监听器构造函数（传入完整资产对象Map）
 * 3. 移除Excel内部重复检查，只检查数据库重复
 * 4. 统一重复数据显示逻辑，不再区分Excel内部和系统重复
 * 5. 优化结果构建逻辑，简化重复数据统计
 * 6. 无限制返回成功和失败记录，支持大数据量场景
 * 7. 使用现有的Excel模板文件，包含完整的示例数据和格式

 * 模板文件配置：
 * - 软件资产模板：classpath:templates/software_asset_template.xlsx
 * - 网信资产模板：classpath:templates/cyber_asset_template.xlsx
 * - 数据内容资产模板：classpath:templates/data_content_asset_template.xlsx

 * 性能优化：
 * - 预加载机制：一次性加载所有现有资产到内存Map
 * - 内存比较：重复检查在内存中进行，避免多次数据库查询
 * - 批量保存：有效数据收集完成后一次性批量保存
 * - 流式读取：Excel流式解析，不占用大内存

 * 使用场景：
 * - 软件资产导入：关键字段（上报单位、资产分类、资产名称）
 * - 网信资产导入：关键字段（上报单位、资产分类、资产名称、资产内容）
 * - 数据内容资产导入：关键字段（上报单位、资产分类、资产名称）
 */
@Slf4j
@RestController
@RequestMapping("/api/asset/import")
@SuppressWarnings("unused")
public class AssetImportController {

    @Autowired
    private SoftwareAssetService softwareAssetService;

    @Autowired
    private CyberAssetService cyberAssetService;

    @Autowired
    private DataContentAssetService dataContentAssetService;


    // ============================ 模板文件路径常量 ============================


    /**
     * 软件资产模板文件路径
     * 位置：src/main/resources/templates/software_asset_template.xlsx
     */
    private static final String SOFTWARE_TEMPLATE_PATH = "templates/software_asset_template.xlsx";

    /**
     * 网信资产模板文件路径
     * 位置：src/main/resources/templates/cyber_asset_template.xlsx
     */
    private static final String CYBER_TEMPLATE_PATH = "templates/cyber_asset_template.xlsx";

    /**
     * 数据内容资产模板文件路径
     * 位置：src/main/resources/templates/data_content_asset_template.xlsx
     */
    private static final String DATA_CONTENT_TEMPLATE_PATH = "templates/data_content_asset_template.xlsx";

    /**
     * 软件资产Excel导入 - 新逻辑（支持大数据量）

     * 处理流程：
     * 1. 文件校验 → 2. 获取数据库现有资产Map → 3. 流式读取Excel → 4. 批量保存有效数据 → 5. 返回完整结果

     * 关键特性：
     * - 使用完整资产对象Map进行关键字段比较
     * - 移除Excel内部重复检查，只检查数据库重复
     * - 支持10万+行大数据量导入
     * - 返回所有成功和失败记录，无数量限制
     *
     * @param file 上传的Excel文件（支持.xlsx和.xls格式，最大100MB）
     * @return ImportResult 包含完整导入结果的响应对象
     *
     * @apiNote 支持大规模数据导入，建议单个文件不超过10万行以保证性能
     */
    @PostMapping("/software")
    public ImportResult importSoftwareAsset(@RequestParam("file") MultipartFile file) {
        log.info("开始导入软件资产Excel文件: {}，文件大小: {} bytes",
                file.getOriginalFilename(), file.getSize());
        try {
            // 步骤1：文件基础校验
            validateFile(file);

            // 步骤2：获取数据库中已存在的完整资产信息（用于关键字段比较）
            var existingAssets = softwareAssetService.getExistingAssetsMap();
            log.info("软件资产数据库现有记录数: {}条", existingAssets.size());

            // 步骤3：创建监听器，传入完整的资产信息用于比较关键字段
            SoftwareAssetExcelListener listener = new SoftwareAssetExcelListener(existingAssets);

            // 步骤4：流式读取Excel文件（不限制行数）
            EasyExcel.read(file.getInputStream(), SoftwareAssetExcelVO.class, listener)
                    .sheet()
                    .headRowNumber(2) // 跳过表头行
                    .doRead();

            // 步骤5：批量保存有效数据（如果有）
            if (!listener.getValidDataList().isEmpty()) {
                softwareAssetService.batchSaveSoftwareAssets(listener.getValidDataList());
                log.info("软件资产导入成功保存{}条数据", listener.getValidDataList().size());
                log.info("服务启动，开始自动填充上报单位的省份字段...");
            } else {
                log.info("软件资产导入无有效数据需要保存");
            }

            // 步骤6：构建并返回完整的导入结果（无记录数量限制）
            return buildImportResult(listener, "软件资产");

        } catch (Exception e) {
            log.error("软件资产导入失败: {}", e.getMessage(), e);
            return buildErrorResult("软件资产导入失败: " + e.getMessage());
        }
    }

    /**
     * 网信资产Excel导入 - 新逻辑（支持大数据量）

     * 处理流程：
     * 1. 文件校验 → 2. 获取数据库现有资产Map → 3. 流式读取Excel → 4. 批量保存有效数据 → 5. 返回完整结果

     * 关键特性：
     * - 网信资产特有：多一个资产内容字段比较
     * - 移除Excel内部重复检查，只检查数据库重复
     * - 支持10万+行大数据量导入
     * - 返回所有成功和失败记录，无数量限制
     *
     * @param file 上传的Excel文件（支持.xlsx和.xls格式，最大100MB）
     * @return ImportResult 包含完整导入结果的响应对象
     *
     * @apiNote 支持大规模数据导入，建议单个文件不超过10万行以保证性能
     */
    @PostMapping("/cyber")
    public ImportResult importCyberAsset(@RequestParam("file") MultipartFile file) {
        log.info("开始导入网信资产Excel文件: {}，文件大小: {} bytes",
                file.getOriginalFilename(), file.getSize());
        try {
            // 步骤1：文件基础校验
            validateFile(file);

            // 步骤2：获取数据库中已存在的完整资产信息
            var existingAssets = cyberAssetService.getExistingAssetsMap();
            log.info("网信资产数据库现有记录数: {}条", existingAssets.size());

            // 步骤3：创建监听器，传入完整的资产信息用于比较关键字段
            CyberAssetExcelListener listener = new CyberAssetExcelListener(existingAssets);

            // 步骤4：流式读取Excel文件（不限制行数）
            EasyExcel.read(file.getInputStream(), CyberAssetExcelVO.class, listener)
                    .sheet()
                    .headRowNumber(2) // 跳过表头行
                    .doRead();

            // 步骤5：批量保存有效数据（如果有）
            if (!listener.getValidDataList().isEmpty()) {
                cyberAssetService.batchSaveCyberAssets(listener.getValidDataList());
                log.info("网信资产导入成功保存{}条数据", listener.getValidDataList().size());
                log.info("服务启动，开始自动填充上报单位的省份字段...");
            } else {
                log.info("网信资产导入无有效数据需要保存");
            }

            // 步骤6：构建并返回完整的导入结果（无记录数量限制）
            return buildImportResult(listener, "网信资产");

        } catch (Exception e) {
            log.error("网信资产导入失败: {}", e.getMessage(), e);
            return buildErrorResult("网信资产导入失败: " + e.getMessage());
        }
    }

    /**
     * 数据内容资产Excel导入 - 新逻辑（支持大数据量）

     * 处理流程：
     * 1. 文件校验 → 2. 获取数据库现有资产Map → 3. 流式读取Excel → 4. 批量保存有效数据 → 5. 返回完整结果

     * 关键特性：
     * - 数据内容资产特有：开发工具等字段校验
     * - 移除Excel内部重复检查，只检查数据库重复
     * - 支持10万+行大数据量导入
     * - 返回所有成功和失败记录，无数量限制
     *
     * @param file 上传的Excel文件（支持.xlsx和.xls格式，最大100MB）
     * @return ImportResult 包含完整导入结果的响应对象
     *
     * @apiNote 支持大规模数据导入，建议单个文件不超过10万行以保证性能
     */
    @PostMapping("/data-content")
    public ImportResult importDataContentAsset(@RequestParam("file") MultipartFile file) {
        log.info("开始导入数据内容资产Excel文件: {}，文件大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // 步骤1：文件基础校验
            validateFile(file);

            // 步骤2：获取数据库中已存在的完整资产信息
            var existingAssets = dataContentAssetService.getExistingAssetsMap();
            log.info("数据内容资产数据库现有记录数: {}条", existingAssets.size());

            // 步骤3：创建监听器，传入完整的资产信息用于比较关键字段
            DataContentAssetExcelListener listener = new DataContentAssetExcelListener(existingAssets);

            // 步骤4：流式读取Excel文件（不限制行数）
            EasyExcel.read(file.getInputStream(), DataContentAssetExcelVO.class, listener)
                    .sheet()
                    .headRowNumber(2) // 跳过表头行
                    .doRead();

            // 步骤5：批量保存有效数据（如果有）
            if (!listener.getValidDataList().isEmpty()) {
                dataContentAssetService.batchSaveDataContentAssets(listener.getValidDataList());
                log.info("数据内容资产导入成功保存{}条数据", listener.getValidDataList().size());
                log.info("服务启动，开始自动填充上报单位的省份字段...");
            } else {
                log.info("数据内容资产导入无有效数据需要保存");
            }

            // 步骤6：构建并返回完整的导入结果（无记录数量限制）
            return buildImportResult(listener, "数据内容资产");

        } catch (Exception e) {
            log.error("数据内容资产导入失败: {}", e.getMessage(), e);
            return buildErrorResult("数据内容资产导入失败: " + e.getMessage());
        }
    }

    // ============================ 模板下载方法（使用现有模板文件） ============================

    /**
     * 下载软件资产导入模板 - 使用现有模板文件

     * 功能说明：
     * - 从静态资源目录读取现有的软件资产模板文件
     * - 直接返回完整的模板文件，包含示例数据、格式和样式
     * - 支持中文文件名编码，确保下载文件名为中文

     * 模板文件位置：
     * - 源文件：src/main/resources/templates/software_asset_template.xlsx
     * - 打包后：BOOT-INF/classes/templates/software_asset_template.xlsx
     *
     * @param response HTTP响应对象，用于设置下载头信息
     * @throws RuntimeException 当模板文件不存在或读取失败时抛出
     *
     * @apiNote 请确保模板文件存在于指定路径，否则会抛出异常
     */
    @GetMapping("/template/software")
    public void downloadSoftwareTemplate(HttpServletResponse response) {
        String filename = "软件资产导入模板.xlsx";
        try {
            // 设置响应头，触发浏览器下载
            setExcelResponseHeader(response, filename);

            // 从classpath读取现有的模板文件
            Resource resource = new ClassPathResource(SOFTWARE_TEMPLATE_PATH);

            // 检查模板文件是否存在
            if (!resource.exists()) {
                log.error("软件资产模板文件不存在: {}", SOFTWARE_TEMPLATE_PATH);
                throw new RuntimeException("软件资产模板文件不存在，请联系管理员");
            }

            log.info("开始读取软件资产模板文件: {}", SOFTWARE_TEMPLATE_PATH);

            // 将模板文件流写入响应输出流
            try (InputStream inputStream = resource.getInputStream()) {
                long bytesCopied = StreamUtils.copy(inputStream, response.getOutputStream());
                log.info("软件资产模板文件下载完成: {}，文件大小: {} bytes", filename, bytesCopied);
            }

            log.info("软件资产导入模板下载成功: {}", filename);

        } catch (Exception e) {
            log.error("软件资产模板下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("软件资产模板下载失败: " + e.getMessage());
        }
    }

    /**
     * 下载网信资产导入模板 - 使用现有模板文件

     * 功能说明：
     * - 从静态资源目录读取现有的网信资产模板文件
     * - 直接返回完整的模板文件，包含示例数据、格式和样式
     * - 支持中文文件名编码，确保下载文件名为中文

     * 模板文件位置：
     * - 源文件：src/main/resources/templates/cyber_asset_template.xlsx
     * - 打包后：BOOT-INF/classes/templates/cyber_asset_template.xlsx
     *
     * @param response HTTP响应对象，用于设置下载头信息
     * @throws RuntimeException 当模板文件不存在或读取失败时抛出
     *
     * @apiNote 请确保模板文件存在于指定路径，否则会抛出异常
     */
    @GetMapping("/template/cyber")
    public void downloadCyberTemplate(HttpServletResponse response) {
        String filename = "网信资产导入模板.xlsx";
        try {
            // 设置响应头，触发浏览器下载
            setExcelResponseHeader(response, filename);

            // 从classpath读取现有的模板文件
            Resource resource = new ClassPathResource(CYBER_TEMPLATE_PATH);

            // 检查模板文件是否存在
            if (!resource.exists()) {
                log.error("网信资产模板文件不存在: {}", CYBER_TEMPLATE_PATH);
                throw new RuntimeException("网信资产模板文件不存在，请联系管理员");
            }

            log.info("开始读取网信资产模板文件: {}", CYBER_TEMPLATE_PATH);

            // 将模板文件流写入响应输出流
            try (InputStream inputStream = resource.getInputStream()) {
                long bytesCopied = StreamUtils.copy(inputStream, response.getOutputStream());
                log.info("网信资产模板文件下载完成: {}，文件大小: {} bytes", filename, bytesCopied);
            }

            log.info("网信资产导入模板下载成功: {}", filename);

        } catch (Exception e) {
            log.error("网信资产模板下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("网信资产模板下载失败: " + e.getMessage());
        }
    }

    /**
     * 下载数据内容资产导入模板 - 使用现有模板文件

     * 功能说明：
     * - 从静态资源目录读取现有的数据内容资产模板文件
     * - 直接返回完整的模板文件，包含示例数据、格式和样式
     * - 支持中文文件名编码，确保下载文件名为中文

     * 模板文件位置：
     * - 源文件：src/main/resources/templates/data_content_asset_template.xlsx
     * - 打包后：BOOT-INF/classes/templates/data_content_asset_template.xlsx
     *
     * @param response HTTP响应对象，用于设置下载头信息
     * @throws RuntimeException 当模板文件不存在或读取失败时抛出
     *
     * @apiNote 请确保模板文件存在于指定路径，否则会抛出异常
     */
    @GetMapping("/template/data-content")
    public void downloadDataContentTemplate(HttpServletResponse response) {
        String filename = "数据内容资产导入模板.xlsx";
        try {
            // 设置响应头，触发浏览器下载
            setExcelResponseHeader(response, filename);

            // 从classpath读取现有的模板文件
            Resource resource = new ClassPathResource(DATA_CONTENT_TEMPLATE_PATH);

            // 检查模板文件是否存在
            if (!resource.exists()) {
                log.error("数据内容资产模板文件不存在: {}", DATA_CONTENT_TEMPLATE_PATH);
                throw new RuntimeException("数据内容资产模板文件不存在，请联系管理员");
            }

            log.info("开始读取数据内容资产模板文件: {}", DATA_CONTENT_TEMPLATE_PATH);

            // 将模板文件流写入响应输出流
            try (InputStream inputStream = resource.getInputStream()) {
                long bytesCopied = StreamUtils.copy(inputStream, response.getOutputStream());
                log.info("数据内容资产模板文件下载完成: {}，文件大小: {} bytes", filename, bytesCopied);
            }

            log.info("数据内容资产导入模板下载成功: {}", filename);

        } catch (Exception e) {
            log.error("数据内容资产模板下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据内容资产模板下载失败: " + e.getMessage());
        }
    }

    /**
     * 文件参数校验

     * 校验规则：
     * 1. 文件不能为空
     * 2. 文件格式必须是.xlsx或.xls
     * 3. 文件大小不超过100MB（支持大文件导入）
     *
     * @param file 上传的Excel文件
     * @throws IllegalArgumentException 当文件不符合要求时抛出
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("只支持.xlsx和.xls格式的Excel文件");
        }

        // 100MB文件大小限制（支持大文件导入）
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过100MB");
        }

        log.debug("文件校验通过: {}，大小: {} bytes", filename, file.getSize());
    }

    /**
     * 构建统一的导入结果对象 - 新逻辑（无限制记录数量）

     * 核心特性：
     * - 移除Excel内部重复统计，统一为系统重复
     * - 简化重复数据显示逻辑
     * - 适配新的监听器方法名
     * - 无限制返回所有成功和失败记录
     * - 支持10万+行数据的完整结果返回

     * 返回结构：
     * - 总处理行数、成功导入数、重复跳过数、错误数量
     * - 完整的错误详情列表（无数量限制）
     * - 完整的重复记录详情（无数量限制）
     * - 完整的成功记录列表（无数量限制）
     *
     * @param listener Excel监听器实例（包含处理结果）
     * @param assetType 资产类型（用于生成结果消息）
     * @return ImportResult 完整的导入结果对象
     */
    private ImportResult buildImportResult(Object listener, String assetType) {
        try {
            // 通过反射获取监听器的结果数据（支持不同资产类型的监听器）
            Method getValidDataList = listener.getClass().getMethod("getValidDataList");
            Method getErrorDataList = listener.getClass().getMethod("getErrorDataList");
            Method getSystemDuplicateCount = listener.getClass().getMethod("getSystemDuplicateCount");
            Method getDuplicateRecords = listener.getClass().getMethod("getDuplicateRecords");

            // 获取处理结果数据
            List<?> validDataList = (List<?>) getValidDataList.invoke(listener);
            @SuppressWarnings("unchecked")
            List<ExcelErrorVO> errorDataList = (List<ExcelErrorVO>) getErrorDataList.invoke(listener);
            int systemDuplicateCount = (int) getSystemDuplicateCount.invoke(listener);

            List<?> duplicateRecords = (List<?>) getDuplicateRecords.invoke(listener);

            // 计算统计信息（直接使用原始数据，不创建冗余变量）
            int totalRows = validDataList.size() + errorDataList.size() + systemDuplicateCount;
            int successCount = validDataList.size();
            int errorCount = errorDataList.size();

            // 创建基础结果对象
            ImportResult result = new ImportResult();
            result.setSuccess(true);

            // 根据处理结果设置相应的提示消息
            if (errorCount > 0) {
                result.setMessage(String.format("%s导入完成，存在%d条需要修正的错误", assetType, errorCount));
            } else if (systemDuplicateCount > 0) {
                result.setMessage(String.format("%s导入完成，自动跳过%d条重复数据", assetType, systemDuplicateCount));
            } else {
                result.setMessage(String.format("%s导入完成，成功导入%d条数据", assetType, successCount));
            }

            // 构建详细的数据结构
            ImportResult.ImportData data = new ImportResult.ImportData();

            // 设置基础统计信息
            data.setTotalRows(totalRows);
            data.setSuccessCount(successCount);
            data.setSkipCount(systemDuplicateCount); // 直接使用系统重复数量
            data.setErrorCount(errorCount);

            // 构建导入汇总信息
            ImportResult.ImportSummary summary = new ImportResult.ImportSummary();
            summary.setTotalProcessed(totalRows);
            summary.setSuccessfullyImported(successCount);
            summary.setDuplicatesSkipped(systemDuplicateCount); // 直接使用系统重复数量
            summary.setCriticalErrors(errorCount);
            data.setImportSummary(summary);

            // 设置错误详情（无数量限制，返回所有错误记录）
            data.setErrorDetails(new ArrayList<>(errorDataList));

            // 构建重复详情（简化逻辑，统一为系统重复）
            ImportResult.DuplicateDetails duplicateDetails = new ImportResult.DuplicateDetails();
            duplicateDetails.setTotalDuplicates(systemDuplicateCount); // 直接使用系统重复数量
            duplicateDetails.setDuplicateRecords(new ArrayList<>(duplicateRecords));
            data.setDuplicateDetails(duplicateDetails);

            // 构建成功记录列表（无数量限制，返回所有成功记录）
            List<ImportResult.SuccessRecord> successRecords = buildSuccessRecords(validDataList);
            data.setSuccessRecords(successRecords);

            // 设置完整数据到结果对象
            result.setData(data);

            // 记录详细的导入结果日志
            log.info("{}导入结果构建完成: 总处理{}行, 成功{}条, 跳过{}条, 错误{}条",
                    assetType, totalRows, successCount, systemDuplicateCount, errorCount);

            return result;

        } catch (Exception e) {
            log.error("构建导入结果时发生异常: {}", e.getMessage(), e);
            return buildErrorResult("处理导入结果时发生异常");
        }
    }

    /**
     * 构建成功记录列表（无数量限制）

     * 功能说明：
     * - 将有效数据转换为成功记录格式
     * - 支持三种资产类型的VO对象转换
     * - 无数量限制，返回所有成功记录
     * - 支持10万+行数据的完整转换

     * 性能考虑：
     * - 使用Stream处理，内存友好
     * - 异常处理确保单条记录失败不影响整体
     * - 支持大规模数据转换
     *
     * @param validDataList 有效数据列表（从监听器获取）
     * @return List<ImportResult.SuccessRecord> 成功记录列表（无数量限制）
     */
    private List<ImportResult.SuccessRecord> buildSuccessRecords(List<?> validDataList) {
        return validDataList.stream()
                .map(validData -> {
                    ImportResult.SuccessRecord record = new ImportResult.SuccessRecord();
                    try {
                        // 根据资产类型设置相应的字段值
                        if (validData instanceof SoftwareAssetExcelVO softwareVO) {
                            record.setExcelRowNum(softwareVO.getExcelRowNum());
                            record.setAssetId(softwareVO.getId());
                            record.setAssetName(softwareVO.getAssetName());
                            record.setReportUnit(softwareVO.getReportUnit());
                        } else if (validData instanceof CyberAssetExcelVO cyberVO) {
                            record.setExcelRowNum(cyberVO.getExcelRowNum());
                            record.setAssetId(cyberVO.getId());
                            record.setAssetName(cyberVO.getAssetName());
                            record.setReportUnit(cyberVO.getReportUnit());
                        } else if (validData instanceof DataContentAssetExcelVO dataVO) {
                            record.setExcelRowNum(dataVO.getExcelRowNum());
                            record.setAssetId(dataVO.getId());
                            record.setAssetName(dataVO.getAssetName());
                            record.setReportUnit(dataVO.getReportUnit());
                        }
                    } catch (Exception e) {
                        // 单条记录转换失败不影响整体，记录警告日志
                        log.warn("构建成功记录时发生异常: {}", e.getMessage());
                    }
                    return record;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建错误结果

     * 功能说明：
     * - 创建标准的错误响应对象
     * - 设置错误状态和错误消息
     * - 用于异常情况的统一错误处理
     *
     * @param message 错误消息描述
     * @return ImportResult 错误结果对象
     */
    private ImportResult buildErrorResult(String message) {
        ImportResult result = new ImportResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    /**
     * 设置Excel文件下载响应头

     * 功能说明：
     * - 设置正确的Content-Type和编码
     * - 处理文件名编码，支持中文文件名
     * - 设置下载头信息，触发浏览器下载
     *
     * @param response HTTP响应对象
     * @param filename 下载的文件名
     */
    private void setExcelResponseHeader(HttpServletResponse response, String filename) {
        try {
            // 设置响应内容类型
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            // 处理文件名编码（支持中文）
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFilename);

            log.debug("设置Excel下载响应头完成: {}", encodedFilename);
        } catch (Exception e) {
            log.error("设置响应头时发生异常: {}", e.getMessage());
            throw new RuntimeException("设置下载响应头失败");
        }
    }
}