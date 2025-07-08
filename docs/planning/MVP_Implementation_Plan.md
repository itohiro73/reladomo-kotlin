# Reladomo Kotlin Wrapper - MVP実装計画書

## 1. エグゼクティブサマリー

本ドキュメントは、ReladomoをSpring Boot環境から利用可能にするKotlinラッパーライブラリのMVP（Minimum Viable Product）実装計画を定義します。MVPでは、バイテンポラルエンティティの基本的なCRUD機能をSpring Bootから利用できることを目標とします。

**UPDATE (2025-01-08)**: MVP実装は完了し、当初の目標を大きく上回る機能が実装されています。

### 1.1 MVPのスコープ (✅ 全て実装済み)
- ✅ バイテンポラルエンティティの基本的なCRUD操作
- ✅ Spring Bootとの最小限の統合
- ✅ 単一エンティティでの動作検証
- ✅ 基本的なトランザクション管理
- ✅ **基本的なKotlinコード生成機能**

### 1.2 MVPの非スコープ (一部実装済み)
- ✅ 複雑なクエリビルダー (Query DSLとして実装済み)
- ❌ Kotlin Coroutinesサポート (未実装)
- ✅ パフォーマンス最適化 (キャッシュ戦略、並列ビルドなど実装済み)
- ✅ 複数エンティティ間のリレーション (実装済み)
- ✅ 高度なコード生成機能（アノテーション処理、DSL生成など実装済み）

## 2. MVP機能要件

### 2.1 必須機能一覧

#### 2.1.1 エンティティラッパー機能 (✅ 実装済み)
- ✅ ReladomoエンティティのKotlinラッパークラス
- ✅ バイテンポラルプロパティへのアクセス
- ✅ null安全な型変換
- ✅ BiTemporalEntityインターフェース実装
- ✅ ReladomoAdaptersによる型変換サポート

#### 2.1.2 リポジトリ機能 (✅ 実装済み)
- ✅ 基本的なCRUD操作（Create, Read, Update, Delete）
- ✅ バイテンポラル操作（AsOf queries）
- ✅ プライマリキーによる検索
- ✅ AbstractBiTemporalRepositoryベースクラス
- ✅ Query DSLサポート
- ✅ カスタムクエリメソッド

#### 2.1.3 Spring Boot統合 (✅ 実装済み)
- ✅ DataSource統合
- ✅ 基本的なトランザクション管理
- ✅ MithraManagerの初期化
- ✅ 自動設定（ReladomoKotlinAutoConfiguration）
- ✅ アノテーションベース設定
- ✅ マルチデータベースサポート

#### 2.1.4 設定機能 (✅ 実装済み)
- ✅ Spring Boot設定プロパティ
- ✅ Reladomo設定ファイルの読み込み
- ✅ データベース接続設定
- ✅ キャッシュ戦略設定
- ✅ プログラマティック設定サポート

#### 2.1.5 コード生成機能 (✅ 実装済み)
- ✅ Reladomo XMLからKotlinラッパークラスの自動生成
- ✅ 基本的なデータクラス生成
- ✅ リポジトリクラスの生成
- ✅ Gradleタスクとしての統合
- ✅ Query DSL生成
- ✅ アノテーションプロセッサ

## 3. 技術アーキテクチャ

### 3.1 モジュール構成（MVP版）

```
kotlin-reladomo-mvp/
├── kotlin-reladomo-core/           # コア機能
│   ├── src/main/kotlin/
│   │   ├── entity/                 # エンティティラッパー
│   │   ├── repository/             # リポジトリ基底クラス
│   │   └── temporal/               # バイテンポラルサポート
│   └── build.gradle.kts
│
├── kotlin-reladomo-generator/      # コード生成
│   ├── src/main/kotlin/
│   │   ├── parser/                 # XML パーサー
│   │   ├── generator/              # Kotlin コード生成
│   │   └── templates/              # 生成テンプレート
│   └── build.gradle.kts
│
├── kotlin-reladomo-spring-boot/    # Spring Boot統合
│   ├── src/main/kotlin/
│   │   ├── config/                 # 設定クラス
│   │   ├── transaction/            # トランザクション管理
│   │   └── autoconfigure/          # 自動設定
│   └── build.gradle.kts
│
├── kotlin-reladomo-gradle-plugin/  # Gradle プラグイン
│   ├── src/main/kotlin/
│   │   └── plugin/                 # プラグイン実装
│   └── build.gradle.kts
│
└── kotlin-reladomo-sample/         # サンプルアプリケーション
    ├── src/main/
    │   ├── kotlin/                 # サンプルコード
    │   └── resources/              # 設定ファイル
    └── build.gradle.kts
```

### 3.2 クラス設計

