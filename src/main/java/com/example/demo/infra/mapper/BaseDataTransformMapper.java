package com.example.demo.infra.mapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import com.example.demo.util.DateTransformUtil;
import com.example.demo.util.JsonParseUtil;

/**
 * Base Data Transforming Mapper 此處定義非原生轉換資料的方法，如: 日期
 */
@Mapper(componentModel = "spring")
public interface BaseDataTransformMapper {

	/**
	 * 字串 Parse 成 Date
	 * 
	 * @param dateStr
	 * @return date
	 */
	@Named("parseStringToDate")
	public default Date parseStringToDate(String dateStr) {
		return DateTransformUtil.parse("yyyy-MM-dd", dateStr);
	}

	/**
	 * Date Format 成 字串
	 * 
	 * @param date Date
	 * @return String
	 */
	@Named("formatDateToString")
	public default String formatDateToString(Date date) {
		return DateTransformUtil.format("yyyy-MM-dd", date);
	}

	/**
	 * LocalDate Format 成 字串
	 * 
	 * @param date LocalDate
	 * @return String
	 */
	@Named("formatLocalDateToString")
	public default String formatLocalDateToString(LocalDate date) {
		return DateTransformUtil.transformLocalDateToString(date);
	}

	/**
	 * 將 List<String> 轉換為以逗號分隔的字串
	 * 
	 * @param target
	 * @return String
	 */
	@Named("transformListToString")
	public default String transformListToString(List<String> target) {
		return String.join(",", target);
	}

	/**
	 * 將以逗號分隔的字串轉換為 List<String>
	 * 
	 * @param target
	 * @return List<String>
	 */
	@Named("transformStringToList")
	public default List<String> transformStringToList(String target) {
		return Arrays.stream(target.split(",")).map(String::trim) // 去除多餘空白
				.filter(s -> !s.isEmpty()) // 過濾掉空字串
				.collect(Collectors.toList());
	}
	
	/**
	 * 將物件轉換為 json 字串
	 * 
	 * @param target 物件
	 * @return json 字串
	 */
	@Named("serializeObjectToJson")
	public default String serializeObjectToJson(Object target) {
		return JsonParseUtil.serialize(target);
	}
	
}