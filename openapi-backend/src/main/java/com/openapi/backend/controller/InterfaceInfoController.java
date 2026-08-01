package com.openapi.backend.controller;

import com.openapi.backend.entity.InterfaceInfo;
import com.openapi.backend.service.InterfaceInfoService;
import com.openapi.common.exception.BusinessException;
import com.openapi.common.model.ApiResponse;
import com.openapi.common.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/interface")
@RequiredArgsConstructor
public class InterfaceInfoController {

    private final InterfaceInfoService interfaceInfoService;

    @GetMapping("/list")
    public ApiResponse<List<InterfaceInfo>> listOnline() {
        return ApiResponse.ok(interfaceInfoService.listOnline());
    }

    @GetMapping("/{id}")
    public ApiResponse<InterfaceInfo> detail(@PathVariable Long id) {
        InterfaceInfo info = interfaceInfoService.getById(id);
        if (info == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不存在");
        }
        return ApiResponse.ok(info);
    }
}
