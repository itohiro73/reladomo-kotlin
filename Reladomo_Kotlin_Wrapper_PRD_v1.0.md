# Reladomo Kotlin Wrapper Library - Product Requirements Document (PRD)

## 1. エグゼクティブサマリー

### 1.1 プロジェクト概要
ReladomoというJava ORMフレームワークをKotlin/Spring Boot環境から透過的に利用可能にするラッパーライブラリの開発プロジェクト。

### 1.2 背景
Reladomoは Goldman Sachs が開発したエンタープライズグレードのJava ORMで、バイテンポラルデータモデルを透過的に扱える唯一のORMとして知られている。しかし、以下の理由によりKotlin/Spring Boot環境での利用に困難が生じている：

- Java中心の設計（XMLからJavaコード生成）
- Kotlin特有の機能（null安全性、コルーチン等）との統合不足
- Spring Bootとの統合に関する公式サポートの欠如
- 複雑な設定プロセス

### 1.3 目的
Kotlin/Spring Boot開発者がReladomoの強力なバイテンポラルデータモデル機能を、Kotlinらしい簡潔で型安全な方法で利用できるようにする。

## 2. 現状の課題の詳細分析

### 2.1 言語レベルの課題

#### 2.1.1 Javaコード生成への依存
```xml
<!-- 現状：ReladomoのXML定義 -->
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Order</ClassName>
    <DefaultTable>ORDERS</DefaultTable>
    
    <Attribute name="orderId" javaType="long" columnName="ORDER_ID" primaryKey="true"/>
    <Attribute name="customerId" javaType="long" columnName="CUSTOMER_ID"/>
    <Attribute name="orderDate" javaType="Timestamp" columnName="ORDER_DATE"/>
    
    <AsOfAttribute name="businessDate" fromColumnName="FROM_Z" toColumnName="THRU_Z"/>
    <AsOfAttribute name="processingDate" fromColumnName="IN_Z" toColumnName="OUT_Z" toIsInclusive="false"/>
</MithraObject>
```

**問題点：**
- 生成されるJavaコードはKotlinのnull安全性を活用していない
- Kotlin特有のデータクラス、プロパティアクセスが使えない
- 生成コードの修正が困難（再生成で上書きされる）

#### 2.1.2 Null安全性の欠如
```java
// 現状：生成されるJavaコード
public class Order extends OrderAbstract {
    public String getCustomerName() {
        // nullを返す可能性があるが、型システムで表現されない
        return this.customer != null ? this.customer.getName() : null;
    }
}
```

#### 2.1.3 ボイラープレートコード
```java
// 現状：Javaでのクエリ
OrderList orders = OrderFinder.findMany(
    OrderFinder.customerId().eq(123)
        .and(OrderFinder.orderDate().greaterThan(startDate))
        .and(OrderFinder.businessDate().eq(businessDate))
);
```

### 2.2 フレームワーク統合の課題

#### 2.2.1 Spring Boot設定の複雑さ
- MithraManagerの手動初期化が必要
- Spring Transactionとの統合が手動
- DataSourceの設定が重複
- Spring Boot Auto-configurationが利用できない

#### 2.2.2 依存性注入の問題
```java
// 現状：手動でのRepository実装が必要
@Repository
public class OrderRepository {
    public OrderList findByCustomerId(long customerId) {
        // Spring管理外のstaticメソッド呼び出し
        return OrderFinder.findMany(OrderFinder.customerId().eq(customerId));
    }
}
```

#### 2.2.3 非同期処理サポートの欠如
- Kotlin Coroutinesとの統合なし
- Reactive Streamsサポートなし
- 非同期トランザクション管理の困難さ

### 2.3 バイテンポラルデータアクセスの複雑さ

#### 2.3.1 タイムスタンプ指定の煩雑さ
```java
// 現状：バイテンポラルクエリ
Timestamp businessDate = new Timestamp(System.currentTimeMillis());
Timestamp processingDate = new Timestamp(System.currentTimeMillis());

OrderList orders = OrderFinder.findMany(
    OrderFinder.customerId().eq(123)
        .and(OrderFinder.businessDate().eq(businessDate))
        .and(OrderFinder.processingDate().eq(processingDate))
);
```

