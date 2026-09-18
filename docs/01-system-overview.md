# 01. システム概要

## 1.1 目的

大手通信会社向け SCM(物流管理)システム案件で使われる技術スタックを、
ローカル環境で一通り体験・学習するための小規模 Web アプリケーション。

特に **MyBatis によるマッピングと DB とのやり取り**を重点的に学べることを主目的とし、
以下が実際に動作する形で実装されている。

- アノテーション方式 / XML 方式の両方の Mapper
- 1:1(association) / 1:N(collection) のマッピング
- 動的SQL(`<where>` `<if>` `<choose>` `<foreach>`)
- 独自 TypeHandler による enum ⇔ DB区分コードの変換
- `@Transactional` による複数テーブル更新のトランザクション制御

## 1.2 業務スコープ

倉庫からの出荷業務を、以下の範囲でモデル化している。

| 業務 | 内容 |
|---|---|
| 商品マスタ管理 | 商品の登録・更新・削除・検索 |
| 在庫管理 | 倉庫別の在庫照会、入庫(数量加算)、安全在庫割れの検知 |
| 出荷指示管理 | 出荷指示の作成(ヘッダ＋明細)、在庫引当、出荷確定、取消、削除 |

## 1.3 技術スタック

| 分類 | 採用技術 | バージョン |
|---|---|---|
| 言語 | Java | 21 (Temurin) |
| フレームワーク | Spring Boot | 3.3.5 |
| O/R マッパー | MyBatis (mybatis-spring-boot-starter) | 3.0.3 |
| テンプレートエンジン | Thymeleaf | Spring Boot 管理 |
| クライアントサイド | jQuery | 3.7.1(ローカル同梱) |
| DB | H2 Database(組込み・ファイルモード) | 2.2.224 |
| テスト | JUnit 5 / AssertJ / Mockito / MockMvc | Spring Boot 管理 |
| ビルド | Maven | 3.9.9 |

## 1.4 アーキテクチャ

典型的な3層構成。Controller は入出力のみを担当し、業務ロジックとトランザクション境界は Service に置く。

```
[ブラウザ]
  │  画面遷移(Thymeleaf)      Ajax(jQuery)
  ↓                            ↓
┌─────────────────────────────────────────────┐
│ Controller 層  web/            web/api/      │  リクエスト受付・画面遷移・JSON返却
├─────────────────────────────────────────────┤
│ Service 層     service/                      │  業務ロジック、@Transactional
├─────────────────────────────────────────────┤
│ Mapper 層      mapper/ + resources/mapper/   │  MyBatis(インタフェース＋SQL)
├─────────────────────────────────────────────┤
│ DB             H2 (data/scmdb.mv.db)         │
└─────────────────────────────────────────────┘
```

### パッケージ構成

| パッケージ | 役割 |
|---|---|
| `com.example.scm` | 起動クラス `ScmStudyApplication` |
| `com.example.scm.domain` | ドメインモデル、検索条件クラス、区分値 enum |
| `com.example.scm.mapper` | MyBatis の Mapper インタフェース |
| `com.example.scm.typehandler` | 独自 TypeHandler |
| `com.example.scm.service` | 業務ロジック、トランザクション境界 |
| `com.example.scm.web` | 画面用 Controller(Thymeleaf) |
| `com.example.scm.web.api` | REST API 用 Controller(jQuery から呼ぶ) |
| `com.example.scm.config` | H2 コンソールの既定接続設定 |
| `com.example.scm.exception` | 業務例外 `BusinessException` |

### 主要クラス

| クラス | 責務 |
|---|---|
| `ItemService` | 商品の検索・登録・更新・削除。ORDER BY のホワイトリスト検証 |
| `StockService` | 在庫照会、入庫(UPDATE→0件ならINSERT) |
| `ShipmentService` | 出荷指示の登録・引当・出荷・取消・削除。採番、明細のマージ |
| `ShipmentStatusTypeHandler` | 出荷ステータス enum ⇔ DB のコード値("10"/"20"...) |
| `GlobalExceptionHandler` | 画面側の業務例外をエラー画面へ |
| `ApiExceptionHandler` | API 側の例外を JSON(400/500)へ |
| `H2ConsoleConfig` | H2 コンソールの接続情報を既定値として表示 |

## 1.5 用語定義

| 用語 | 英語/コード | 説明 |
|---|---|---|
| 倉庫 | warehouse | 在庫を保管する拠点。物流センター、デポなど |
| 商品 | item | 出荷対象の物品。商品コードで一意 |
| 在庫 | stock | 商品×倉庫ごとの保有数量 |
| 安全在庫 | safety stock | 商品ごとに定める在庫数の下限値。下回ると警告表示 |
| 出荷指示 | shipment | 出荷の指示伝票。ヘッダ1件＋明細N件で構成 |
| 出荷明細 | shipment detail | 出荷指示に含まれる商品と数量 |
| 引当 | allocate | 出荷予定の数量を在庫から確保(減算)すること |
| 入庫 | receive | 在庫を増やすこと |

## 1.6 非機能事項

| 項目 | 方針 |
|---|---|
| 想定利用者 | 学習者1名(ローカル環境での単独利用) |
| データ量 | 商品10件・在庫15件程度の初期データ。性能要件なし |
| 文字コード | UTF-8(ソース、テンプレート、SQL、DB すべて) |
| 日付 | サーバのシステム日付。タイムゾーンは JST 前提 |
| データ永続化 | H2 ファイルモード(`data/scmdb.mv.db`)。再起動してもデータは保持される |
| ログ | 標準出力。Mapper パッケージを DEBUG にし、発行SQLとバインド値を出力する |
| ブラウザ | モダンブラウザ(Chrome / Edge)を想定 |

## 1.7 対象外事項

学習用のため、以下は実装していない。実案件では別途考慮が必要。

- 認証・認可(ログイン、権限制御) ※Spring Security 未導入
- 排他制御(楽観ロック用の更新日時チェック、悲観ロック)
- 監査ログ、操作履歴
- ページング(一覧は全件取得)
- 帳票出力(PDF / Excel)、バッチ処理
- 複数ユーザーの同時更新を前提とした整合性設計
- 多言語対応、アクセシビリティ対応