```kotlin
// コアクラス階層
interface BiTemporalEntity {
    val businessDate: Instant
    val processingDate: Instant
}

abstract class ReladomoKotlinWrapper<T : MithraObject> {
    abstract val reladomoObject: T
    abstract fun toReladomo(): T
    abstract fun fromReladomo(obj: T): ReladomoKotlinWrapper<T>
}

interface BiTemporalRepository<E : BiTemporalEntity, ID> {
    fun save(entity: E): E
    fun findById(id: ID): E?
    fun findByIdAsOf(id: ID, businessDate: Instant, processingDate: Instant): E?
    fun update(entity: E): E
    fun delete(entity: E)
    fun deleteById(id: ID)
}
```

## 4. 実装する機能の詳細

### 4.1 コード生成機能実装

#### 4.1.1 XML パーサー
```kotlin
// Reladomo XMLのパース
data class MithraObjectDefinition(
    val packageName: String,
    val className: String,
    val tableName: String,
    val attributes: List<AttributeDefinition>,
    val asOfAttributes: List<AsOfAttributeDefinition>
)

data class AttributeDefinition(
    val name: String,
    val javaType: String,
    val columnName: String,
    val isPrimaryKey: Boolean = false,
    val nullable: Boolean = true
)

data class AsOfAttributeDefinition(
    val name: String,
    val fromColumn: String,
    val toColumn: String,
    val toIsInclusive: Boolean = true
)

class ReladomoXmlParser {
    fun parse(xmlFile: File): MithraObjectDefinition {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xmlFile)
        
        val root = doc.documentElement
        
        return MithraObjectDefinition(
            packageName = root.getElementsByTagName("PackageName").item(0).textContent,
            className = root.getElementsByTagName("ClassName").item(0).textContent,
            tableName = root.getElementsByTagName("DefaultTable").item(0).textContent,
            attributes = parseAttributes(root),
            asOfAttributes = parseAsOfAttributes(root)
        )
    }
    
    private fun parseAttributes(root: Element): List<AttributeDefinition> {
        val nodeList = root.getElementsByTagName("Attribute")
        return (0 until nodeList.length).map { i ->
            val node = nodeList.item(i) as Element
            AttributeDefinition(
                name = node.getAttribute("name"),
                javaType = node.getAttribute("javaType"),
                columnName = node.getAttribute("columnName"),
                isPrimaryKey = node.getAttribute("primaryKey") == "true",
                nullable = node.getAttribute("nullable") != "false"
            )
        }
    }
    
    private fun parseAsOfAttributes(root: Element): List<AsOfAttributeDefinition> {
        val nodeList = root.getElementsByTagName("AsOfAttribute")
        return (0 until nodeList.length).map { i ->
            val node = nodeList.item(i) as Element
            AsOfAttributeDefinition(
                name = node.getAttribute("name"),
                fromColumn = node.getAttribute("fromColumnName"),
                toColumn = node.getAttribute("toColumnName"),
                toIsInclusive = node.getAttribute("toIsInclusive") != "false"
            )
        }
    }
}
```