#### 2.3.2 履歴データアクセスの複雑さ
- 特定時点のデータ取得が煩雑
- 履歴変更の追跡が困難
- ビジネス日付と処理日付の管理が複雑

## 3. ソリューション設計

### 3.1 アーキテクチャ概要

```
┌─────────────────────────────────────────────────────┐
│             Kotlin Application Layer                 │
├─────────────────────────────────────────────────────┤
│         Reladomo Kotlin Wrapper Library             │
│  ┌─────────────────┐  ┌──────────────────────┐    │
│  │ Kotlin DSL      │  │ Spring Boot          │    │
│  │ Query Builder   │  │ Auto Configuration   │    │
│  ├─────────────────┤  ├──────────────────────┤    │
│  │ Coroutine       │  │ Transaction          │    │
│  │ Support         │  │ Management           │    │
│  ├─────────────────┤  ├──────────────────────┤    │
│  │ Type-Safe       │  │ Repository           │    │
│  │ Wrapper Classes │  │ Base Classes         │    │
│  └─────────────────┘  └──────────────────────┘    │
├─────────────────────────────────────────────────────┤
│              Original Reladomo Core                  │
└─────────────────────────────────────────────────────┘
```

### 3.2 主要コンポーネント

#### 3.2.1 Kotlin Code Generator
**機能：**
- Reladomo XMLからKotlinラッパークラスを生成
- null安全性を考慮した型定義
- データクラスとの統合
- 拡張関数の自動生成

**生成例：**
```kotlin
// 生成されるKotlinラッパー
@ReladomoEntity
data class OrderKt(
    val orderId: Long,
    val customerId: Long,
    val orderDate: Instant,
    val amount: BigDecimal,
    val status: OrderStatus
) : BiTemporalEntity {
    
    // 関連エンティティへの型安全なアクセス
    suspend fun customer(): CustomerKt? = 
        CustomerKtFinder.findOne { customerId eq this@OrderKt.customerId }
    
    // バイテンポラル操作
    fun asOf(businessDate: Instant, processingDate: Instant = Instant.now()): OrderKt? =
        this.reladomoObject.asOf(businessDate, processingDate)?.toKotlin()
}
```

#### 3.2.2 Kotlin DSL Query Builder
**機能：**
- 型安全なクエリ構築
- IDE自動補完サポート
- Kotlinらしい簡潔な記法

**使用例：**
```kotlin
// Kotlin DSLでのクエリ
val orders = OrderKtFinder.find {
    customerId eq 123
    orderDate greaterThan startDate
    amount between (100.0 to 1000.0)
    status inList listOf(OrderStatus.PENDING, OrderStatus.PROCESSING)
    
    // バイテンポラル条件
    asOf(businessDate = Instant.now(), processingDate = Instant.now())
}

// コルーチンサポート
suspend fun findActiveOrders(customerId: Long): Flow<OrderKt> = 
    OrderKtFinder.findFlow {
        this.customerId eq customerId
        status eq OrderStatus.ACTIVE
    }
```

#### 3.2.3 Spring Boot Auto Configuration
**機能：**
- 自動的なMithraManager初期化
- DataSource統合
- トランザクション管理統合
- キャッシュ設定の自動化

**設定例：**
```yaml
# application.yml
reladomo:
  kotlin:
    enabled: true
    code-generation:
      package: com.example.domain.kotlin
      target-directory: build/generated/kotlin
    
  connection-manager:
    default:
      datasource-ref: dataSource
      schema-name: PUBLIC
      
  cache:
    partial:
      - Order
      - Customer
    full:
      - Product
      - Category
      
  bitemporal:
    default-business-date: CURRENT_TIMESTAMP
    default-processing-date: CURRENT_TIMESTAMP
```

#### 3.2.4 Coroutine Support
**機能：**
- 非同期データアクセス
- Flow APIサポート
- サスペンド関数での操作

