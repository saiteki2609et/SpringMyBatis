# 05. API 仕様

画面から jQuery(Ajax)で呼び出す REST API の仕様。
Controller は `com.example.scm.web.api` パッケージ配下。

## 5.1 共通仕様

| 項目 | 内容 |
|---|---|
| ベースURL | `http://localhost:8080` |
| 文字コード | UTF-8 |
| レスポンス形式 | JSON(`application/json`) |
| 認証 | なし |
| 日付形式 | `yyyy-MM-dd`(LocalDate) |
| 日時形式 | `yyyy-MM-ddTHH:mm:ss.SSSSSS`(ISO-8601、LocalDateTime) |
| 数値 | 単価は小数2桁(`18500.00`) |

### エラーレスポンス

`ApiExceptionHandler` が共通で処理する。

| HTTPステータス | 発生条件 | 例外 |
|---|---|---|
| 400 Bad Request | 業務エラー(在庫不足、ステータス不正など) | `BusinessException` |
| 500 Internal Server Error | 想定外のエラー | 上記以外の `Exception` |

```json
{
  "timestamp": "2026-09-17T15:25:43.656368400",
  "message": "在庫が不足しています: モバイルルーター MR-30 (要求数 9999)"
}
```

| フィールド | 型 | 説明 |
|---|---|---|
| timestamp | string | エラー発生日時 |
| message | string | 画面に表示するメッセージ。500 の場合は例外クラス名を含む定型文 |

---

## 5.2 API 一覧

| # | メソッド | パス | 概要 | 呼び出し元画面 |
|---|---|---|---|---|
| API-01 | GET | `/api/items` | 商品検索 | SC-02 商品マスタ一覧 |
| API-02 | GET | `/api/stocks` | 在庫一覧取得 | SC-05 在庫照会 |
| API-03 | POST | `/api/stocks/receive` | 入庫 | SC-05 在庫照会 |
| API-04 | GET | `/api/shipments/{id}` | 出荷指示照会 | SC-08(JSON確認用リンク) |
| API-05 | GET | `/api/shipments/status-counts` | ステータス別件数 | (画面未使用・学習用) |
| API-06 | POST | `/api/shipments/{id}/{action}` | 出荷ステータス操作 | SC-08 出荷指示詳細 |

---

## API-01 商品検索

```
GET /api/items
```

### リクエストパラメータ(すべて任意)

| パラメータ | 型 | 説明 |
|---|---|---|
| keyword | string | 商品名・商品コードの部分一致(大文字小文字を区別しない) |
| category | string | カテゴリの完全一致 |
| minPrice | number | 単価の下限(以上) |
| maxPrice | number | 単価の上限(以下) |
| sort | string | 並び順。`code` / `name` / `price` / `category`。それ以外・未指定は `code` 相当 |

- 空文字のパラメータは未指定として扱う(サーバ側で null 変換)。
- 未指定の条件は WHERE 句に含まれない(動的SQL)。

### レスポンス(200)

商品の配列。

```json
[
  {
    "id": 1,
    "itemCode": "ITM-1001",
    "itemName": "光回線ルーター RX-100",
    "category": "ネットワーク機器",
    "unitPrice": 18500.00,
    "safetyStock": 30,
    "createdAt": "2026-09-17T15:28:02.173452"
  }
]
```

### 呼び出し例

```
GET /api/items?keyword=ルーター&category=モバイル&minPrice=10000&sort=price
```

---

## API-02 在庫一覧取得

```
GET /api/stocks
```

### リクエストパラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| warehouseId | number | ― | 倉庫ID。未指定なら全倉庫 |

### レスポンス(200)

在庫の配列。商品・倉庫の情報が入れ子で含まれる(MyBatis の `<association>` によるマッピング結果)。

```json
[
  {
    "id": 1,
    "itemId": 1,
    "warehouseId": 1,
    "quantity": 110,
    "updatedAt": "2026-09-17T15:28:02.173452",
    "item": {
      "id": 1,
      "itemCode": "ITM-1001",
      "itemName": "光回線ルーター RX-100",
      "category": "ネットワーク機器",
      "unitPrice": 18500.00,
      "safetyStock": 30,
      "createdAt": "2026-09-17T15:28:02.173452"
    },
    "warehouse": {
      "id": 1,
      "code": "WH01",
      "name": "東京物流センター",
      "address": "東京都江東区新木場1-1-1"
    },
    "belowSafetyStock": false
  }
]
```

| フィールド | 説明 |
|---|---|
| belowSafetyStock | 在庫数が安全在庫を下回っているか。Java 側の判定メソッドの戻り値 |
| item / warehouse | JOIN して取得したマスタ情報。**全項目がマッピングされる**(一部だけ null にはならない) |

- 並び順: 倉庫コード → 商品コード

---

## API-03 入庫

```
POST /api/stocks/receive
Content-Type: application/json
```

### リクエストボディ

