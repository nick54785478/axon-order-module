package com.example.demo.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Json 轉換器
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonParseUtil {

	protected static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * 序列化物件 為 JSON
	 * 
	 * @param target
	 * @return 序列化 JSON 字串
	 */
	public static String serialize(Object target) {
		try {
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			return mapper.writeValueAsString(target);
		} catch (JsonProcessingException e) {
			log.error("Occurred JsonMapping Exception", e);
			return "";
		}
	}

	/**
	 * 反序列化 JSON 回 物件
	 * 
	 * @param target  目標 序列化 JSON 字串
	 * @param 欲轉換物件類型
	 * @return 反序列化物件
	 */
	public static <T> T unserialize(String target, Class<T> clazz) {
		try {
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			return mapper.readValue(target, clazz);
		} catch (JsonMappingException e) {
			log.error("Occurred JsonMapping Exception", e);
			return null;
		} catch (JsonProcessingException e) {
			log.error("Occurred JsonProcessing Exception", e);
			return null;
		}
	}

	/**
	 * 反序列化 JSON 回 物件列表
	 * 
	 * @param target json 字串
	 * @param clazz  目標物件類型
	 * @return 物件列表
	 */
	public static <T> List<T> unserializeArrayOfObject(String target, Class<T> clazz) {
		try {
			return mapper.readValue(target, mapper.getTypeFactory().constructCollectionType(List.class, clazz));
		} catch (JsonProcessingException e) {
			log.error("Occurred JsonProcessing Exception", e);
			return new ArrayList<>();
		}
	}
}