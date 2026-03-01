# Order Management System - Event-Driven Saga

這是一個基於 Axon Framework 實作的高性能、彈性訂單管理系統。本專案是一個採用 事件溯源 (Event Sourcing) 與 CQRS (命令查詢職責分離) 模式構建的現代化訂單管理系統，其核心目標是透過 Orchestration Saga 模式，優雅地解決微服務架構中複雜的跨領域一致性問題。

**技術棧與基礎設施**

| 分類 | 技術選型 | 職責說明 |
| --- | --- | --- |
| 核心框架 | Axon Framework | 提供 Command/Event/Query Bus 與生命週期管理 | 
| 資料轉換 | MapStruct | 實作 ACL (防腐層)，確保 Domain 與 API 契約解耦 | 
| 持久化 (寫) | Axon Server | 專用的 Event Store，存儲不可變的事實 (Facts) | 
| 持久化 (讀) | MySQL + JPA | 針對查詢優化的投影模型 (Read Model) | 
| 序列化	 | Jackson | 標準化訊息傳輸格式 | 

---

## 核心架構：六角形與整潔架構 (Hexagonal Architecture)

專案遵循整潔架構原則，確保業務邏輯 (Domain Layer) 與技術細節（Infrastructure Layer，如資料庫、API 協議）完全隔離，提升測試覆蓋率與維護靈活性。

* **Domain Layer (核心層)**：包含各個聚合根（Aggregates），定義純粹的業務規則與防禦邏輯。

>* Order Aggregate：管理訂單狀態機（建立、取消、完結）。
>* Product Aggregate：管理產品資訊與庫存水位。
>* Payment Aggregate：管理支付紀錄與金額校驗邏輯。

* **Application Layer (應用層)**：

>* Saga： 由 OrderManagementSaga 擔任劇本導演，協調訂單、支付與出貨的流轉。
>* Application Services：透過 CommandService 與 QueryService 封裝 Use Case，並利用 MapStruct 進行 Entity 與 DTO 的轉換，確保 API 契約的穩定性。

* **Infrastructure Adapter (基礎設施層)**：

>* Input Adapters：REST 控制器（Controllers），處理外界請求。
>* Output Adapters：JPA 投影處理器（Projections），負責同步讀取模型，並將事件流精確投影至 MySQL 讀取模型。

---

## Saga 業務流程設計

本系統採用 **「協調型 Saga (Orchestration)」**，將訂單建立與支付執行解耦，支援手動觸發支付與自動超時保護，並確保分散式環境下的最終一致性。

### 標準成功路徑 (Happy Path)：

**1. 下單**：用戶發送 CreateOrderCommand。

**2. 庫存預留**：Saga 監聽到訂單建立，發起 ReduceStockCommand。

**3. 支付準備**：庫存扣減成功後，Saga 建立支付紀錄並開啟 10 分鐘倒數 (Deadline)。

**4. 執行扣款**：用戶觸發支付，Payment 執行金額與餘額校驗。

**5. 流程終點**：支付成功，Saga 觸發出貨通知並結束生命週期。

### 自動超時保護 (Timeout Path)

* 若用戶在 10 分鐘內未完成支付，DeadlineManager 觸發 handlePaymentTimeout。

* Saga 自動發送 CancelOrderCommand。

* 全鏈路同步：Saga 監聽到訂單取消，同步發送 CancelPaymentCommand 關閉支付紀錄，防止「過期支付」。

### 精確補償機制 (Resilience & Compensation)

* **庫存不足**：若任一品項扣減失敗，Saga 立即觸發 CancelOrderCommand。
* **支付超時**：Deadline 觸發後，Saga 自動取消訂單，並同步發起庫存還原 (AddStockCommand)。
* **退款處理**：若訂單在已付款狀態下取消，Saga 會自動觸發 RefundPaymentCommand。

註. 
1. 用戶可隨時發起取消。
2. Saga 具備 狀態感知能力：
>* 若未付款：僅同步取消支付紀錄。
>* 若已付款：自動觸發 RefundPaymentCommand 進行退款。

---

## 技術關鍵點 (Technical Highlights)

* **Event Sourcing**： 所有狀態變更均由事件驅動，提供完美的審計日誌與歷史追溯。

* **Snapshotting (快照)**： 配置 EventCountSnapshotTriggerDefinition。每累積數個事件自動產生快照，確保聚合根載入效能恆定在 $O(1)$。。

* **Defensive Design (防禦性設計)**： Payment 聚合根內建狀態機校驗，杜絕「錢扣了但訂單已取消」的異態。

* **MapStruct DTO**： 所有查詢 API 均不直接回傳實體，透過 Mapper 轉為 DTO，並支持充血模型的方法轉換（如 canShip）。

* **Non-blocking API**： 全面採用 CompletableFuture<ResponseEntity<T>>，提升系統吞吐量。

* **樂觀鎖控制 (Optimistic Locking)**： Product 聚合根使用 @AggregateVersion。前端更新產品時必須攜帶 version，由 Axon 自動偵測併發衝突，杜絕 Lost Update。

* **強一致性金額校驗**： 在 Payment 聚合根內部執行金額比對（指令金額 vs 紀錄金額），利用聚合根的強一致性邊界防止惡意篡改。

* **非同步分頁查詢**： 查詢端支援 PageQueriedView 具體類別，提供高性能的名稱模糊搜尋與後端分頁，規避分散式環境下的泛型擦除問題。


## 快速開始

1. 啟動 Axon Server、MySQL (可透過 docker-compose.yml 建立容器)。

2. 配置 MySQL product_view 與 order_view 等表結構 (可執行 init-schema.sql 內的 SQL 語法)。

3. 使用 Postman 等 API 工具呼叫 API 進行測試。