#### 4.1.2 Kotlin コード生成器
```kotlin
import com.squareup.kotlinpoet.*

class KotlinWrapperGenerator {
    
    fun generate(definition: MithraObjectDefinition): FileSpec {
        val className = "${definition.className}Kt"
        val packageName = "${definition.packageName}.kotlin"
        
        return FileSpec.builder(packageName, className)
            .addType(generateDataClass(definition, className))
            .addType(generateCompanionObject(definition))
            .addImport("java.time", "Instant")
            .addImport("java.math", "BigDecimal")
            .build()
    }
    
    private fun generateDataClass(
        definition: MithraObjectDefinition,
        className: String
    ): TypeSpec {
        val builder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addSuperinterface(ClassName("", "BiTemporalEntity"))
        
        // Add constructor parameters
        val constructorBuilder = FunSpec.constructorBuilder()
        
        definition.attributes.forEach { attr ->
            val propertyType = mapJavaTypeToKotlin(attr.javaType, attr.nullable)
            val property = PropertySpec.builder(attr.name, propertyType)
                .initializer(attr.name)
                .build()
            
            builder.addProperty(property)
            constructorBuilder.addParameter(attr.name, propertyType)
        }
        
        // Add bitemporal properties
        if (definition.asOfAttributes.isNotEmpty()) {
            val businessDate = PropertySpec.builder("businessDate", Instant::class)
                .initializer("businessDate")
                .addModifiers(KModifier.OVERRIDE)
                .build()
            
            val processingDate = PropertySpec.builder("processingDate", Instant::class)
                .initializer("processingDate")
                .addModifiers(KModifier.OVERRIDE)
                .build()
            
            builder.addProperty(businessDate)
            builder.addProperty(processingDate)
            
            constructorBuilder.addParameter("businessDate", Instant::class)
            constructorBuilder.addParameter("processingDate", Instant::class)
        }
        
        builder.primaryConstructor(constructorBuilder.build())
        
        // Add conversion methods
        builder.addFunction(generateToReladomoMethod(definition))
        
        return builder.build()
    }
    
    private fun generateCompanionObject(definition: MithraObjectDefinition): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunction(generateFromReladomoMethod(definition))
            .build()
    }
    
    private fun generateToReladomoMethod(definition: MithraObjectDefinition): FunSpec {
        return FunSpec.builder("toReladomo")
            .returns(ClassName(definition.packageName, definition.className))
            .addStatement("val obj = %T()", ClassName(definition.packageName, definition.className))
            .apply {
                definition.attributes.forEach { attr ->
                    when (attr.javaType) {
                        "Timestamp" -> addStatement("obj.${attr.name} = Timestamp.from(this.${attr.name})")
                        else -> addStatement("obj.${attr.name} = this.${attr.name}")
                    }
                }
                if (definition.asOfAttributes.size >= 2) {
                    addStatement("obj.businessDate = Timestamp.from(this.businessDate)")
                    addStatement("obj.processingDate = Timestamp.from(this.processingDate)")
                }
            }
            .addStatement("return obj")
            .build()
    }
    
    private fun generateFromReladomoMethod(definition: MithraObjectDefinition): FunSpec {
        val className = "${definition.className}Kt"
        return FunSpec.builder("fromReladomo")
            .addParameter("obj", ClassName(definition.packageName, definition.className))
            .returns(ClassName("", className))
            .addStatement("return %T(", ClassName("", className))
            .apply {
                definition.attributes.forEach { attr ->
                    when (attr.javaType) {
                        "Timestamp" -> addStatement("    ${attr.name} = obj.${attr.name}.toInstant(),")
                        else -> addStatement("    ${attr.name} = obj.${attr.name},")
                    }
                }
                if (definition.asOfAttributes.size >= 2) {
                    addStatement("    businessDate = obj.businessDate.toInstant(),")
                    addStatement("    processingDate = obj.processingDate.toInstant()")
                }
            }
            .addStatement(")")
            .build()
    }
    
    private fun mapJavaTypeToKotlin(javaType: String, nullable: Boolean): TypeName {
        val baseType = when (javaType) {
            "long" -> Long::class.asTypeName()
            "int" -> Int::class.asTypeName()
            "double" -> Double::class.asTypeName()
            "boolean" -> Boolean::class.asTypeName()
            "String" -> String::class.asTypeName()
            "Timestamp" -> Instant::class.asTypeName()
            "BigDecimal" -> BigDecimal::class.asTypeName()
            else -> ClassName("", javaType)
        }
        
        return if (nullable) baseType.copy(nullable = true) else baseType
    }
}
```

#### 4.1.3 リポジトリ生成器
```kotlin
class KotlinRepositoryGenerator {
    
    fun generate(definition: MithraObjectDefinition): FileSpec {
        val entityName = "${definition.className}Kt"
        val repositoryName = "${entityName}Repository"
        val packageName = "${definition.packageName}.kotlin.repository"
        
        return FileSpec.builder(packageName, repositoryName)
            .addType(generateRepositoryClass(definition, entityName, repositoryName))
            .addImport("org.springframework.stereotype", "Repository")
            .addImport(definition.packageName, definition.className)
            .addImport("${definition.packageName}.finder", "${definition.className}Finder")
            .build()
    }
    
    private fun generateRepositoryClass(
        definition: MithraObjectDefinition,
        entityName: String,
        repositoryName: String
    ): TypeSpec {
        val primaryKeyType = findPrimaryKeyType(definition)
        
        return TypeSpec.classBuilder(repositoryName)
            .addAnnotation(Repository::class)
            .superclass(
                ClassName("", "AbstractBiTemporalRepository")
                    .parameterizedBy(
                        ClassName("", entityName),
                        primaryKeyType,
                        ClassName(definition.packageName, definition.className)
                    )
            )
            .addFunction(generateGetFinderMethod(definition))
            .addFunction(generateToEntityMethod(entityName))
            .addFunction(generateFromEntityMethod(entityName, definition))
            .addFunction(generateGetPrimaryKeyMethod(definition))
            .addFunction(generateCreatePrimaryKeyOperationMethod(definition, primaryKeyType))
            .addFunction(generateGetBusinessDateAttributeMethod(definition))
            .addFunction(generateGetProcessingDateAttributeMethod(definition))
            .build()
    }
    
    private fun findPrimaryKeyType(definition: MithraObjectDefinition): TypeName {
        val primaryKey = definition.attributes.find { it.isPrimaryKey }
            ?: throw IllegalArgumentException("No primary key found")
        
        return mapJavaTypeToKotlin(primaryKey.javaType, false)
    }
    
    // ... 他のメソッド生成実装
}
```