**使用例：**
```kotlin
@Service
class OrderService(
    private val orderRepository: OrderKtRepository
) {
    suspend fun processOrders(customerId: Long): List<OrderKt> = coroutineScope {
        val orders = orderRepository.findByCustomerId(customerId)
        
        orders.map { order ->
            async {
                // 非同期処理
                processOrder(order)
            }
        }.awaitAll()
    }
    
    fun streamOrders(): Flow<OrderKt> = flow {
        OrderKtFinder.findFlow {
            status eq OrderStatus.PENDING
        }.collect { order ->
            emit(order)
        }
    }
}
```

#### 3.2.5 Repository Base Classes
**機能：**
- Spring Data風のRepositoryインターフェース
- 共通CRUD操作の実装
- カスタムクエリのサポート

**使用例：**
```kotlin
@Repository
interface OrderKtRepository : ReladomoBiTemporalRepository<OrderKt, Long> {
    
    // 自動実装されるメソッド
    suspend fun findByCustomerId(customerId: Long): List<OrderKt>
    
    suspend fun findByStatusAndOrderDateBetween(
        status: OrderStatus,
        startDate: Instant,
        endDate: Instant
    ): List<OrderKt>
    
    // カスタムクエリ
    @Query
    suspend fun findHighValueOrders(minAmount: BigDecimal): List<OrderKt> = 
        find {
            amount greaterThan minAmount
            status notEq OrderStatus.CANCELLED
        }
    
    // バイテンポラル操作
    suspend fun findAsOf(
        businessDate: Instant,
        processingDate: Instant = Instant.now()
    ): List<OrderKt>
}
```

### 3.3 バイテンポラルデータアクセスの簡素化

#### 3.3.1 Timeline API
**機能：**
- 履歴データの直感的なアクセス
- 変更履歴の追跡
- タイムトラベルクエリ

**使用例：**
```kotlin
// タイムライン操作
val orderTimeline = order.timeline()

// 特定時点のデータ取得
val orderLastMonth = orderTimeline.asOf(
    businessDate = Instant.now().minus(30, ChronoUnit.DAYS)
)

// 履歴の取得
val history = orderTimeline.history(
    from = startDate,
    to = endDate,
    dimension = TimeDimension.BUSINESS_DATE
)

// 変更の検出
val changes = orderTimeline.changes(
    since = lastCheckDate,
    dimension = TimeDimension.PROCESSING_DATE
)
```

#### 3.3.2 Temporal Context
**機能：**
- スレッドローカルでの時間コンテキスト管理
- 自動的なタイムスタンプ付与

**使用例：**
```kotlin
// 時間コンテキストの設定
withTemporalContext(
    businessDate = reportDate,
    processingDate = Instant.now()
) {
    // このブロック内のすべてのクエリが指定された時点で実行される
    val orders = orderRepository.findAll()
    val customers = customerRepository.findActive()
}
```

### 3.4 型安全性の強化

#### 3.4.1 Sealed Class for Enums
```kotlin
// 型安全な列挙型
sealed class OrderStatus(val code: String) {
    object Pending : OrderStatus("PENDING")
    object Processing : OrderStatus("PROCESSING")
    object Completed : OrderStatus("COMPLETED")
    object Cancelled : OrderStatus("CANCELLED")
}
```

#### 3.4.2 Value Classes
```kotlin
// 型安全なID
@JvmInline
value class OrderId(val value: Long)

@JvmInline
value class CustomerId(val value: Long)

// 使用例
data class OrderKt(
    val orderId: OrderId,
    val customerId: CustomerId,
    // ...
)
```

## 4. 実装要件

### 4.1 技術スタック
- **言語**: Kotlin 1.9+
- **フレームワーク**: Spring Boot 3.2+
- **ビルドツール**: Gradle 8.0+ with Kotlin DSL
- **依存関係**:
  - Reladomo 18.0+
  - Kotlin Coroutines 1.7+
  - Kotlin Poet (コード生成用)
  - Spring Boot Starter

### 4.2 パフォーマンス要件
- コード生成: 100エンティティを10秒以内
- ラッパーのオーバーヘッド: 5%以内
- メモリ使用量: Reladomo単体使用時の110%以内

### 4.3 互換性要件
- 既存のReladomoコードとの共存
- 段階的な移行をサポート
- Reladomoのすべての機能へのアクセス

## 5. プロジェクトモジュール構成

