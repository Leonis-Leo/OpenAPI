package com.openapi.backend.controller;

import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.service.InterfaceInfoService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/interface")
@RequiredArgsConstructor
@Tag(name = "接口管理")
public class InterfaceInfoController {

    private final InterfaceInfoService interfaceInfoService;

    @GetMapping("/list")
    @Operation(summary = "查询已上线接口")
    public ApiResponse<List<InterfaceInfo>> listOnline() {
        return ApiResponse.ok(interfaceInfoService.listOnline());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询接口详情")
    public ApiResponse<InterfaceInfo> detail(
            @Parameter(description = "接口 ID", example = "1") @PathVariable Long id) {
        InterfaceInfo info = interfaceInfoService.getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        return ApiResponse.ok(info);
    }
}