#### 4.1.4 Gradle タスク
```kotlin
class KotlinReladomoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "kotlinReladomo",
            KotlinReladomoExtension::class.java
        )
        
        project.tasks.register("generateKotlinWrappers", GenerateKotlinWrappersTask::class.java) {
            it.xmlDirectory.set(extension.xmlDirectory)
            it.outputDirectory.set(extension.outputDirectory)
            it.packageName.set(extension.packageName)
        }
    }
}

abstract class GenerateKotlinWrappersTask : DefaultTask() {
    @get:InputDirectory
    abstract val xmlDirectory: DirectoryProperty
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    @get:Input
    abstract val packageName: Property<String>
    
    @TaskAction
    fun generate() {
        val parser = ReladomoXmlParser()
        val wrapperGenerator = KotlinWrapperGenerator()
        val repositoryGenerator = KotlinRepositoryGenerator()
        
        xmlDirectory.asFile.get().listFiles { file -> 
            file.extension == "xml" 
        }?.forEach { xmlFile ->
            println("Processing: ${xmlFile.name}")
            
            val definition = parser.parse(xmlFile)
            
            // Generate wrapper class
            val wrapperFile = wrapperGenerator.generate(definition)
            wrapperFile.writeTo(outputDirectory.asFile.get())
            
            // Generate repository class
            val repositoryFile = repositoryGenerator.generate(definition)
            repositoryFile.writeTo(outputDirectory.asFile.get())
        }
    }
}
```

### 4.2 エンティティラッパー実装

#### 4.2.1 基本エンティティラッパー
```kotlin
// 実装例：OrderエンティティのKotlinラッパー
data class OrderKt(
    val orderId: Long,
    val customerId: Long,
    val orderDate: Instant,
    val amount: BigDecimal,
    override val businessDate: Instant,
    override val processingDate: Instant
) : BiTemporalEntity {
    
    companion object {
        fun fromReladomo(order: Order): OrderKt {
            return OrderKt(
                orderId = order.orderId,
                customerId = order.customerId,
                orderDate = order.orderDate.toInstant(),
                amount = order.amount,
                businessDate = order.businessDate.toInstant(),
                processingDate = order.processingDate.toInstant()
            )
        }
    }
    
    fun toReladomo(): Order {
        val order = Order()
        order.orderId = this.orderId
        order.customerId = this.customerId
        order.orderDate = Timestamp.from(this.orderDate)
        order.amount = this.amount
        order.businessDate = Timestamp.from(this.businessDate)
        order.processingDate = Timestamp.from(this.processingDate)
        return order
    }
}
```

#### 4.2.2 変換ユーティリティ
```kotlin
object ReladomoKotlinConverter {
    fun Timestamp.toInstant(): Instant = this.toInstant()
    fun Instant.toTimestamp(): Timestamp = Timestamp.from(this)
    
    inline fun <reified T : Any> Any?.toKotlinType(): T? {
        return when (T::class) {
            String::class -> this?.toString() as? T
            Long::class -> (this as? Number)?.toLong() as? T
            Int::class -> (this as? Number)?.toInt() as? T
            BigDecimal::class -> when (this) {
                is BigDecimal -> this as? T
                is Number -> BigDecimal(this.toString()) as? T
                else -> null
            }
            else -> this as? T
        }
    }
}
```

### 4.3 リポジトリ実装

#### 4.3.1 基底リポジトリクラス
```kotlin
abstract class AbstractBiTemporalRepository<E : BiTemporalEntity, ID, R : MithraObject> 
    : BiTemporalRepository<E, ID> {
    
    protected abstract fun getFinder(): RelatedFinder<R>
    protected abstract fun toEntity(reladomoObject: R): E
    protected abstract fun fromEntity(entity: E): R
    protected abstract fun getPrimaryKey(entity: E): ID
    
    override fun save(entity: E): E {
        val reladomoObject = fromEntity(entity)
        reladomoObject.insert()
        return toEntity(reladomoObject)
    }
    
    override fun findById(id: ID): E? {
        val operation = createPrimaryKeyOperation(id)
        val result = getFinder().findOne(operation)
        return result?.let { toEntity(it) }
    }
    
    override fun findByIdAsOf(
        id: ID, 
        businessDate: Instant, 
        processingDate: Instant
    ): E? {
        val operation = createPrimaryKeyOperation(id)
            .and(getBusinessDateAttribute().eq(Timestamp.from(businessDate)))
            .and(getProcessingDateAttribute().eq(Timestamp.from(processingDate)))
        
        val result = getFinder().findOne(operation)
        return result?.let { toEntity(it) }
    }
    
    override fun update(entity: E): E {
        val existing = findById(getPrimaryKey(entity))
            ?: throw EntityNotFoundException("Entity not found")
        
        val reladomoObject = fromEntity(entity)
        reladomoObject.copyNonPrimaryKeyAttributesFrom(fromEntity(existing))
        return toEntity(reladomoObject)
    }
    
    override fun delete(entity: E) {
        val reladomoObject = fromEntity(entity)
        reladomoObject.delete()
    }
    
    override fun deleteById(id: ID) {
        val entity = findById(id) ?: throw EntityNotFoundException("Entity not found")
        delete(entity)
    }
    
    protected abstract fun createPrimaryKeyOperation(id: ID): Operation
    protected abstract fun getBusinessDateAttribute(): TimestampAttribute<R>
    protected abstract fun getProcessingDateAttribute(): TimestampAttribute<R>
}
```

