# Reladomo Kotlin Demo - Product Price Management

カンファレンス向けデモプロジェクト: Reladomo Kotlinのバイテンポラルデータモデル機能を実演します。

## 概要

このデモは、3つの段階的なデータモデルを通じて、Reladomo Kotlinの機能を紹介します:

1. **非テンポラル (Non-temporal)**: Category - シンプルなCRUD操作
2. **非テンポラル (Non-temporal)**: Product - カテゴリとの関連
3. **バイテンポラル (Bitemporal)**: ProductPrice - ビジネス日付 × 処理日付の2次元時系列データ

## アーキテクチャ

```
reladomo-kotlin-demo/
├── backend/          # Spring Boot REST API (ポート 8081)
│   ├── src/main/resources/reladomo/  # Reladomo XMLエンティティ定義
│   └── src/main/kotlin/              # Kotlin実装
└── frontend/         # Vite + React + TypeScript (ポート 3000)
    ├── src/components/              # Reactコンポーネント
    ├── src/api/                     # APIクライアント
    └── src/types/                   # TypeScript型定義
```

## 起動方法

### バックエンド起動

**ターミナル1:**

```bash
cd /Users/hiroshi.ito/Development/github/itohiro73/reladomo-kotlin
./gradlew :reladomo-kotlin-demo:backend:bootRun
```

バックエンドは http://localhost:8081 で起動します。

### フロントエンド起動

**ターミナル2:**

```bash
cd /Users/hiroshi.ito/Development/github/itohiro73/reladomo-kotlin/reladomo-kotlin-demo/frontend

# 初回のみ: 依存関係のインストール
npm install

# 開発サーバー起動
npm run dev
```

フロントエンドは http://localhost:3000 で起動します。

ブラウザで http://localhost:3000 を開くと、以下の画面が表示されます:
- 📁 カテゴリ一覧（非テンポラル）
- 📦 商品一覧（非テンポラル）
- 💰 商品価格履歴タイムライン（バイテンポラル）
- 🗄️ データベースビューア（生のテーブルデータ）

### H2コンソール - データベースを直接確認

バックエンドが起動している間、H2のWebコンソールでデータベースを直接確認できます。

**アクセス方法:**

1. ブラウザで http://localhost:8081/h2-console を開く
2. ログイン画面で以下を入力:
   - **JDBC URL**: `jdbc:h2:mem:demodb`
   - **User Name**: `sa`
   - **Password**: (空白のまま)
3. **Connect** ボタンをクリック

**便利なSQLクエリ例:**

```sql
-- 全テーブル一覧
SELECT * FROM INFORMATION_SCHEMA.TABLES;

-- カテゴリを見る
SELECT * FROM CATEGORIES;

-- 商品を見る
SELECT * FROM PRODUCTS;

-- バイテンポラル価格データを見る（4つのタイムスタンプ列に注目！）
SELECT * FROM PRODUCT_PRICES
ORDER BY PRODUCT_ID, BUSINESS_FROM, PROCESSING_FROM;

-- シーケンステーブルを見る
SELECT * FROM MITHRA_SEQUENCE;
```

**デモのポイント:**
- `PRODUCT_PRICES` テーブルで4つのタイムスタンプ列を確認できます
  - `BUSINESS_FROM` / `BUSINESS_THRU`: いつから価格が有効か
  - `PROCESSING_FROM` / `PROCESSING_THRU`: いつその情報を記録したか
- 同じ商品IDに対して複数のレコードが存在し、時間の変化を追跡できます

## API エンドポイント

### Category API (非テンポラル)

```bash
# 全カテゴリ取得
curl http://localhost:8081/api/categories | python3 -m json.tool

# レスポンス例:
# [
#   {
#     "id": 1,
#     "name": "Electronics",
#     "description": "Electronic devices and accessories"
#   },
#   {
#     "id": 2,
#     "name": "Books",
#     "description": "Physical and digital books"
#   },
#   {
#     "id": 3,
#     "name": "Clothing",
#     "description": "Apparel and fashion items"
#   }
# ]
```

### Product API (非テンポラル)

```bash
# 全商品取得
curl http://localhost:8081/api/products | python3 -m json.tool

# レスポンス例:
# [
#   {
#     "id": 1,
#     "categoryId": 1,
#     "categoryName": "Electronics",
#     "name": "Laptop Pro 15",
#     "description": "High-performance laptop"
#   },
#   {
#     "id": 2,
#     "categoryId": 3,
#     "categoryName": "Clothing",
#     "name": "Summer T-Shirt",
#     "description": "Limited edition summer collection"
#   },
#   {
#     "id": 3,
#     "categoryId": 2,
#     "categoryName": "Books",
#     "name": "Programming Guide 2024",
#     "description": "Annual programming reference"
#   }
# ]
```

### ProductPrice API (バイテンポラル)