### 5.1 モジュール分割
```
kotlin-reladomo/
├── kotlin-reladomo-core/              # コアライブラリ
├── kotlin-reladomo-generator/         # コード生成ツール
├── kotlin-reladomo-spring-boot/       # Spring Boot統合
├── kotlin-reladomo-spring-boot-starter/ # Auto Configuration
├── kotlin-reladomo-gradle-plugin/     # Gradleプラグイン
└── kotlin-reladomo-samples/           # サンプルプロジェクト
```

### 5.2 各モジュールの責務

#### kotlin-reladomo-core
- 基本的なラッパークラス
- DSLクエリビルダー
- 型変換ユーティリティ
- Temporal API

#### kotlin-reladomo-generator
- XMLパーサー
- Kotlinコード生成エンジン
- テンプレート管理
- 検証ロジック

#### kotlin-reladomo-spring-boot
- Repository基底クラス
- トランザクション管理
- 例外ハンドリング
- イベントリスナー

#### kotlin-reladomo-spring-boot-starter
- Auto Configuration
- Property管理
- ヘルスチェック
- メトリクス

#### kotlin-reladomo-gradle-plugin
- コード生成タスク
- ビルド統合
- インクリメンタルビルドサポート

## 6. API仕様

### 6.1 基本的なCRUD操作
```kotlin
// Create
val order = OrderKt(
    orderId = OrderId(1),
    customerId = CustomerId(100),
    orderDate = Instant.now(),
    amount = BigDecimal("999.99"),
    status = OrderStatus.Pending
)
val savedOrder = orderRepository.save(order)

// Read
val foundOrder = orderRepository.findById(OrderId(1))
val orders = orderRepository.findAll()

// Update
val updatedOrder = savedOrder.copy(
    status = OrderStatus.Processing
)
orderRepository.update(updatedOrder)

// Delete
orderRepository.delete(savedOrder)
orderRepository.deleteById(OrderId(1))
```

### 6.2 バイテンポラル操作
```kotlin
// 特定時点での作成
orderRepository.saveAsOf(
    entity = order,
    businessDate = effectiveDate,
    processingDate = Instant.now()
)

// 履歴の更新
orderRepository.updateHistory(
    entity = order,
    businessDateRange = DateRange(startDate, endDate),
    changes = mapOf("status" to OrderStatus.Completed)
)

// 履歴の削除（論理削除）
orderRepository.terminateAsOf(
    entity = order,
    businessDate = terminationDate
)
```

### 6.3 バッチ操作
```kotlin
// バッチ挿入
orderRepository.saveAll(orders)

// バッチ更新
orderRepository.updateAll(
    finder = { status eq OrderStatus.Pending },
    updates = { order ->
        order.copy(status = OrderStatus.Processing)
    }
)

// Multi-Threaded Loader統合
orderRepository.loadFrom(csvFile) {
    matchOn { orderId }
    onInsert { order -> /* 新規作成時の処理 */ }
    onUpdate { existing, new -> /* 更新時の処理 */ }
    onDelete { order -> /* 削除時の処理 */ }
}
```

### 6.4 トランザクション管理
```kotlin
@Transactional
suspend fun processOrder(orderId: OrderId) {
    val order = orderRepository.findById(orderId) 
        ?: throw OrderNotFoundException(orderId)
    
    // Reladomoトランザクションとの統合
    withReladomoTransaction {
        order.updateStatus(OrderStatus.Processing)
        inventoryService.reserveItems(order.items)
        paymentService.processPayment(order.payment)
        order.updateStatus(OrderStatus.Completed)
    }
}
```

## 7. 設定仕様

### 7.1 Gradle設定
```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.0"
    id("io.github.kotlin-reladomo") version "1.0.0"
}

kotlinReladomo {
    xmlDirectory = file("src/main/resources/reladomo")
    generatedSourcesDirectory = file("build/generated/kotlin")
    packageName = "com.example.domain.kotlin"
    
    features {
        generateDataClasses = true
        generateCoroutineSupport = true
        generateSpringRepositories = true
        generateValueClasses = true
    }
    
    temporal {
        defaultBusinessDateProvider = "CURRENT_TIMESTAMP"
        defaultProcessingDateProvider = "CURRENT_TIMESTAMP"
    }
}

dependencies {
    implementation("io.github.kotlin-reladomo:kotlin-reladomo-spring-boot-starter:1.0.0")
}
```

