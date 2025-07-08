# Reladomo Kotlin Wrapper - 残機能実装ロードマップ

**作成日**: 2025-01-08  
**優先度**: 高 → 中 → 低

## 🎯 高優先度機能（推奨実装順）

### 1. Kotlin Coroutinesサポート
**推定工数**: 1週間

#### 実装内容
```kotlin
// Suspend関数でのリポジトリ操作
suspend fun OrderKtRepository.findByIdSuspend(id: Long): OrderKt?
suspend fun OrderKtRepository.saveAll(orders: List<OrderKt>): List<OrderKt>

// Flow APIサポート
fun OrderKtRepository.findAllAsFlow(): Flow<OrderKt>
fun OrderKtRepository.observeChanges(): Flow<ChangeEvent<OrderKt>>

// 非同期トランザクション
suspend fun <T> withReladomoTransaction(block: suspend () -> T): T
```

#### 実装手順
1. コルーチンコンテキストの定義
2. リポジトリインターフェースへのsuspend関数追加
3. 非同期トランザクションマネージャーの実装
4. Flow APIアダプターの作成
5. テストとドキュメント作成

### 2. Timeline API
**推定工数**: 1週間

#### 実装内容
```kotlin
// タイムライン操作
val timeline = order.timeline()
val historicalOrder = timeline.asOf(businessDate)
val changes = timeline.changesSince(lastCheckDate)
val history = timeline.history(from, to)

// 変更追跡
timeline.onBusinessDateChange { old, new ->
    // ビジネス日付変更時の処理
}
```

#### 実装手順
1. Timeline インターフェースの設計
2. 履歴データアクセスAPIの実装
3. 変更検出メカニズムの構築
4. イベントリスナーサポート
5. ビジュアライゼーションヘルパー

### 3. 高度なバッチ操作
**推定工数**: 3-4日

#### 実装内容
```kotlin
// バッチ更新
orderRepository.batchUpdate {
    where { status eq OrderStatus.PENDING }
    set { status = OrderStatus.PROCESSING }
    set { updatedAt = Instant.now() }
}

// ストリーミング処理
orderRepository.stream()
    .filter { it.amount > 1000 }
    .map { it.copy(priority = Priority.HIGH) }
    .collect { orderRepository.save(it) }
```

## 🔧 中優先度機能

### 4. Reactive Streamsサポート
**推定工数**: 1週間

#### 実装内容
- Project Reactor統合
- Mono/Flux返却型のサポート
- リアクティブトランザクション管理
- バックプレッシャー対応

### 5. 高度なクエリDSL機能
**推定工数**: 3-4日

#### 実装内容
```kotlin
// サブクエリ
OrderKtFinder.find {
    customerId inSubquery {
        CustomerKtFinder.select(customerId) {
            country eq "Japan"
        }
    }
}

// 集計関数
OrderKtFinder.aggregate {
    groupBy(customerId)
    sum(amount) as "totalAmount"
    count() as "orderCount"
    having { sum(amount) > 10000 }
}
```

### 6. 監視・メトリクス
**推定工数**: 3日

#### 実装内容
- Micrometer統合
- クエリ実行時間の計測
- キャッシュヒット率
- トランザクション統計

## 📊 低優先度機能

### 7. IntelliJ IDEAプラグイン
**推定工数**: 2-3週間

- Reladomo XMLの構文ハイライト
- コード生成のプレビュー
- クエリDSLの自動補完強化
- リファクタリングサポート

### 8. GraphQL統合
**推定工数**: 1-2週間

- スキーマ自動生成
- バイテンポラルクエリのGraphQL表現
- DataLoaderパターンの実装

### 9. マルチテナンシー
**推定工数**: 2週間

- テナント別スキーマ分離
- 動的接続切り替え
- テナント別キャッシュ戦略

## 📋 実装推奨順序

1. **Phase 1 (2週間)**
   - Kotlin Coroutinesサポート
   - Timeline API
   
2. **Phase 2 (1週間)**
   - 高度なバッチ操作
   - 高度なクエリDSL機能
   
3. **Phase 3 (1週間)**
   - Reactive Streamsサポート
   - 監視・メトリクス
   
4. **Phase 4 (必要に応じて)**
   - IntelliJ IDEAプラグイン
   - GraphQL統合
   - マルチテナンシー

## 🚀 Quick Wins（すぐに実装可能な改善）

1. **エラーメッセージの改善** (1日)
   - より具体的な例外メッセージ
   - スタックトレースの最適化

2. **デフォルト設定の最適化** (半日)
   - より良いデフォルト値
   - 一般的なユースケースの簡素化

3. **ログ出力の改善** (半日)
   - 構造化ログ
   - デバッグレベルの追加

4. **ドキュメント生成** (1日)
   - KDocからのAPIドキュメント生成
   - サンプルコードの追加

これらの機能を段階的に実装することで、Reladomo Kotlin Wrapperはより完成度の高い、エンタープライズレベルのORMラッパーとなります。