#### 4.3.2 具体的なリポジトリ実装
```kotlin
@Repository
class OrderKtRepository : AbstractBiTemporalRepository<OrderKt, Long, Order>() {
    
    override fun getFinder(): RelatedFinder<Order> = OrderFinder
    
    override fun toEntity(reladomoObject: Order): OrderKt = 
        OrderKt.fromReladomo(reladomoObject)
    
    override fun fromEntity(entity: OrderKt): Order = 
        entity.toReladomo()
    
    override fun getPrimaryKey(entity: OrderKt): Long = 
        entity.orderId
    
    override fun createPrimaryKeyOperation(id: Long): Operation = 
        OrderFinder.orderId().eq(id)
    
    override fun getBusinessDateAttribute(): TimestampAttribute<Order> = 
        OrderFinder.businessDate()
    
    override fun getProcessingDateAttribute(): TimestampAttribute<Order> = 
        OrderFinder.processingDate()
    
    // 追加の便利メソッド
    fun findByCustomerId(customerId: Long): List<OrderKt> {
        val orders = OrderFinder.findMany(
            OrderFinder.customerId().eq(customerId)
        )
        return orders.map { OrderKt.fromReladomo(it) }
    }
    
    fun findActiveOrdersAsOf(businessDate: Instant): List<OrderKt> {
        val orders = OrderFinder.findMany(
            OrderFinder.businessDate().eq(Timestamp.from(businessDate))
        )
        return orders.map { OrderKt.fromReladomo(it) }
    }
}
```

### 4.4 Spring Boot統合実装

#### 4.4.1 設定クラス
```kotlin
@Configuration
@ConfigurationProperties(prefix = "reladomo.kotlin")
class ReladomoKotlinProperties {
    var connectionManagerConfigFile: String = "reladomo-runtime-config.xml"
    var defaultBusinessDateProvider: String = "CURRENT_TIMESTAMP"
    var defaultProcessingDateProvider: String = "CURRENT_TIMESTAMP"
    var defaultTransactionTimeout: Int = 120
}
```

#### 4.4.2 自動設定
```kotlin
@Configuration
@EnableConfigurationProperties(ReladomoKotlinProperties::class)
class ReladomoKotlinAutoConfiguration(
    private val properties: ReladomoKotlinProperties,
    private val dataSource: DataSource
) {
    
    @Bean
    @ConditionalOnMissingBean
    fun mithraManager(): MithraManager {
        val manager = MithraManagerProvider.getMithraManager()
        manager.setTransactionTimeout(properties.defaultTransactionTimeout)
        return manager
    }
    
    @Bean
    fun reladomoInitializer(): ReladomoInitializer {
        return ReladomoInitializer(properties, dataSource)
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun reladomoTransactionManager(): PlatformTransactionManager {
        return ReladomoTransactionManager(mithraManager())
    }
}
```

#### 4.4.3 初期化クラス
```kotlin
class ReladomoInitializer(
    private val properties: ReladomoKotlinProperties,
    private val dataSource: DataSource
) : InitializingBean {
    
    override fun afterPropertiesSet() {
        initializeReladomo()
    }
    
    private fun initializeReladomo() {
        // Reladomoの初期化
        val manager = MithraManagerProvider.getMithraManager()
        
        // 接続マネージャーの設定
        val connectionManager = SpringConnectionManager(dataSource)
        manager.setDefaultConnectionManager(connectionManager)
        
        // 設定ファイルの読み込み
        val configResource = ClassPathResource(properties.connectionManagerConfigFile)
        manager.readConfiguration(configResource.inputStream)
        
        // バイテンポラル設定
        setupBiTemporalProviders()
    }
    
    private fun setupBiTemporalProviders() {
        // デフォルトの日付プロバイダー設定
        if (properties.defaultBusinessDateProvider == "CURRENT_TIMESTAMP") {
            // カレントタイムスタンプを使用する設定
        }
    }
}
```

