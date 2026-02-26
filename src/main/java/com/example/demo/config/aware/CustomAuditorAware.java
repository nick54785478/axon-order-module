package com.example.demo.config.aware;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class CustomAuditorAware implements AuditorAware<String> {

	/**
	 * 取得當前的使用者帳號作為資料庫欄位的值。
	 * 
	 * @return 包含當前使用者帳號的 Optional 物件，若當前使用者帳號為空，則回傳 SYSTEM。
	 */
	@Override
	public Optional<String> getCurrentAuditor() {
		String currentUserAccount = "SYSTEM"; // 從上下文取得 使用者帳號
		return Optional.of(StringUtils.isNotBlank(currentUserAccount) ? currentUserAccount : "SYSTEM");
	}

}