### 7.2 Spring Boot設定
```kotlin
@Configuration
@EnableReladomoKotlin
class ReladomoConfig {
    
    @Bean
    fun reladomoCustomizer(): ReladomoKotlinCustomizer {
        return ReladomoKotlinCustomizer { config ->
            config.apply {
                defaultIsolationLevel = IsolationLevel.READ_COMMITTED
                defaultTimeZone = ZoneId.of("Asia/Tokyo")
                enableMetrics = true
                enableTracing = true
            }
        }
    }
    
    @Bean
    fun temporalContextProvider(): TemporalContextProvider {
        return CurrentTimestampProvider()
    }
}
```

## 8. エラーハンドリング

### 8.1 例外階層
```kotlin
sealed class ReladomoKotlinException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class EntityNotFoundException(
    entityType: KClass<*>,
    id: Any
) : ReladomoKotlinException("Entity ${entityType.simpleName} with id $id not found")

class TemporalConstraintViolationException(
    message: String
) : ReladomoKotlinException(message)

class OptimisticLockException(
    entity: Any,
    expectedVersion: Int,
    actualVersion: Int
) : ReladomoKotlinException(
    "Optimistic lock failed for $entity. Expected version: $expectedVersion, Actual: $actualVersion"
)
```

### 8.2 エラーハンドリング戦略
```kotlin
// グローバル例外ハンドラー
@RestControllerAdvice
class ReladomoExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                code = "ENTITY_NOT_FOUND",
                message = ex.message ?: "Entity not found"
            ))
    }
    
    @ExceptionHandler(TemporalConstraintViolationException::class)
    fun handleTemporalConstraint(ex: TemporalConstraintViolationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                code = "TEMPORAL_CONSTRAINT_VIOLATION",
                message = ex.message ?: "Temporal constraint violated"
            ))
    }
}
```

## 9. テスト戦略

### 9.1 単体テスト
```kotlin
@ReladomoKotlinTest
class OrderRepositoryTest {
    
    @Autowired
    lateinit var orderRepository: OrderKtRepository
    
    @Test
    fun `should save and retrieve order`() = runTest {
        // Given
        val order = OrderKt(
            orderId = OrderId(1),
            customerId = CustomerId(100),
            orderDate = Instant.now(),
            amount = BigDecimal("999.99"),
            status = OrderStatus.Pending
        )
        
        // When
        val saved = orderRepository.save(order)
        val found = orderRepository.findById(OrderId(1))
        
        // Then
        assertThat(found).isNotNull()
        assertThat(found?.orderId).isEqualTo(OrderId(1))
    }
    
    @Test
    fun `should handle bitemporal queries correctly`() = runTest {
        // テスト用の時間コンテキスト
        withTestTemporalContext(
            businessDate = "2024-01-01T00:00:00Z",
            processingDate = "2024-01-01T12:00:00Z"
        ) {
            // バイテンポラルテスト
        }
    }
}
```

### 9.2 統合テスト
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Test
    fun `should process order through API`() {
        mockMvc.post("/api/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "customerId": 100,
                    "items": [
                        {"productId": 1, "quantity": 2},
                        {"productId": 2, "quantity": 1}
                    ]
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderId") { exists() }
            jsonPath("$.status") { value("PENDING") }
        }
    }
}
```

## 10. マイグレーション戦略

### 10.1 段階的移行
```kotlin
// Phase 1: 既存のReladomoエンティティと共存
@Repository
class HybridOrderRepository(
    private val kotlinRepository: OrderKtRepository
) {
    // 既存のJava API
    fun findOrder(id: Long): Order? {
        return OrderFinder.findByPrimaryKey(id)
    }
    
    // 新しいKotlin API
    suspend fun findOrderKt(id: OrderId): OrderKt? {
        return kotlinRepository.findById(id)
    }
}

