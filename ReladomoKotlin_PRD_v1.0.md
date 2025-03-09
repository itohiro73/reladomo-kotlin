# Reladomo Kotlin Wrapper - Product Requirements Document

## 1. 概要

### 1.1 製品ビジョン
Reladomoの強力なORM機能をKotlinプロジェクト、特にSpring Bootアプリケーションで簡単に利用できるようにする薄いラッパーライブラリを提供する。このライブラリは、JavaベースのReladomoAPIをKotlinのイディオムに合わせて変換し、開発者の生産性を向上させる。

### 1.2 目的
- Reladomoの機能をKotlinの言語機能を活かした形で提供する
- Spring Bootとの統合を容易にする
- 最小限のオーバーヘッドで、Reladomoのすべてのコア機能にアクセスできるようにする
- Kotlinならではの簡潔な構文とNull安全性を活かしたAPIを提供する

### 1.3 対象ユーザー
- KotlinでSpring Bootアプリケーションを開発するエンジニア
- ReladomoをKotlinプロジェクトで使用したいデベロッパー
- Javaから移行中のプロジェクトチーム

## 2. 機能要件

### 2.1 コア機能

#### 2.1.1 Kotlin DSL for Reladomo
- Reladomoのクエリ機能をKotlin DSLとして提供
- ラムダ式を活用した直感的なクエリ構築
- 例:
  ```kotlin
  findPersons {
    where { Person::age greaterThan 30 }
    orderBy { Person::lastName ascending }
  }
  ```

#### 2.1.2 コルーチンサポート
- 非同期操作のためのsuspend関数をサポート
- Flow APIとの統合
- 例:
  ```kotlin
  suspend fun getPersonsAsync(): Flow<Person> = 
    findPersonsAsync {
      where { Person::department eq "Engineering" }
    }
  ```

#### 2.1.3 拡張関数
- Reladomoの主要クラスに対するKotlin拡張関数
- Null安全な操作を提供
- 例:
  ```kotlin
  person.updateWithTransaction {
    name = "John"
    age = 30
  }
  ```

#### 2.1.4 リレーションシップ DSL
- Reladomoのリレーションシップ機能をKotlin DSLとして提供
- 型安全なリレーションナビゲーション
- ラムダ式を使用した直感的なリレーションアクセス
- 例:
  ```kotlin
  // リレーションシップの定義
  class Person : ReladomoEntity<Person>() {
    var name: String by property()
    var address: Address by relationship()
    var orders: List<Order> by relationship()
    
    companion object : ReladomoCompanion<Person, Int>()
  }
  
  // リレーションシップの使用
  val person = Person.findById(1)
  val orderCount = person.orders.size
  
  // ディープフェッチ
  val personsWithOrders = Person.find {
    where { Person::age greaterThan 30 }
    deepFetch { Person::orders }
  }
  
  // リレーションシップ条件によるフィルタリング
  val personsWithExpensiveOrders = Person.find {
    where { 
      with(Person::orders) {
        Order::amount greaterThan 1000
      }
    }
  }
  ```

#### 2.1.5 バイテンポラルデータモデル
- Reladomoのバイテンポラルデータモデルを完全サポート
- 処理時間と業務時間の両方を扱うKotlin DSL
- 時間操作のための直感的なAPI
- 例:
  ```kotlin
  // バイテンポラルエンティティの定義
  class Product : ReladomoBitemporalEntity<Product>() {
    var name: String by property()
    var price: BigDecimal by property()
    
    companion object : ReladomoBitemporalCompanion<Product, Int>()
  }
  
  // 時間ベースの操作
  val product = Product.findById(1)
  
  // 業務日付での更新
  product.updateBusinessDate(businessDate = LocalDate.now().plusDays(1)) {
    price = BigDecimal("29.99")
  }
  
  // 特定時点のデータ検索
  val historicalProducts = Product.findAsOf {
    businessDate = LocalDate.of(2022, 1, 1)
    processingDate = LocalDateTime.of(2022, 6, 1, 0, 0)
    where { Product::price lessThan BigDecimal("50.00") }
  }
  
  // 時間範囲での検索
  val priceHistory = Product.findByIdForRange(
    id = 1,
    businessDateRange = LocalDate.of(2022, 1, 1)..LocalDate.of(2022, 12, 31)
  )
  
  // 履歴トラバース
  val priceChanges = product.businessDateHistory.map { it.price }
  ```

#### 2.1.6 データクラス変換
- ReladomoのエンティティとKotlinのデータクラス間の変換機能
- 自動マッピングサポート
- 例:
  ```kotlin
  data class PersonDTO(val id: Int, val name: String, val age: Int)
  
  val personDto: PersonDTO = person.toDataClass()
  ```

### 2.2 Spring Boot統合

