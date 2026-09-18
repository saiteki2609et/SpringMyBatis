# 06. MyBatis マッピング仕様

本アプリの中心となる、MyBatis の設定・Mapper・SQL の仕様。

## 6.1 MyBatis 設定

`src/main/resources/application.yml`

| 設定 | 値 | 意味 |
|---|---|---|
| `mybatis.mapper-locations` | `classpath:mapper/*.xml` | XML マッパーの探索先 |
| `mybatis.type-aliases-package` | `com.example.scm.domain` | XML で `type="Item"` と短縮表記できるようにする |
| `mybatis.type-handlers-package` | `com.example.scm.typehandler` | 独自 TypeHandler の自動登録 |
| `mybatis.configuration.map-underscore-to-camel-case` | `true` | `item_code` → `itemCode` の自動変換 |
| `mybatis.configuration.call-setters-on-nulls` | `true` | 値が NULL でも setter を呼ぶ |
| `mybatis.configuration.log-impl` | `Slf4jImpl` | SQL ログを SLF4J へ出力 |
| `logging.level.com.example.scm.mapper` | `debug` | 発行SQL・バインド値・取得件数を出力 |

- Mapper インタフェースは `@Mapper` を付けるだけで DI 登録される
  (`@MapperScan` は不要。起動クラスのパッケージ配下が自動スキャンされる)。
- 実装クラスは MyBatis が動的プロキシで生成するため、開発者は書かない。

### SQL ログの出力例

```
==>  Preparing: SELECT id, item_code, ... FROM item WHERE category = ? AND unit_price >= ?
==> Parameters: ネットワーク機器(String), 10000(BigDecimal)
<==      Total: 3
```

## 6.2 Mapper 一覧

| Mapper | 方式 | SQL の記述場所 | 主な学習テーマ |
|---|---|---|---|
| `WarehouseMapper` | アノテーション | インタフェース内 | 最小構成 |
| `ItemMapper` | アノテーション | インタフェース内 | `#{}` と `${}`、`useGeneratedKeys`、`<script>` 動的SQL |
| `StockMapper` | XML | `resources/mapper/StockMapper.xml` | `resultMap`、`association`、`<sql>`/`<include>` |
| `ShipmentMapper` | XML | `resources/mapper/ShipmentMapper.xml` | `collection`(1:N)、`foreach`、`choose` |

> XML 方式では、**XML の `namespace` と Mapper インタフェースの FQCN を完全一致**させる必要がある。
> ここがズレると `Invalid bound statement (not found)` が発生する。

---

## 6.3 WarehouseMapper

| メソッド | 種別 | SQL 概要 |
|---|---|---|
| `findAll()` | SELECT | 倉庫全件(コード順) |
| `findById(Integer)` | SELECT | 倉庫1件 |

---

## 6.4 ItemMapper(アノテーション方式)

| メソッド | 種別 | 説明 |
|---|---|---|
| `findAll()` | SELECT | 商品全件(商品コード順) |
| `findById(Integer)` | SELECT | 商品1件 |
| `findByItemCode(String)` | SELECT | 商品コードで1件(重複チェック用) |
| `findCategories()` | SELECT | カテゴリの一覧(`DISTINCT`) |
| `search(criteria, orderByColumn)` | SELECT | 動的SQL による検索 |
| `insert(Item)` | INSERT | 採番された ID を引数へ書き戻す |
| `update(Item)` | UPDATE | 商品の更新 |
| `deleteById(Integer)` | DELETE | 商品の削除 |

### 学習ポイント

| 項目 | 内容 |
|---|---|
| `#{}` | PreparedStatement のプレースホルダ(`?`)になる。**値は必ずこちら** |
| `${}` | 文字列をそのまま SQL に埋め込む。SQLインジェクションの原因になるため、ORDER BY のカラム名など値以外の箇所に限定し、必ずホワイトリスト検証した値のみ渡す |
| `@Options(useGeneratedKeys = true, keyProperty = "id")` | INSERT 後、DB が採番した ID が引数オブジェクトの `id` に設定される |
| `@Param` | 引数が2つ以上ある場合は必須。付けないと `arg0` / `param1` という名前になる |
| `<script>` | アノテーション内で動的SQLを書くための囲みタグ |

### search の動的SQL

```sql
SELECT ... FROM item
<where>
  <if test="criteria.keyword != null">
    AND (LOWER(item_name) LIKE '%' || LOWER(#{criteria.keyword}) || '%'
      OR LOWER(item_code) LIKE '%' || LOWER(#{criteria.keyword}) || '%')
  </if>
  <if test="criteria.category != null"> AND category = #{criteria.category} </if>
  <if test="criteria.minPrice != null"> AND unit_price &gt;= #{criteria.minPrice} </if>
  <if test="criteria.maxPrice != null"> AND unit_price &lt;= #{criteria.maxPrice} </if>
</where>
ORDER BY ${orderByColumn}
```

| 要素 | 役割 |
|---|---|
| `<where>` | 条件が1つもなければ WHERE 句ごと消え、先頭の `AND` も自動で除去される |
| `${orderByColumn}` | `ItemService` のホワイトリストで変換済みの文字列のみが渡る |