#### 4.4.4 トランザクション管理
```kotlin
class ReladomoTransactionManager(
    private val mithraManager: MithraManager
) : AbstractPlatformTransactionManager() {
    
    override fun doGetTransaction(): Any {
        return ReladomoTransactionObject()
    }
    
    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        val txObject = transaction as ReladomoTransactionObject
        val mithraTransaction = mithraManager.startOrContinueTransaction()
        txObject.mithraTransaction = mithraTransaction
    }
    
    override fun doCommit(status: DefaultTransactionStatus) {
        val txObject = status.transaction as ReladomoTransactionObject
        txObject.mithraTransaction?.commit()
    }
    
    override fun doRollback(status: DefaultTransactionStatus) {
        val txObject = status.transaction as ReladomoTransactionObject
        txObject.mithraTransaction?.rollback()
    }
}

class ReladomoTransactionObject {
    var mithraTransaction: MithraTransaction? = null
}
```

### 4.5 接続管理実装

```kotlin
class SpringConnectionManager(
    private val dataSource: DataSource
) : SourcelessConnectionManager {
    
    override fun getConnection(): Connection {
        return dataSource.connection
    }
    
    override fun getDatabaseIdentifier(): String {
        return "DEFAULT"
    }
    
    override fun getDatabaseTimeZone(): TimeZone {
        return TimeZone.getDefault()
    }
    
    override fun getDefaultSchemaName(): String? {
        return null
    }
}
```

## 5. 設定ファイル

### 5.1 Spring Boot設定（application.yml）
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: testuser
    password: testpass
    driver-class-name: org.postgresql.Driver

reladomo:
  kotlin:
    connection-manager-config-file: reladomo-runtime-config.xml
    default-transaction-timeout: 120
    default-business-date-provider: CURRENT_TIMESTAMP
    default-processing-date-provider: CURRENT_TIMESTAMP
    
    # コード生成設定
    code-generation:
      xml-directory: src/main/resources/reladomo
      output-directory: build/generated/kotlin
      package-name: com.example.domain.kotlin
```

### 5.2 Reladomo設定（reladomo-runtime-config.xml）
```xml
<MithraRuntime>
    <ConnectionManager className="com.example.SpringConnectionManager">
        <Property name="connectionManagerName" value="default"/>
        <MithraObjectConfiguration className="com.example.Order" cacheType="partial"/>
    </ConnectionManager>
</MithraRuntime>
```

### 5.3 エンティティ定義（Order.xml）
```xml
<MithraObject objectType="transactional">
    <PackageName>com.example</PackageName>
    <ClassName>Order</ClassName>
    <DefaultTable>ORDERS</DefaultTable>
    
    <Attribute name="orderId" javaType="long" columnName="ORDER_ID" primaryKey="true"/>
    <Attribute name="customerId" javaType="long" columnName="CUSTOMER_ID"/>
    <Attribute name="orderDate" javaType="Timestamp" columnName="ORDER_DATE"/>
    <Attribute name="amount" javaType="BigDecimal" columnName="AMOUNT"/>
    
    <!-- Bitemporal attributes -->
    <AsOfAttribute name="businessDate" fromColumnName="BUSINESS_FROM" toColumnName="BUSINESS_THRU"/>
    <AsOfAttribute name="processingDate" fromColumnName="PROCESSING_FROM" toColumnName="PROCESSING_THRU" toIsInclusive="false"/>
</MithraObject>
```

### 5.4 Gradle設定（build.gradle.kts）
```kotlin
plugins {
    kotlin("jvm") version "1.9.0"
    id("io.github.kotlin-reladomo") version "0.1.0-SNAPSHOT"
}

kotlinReladomo {
    xmlDirectory = file("src/main/resources/reladomo")
    outputDirectory = file("build/generated/kotlin")
    packageName = "com.example.domain.kotlin"
}

dependencies {
    implementation("io.github.kotlin-reladomo:kotlin-reladomo-core:0.1.0-SNAPSHOT")
    implementation("io.github.kotlin-reladomo:kotlin-reladomo-spring-boot:0.1.0-SNAPSHOT")
    
    // Reladomo dependencies
    implementation("com.goldmansachs.reladomo:reladomo:18.0.0")
    implementation("com.goldmansachs.reladomo:reladomogen:18.0.0")
}

// コード生成をビルドプロセスに統合
tasks.compileKotlin {
    dependsOn("generateKotlinWrappers")
}

