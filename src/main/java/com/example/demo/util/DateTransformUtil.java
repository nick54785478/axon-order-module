package com.example.demo.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 日期轉換工具
 */
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateTransformUtil {

	/**
	 * 將 LocalDate 轉為 字串
	 * 
	 * @param localDate 日期
	 * @return 日期字串
	 */
	public static String transformLocalDateToString(LocalDate localDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		if (Objects.isNull(localDate)) {
			return null;
		}
		return localDate.format(formatter);
	}

	/**
	 * 將 字串 轉為 LocalDate
	 */
	public static LocalDate transformStringToLocalDate(String localDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return LocalDate.parse(localDate, formatter);
	}

	/**
	 * String 轉換 Date
	 */
	public static Date parse(String pattern, String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		if (StringUtils.isBlank(date)) {
			return null;
		}

		return transformLocalDateTimeToDate(LocalDateTime.parse(date, formatter));
	}

	/**
	 * Date 轉換 String
	 */
	public static String format(String pattern, Date date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		if (Objects.isNull(date)) {
			return null;
		}
		LocalDateTime localDateTime = transformDateToLocalDateTime(date);
		return localDateTime.format(formatter);
	}

	/**
	 * 轉換字串為LocalDateTime
	 */
	public static LocalDateTime transformStringToLocalDateTime(String pattern, String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		return LocalDateTime.parse(date, formatter);
	}

	/**
	 * 將 LocalDateTime 轉換為 Date
	 */
	private static Date transformLocalDateTimeToDate(LocalDateTime date) {
		if (Objects.isNull(date)) {
			return null;
		}
		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * 將 Date 轉換為 LocalDateTime
	 */
	private static LocalDateTime transformDateToLocalDateTime(Date date) {
		if (Objects.isNull(date)) {
			return null;
		}
		Instant instant = date.toInstant();
		return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

}