```bash
# 全価格履歴取得
curl http://localhost:8081/api/product-prices | python3 -m json.tool

# レスポンス例:
# [
#   {
#     "id": 1,
#     "productId": 1,
#     "productName": "Laptop Pro 15",
#     "price": 1000.00,
#     "businessFrom": "2024-01-01T00:00:00Z",
#     "businessThru": "2024-11-30T23:59:59Z",
#     "processingFrom": "2024-01-01T00:00:00Z",
#     "processingThru": "2024-11-01T00:00:00Z"
#   },
#   {
#     "id": 2,
#     "productId": 1,
#     "productName": "Laptop Pro 15",
#     "price": 1200.00,
#     "businessFrom": "2024-12-01T00:00:00Z",
#     "businessThru": "9999-12-01T23:59:00Z",
#     "processingFrom": "2024-11-01T00:00:00Z",
#     "processingThru": "2024-11-15T00:00:00Z"
#   },
#   {
#     "id": 3,
#     "productId": 1,
#     "productName": "Laptop Pro 15",
#     "price": 1100.00,
#     "businessFrom": "2024-12-01T00:00:00Z",
#     "businessThru": "9999-12-01T23:59:00Z",
#     "processingFrom": "2024-11-15T00:00:00Z",
#     "processingThru": "9999-12-01T23:59:00Z"
#   }
# ]
```

## バイテンポラルデータのシナリオ例

上記のProductPrice APIのレスポンスは、以下のビジネスシナリオを表現しています:

### Laptop Pro 15の価格履歴

1. **初期価格 (ID=1)**
   - 価格: 1000.00円
   - ビジネス有効期間: 2024/1/1 〜 2024/11/30
   - システム記録期間: 2024/1/1 〜 2024/11/1
   - 意味: 2024年1月1日に記録された、同日から有効な価格

2. **値上げ計画 (ID=2)**
   - 価格: 1200.00円
   - ビジネス有効期間: 2024/12/1 〜 無期限
   - システム記録期間: 2024/11/1 〜 2024/11/15
   - 意味: 11月1日に記録された、12月1日からの値上げ計画（後に修正される）

3. **修正後の価格 (ID=3)**
   - 価格: 1100.00円
   - ビジネス有効期間: 2024/12/1 〜 無期限
   - システム記録期間: 2024/11/15 〜 無期限
   - 意味: 11月15日に修正された、実際の12月1日からの価格

このように、バイテンポラルデータモデルでは:
- **ビジネス日付**: いつから価格が有効か
- **処理日付**: いつその情報をシステムに記録したか

を別々に管理でき、過去の計画や修正履歴を完全に追跡できます。

## 技術スタック

### バックエンド
- **Framework**: Spring Boot 3.2.0
- **ORM**: Reladomo 18.1.0 + reladomo-kotlin wrapper
- **Database**: H2 (in-memory)
- **Language**: Kotlin 1.9
- **Build Tool**: Gradle 8.5

### フロントエンド
- **Framework**: React 18.2 + TypeScript
- **Build Tool**: Vite 5.0
- **HTTP Client**: Axios 1.6
- **Styling**: CSS Modules

## データモデル

### Category (非テンポラル)
- ID, Name, Description
- シンプルなマスタデータ

### Product (非テンポラル)
- ID, CategoryID, Name, Description
- カテゴリとの1対多関連

### ProductPrice (バイテンポラル)
- ID, ProductID, Price
- ビジネス日付: BUSINESS_FROM, BUSINESS_THRU
- 処理日付: PROCESSING_FROM, PROCESSING_THRU
- 複合主キー: (ID, BUSINESS_FROM, PROCESSING_FROM)

## 開発コマンド

### バックエンド

```bash
# ビルド
./gradlew :reladomo-kotlin-demo:backend:build

# テスト
./gradlew :reladomo-kotlin-demo:backend:test

# Kotlinラッパー生成
./gradlew :reladomo-kotlin-demo:backend:generateKotlinWrappers

# クリーンビルド
./gradlew :reladomo-kotlin-demo:backend:clean build
```

### フロントエンド

```bash
cd reladomo-kotlin-demo/frontend

# 開発サーバー起動
npm run dev

# プロダクションビルド
npm run build

# プロダクションプレビュー
npm run preview

# Linting
npm run lint
```

## デモのポイント

### 1. 非テンポラルデータ (Category, Product)
- シンプルなCRUD操作
- 通常のデータベーステーブルと同じ扱い
- カテゴリと商品の関連を表示

### 2. バイテンポラルデータ (ProductPrice)
- **2つの時間軸**による履歴管理:
  - ビジネス日付: その価格がいつから有効か（未来の計画も記録可能）
  - 処理日付: その情報をいつシステムに記録したか（監査履歴）
- **タイムライン表示**で価格変更履歴を視覚化
- **修正履歴の追跡**: 過去の計画が後で修正された場合も完全に記録

### 3. データベースビューア
- 生のテーブルデータをリアルタイムで表示
- テンポラル列（BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU）をハイライト
- データモデルの理解を深めるための教育的ツール