---

## 6.5 StockMapper(XML方式)

namespace: `com.example.scm.mapper.StockMapper`

| メソッド | 種別 | 説明 |
|---|---|---|
| `findAll()` | SELECT | 在庫全件(商品・倉庫を JOIN) |
| `findByWarehouse(warehouseId)` | SELECT | 倉庫で絞り込み。null なら全件 |
| `findByItemAndWarehouse(itemId, warehouseId)` | SELECT | 商品×倉庫で1件 |
| `findBelowSafetyStock()` | SELECT | 安全在庫割れの在庫 |
| `decrease(itemId, warehouseId, quantity)` | UPDATE | 在庫の減算。戻り値は更新件数 |
| `increase(itemId, warehouseId, quantity)` | UPDATE | 在庫の加算。戻り値は更新件数 |
| `insert(Stock)` | INSERT | 在庫レコードの新規作成 |

### resultMap 定義

| resultMap | 型 | 内容 |
|---|---|---|
| `itemResultMap` | `Item` | 商品のカラム対応 |
| `warehouseResultMap` | `Warehouse` | 倉庫のカラム対応 |
| `stockResultMap` | `Stock` | 在庫本体＋`item`(association)＋`warehouse`(association) |

```xml
<resultMap id="stockResultMap" type="Stock">
  <id     property="id"          column="id"/>
  <result property="quantity"    column="quantity"/>
  <association property="item"      javaType="Item"      resultMap="itemResultMap"      columnPrefix="i_"/>
  <association property="warehouse" javaType="Warehouse" resultMap="warehouseResultMap" columnPrefix="w_"/>
</resultMap>
```

| 要素 | 役割 |
|---|---|
| `<id>` | 主キーの対応付け。オブジェクトの同一性判定に使われる |
| `<association>` | 多対一(1件をぶら下げる) |
| `columnPrefix` | JOIN 時の列名衝突を回避。`i_` を付けると `itemResultMap` の `item_code` は `i_item_code` 列を見る |
| `<sql>` / `<include>` | SELECT 句・FROM 句の共通化 |

> **入れ子オブジェクトのマッピング方針**
> 本アプリでは、`association` / `collection` でぶら下げる `Item` / `Warehouse` は
> **全プロパティを対応付ける**方針にしている。
> 一部の列だけをマッピングすると、JSON や画面で「DB には値があるのに null」という状態になり
> 原因調査に時間を取られるため。
> (取得列を絞るのは性能上有効な手段だが、その場合は「どの項目が入らないか」を明示すること)

### 在庫減算 SQL(重要)

```sql
UPDATE stock
   SET quantity = quantity - #{quantity},
       updated_at = CURRENT_TIMESTAMP
 WHERE item_id = #{itemId}
   AND warehouse_id = #{warehouseId}
   AND quantity >= #{quantity}     -- ← 在庫不足なら 0 件更新になる
```

**更新件数が0なら在庫不足**と判定する。
「SELECT で残数を確認してから UPDATE」より、同時実行時の競合に強い書き方。

---

## 6.6 ShipmentMapper(XML方式)

namespace: `com.example.scm.mapper.ShipmentMapper`

| メソッド | 種別 | 説明 |
|---|---|---|
| `findById(id)` | SELECT | ヘッダ＋明細＋商品を1回の SQL で取得 |
| `search(criteria)` | SELECT | 動的SQL による検索(ヘッダのみ) |
| `countByStatus()` | SELECT | ステータス別件数 |
| `findMaxShipmentNo(prefix)` | SELECT | 出荷番号の最大値(採番用)。該当なしは null |
| `insertShipment(Shipment)` | INSERT | ヘッダ登録。ID を書き戻す |
| `insertDetails(shipmentId, details)` | INSERT | 明細の一括登録(`foreach`) |
| `updateStatus(id, status)` | UPDATE | ステータス更新 |
| `deleteDetailsByShipmentId(shipmentId)` | DELETE | 明細の削除 |
| `deleteById(id)` | DELETE | ヘッダの削除 |

### resultMap 定義

| resultMap | 型 | 内容 |
|---|---|---|
| `itemResultMap` | `Item` | 明細内の商品 |
| `warehouseResultMap` | `Warehouse` | 出荷元倉庫 |
| `shipmentDetailResultMap` | `ShipmentDetail` | 明細＋`item`(association) |
| `shipmentWithDetailsResultMap` | `Shipment` | ヘッダ＋`warehouse`＋`details`(collection) |
| `shipmentHeaderResultMap` | `Shipment` | ヘッダのみ(一覧用) |
| `statusCountResultMap` | `StatusCount` | ステータス別件数 |

### collection(1:N)

```xml
<resultMap id="shipmentWithDetailsResultMap" type="Shipment">
  <id property="id" column="id"/>
  ...
  <collection property="details" ofType="ShipmentDetail"
              resultMap="shipmentDetailResultMap" columnPrefix="d_"/>
</resultMap>
```

