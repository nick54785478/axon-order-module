package com.example.demo.config.config;

import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

@Configuration
public class AxonConfiguration {

	@Bean
	@Primary
	public Serializer jacksonSerializer(ObjectMapper objectMapper) {
		ObjectMapper axonMapper = objectMapper.copy();

		// 1. 基礎模組註冊
		axonMapper.registerModule(new JavaTimeModule());
		axonMapper.registerModule(new ParameterNamesModule());

		// 2. 關鍵修正：忽略未知屬性，避免 ReplayToken 報錯
		axonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		// 3. 調整可見性，支援 Java Record
		axonMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		axonMapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

		// 4. 禁用時間戳格式
		axonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 5. 設定型別資訊
		axonMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL,
				JsonTypeInfo.As.WRAPPER_ARRAY);

		return JacksonSerializer.builder().objectMapper(axonMapper).build();
	}

	// 定義快照觸發器，這裡設定為每 50 個事件存一次快照
	@Bean
	public SnapshotTriggerDefinition orderSnapshotTriggerDefinition(Snapshotter snapshotter) {
		// 使用 EventCountSnapshotTriggerDefinition，這是最常見的根據事件數量觸發的策略
		return new EventCountSnapshotTriggerDefinition(snapshotter, 50);
	}

	/**
	 * 定義快照觸發策略
	 * <p>
	 * 當單一 Aggregate 的事件每增加 100 筆時，自動產生一個快照並存入 Axon Server。
	 * </p>
	 */
	@Bean(name = "productSnapshotTriggerDefinition")
	public SnapshotTriggerDefinition productSnapshotTriggerDefinition(Snapshotter snapshotter) {
		// 閾值設定為 100 (可依業務頻率調整)
		return new EventCountSnapshotTriggerDefinition(snapshotter, 100);
	}

	/**
	 * 配置 DeadlineManager
	 */
	@Bean
	public DeadlineManager deadlineManager(org.axonframework.common.transaction.TransactionManager transactionManager,
			org.axonframework.config.Configuration configuration) {
		// 使用 SimpleDeadlineManager，它使用線程池來管理任務
		return SimpleDeadlineManager.builder().scopeAwareProvider(new ConfigurationScopeAwareProvider(configuration))
				.transactionManager(transactionManager).build();
	}
}