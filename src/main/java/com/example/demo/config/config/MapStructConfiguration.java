package com.example.demo.config.config;

import org.mapstruct.MapperConfig;

import com.example.demo.infra.mapper.BaseDataTransformMapper;

/**
 * MapStruct 配置，注入相關公用操作的 Mapper，只要在 Mapper 的 config 參數標註該 Config 即可
 */
@MapperConfig(componentModel = "spring", uses = { BaseDataTransformMapper.class })
public interface MapStructConfiguration {
}