```json
{ "itemId": 1, "warehouseId": 1, "quantity": 10 }
```

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| itemId | number | ○ | 商品ID |
| warehouseId | number | ○ | 入庫先倉庫ID |
| quantity | number | ○ | 入庫数(1以上) |

### レスポンス(200)

入庫後の、**入庫先倉庫の在庫一覧**(形式は API-02 と同じ)。

### エラー

| ステータス | message | 条件 |
|---|---|---|
| 400 | 入庫数は1以上で指定してください | quantity が 0 以下 |
| 500 | (制約違反) | 存在しない商品ID・倉庫IDを指定した場合(外部キー制約) |

---

## API-04 出荷指示照会

```
GET /api/shipments/{id}
```

### パスパラメータ

| パラメータ | 型 | 説明 |
|---|---|---|
| id | number | 出荷指示ID |

### レスポンス(200)

ヘッダ・倉庫・明細・明細内の商品までを1回の SQL で取得した結果
(MyBatis の `<collection>` によるマッピング結果)。

```json
{
  "id": 1,
  "shipmentNo": "SH-20260901-001",
  "customerName": "株式会社アルファ通信",
  "warehouseId": 1,
  "status": "SHIPPED",
  "shipDate": "2026-09-01",
  "remarks": "定期補充",
  "createdAt": "2026-09-17T15:28:02.204678",
  "warehouse": { "id": 1, "code": "WH01", "name": "東京物流センター", "address": "東京都江東区新木場1-1-1" },
  "details": [
    {
      "id": 1,
      "shipmentId": 1,
      "itemId": 1,
      "quantity": 10,
      "item": {
        "id": 1,
        "itemCode": "ITM-1001",
        "itemName": "光回線ルーター RX-100",
        "category": "ネットワーク機器",
        "unitPrice": 18500.00,
        "safetyStock": 30,
        "createdAt": "2026-09-17T15:28:02.173452"
      }
    }
  ],
  "totalQuantity": 60
}
```

| フィールド | 説明 |
|---|---|
| status | enum 名(`DRAFT` / `ALLOCATED` / `SHIPPED` / `CANCELLED`)。**DB のコード値(10/20/30/90)ではない** |
| totalQuantity | 明細数量の合計。Java 側で算出 |
| warehouse / details[].item | JOIN して取得したマスタ情報。**全項目がマッピングされる** |

### エラー

| ステータス | message | 条件 |
|---|---|---|
| 400 | 出荷指示が見つかりません (id=◯) | 存在しないID |

---

## API-05 ステータス別件数

```
GET /api/shipments/status-counts
```

### レスポンス(200)

```json
[
  { "status": "DRAFT",     "count": 1, "label": "登録済" },
  { "status": "ALLOCATED", "count": 1, "label": "引当済" },
  { "status": "SHIPPED",   "count": 2, "label": "出荷済" }
]
```

- 件数0のステータスは配列に含まれない(`GROUP BY` の結果のため)。
- 並び順はステータスコードの昇順。

---

## API-06 出荷ステータス操作

```
POST /api/shipments/{id}/{action}
```

### パスパラメータ

| パラメータ | 型 | 説明 |
|---|---|---|
| id | number | 出荷指示ID |
| action | string | `allocate`(引当) / `ship`(出荷確定) / `cancel`(取消) |

> 画面用 Controller(`POST /shipments/{id}/{action}`)では `delete` も指定できるが、
> 本 API では `delete` は未対応(「不正な操作です」エラーになる)。

### 処理内容

| action | 前提ステータス | 処理 | 遷移後 |
|---|---|---|---|
| allocate | 登録済(10) | 明細の数量だけ在庫を減算 | 引当済(20) |
| ship | 引当済(20) | ステータス更新のみ | 出荷済(30) |
| cancel | 登録済(10) / 引当済(20) | 引当済なら在庫を戻す | 取消(90) |

### レスポンス(200)

処理後の出荷指示(形式は API-04 と同じ)。

### エラー

| ステータス | message | 条件 |
|---|---|---|
| 400 | 引当できるのは「登録済」の出荷指示のみです(現在: ◯◯) | ステータス不正 |
| 400 | 在庫が不足しています: ◯◯ (要求数 N) | 引当時の在庫不足。**在庫は1件も減らない(ロールバック)** |
| 400 | 出荷できるのは「引当済」の出荷指示のみです(現在: ◯◯) | ステータス不正 |
| 400 | 出荷済の出荷指示は取消できません | 出荷済の取消 |
| 400 | すでに取消済です | 取消済の再取消 |
| 400 | 不正な操作です: ◯◯ | 未定義のアクション名 |

### 呼び出し例(jQuery)

```javascript
$.ajax({
  url: '/api/shipments/' + shipmentId + '/allocate',
  type: 'POST'
}).done(function (shipment) {
  // shipment.status = "ALLOCATED"
}).fail(function (xhr) {
  // xhr.responseJSON.message にエラーメッセージ
});
```
