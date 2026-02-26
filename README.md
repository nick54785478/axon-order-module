# Order Management System - Event-Driven Saga

這是一個基於 Axon Framework 實作的高性能、彈性訂單管理系統。本專案採用 事件溯源 (Event Sourcing) 與 CQRS (命令查詢職責分離) 模式，並透過 Orchestration Saga 處理複雜的跨領域長事務。

**技術及外部依賴**

**1. 技術**
* Axon Framwork
* MapStruct : 資料轉換
* Jackson
* Spring Data JPA

**2. 外部依賴**
* Axon Server : 事件儲存 (寫)
* MySQL : 查詢資料庫 (讀)

---

## 核心架構：六角形與整潔架構 (Hexagonal Architecture)

專案遵循整潔架構原則，確保業務邏輯（Domain）與技術細節（Infrastructure）完全解耦：

* Domain Layer (核心層)：包含 Order 與 Payment 聚合根（Aggregates），定義純粹的業務規則與防禦邏輯。

* Application Layer (應用層)：
>* Saga： 由 OrderManagementSaga 擔任劇本導演，協調訂單、支付與出貨的流轉。
>* Application Services：透過 CommandService 與 QueryService 封裝 Use Case，並利用 MapStruct 進行 Entity 與 DTO 的轉換，確保 API 契約的穩定性。

* Infrastructure Adapter (基礎設施層)：

>* Input Adapters：REST 控制器（Controllers），處理外界請求。
>* Output Adapters：JPA 投影處理器（Projections），負責讀取模型同步。

---

## Saga 業務流程設計

本系統採用 **「協調型 Saga (Orchestration)」**，將訂單建立與支付執行解耦，支援手動觸發支付與自動超時保護。

### 標準成功路徑 (Happy Path)：

**1. 下單**：用戶發送 CreateOrderCommand。

**2. Saga 啟動**：監聽到 OrderCreatedEvent，預約一個 10 分鐘支付倒數 (Deadline)。

**3. 支付準備**：Saga 發送 CreatePaymentCommand 建立「待支付」紀錄。

**4. 外部支付**：用戶透過支付 API 觸發 ProcessPaymentCommand。

**5. 支付成功**：Saga 收到 PaymentProcessedEvent，取消倒數，並發送 NotifyShipmentCommand。

### 自動超時保護 (Timeout Path)

* 若用戶在 10 分鐘內未完成支付，DeadlineManager 觸發 handlePaymentTimeout。

* Saga 自動發送 CancelOrderCommand。

* 全鏈路同步：Saga 監聽到訂單取消，同步發送 CancelPaymentCommand 關閉支付紀錄，防止「過期支付」。

### 手動取消與退款 (Compensation Path)

* 用戶可隨時發起取消。

* Saga 具備 狀態感知能力：

>* 若未付款：僅同步取消支付紀錄。
>* 若已付款：自動觸發 RefundPaymentCommand 進行退款。

---

## 技術關鍵點 (Technical Highlights)

* **Event Sourcing**：所有狀態變更均由事件驅動，提供完美的審計日誌與歷史追溯。

* **Snapshotting (快照)**：配置 EventCountSnapshotTriggerDefinition，每 50 個事件自動產生快照，確保讀取性能 $O(1)$。

* **Defensive Design (防禦性設計)**：Payment 聚合根內建狀態機校驗，杜絕「錢扣了但訂單已取消」的異態。

* **MapStruct DTO**：所有查詢 API 均不直接回傳實體，透過 Mapper 轉為 DTO，並支持充血模型的方法轉換（如 canShip）。

* **Non-blocking API**：全面採用 CompletableFuture<ResponseEntity<T>>，提升系統吞吐量。