#### 2.2.1 自動構成
- Spring Bootのオートコンフィグレーション対応
- 最小限の設定で使用可能
- 例:
  ```kotlin
  @EnableReladomoKotlin
  @SpringBootApplication
  class MyApplication
  ```

#### 2.2.2 トランザクション管理
- Spring Transactionalアノテーションとの互換性
- 宣言的トランザクション管理
- 例:
  ```kotlin
  @Service
  class PersonService(private val personRepository: PersonRepository) {
    @Transactional
    fun updatePerson(id: Int, name: String) {
      val person = personRepository.findById(id)
      person.name = name
    }
  }
  ```

#### 2.2.3 リポジトリパターン
- Spring Data風のリポジトリインターフェース
- カスタムメソッド定義サポート
- 例:
  ```kotlin
  interface PersonRepository : ReladomoRepository<Person, Int> {
    fun findByLastName(lastName: String): List<Person>
    fun countByDepartment(department: String): Int
  }
  ```

### 2.3 ユーティリティ機能

#### 2.3.1 テスト支援
- テスト用のユーティリティクラス
- インメモリデータベース対応
- 例:
  ```kotlin
  @Test
  fun `test person creation`() {
    reladomoTest {
      val person = Person.create("John", 30)
      person.save()
      
      val found = Person.findById(person.id)
      assertEquals("John", found.name)
    }
  }
  ```

#### 2.3.2 デバッグ・ロギング
- クエリやパフォーマンス情報のログ出力機能
- カスタマイズ可能なログレベル
- 例:
  ```kotlin
  ReladomoKotlin.enableLogging(LogLevel.DEBUG)
  ```

#### 2.3.3 マイグレーションツール
- JavaからKotlinへの移行をサポートするツール
- 既存のReladomoコードの変換支援
- 例:
  ```kotlin
  ReladomoMigrator.convertJavaToKotlin(javaFile, outputDir)
  ```

## 3. 非機能要件

### 3.1 パフォーマンス
- Reladomoのネイティブパフォーマンスを維持する（オーバーヘッド1%未満）
- ラッパーによる追加メモリ消費を最小限に抑える
- 大規模データセット処理の効率性を維持

### 3.2 互換性
- Reladomoの全バージョン（最低21.0以上）と互換性を持つ
- Java 11以上をサポート
- Kotlin 1.6以上をサポート
- Spring Boot 2.7および3.x系と互換性がある

### 3.3 拡張性
- カスタムReladomoプラグインと統合可能
- ユーザー定義の拡張を可能にする拡張ポイント
- サードパーティライブラリとの連携をサポート

### 3.4 保守性
- 包括的なドキュメント
- 80%以上のコードカバレッジを持つテスト
- 明確なバージョニングポリシー（セマンティックバージョニング）

## 4. 技術仕様

### 4.1 アーキテクチャ

#### 4.1.1 コンポーネント構成
- `core`: 基本的なラッパーとKotlin DSL
- `spring`: Spring Boot統合
- `coroutines`: コルーチンサポート
- `test`: テストユーティリティ
- `migration`: 移行ツール

#### 4.1.2 依存関係
- Reladomo (コア依存)
- Kotlin stdlib & reflection
- Kotlinx.coroutines (オプション)
- Spring Boot (オプション)

### 4.2 APIデザイン

#### 4.2.1 パッケージ構造
```
com.reladomokotlin
├── core
│   ├── dsl
│   ├── extensions
│   └── utils
├── spring
│   ├── config
│   └── repository
├── coroutines
├── test
└── migration
```

#### 4.2.2 主要インターフェース
- `ReladomoKotlin`: 主要設定とエントリーポイント
- `ReladomoQuery`: クエリ構築DSL
- `ReladomoRepository`: リポジトリパターン基本インターフェース
- `ReladomoCoroutines`: コルーチンサポート
- `ReladomoTest`: テストユーティリティ
- `ReladomoEntity`: 基本エンティティクラス
- `ReladomoBitemporalEntity`: バイテンポラルエンティティクラス
- `ReladomoRelationship`: リレーションシップ設定と操作

### 4.3 実装方針

#### 4.3.1 ラッパーアプローチ
- 委譲パターンを使用してReladomoのオリジナルAPIを呼び出す
- 最小限のオーバーヘッドを保証
- ラッパーオブジェクトのキャッシング

#### 4.3.2 最適化戦略
- リフレクションの使用を最小限に抑える
- インライン関数とreifiedジェネリクスの活用
- コードの自動生成によるパフォーマンス向上

#### 4.3.3 型安全性
- Kotlinの型システムを最大限に活用
- Null安全性を確保
- コンパイル時の型チェック

## 5. ユーザーエクスペリエンス

### 5.1 開発者体験

#### 5.1.1 簡単な導入
```kotlin
// build.gradle.kts
implementation("com.reladomokotlin:reladomo-kotlin-core:1.0.0")
implementation("com.reladomokotlin:reladomo-kotlin-spring:1.0.0") // オプション
```