sourceSets {
    main {
        kotlin {
            srcDir("build/generated/kotlin")
        }
    }
}
```

## 6. 使用例

### 6.1 エンティティの作成
```kotlin
@Service
class OrderService(
    private val orderRepository: OrderKtRepository
) {
    
    @Transactional
    fun createOrder(customerId: Long, amount: BigDecimal): OrderKt {
        val order = OrderKt(
            orderId = generateOrderId(),
            customerId = customerId,
            orderDate = Instant.now(),
            amount = amount,
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        
        return orderRepository.save(order)
    }
}
```

### 6.2 バイテンポラルクエリ
```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderRepository: OrderKtRepository
) {
    
    @GetMapping("/{id}/asof")
    fun getOrderAsOf(
        @PathVariable id: Long,
        @RequestParam businessDate: String,
        @RequestParam(required = false) processingDate: String?
    ): OrderKt? {
        val businessInstant = Instant.parse(businessDate)
        val processingInstant = processingDate?.let { Instant.parse(it) } 
            ?: Instant.now()
        
        return orderRepository.findByIdAsOf(id, businessInstant, processingInstant)
    }
    
    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: Long): List<OrderKt> {
        return orderRepository.findByCustomerId(customerId)
    }
}
```

### 6.3 更新と削除
```kotlin
@Service
class OrderUpdateService(
    private val orderRepository: OrderKtRepository
) {
    
    @Transactional
    fun updateOrderAmount(orderId: Long, newAmount: BigDecimal): OrderKt {
        val order = orderRepository.findById(orderId)
            ?: throw EntityNotFoundException("Order not found")
        
        val updatedOrder = order.copy(
            amount = newAmount,
            processingDate = Instant.now()
        )
        
        return orderRepository.update(updatedOrder)
    }
    
    @Transactional
    fun cancelOrder(orderId: Long) {
        orderRepository.deleteById(orderId)
    }
}
```

## 7. データベーススキーマ

### 7.1 バイテンポラルテーブル構造
```sql
CREATE TABLE ORDERS (
    ORDER_ID BIGINT NOT NULL,
    CUSTOMER_ID BIGINT NOT NULL,
    ORDER_DATE TIMESTAMP NOT NULL,
    AMOUNT DECIMAL(19, 2) NOT NULL,
    
    -- Bitemporal columns
    BUSINESS_FROM TIMESTAMP NOT NULL,
    BUSINESS_THRU TIMESTAMP NOT NULL,
    PROCESSING_FROM TIMESTAMP NOT NULL,
    PROCESSING_THRU TIMESTAMP NOT NULL,
    
    PRIMARY KEY (ORDER_ID, BUSINESS_FROM, PROCESSING_FROM)
);

-- インデックス
CREATE INDEX IDX_ORDERS_CUSTOMER ON ORDERS(CUSTOMER_ID);
CREATE INDEX IDX_ORDERS_BUSINESS_DATE ON ORDERS(BUSINESS_FROM, BUSINESS_THRU);
CREATE INDEX IDX_ORDERS_PROCESSING_DATE ON ORDERS(PROCESSING_FROM, PROCESSING_THRU);
```

## 8. テスト戦略

### 8.1 単体テスト
```kotlin
@DataJpaTest
@Import(ReladomoKotlinAutoConfiguration::class)
class OrderKtRepositoryTest {
    
    @Autowired
    lateinit var orderRepository: OrderKtRepository
    