| 注意点 | 内容 |
|---|---|
| `<id>` 必須 | JOIN するとヘッダ行が明細数だけ重複するが、`<id>` を手がかりに1オブジェクトへ集約される。書き忘れると明細がまとまらない |
| columnPrefix の連結 | 入れ子になるとプレフィックスは連結される。`collection(d_)` → `association(i_)` の列は **`d_i_item_name`** となる。SELECT 側の別名もこれに合わせる |
| N+1 対策 | 明細を別 SELECT(`select=` 属性)で取らず、JOIN で1回にまとめている |

### 動的SQL(search)

| 要素 | 使用箇所 |
|---|---|
| `<where>` / `<if>` | 出荷先(部分一致)、倉庫ID |
| `<foreach>` | ステータスの IN 句。`open="(" separator="," close=")"` |
| `<choose>` / `<when>` / `<otherwise>` | 出荷日 From/To の指定有無による条件の出し分け |

```xml
<if test="criteria.statuses != null and criteria.statuses.size() > 0">
  AND sh.status IN
  <foreach item="st" collection="criteria.statuses" open="(" separator="," close=")">
    #{st}
  </foreach>
</if>
<choose>
  <when test="criteria.shipDateFrom != null and criteria.shipDateTo != null">
    AND sh.ship_date BETWEEN #{criteria.shipDateFrom} AND #{criteria.shipDateTo}
  </when>
  <when test="criteria.shipDateFrom != null"> AND sh.ship_date &gt;= #{criteria.shipDateFrom} </when>
  <when test="criteria.shipDateTo != null">   AND sh.ship_date &lt;= #{criteria.shipDateTo} </when>
  <otherwise/>
</choose>
```

### 明細の一括 INSERT(foreach)

```xml
<insert id="insertDetails">
  INSERT INTO shipment_detail (shipment_id, item_id, quantity)
  VALUES
  <foreach item="d" collection="details" separator=",">
    (#{shipmentId}, #{d.itemId}, #{d.quantity})
  </foreach>
</insert>
```

1件ずつ INSERT をループするより DB との往復回数が減る。
件数が非常に多い場合は `ExecutorType.BATCH` の利用も検討する。

---

## 6.7 TypeHandler

`com.example.scm.typehandler.ShipmentStatusTypeHandler`

| 項目 | 内容 |
|---|---|
| 対象 | `ShipmentStatus`(enum) ⇔ `shipment.status`(VARCHAR のコード値) |
| 登録方法 | `mybatis.type-handlers-package` で自動登録。`@MappedTypes(ShipmentStatus.class)` で対象型を指定 |
| Java → DB | `setNonNullParameter` で `status.getCode()`(`"10"` など)を設定 |
| DB → Java | `getNullableResult` で `ShipmentStatus.fromCode(...)` に変換 |
| 未知コード | `IllegalArgumentException` を送出(不正データを早期検知) |

XML 側に `typeHandler=` を書かなくても、プロパティの型から自動的に適用される。
`<foreach>` 内の `#{st}` のようなパラメータにも適用される。

> レガシーな業務DBは区分値をコードで持つことが多く、実案件で最も出番の多い TypeHandler のパターン。

---

## 6.8 トランザクション

| 項目 | 方針 |
|---|---|
| 境界 | Service 層(`@Transactional`) |
| 既定 | クラスに `@Transactional(readOnly = true)`、更新メソッドに `@Transactional` |
| ロールバック | `RuntimeException` で自動ロールバック。検査例外では**ロールバックされない** |
| 注意点 | 同一クラス内のメソッドを `this` 経由で呼ぶと `@Transactional` が効かない(自己呼び出し問題) |

### 引当処理のトランザクション

```java
@Transactional
public Shipment allocate(Integer id) {
    Shipment shipment = findById(id);
    for (ShipmentDetail d : shipment.getDetails()) {
        int updated = stockMapper.decrease(d.getItemId(), shipment.getWarehouseId(), d.getQuantity());
        if (updated == 0) {
            throw new BusinessException("在庫が不足しています");   // ← ここで全てロールバック
        }
    }
    shipmentMapper.updateStatus(id, ShipmentStatus.ALLOCATED);
    return shipment;
}
```

1行目の明細で在庫を減算済みでも、2行目で在庫不足になれば**1行目の減算も取り消される**。
この挙動は `ShipmentServiceTest#allocateRollsBackWhenStockIsShort` で検証している。

---

## 6.9 よくあるエラーと原因

| エラー | 原因 |
|---|---|
| `Invalid bound statement (not found)` | ①XML の namespace が FQCN と不一致 ②`<select id>` とメソッド名が不一致 ③`mapper-locations` の指定漏れ |
| プロパティが null のまま | カラム別名とプロパティ名の不一致。`map-underscore-to-camel-case` の設定漏れ |
| 1:N の明細が1件しか入らない / 重複する | resultMap に `<id>` がない |
| `Parameter 'xxx' not found` | 引数が2つ以上あるのに `@Param` がない |
| 区分値が enum に変換されない | TypeHandler が登録されていない(`type-handlers-package` の指定漏れ) |