// Phase 2: 完全移行
@Deprecated("Use OrderKtRepository instead")
class LegacyOrderRepository
```

### 10.2 移行ツール
```kotlin
// 移行支援ツール
object ReladomoMigrationTool {
    fun generateMigrationReport(projectPath: Path): MigrationReport {
        // 既存のReladomo使用箇所を分析
        // 移行の影響度を評価
        // 推奨される移行手順を生成
    }
    
    fun convertJavaToKotlin(
        javaRepository: Path,
        outputPath: Path
    ): ConversionResult {
        // JavaリポジトリをKotlinに変換
        // 必要な設定を生成
    }
}
```

## 11. パフォーマンス最適化

### 11.1 キャッシュ戦略
```kotlin
@Cacheable(
    strategy = CacheStrategy.FULL,
    evictionPolicy = EvictionPolicy.LRU,
    maxSize = 10000
)
data class ProductKt(
    val productId: ProductId,
    val name: String,
    val price: BigDecimal
)
```

### 11.2 バッチ処理最適化
```kotlin
// 効率的なバッチ処理
orderRepository.batchProcess(
    batchSize = 1000,
    parallel = true,
    processors = 4
) { batch ->
    // バッチ単位での処理
}
```

## 12. モニタリングとメトリクス

### 12.1 メトリクス収集
```kotlin
@Component
class ReladomoMetricsCollector {
    
    @EventListener
    fun onQueryExecuted(event: QueryExecutedEvent) {
        metrics.record(
            name = "reladomo.query.execution",
            tags = mapOf(
                "entity" to event.entityType,
                "operation" to event.operationType
            ),
            duration = event.duration
        )
    }
}
```

### 12.2 ヘルスチェック
```kotlin
@Component
class ReladomoHealthIndicator : HealthIndicator {
    
    override fun health(): Health {
        return try {
            val cacheStats = MithraManagerProvider.getMithraManager().cacheStats
            Health.up()
                .withDetail("totalCached", cacheStats.totalCached)
                .withDetail("hitRate", cacheStats.hitRate)
                .build()
        } catch (ex: Exception) {
            Health.down(ex).build()
        }
    }
}
```

## 13. セキュリティ考慮事項

### 13.1 データアクセス制御
```kotlin
@PreAuthorize("hasRole('ADMIN') or #customerId == authentication.principal.customerId")
suspend fun findOrdersByCustomer(customerId: CustomerId): List<OrderKt>

@PostFilter("hasPermission(filterObject, 'READ')")
suspend fun findAllOrders(): List<OrderKt>
```

### 13.2 監査ログ
```kotlin
@Audited
data class OrderKt(
    // ... fields ...
) {
    @CreatedBy
    val createdBy: String? = null
    
    @CreatedDate
    val createdDate: Instant? = null
    
    @LastModifiedBy
    val lastModifiedBy: String? = null
    
    @LastModifiedDate
    val lastModifiedDate: Instant? = null
}
```

## 14. リリース計画

### 14.1 マイルストーン

#### v0.1.0 (MVP)
- 基本的なKotlinラッパー生成
- シンプルなクエリビルダー
- 基本的なSpring Boot統合

#### v0.2.0
- Coroutineサポート
- 高度なクエリビルダー機能
- バイテンポラルAPI

#### v0.3.0
- Spring Boot Starter
- 自動設定
- Repository基底クラス

#### v1.0.0 (GA)
- 完全な機能セット
- パフォーマンス最適化
- 本番環境対応

### 14.2 成功指標
- Kotlin/Spring Bootプロジェクトでの採用率
- 開発生産性の向上（コード量50%削減）
- パフォーマンスオーバーヘッド5%以内
- バグ発生率の低減

## 15. 付録

### 15.1 用語集
- **バイテンポラル**: ビジネス時間と処理時間の2つの時間軸を持つデータモデル
- **AsOf**: 特定時点でのデータ状態を表す
- **Timeline**: エンティティの履歴を表現するAPI

### 15.2 参考資料
- [Reladomo公式ドキュメント](https://goldmansachs.github.io/reladomo/)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

**ドキュメントバージョン**: 1.0
**最終更新日**: 2024-12-28
**作成者**: Claude Code
**レビュー状態**: Draft