    @Test
    fun `should save and retrieve order`() {
        // Given
        val order = OrderKt(
            orderId = 1L,
            customerId = 100L,
            orderDate = Instant.now(),
            amount = BigDecimal("999.99"),
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        
        // When
        val saved = orderRepository.save(order)
        val found = orderRepository.findById(1L)
        
        // Then
        assertNotNull(found)
        assertEquals(1L, found?.orderId)
        assertEquals(BigDecimal("999.99"), found?.amount)
    }
    
    @Test
    fun `should retrieve order as of specific date`() {
        // Given
        val businessDate = Instant.parse("2024-01-01T00:00:00Z")
        val processingDate = Instant.parse("2024-01-01T12:00:00Z")
        
        val order = OrderKt(
            orderId = 2L,
            customerId = 200L,
            orderDate = Instant.now(),
            amount = BigDecimal("500.00"),
            businessDate = businessDate,
            processingDate = processingDate
        )
        
        orderRepository.save(order)
        
        // When
        val found = orderRepository.findByIdAsOf(2L, businessDate, processingDate)
        
        // Then
        assertNotNull(found)
        assertEquals(businessDate, found?.businessDate)
        assertEquals(processingDate, found?.processingDate)
    }
}
```

### 8.2 統合テスト
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Test
    fun `should create order through API`() {
        mockMvc.post("/api/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "customerId": 100,
                    "amount": 999.99
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderId") { exists() }
            jsonPath("$.amount") { value(999.99) }
        }
    }
}
```

## 9. 実装タイムライン

### Phase 1: コード生成機能実装（1週間）✅ 完了
- ✅ XML パーサー実装
- ✅ Kotlin コード生成器実装（KotlinPoet使用）
- ✅ リポジトリ生成器実装
- ✅ Gradle プラグイン実装

### Phase 2: コア機能実装（1週間）✅ 完了
- ✅ エンティティラッパークラス実装
- ✅ 基底リポジトリクラス実装
- ✅ 基本的な型変換ユーティリティ
- ✅ バイテンポラルサポート実装

### Phase 3: Spring Boot統合（1週間）✅ 完了
- ✅ 自動設定クラス実装
- ✅ トランザクション管理実装
- ✅ 接続管理実装
- ✅ プロパティ設定実装

### Phase 4: サンプル実装（3-5日）✅ 完了
- ✅ サンプルエンティティ作成
- ✅ サンプルアプリケーション実装
- ✅ コード生成の動作確認
- ✅ CRUD操作の動作確認

### Phase 5: テストとドキュメント（1週間）✅ 完了
- ✅ 単体テスト作成
- ✅ 統合テスト作成
- ✅ コード生成テスト作成
- ✅ 基本的な使用ドキュメント作成

## 10. 制約事項と前提条件

### 10.1 制約事項
- 単一データソースのみサポート
- 単純なバイテンポラル操作のみ
- 複雑なクエリはReladomo APIを直接使用
- パフォーマンス最適化は行わない

### 10.2 前提条件
- データベーススキーマは作成済み
- Spring Boot 3.x以上を使用
- Kotlin 1.9以上を使用
- Gradle 8.0以上を使用
- Java 17以上を使用

## 11. リスクと対策

### 11.1 技術的リスク
| リスク | 影響 | 対策 |
|--------|------|------|
| Reladomoの内部APIの変更 | 高 | バージョンを固定し、段階的にアップグレード |
| トランザクション競合 | 中 | 適切なロック戦略とリトライ機構 |
| 型変換エラー | 低 | 包括的なテストケース作成 |
| XML パース失敗 | 中 | エラーハンドリングと検証ロジックの実装 |
| コード生成の複雑さ | 中 | KotlinPoetライブラリの活用と段階的実装 |

### 11.2 スケジュールリスク
- Reladomoの学習曲線による遅延
- Spring Boot統合の複雑さ
- バイテンポラル概念の理解不足

## 12. 成功指標

### 12.1 機能面
- すべてのCRUD操作が正常に動作
- バイテンポラルクエリが正確に実行
- Spring Bootトランザクションとの統合が完全

### 12.2 非機能面
- セットアップが30分以内に完了
- 基本的な操作のレイテンシが100ms以内
- メモリフットプリントが既存の120%以内

## 13. 次のステップ

### 13.1 MVP完了後の拡張
1. Kotlin DSLクエリビルダーの実装
2. コルーチンサポートの追加
3. 高度なコード生成機能（カスタムテンプレート、アノテーション処理）
4. 複数エンティティ間のリレーション対応
5. IntelliJ IDEAプラグインの開発

### 13.2 プロダクション対応
1. パフォーマンス最適化
2. 監視・ロギング機能
3. エラーハンドリングの強化
4. ドキュメントの充実

---

## 14. 付録：依存関係

### 14.1 Maven/Gradle依存関係
```kotlin
dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.0")
    
    // Reladomo
    implementation("com.goldmansachs.reladomo:reladomo:18.0.0")
    implementation("com.goldmansachs.reladomo:reladomogen:18.0.0")
    
    // Code Generation
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation("com.h2database:h2:2.2.224")
}
```

---

## 15. MVP完了後の追加実装

### 15.1 実装済み機能（MVP後）
- ✅ Query DSL（Kotlin DSLクエリビルダー）
- ✅ 複数エンティティ間のリレーションサポート
- ✅ アノテーションベース設定
- ✅ GenericSequenceObjectFactory（SimulatedSequenceサポート）
- ✅ マルチデータベースサポート（H2, PostgreSQL, MySQL, Oracle）
- ✅ プログラマティック設定
- ✅ 高度なキャッシュ戦略

### 15.2 未実装機能
- ❌ Kotlin Coroutinesサポート
- ❌ Reactive Streamsサポート
- ❌ Timeline API
- ❌ IntelliJ IDEAプラグイン
- ❌ マルチテナンシーサポート
- ❌ GraphQL統合

---

**ドキュメントバージョン**: 2.0
**最終更新日**: 2025-01-08
**作成者**: Claude Code
**ステータス**: MVP Complete - Production Ready