#### 5.1.2 設定例
```kotlin
// application.properties
reladomo.kotlin.package-scan=com.example.domain
reladomo.kotlin.enable-logging=true
```

#### 5.1.3 基本的な使用例
```kotlin
// 基本エンティティ定義
class Person : ReladomoEntity<Person>() {
  var name: String by property()
  var age: Int by property()
  var address: Address by relationship()
  var orders: List<Order> by relationship()
  
  companion object : ReladomoCompanion<Person, Int>()
}

// バイテンポラルエンティティ定義
class Product : ReladomoBitemporalEntity<Product>() {
  var name: String by property()
  var price: BigDecimal by property()
  var category: Category by relationship()
  
  companion object : ReladomoBitemporalCompanion<Product, Int>()
}

// 基本的な検索
val persons = Person.find {
  where { Person::age greaterThan 30 }
  orderBy { Person::name ascending }
}

// リレーションシップを含む検索
val personsWithOrders = Person.find {
  where { Person::age greaterThan 30 }
  deepFetch { Person::orders }
}

// バイテンポラル検索
val productsAsOf = Product.findAsOf {
  businessDate = LocalDate.of(2023, 1, 1)
  where { Product::price lessThan BigDecimal("100.00") }
}

// エンティティ作成と保存
val newPerson = Person.create {
  name = "Alice"
  age = 25
}
newPerson.save()

// バイテンポラル更新
val product = Product.findById(1)
product.updateBusinessDate(businessDate = LocalDate.now().plusDays(1)) {
  price = BigDecimal("29.99")
}
```

### 5.2 ドキュメンテーション

#### 5.2.1 提供ドキュメント
- オンラインリファレンスガイド
- チュートリアルとサンプルプロジェクト
- Kotlin Doc API ドキュメント
- 移行ガイド（JavaからKotlin）

#### 5.2.2 サンプル
- Spring Boot統合サンプル
- コルーチン使用例
- 高度なクエリサンプル
- テスト例

## 6. ロードマップと優先度

### 6.1 リリースプラン

#### 6.1.1 v0.1.0 (Alpha)
- 基本的なKotlin DSLとコア機能
- 初期ドキュメント
- リミテッドユーザーへのフィードバック収集

#### 6.1.2 v0.5.0 (Beta)
- Spring Boot統合
- コルーチンサポート
- テストユーティリティ
- 包括的なドキュメント

#### 6.1.3 v1.0.0 (Stable)
- すべての計画機能の完成
- 包括的なテスト
- パフォーマンス最適化
- 本番環境での使用準備完了

### 6.2 優先順位

1. **高優先度**
   - コアKotlin DSL
   - リレーションシップDSL
   - バイテンポラルデータモデルサポート
   - 基本的な拡張関数
   - Null安全性
   - 基本的なSpring統合

2. **中優先度**
   - コルーチンサポート
   - リポジトリパターン
   - テストユーティリティ
   - 詳細なドキュメント
   - バイテンポラル操作の簡略化API

3. **低優先度**
   - 移行ツール
   - 高度なロギング
   - コード生成ツール
   - パフォーマンス最適化
   - 高度なリレーションシップ操作

## 7. 評価指標

### 7.1 成功基準
- リリース後6ヶ月以内に10以上の実運用プロジェクトで採用
- GitHub上で200以上のスター
- 月間ダウンロード数1000以上
- バグ報告の平均解決時間5日以内

### 7.2 ユーザーフィードバック
- GitHubイシューとディスカッション
- ユーザーサーベイ
- コミュニティQ&A
- プルリクエスト数

## 8. リスクと対策

### 8.1 リスク評価

| リスク | 影響度 | 確率 | 対策 |
|--------|---------|----------|---------|
| Reladomoの互換性が崩れる | 高 | 低 | 各バージョンでの自動テスト |
| パフォーマンスのオーバーヘッド | 中 | 中 | ベンチマークテストの実施 |
| Kotlin/Spring Bootのバージョン非互換 | 中 | 中 | マトリックステスト |
| 採用率が低い | 高 | 中 | 優れたドキュメント提供とコミュニティ育成 |

### 8.2 リスク緩和策
- CIでの自動互換性テスト
- ユーザーフィードバックの早期収集
- 包括的なドキュメント
- メンテナンス計画の明確化

## 9. 結論

Reladomo Kotlin Wrapperは、ReladomoのパワーとKotlinの簡潔性・安全性を組み合わせ、特にSpring Bootプロジェクトにおいて開発者の生産性を向上させるためのライブラリです。最小限のオーバーヘッドで、Kotlinのイディオムに合わせた直感的なAPIを提供し、開発者がReladomoの機能を最大限に活用できるようにします。