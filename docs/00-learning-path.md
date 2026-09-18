# 00. MyBatis 学習ロードマップ

このアプリを使って **MyBatis を上から順に理解していくための道順**です。
STEP 0 から順番に進めれば、「アノテーション方式 → XML方式 → マッピング(1:1 / 1:N) →
動的SQL → TypeHandler → トランザクション → テスト」と、実案件で必要になる順に積み上がります。

## このロードマップの使い方

各 STEP は次の4点セットになっています。**読むだけで終わらせず、必ず「手を動かす」まで進めてください。**

| | 内容 |
|---|---|
| ねらい | この STEP で何が分かるようになるか |
| 読む | 対象のソース・仕様書 |
| 手を動かす | 実際に触る/書き換える作業 |
| 理解チェック | 人に説明できるか。できなければ「読む」に戻る |

> **常に開いておくもの**
> - アプリ: `mvn spring-boot:run` → <http://localhost:8080>
> - 起動ターミナル: 発行された SQL とバインド値が流れる(**ここを見るのが一番の学習**)
> - 早見表: <http://localhost:8080/learn>(書き方と実装ファイルの対応)
> - H2 コンソール: <http://localhost:8080/h2-console>

---

## 全体像

| STEP | テーマ | 中心の教材ファイル | 目安 |
|---|---|---|---|
| 0 | 環境を動かす / DB とテーブルを知る | `schema.sql`, H2コンソール | 0.5h |
| 1 | MyBatis の仕組みと設定、SQLログの読み方 | `application.yml`, `WarehouseMapper` | 1h |
| 2 | アノテーション方式で CRUD | `ItemMapper` | 1.5h |
| 3 | `#{}` と `${}` / `@Param` / 採番IDの受け取り | `ItemMapper`, `ItemService` | 1h |
| 4 | XML方式と `resultMap` の基礎 | `StockMapper.xml` | 1.5h |
| 5 | `association`(多対一)と `columnPrefix` | `StockMapper.xml` | 1.5h |
| 6 | `collection`(一対多)と N+1 問題 | `ShipmentMapper.xml` | 2h |
| 7 | 動的SQL(`where`/`if`/`choose`/`foreach`) | 両XML + `ItemMapper` | 2h |
| 8 | TypeHandler(enum ⇔ DBコード値) | `ShipmentStatusTypeHandler` | 1h |
| 9 | トランザクション(Service層) | `ShipmentService` | 1.5h |
| 10 | テストで SQL とマッピングを検証する | `src/test/java` | 2h |
| 11 | Web層まで通す(Controller / REST API / jQuery) | `web/`, `static/js/app.js` | 1.5h |
| 12 | 応用と実案件とのギャップ | 課題集 | ― |

依存関係:

```
STEP 0 → 1 → 2 → 3 ─┐
                     ├→ 4 → 5 → 6 → 7 → 8 → 9 → 10 → (11) → 12
   (SQL/Java の基礎) ─┘
```

STEP 4 以降が MyBatis の本番です。STEP 1〜3 は土台なので飛ばさないでください。

---

## STEP 0. 環境を動かす / DB とテーブルを知る

**ねらい**: 「MyBatis を学ぶ」前に、**どんなテーブルに何が入っているか**を頭に入れる。
SQL の対象が分からないままマッピングを読んでも理解できません。

**読む**
- [../README.md](../README.md) の「1. 動かし方」
- [08-setup-operations.md](08-setup-operations.md)
- [04-database-spec.md](04-database-spec.md) ―― ER図・テーブル定義
- [../src/main/resources/schema.sql](../src/main/resources/schema.sql) / [../src/main/resources/data.sql](../src/main/resources/data.sql)

**手を動かす**
1. `mvn spring-boot:run` で起動し、<http://localhost:8080> を開く
2. H2 コンソールで以下を自分で打ってみる(Connect を押すだけで繋がります)
   ```sql
   SELECT * FROM warehouse;
   SELECT * FROM item;
   SELECT * FROM stock;
   SELECT * FROM shipment;
   SELECT * FROM shipment_detail;
   ```
3. 在庫を商品名・倉庫名つきで出す JOIN を**自分で書いてみる**(STEP 4 の答え合わせに使う)
   ```sql
   SELECT s.id, i.item_name, w.name, s.quantity
     FROM stock s JOIN item i ON s.item_id = i.id
                  JOIN warehouse w ON s.warehouse_id = w.id;
   ```
4. `/shipments/new` で出荷指示を作り、詳細画面で「在庫引当」→ `/stocks` で在庫が減ったことを確認

**理解チェック**
- `shipment` と `shipment_detail` はどういう関係か(1:N のどちら側が N か)
- `stock` のユニーク制約は何か(何と何の組み合わせで1行か)
- 初期状態に戻す手順は?(→ `data` フォルダを削除して再起動)

---

## STEP 1. MyBatis の仕組みと設定、SQLログの読み方

**ねらい**: 「インタフェースしか書いていないのに動く」理由と、設定1つ1つの意味を理解する。
そして **SQLログを読めるようになる**(以降の全STEPで使う道具)。

**読む**
- [../src/main/resources/application.yml](../src/main/resources/application.yml) の `mybatis:` ブロック(コメント付き)
- [06-mybatis-spec.md](06-mybatis-spec.md) の「6.1 MyBatis 設定」
- [../src/main/java/com/example/scm/mapper/WarehouseMapper.java](../src/main/java/com/example/scm/mapper/WarehouseMapper.java) ―― 最小構成。まずこれだけ読む

押さえる点:

| 項目 | 意味 |
|---|---|
| `@Mapper` | インタフェースが Spring の Bean になる。**実装クラスは MyBatis が動的プロキシで自動生成**するので自分では書かない |
| `mapper-locations` | XML マッパーの探索先 |
| `type-aliases-package` | XML で `type="Item"` と短く書けるようになる |
| `map-underscore-to-camel-case` | `item_code` → `itemCode` の自動変換。**これがあるから resultMap を全部書かなくて済む** |
| `log-impl` + `logging.level` | 発行SQL・バインド値・取得件数がログに出る |

**手を動かす**
1. 一覧画面を開き、ターミナルのログを読む。次の3行の意味を言えるようにする
   ```
   ==>  Preparing: SELECT ... FROM item WHERE category = ? AND unit_price >= ?
   ==> Parameters: ネットワーク機器(String), 10000(BigDecimal)
   <==      Total: 3
   ```
2. `application.yml` の `map-underscore-to-camel-case` を **一時的に `false`** にして再起動し、
   商品一覧で商品コードなどが空になることを確認 → **元に戻す**
3. `logging.level.com.example.scm.mapper` を `info` にするとログが消えることを確認 → 戻す

**理解チェック**
- `Preparing` の `?` と `Parameters` の対応関係を説明できるか
- Mapper インタフェースの実装は誰が作っているか
- `map-underscore-to-camel-case` を切ると何が壊れるか

---

## STEP 2. アノテーション方式で CRUD

**ねらい**: SQL をインタフェースに直接書く、一番素朴な書き方を身につける。

**読む**
- [../src/main/java/com/example/scm/mapper/ItemMapper.java](../src/main/java/com/example/scm/mapper/ItemMapper.java)
  ―― `findAll` / `findById` / `findByItemCode` / `insert` / `update` / `deleteById` だけを先に読む
  (`search` の動的SQLは STEP 7 で扱うので**今は飛ばす**)
- [06-mybatis-spec.md](06-mybatis-spec.md) の「6.4 ItemMapper」
- 呼び出し側: [../src/main/java/com/example/scm/service/ItemService.java](../src/main/java/com/example/scm/service/ItemService.java)

押さえる点:
- `@Select` / `@Insert` / `@Update` / `@Delete` の4つ
- 戻り値の型で挙動が変わる(`List<Item>` = 複数件、`Item` = 1件/該当なしは `null`、`int` = 更新件数)
- `String COLUMNS = "..."` のように定数で SELECT 句を使い回せる

**手を動かす**
1. `ItemMapper` に「カテゴリを指定して商品を取る」メソッドを自分で追加する
   ```java
   @Select("SELECT " + COLUMNS + " FROM item WHERE category = #{category}")
   List<Item> findByCategory(String category);
   ```
2. `ItemMapperTest` を真似て、追加メソッドのテストを1つ書き `mvn test -Dtest=ItemMapperTest` で通す
3. わざと SQL のテーブル名を間違えて、どんな例外が出るかを見る → 直す

**理解チェック**
- 1件取得で該当なしのとき、戻り値はどうなるか
- UPDATE / DELETE の戻り値 `int` は何を表すか
- アノテーション方式が**つらくなる**のはどんなときか(→ SQL が長い/動的になる → STEP 4 の XML へ)

---

## STEP 3. `#{}` と `${}` / `@Param` / 採番IDの受け取り

**ねらい**: **実務とレビューと面接で最も問われる論点**を押さえる。ここは暗記ではなく理由まで。

**読む**
- [../src/main/java/com/example/scm/mapper/ItemMapper.java](../src/main/java/com/example/scm/mapper/ItemMapper.java) の `insert` と `search` のシグネチャ
- [../src/main/java/com/example/scm/service/ItemService.java:43](../src/main/java/com/example/scm/service/ItemService.java#L43)
  ―― `ORDER BY` に渡す文字列の**ホワイトリスト検証**
- [06-mybatis-spec.md](06-mybatis-spec.md) の「学習ポイント」表

押さえる点:

| 記法 | 展開 | 使いどころ |
|---|---|---|
| `#{value}` | `?`(PreparedStatement のプレースホルダ) | **値は必ずこちら**。SQLインジェクションを防げる |
| `${value}` | 文字列をそのまま埋め込む | ORDER BY のカラム名など**値ではない箇所のみ**。必ずホワイトリスト検証した値だけ渡す |

- `@Options(useGeneratedKeys = true, keyProperty = "id")`
  → INSERT 後、DB が採番した ID が**引数オブジェクトに書き戻される**(ヘッダ→明細の登録で必須)
- 引数が2つ以上あるときは `@Param` 必須(付けないと `arg0` / `param1` になる)

**手を動かす**
1. 商品一覧の並び替えを操作し、ログの `ORDER BY` が変わることを確認
2. `ItemService#search` のホワイトリストを**一時的に外して**不正な文字列を渡すと SQL が壊れることを見る
   → **必ず元に戻す**(なぜホワイトリストが必要かを体で理解するため)
3. `@Param` を1つ消して起動し、`Parameter 'xxx' not found` を出してみる → 直す
4. `ItemMapperTest#insertSetsGeneratedId` を読み、`mvn test -Dtest=ItemMapperTest#insertSetsGeneratedId` を実行

**理解チェック**
- 「`WHERE item_code = ${itemCode}` は何が危険か」を具体例付きで説明できるか
- `${}` を使ってよいのはどこか、その際に必ずやることは何か
- `useGeneratedKeys` が無いと、明細の INSERT で何が困るか

---

## STEP 4. XML方式と `resultMap` の基礎

**ねらい**: SQL を XML に外出しする方式へ移行し、**カラムとプロパティの対応付け(マッピング)**を理解する。

**読む**
- [../src/main/java/com/example/scm/mapper/StockMapper.java](../src/main/java/com/example/scm/mapper/StockMapper.java) ―― SQL の無いインタフェース
- [../src/main/resources/mapper/StockMapper.xml](../src/main/resources/mapper/StockMapper.xml) の先頭〜`stockResultMap`、`<sql>` / `<include>`、`findAll`
- [06-mybatis-spec.md](06-mybatis-spec.md) の「6.5 StockMapper」

押さえる点:
- **XML の `namespace` はインタフェースの FQCN と完全一致**、`<select id="...">` はメソッド名と完全一致
  → ズレると `Invalid bound statement (not found)`
- `resultType`(単純な型・自動マッピング)と `resultMap`(明示的な対応付け)の使い分け
- `<id>` は主キーの対応付け。**オブジェクトの同一性判定に使われる**(STEP 6 で効いてくる)
- `<sql>` / `<include refid="...">` で SELECT 句・FROM 句を使い回す

**手を動かす**
1. STEP 0 の 3 で自分が書いた JOIN と、`StockMapper.xml` の `findAll` が出す SQL(ログ)を見比べる
2. `<select id="findAll">` の `id` をわざと `findAll2` に変え、`Invalid bound statement (not found)` を出す → 直す
3. `namespace` を1文字変えて同じエラーを出す → 直す(**このエラーの原因を2種類体験しておく**)
4. `resultMap="stockResultMap"` を `resultType="Stock"` に変えて、ネストした `item` / `warehouse` が
   マッピングされなくなることを確認 → 戻す

**理解チェック**
- `Invalid bound statement (not found)` の原因を3つ挙げられるか
- `resultType` で足りるのはどんなとき、`resultMap` が必要なのはどんなときか
- `<sql>` / `<include>` は何のためにあるか

---

## STEP 5. `association`(多対一)と `columnPrefix`

**ねらい**: 1回の JOIN 結果を、**ネストしたオブジェクト**(`Stock` の中に `Item` と `Warehouse`)に組み立てる。

**読む**
- [../src/main/resources/mapper/StockMapper.xml:37-44](../src/main/resources/mapper/StockMapper.xml#L37-L44) ―― `stockResultMap` の `association`
- [../src/main/resources/mapper/StockMapper.xml:50-68](../src/main/resources/mapper/StockMapper.xml#L50-L68) ―― `i_` / `w_` の列別名の付け方
- [../src/main/java/com/example/scm/domain/Stock.java](../src/main/java/com/example/scm/domain/Stock.java)
- 画面: `/stocks`(3テーブルJOINの結果がそのまま表示されている)

押さえる点:
- `<association property="item" resultMap="itemResultMap" columnPrefix="i_"/>`
  = 「1件ぶら下げる(多対一)」
- `columnPrefix` は **JOIN したときの列名衝突を避ける仕組み**。
  `i_` を付けると `itemResultMap` の `column="item_code"` は `i_item_code` 列を見る
  → だから SELECT 句側で `i.item_code AS i_item_code` と別名を付けている
- `resultMap` は使い回せる(`itemResultMap` は `ShipmentMapper.xml` でも同じ考え方で登場)

**手を動かす**
1. `/stocks` を開き、ログの SELECT 句に `i_` / `w_` の別名が並んでいるのを確認
2. SELECT 句の別名 `i_item_name` を `item_name2` などに変え、
   画面の商品名が空になる(= 列名とマッピングがズレた)ことを確認 → 戻す
3. `StockMapperTest#findAllMapsAssociations` を読み、実行して通ることを確認
4. `stock.quantity` と `item.safety_stock` を比べる `findBelowSafetyStock` を読み、ダッシュボード `/` と照合

**理解チェック**
- `columnPrefix` を消したら何が起きるか(`item.id` と `stock.id` の衝突)
- `association` と、後で出る `collection` の違いは何か
- 「3テーブルJOIN 1回でネストしたオブジェクトを作る」ことの利点は何か

---

## STEP 6. `collection`(一対多)と N+1 問題

**ねらい**: **MyBatis で一番つまずくところ**。ヘッダ1件に明細N件をぶら下げる。

**読む**
- [../src/main/resources/mapper/ShipmentMapper.xml:28-55](../src/main/resources/mapper/ShipmentMapper.xml#L28-L55)
  ―― `shipmentDetailResultMap` と `shipmentWithDetailsResultMap`
- [../src/main/resources/mapper/ShipmentMapper.xml:76-96](../src/main/resources/mapper/ShipmentMapper.xml#L76-L96) ―― `findById` の SELECT 句
- [06-mybatis-spec.md](06-mybatis-spec.md) の「collection(1:N)」
- 画面: `/shipments/{id}`

押さえる点:
- `<collection property="details" ofType="ShipmentDetail" resultMap="..." columnPrefix="d_"/>`
- **`<id>` が必須**。JOIN するとヘッダ行が明細数だけ重複するが、`<id>` を手がかりに1オブジェクトへ集約される。
  書き忘れると明細がまとまらない/重複する ―― 定番の不具合
- **`columnPrefix` は入れ子で連結される**:
  `collection(d_)` → その中の `association(i_)` の列は **`d_i_item_name`**
- N+1 問題: 明細を別SELECT(`select=` 属性)で取ると、ヘッダN件につきSQLがN+1本になる。
  このアプリは **JOIN で1本にまとめている**

**手を動かす**
1. 明細を3行入れた出荷指示を作り、詳細画面を開いてログの SQL が **1本だけ**であることを確認
2. `shipmentWithDetailsResultMap` の `<id property="id" column="id"/>` を
   `<result .../>` に変えて画面を開く → 明細の見え方が壊れることを確認 → **戻す**
3. `ShipmentMapperTest#findByIdLoadsDetails` を実行
4. (発展)`<collection>` を `select=` 属性の遅延ロードに書き換え、SQLの本数を比べる → 戻す

**理解チェック**
- ヘッダ1件+明細3件の JOIN 結果は何行返ってくるか。それがなぜ1オブジェクトになるのか
- `<id>` を書き忘れると何が起きるか
- `d_i_item_name` という列別名がなぜ必要か
- N+1 問題とは何か。どう避けたか

---

## STEP 7. 動的SQL(`where` / `if` / `choose` / `foreach`)

**ねらい**: 検索条件の有無で SQL を組み立てる。**業務システムで最も書く部分**。

**読む**
- [../src/main/resources/mapper/ShipmentMapper.xml:100-132](../src/main/resources/mapper/ShipmentMapper.xml#L100-L132) ―― `search`
- [../src/main/resources/mapper/ShipmentMapper.xml:158-165](../src/main/resources/mapper/ShipmentMapper.xml#L158-L165) ―― `insertDetails`(`foreach` で複数行INSERT)
- [../src/main/java/com/example/scm/mapper/ItemMapper.java:54-77](../src/main/java/com/example/scm/mapper/ItemMapper.java#L54-L77) ―― アノテーション内の `<script>`
- [06-mybatis-spec.md](06-mybatis-spec.md) の「動的SQL(search)」

押さえる点:

| 要素 | 役割 |
|---|---|
| `<where>` | 条件が0個なら WHERE 句ごと消える。先頭の余った `AND` も自動除去 |
| `<if test="...">` | OGNL 式。`!= null`、`.size() > 0` など |
| `<choose>`/`<when>`/`<otherwise>` | switch 相当。**上から最初にマッチした1つだけ**採用(From/To の出し分け) |
| `<foreach>` | `IN (...)` の組み立て、`VALUES (...),(...)` の一括INSERT。`open`/`separator`/`close` |
| `<set>` | UPDATE の SET 句版(末尾カンマを除去)。このアプリでは未使用 |
| XMLエスケープ | `<` は `&lt;`、`>` は `&gt;`(`>=` は `&gt;=`) |

**手を動かす**
1. `/items` で検索条件を「1つも入れない → キーワードだけ → 価格も追加」と増やし、
   **ログの WHERE 句が変わっていく**のを確認
2. `/shipments` でステータスを複数選択し、`IN (?, ?)` の `?` の数が増えるのを確認
3. `/shipments` で出荷日 From だけ / To だけ / 両方を試し、`<choose>` の分岐を確認
4. `ItemSearchCriteria` に条件を1つ追加し、`ItemMapper#search` の `<if>` を足して動かす
   (→ README「5. 手を動かす課題」の 1 番)

**理解チェック**
- `<where>` を使わず `WHERE 1=1` と書く方式との違いを説明できるか
- `<if>` を並べるのと `<choose>` の違いは何か
- 明細を1件ずつ INSERT するのと `foreach` で1回にするのは何が違うか(DBとの往復回数)
- XML に `>=` をそのまま書くとどうなるか

---

## STEP 8. TypeHandler(enum ⇔ DBコード値)

**ねらい**: 区分値をコード(`"10"`, `"20"`)で持つ**レガシー業務DB**と、Java の enum をつなぐ。
実案件で TypeHandler の出番が一番多いパターン。

**読む**
- [../src/main/java/com/example/scm/domain/ShipmentStatus.java](../src/main/java/com/example/scm/domain/ShipmentStatus.java)
- [../src/main/java/com/example/scm/typehandler/ShipmentStatusTypeHandler.java](../src/main/java/com/example/scm/typehandler/ShipmentStatusTypeHandler.java)
- [../src/main/resources/application.yml](../src/main/resources/application.yml) の `type-handlers-package`
- [06-mybatis-spec.md](06-mybatis-spec.md) の「6.7 TypeHandler」

押さえる点:
- Java → DB: `setNonNullParameter` で `status.getCode()`(`"10"`)を設定
- DB → Java: `getNullableResult` で `ShipmentStatus.fromCode(...)` に変換
- `@MappedTypes(ShipmentStatus.class)` + `type-handlers-package` で**自動登録**
  → XML に `typeHandler=` を書かなくても、プロパティの型から自動適用される
- `<foreach>` 内の `#{st}` のようなパラメータにも効く
- 未知コードは `IllegalArgumentException`(不正データを早期に検知する設計)

**手を動かす**
1. H2 コンソールで `SELECT id, status FROM shipment;` → **DBには `"10"` などのコードが入っている**ことを確認
2. 出荷一覧を開き、画面には「登録済/引当済…」と出ること、ログの Parameters は `10(String)` であることを確認
3. `ShipmentMapperTest#typeHandlerConvertsCodeToEnum` を読んで実行
4. H2 コンソールで `UPDATE shipment SET status = '99' WHERE id = ...` と不正値を入れ、
   画面を開いて例外になることを確認 → 正しい値に戻す

**理解チェック**
- TypeHandler が無かったら `Shipment#status` はどうなるか
- 登録方法(自動登録)と、XML に明示的に書く方法の違い
- enum の `name()` をそのまま DB に入れる設計と、コード値を持つ設計の違い

---

## STEP 9. トランザクション(Service層)

**ねらい**: 複数テーブルの更新を**1つの業務処理**としてまとめ、失敗したら全部取り消す。

**読む**
- [../src/main/java/com/example/scm/service/ShipmentService.java:94](../src/main/java/com/example/scm/service/ShipmentService.java#L94) ―― `allocate`(在庫引当)
- [../src/main/java/com/example/scm/service/ShipmentService.java:72](../src/main/java/com/example/scm/service/ShipmentService.java#L72) ―― `create`(ヘッダ→採番ID→明細)
- [../src/main/java/com/example/scm/service/StockService.java:33](../src/main/java/com/example/scm/service/StockService.java#L33) ―― 入庫(UPDATE→0件ならINSERT)
- [../src/main/resources/mapper/StockMapper.xml:107](../src/main/resources/mapper/StockMapper.xml#L107) ―― `decrease` の SQL
- [06-mybatis-spec.md](06-mybatis-spec.md) の「6.8 トランザクション」

押さえる点:
- 境界は **Service 層**。クラスに `@Transactional(readOnly = true)`、更新メソッドに `@Transactional`
- `RuntimeException` で自動ロールバック。**検査例外ではロールバックされない**
- 同一クラス内のメソッドを `this` 経由で呼ぶと `@Transactional` が効かない(自己呼び出し問題)
- **在庫減算のイディオム**:
  ```sql
  UPDATE stock SET quantity = quantity - #{quantity}
   WHERE item_id = #{itemId} AND warehouse_id = #{warehouseId}
     AND quantity >= #{quantity}   -- 在庫不足なら 0 件更新
  ```
  **更新件数0 = 在庫不足**と判定する。「SELECT で残数確認 → UPDATE」より同時実行に強い

**手を動かす**
1. 在庫が十分な明細2行で引当 → 在庫が2件とも減り、ステータスが「引当済」になることを確認
2. **1行目は在庫あり / 2行目は在庫不足**になる出荷指示を作って引当する
   → 「在庫が不足しています」が出て、**1行目の在庫も減っていない**ことを `/stocks` で確認(=ロールバック)
3. そのときのログを見て、UPDATE が発行されたのに結果が残っていないことを確認
4. (発展)`BusinessException` を検査例外に変えると引当途中の減算が残ってしまうことを考えてみる

**理解チェック**
- ロールバックされる例外・されない例外は何か
- 「更新件数0で在庫不足を判定する」書き方の利点を、SELECT→UPDATE 方式と比べて説明できるか
- トランザクション境界を Controller や Mapper に置くと何が困るか

---

## STEP 10. テストで SQL とマッピングを検証する

**ねらい**: 画面をポチポチせずに、**Mapper の SQL とマッピングを機械的に確認**できるようにする。
ここまで来たら、以降の学習・改造はすべてテストで回せます。

**読む**
- [07-test-spec.md](07-test-spec.md)
- [../src/test/java/com/example/scm/mapper/ItemMapperTest.java](../src/test/java/com/example/scm/mapper/ItemMapperTest.java)(`@MybatisTest`)
- [../src/test/java/com/example/scm/mapper/ShipmentMapperTest.java](../src/test/java/com/example/scm/mapper/ShipmentMapperTest.java)(JdbcTemplate で生の値を確認)
- [../src/test/java/com/example/scm/service/ShipmentServiceTest.java:111](../src/test/java/com/example/scm/service/ShipmentServiceTest.java#L111)
  ―― `allocateRollsBackWhenStockIsShort`。**あえてテストをトランザクションで包まず**、本当にロールバックされたかを検証
- [../src/test/java/com/example/scm/web/ItemApiControllerTest.java](../src/test/java/com/example/scm/web/ItemApiControllerTest.java)(`@WebMvcTest`)

押さえる点:

| アノテーション | 起動範囲 | 用途 |
|---|---|---|
| `@MybatisTest` | Mapper + DataSource のみ | SQL とマッピングの確認。毎回ロールバックされる |
| `@SpringBootTest` | アプリ全体 | Service の業務ロジック・トランザクションの確認 |
| `@WebMvcTest` | Web層のみ(Service は `@MockBean`) | Controller の入出力 |

**手を動かす**
```powershell
mvn test                                   # 全件
mvn test -Dtest=ShipmentMapperTest         # クラス指定
mvn test -Dtest=ShipmentServiceTest#allocateRollsBackWhenStockIsShort
```
1. まず全件通ることを確認
2. STEP 7 で追加した検索条件に対するテストを1つ書く
3. `StockMapper.xml` の `decrease` から `AND quantity >= #{quantity}` を消すと**どのテストが落ちるか**を予想し、
   実際に消して確認 → 戻す(テストが仕様を守っていることの実感)

**理解チェック**
- `@MybatisTest` と `@SpringBootTest` を使い分ける基準は何か
- テストが毎回ロールバックされると、なぜロールバックの検証だけ別扱いが必要なのか
- 「SQLを直した」ときに、まずどのテストを走らせるか

---

## STEP 11. Web層まで通す(任意)

**ねらい**: 画面の操作が Controller → Service → Mapper → SQL とどう繋がるかを1本の線で見る。
MyBatis 本体ではないので、案件で Spring MVC / jQuery も触るなら読む。

**読む**
- [03-screen-spec.md](03-screen-spec.md) / [05-api-spec.md](05-api-spec.md)
- [../src/main/java/com/example/scm/web/ItemController.java](../src/main/java/com/example/scm/web/ItemController.java)(Thymeleaf 画面)
- [../src/main/java/com/example/scm/web/api/ItemApiController.java](../src/main/java/com/example/scm/web/api/ItemApiController.java)(REST API)
- [../src/main/java/com/example/scm/web/api/ApiExceptionHandler.java](../src/main/java/com/example/scm/web/api/ApiExceptionHandler.java) / [../src/main/java/com/example/scm/web/GlobalExceptionHandler.java](../src/main/java/com/example/scm/web/GlobalExceptionHandler.java)
- [../src/main/resources/static/js/app.js](../src/main/resources/static/js/app.js)(jQuery の Ajax)

**手を動かす**
1. ブラウザの開発者ツール(Network)を開いて `/items` で検索 → 飛んでいる Ajax リクエストを確認
2. 同時にサーバログを見て、**1つの画面操作 → 1本のSQL**の対応を目で追う
3. 在庫不足エラーを起こし、`BusinessException` が画面/APIでどうユーザ向けメッセージになるかを追う

**理解チェック**
- 画面操作からSQL発行までに通るクラスを順に言えるか
- 業務エラー(`BusinessException`)はどこで捕まえて、どう返しているか

---

## STEP 12. 応用課題と実案件とのギャップ

ここまで終わったら、**自分で機能を足す**のが最速です。課題は [../README.md](../README.md) の
「5. 手を動かす課題」にやさしい順で並んでいます。加えて、実案件で必ず出会う次のテーマを調べてみてください。

| テーマ | 調べる内容 |
|---|---|
| ページング | `LIMIT #{limit} OFFSET #{offset}` + 件数取得SELECT、`PageHelper` |
| 一括処理 | `ExecutorType.BATCH`(大量INSERT時) |
| 遅延ロード | `<collection select="...">`、`fetchType="lazy"` と N+1 のトレードオフ |
| 排他制御 | 更新日時によるオプティミスティックロック(`WHERE updated_at = #{updatedAt}`) |
| SQL方言 | Oracle / PostgreSQL の違い(シーケンス、`ROWNUM`、`||` 連結、日付関数) |
| キャッシュ | MyBatis の1次/2次キャッシュ。業務系では基本オフで考える |
| 構成の違い | 実案件は Spring MVC + JSP を war デプロイする構成も多い。3層構造とMyBatisの書き方は共通 |

---

## 最終セルフチェック(口で説明できるか)

- [ ] `@Mapper` を付けたインタフェースの実装は誰が作るか
- [ ] `#{}` と `${}` の違いと、`${}` を使うときの必須条件
- [ ] アノテーション方式と XML 方式の使い分け
- [ ] `resultType` と `resultMap` の使い分け
- [ ] `association` と `collection` の違い
- [ ] `resultMap` の `<id>` が無いと 1:N で何が起きるか
- [ ] `columnPrefix` の役割と、入れ子で連結されること
- [ ] N+1 問題とは何か、このアプリではどう避けているか
- [ ] `<where>` / `<if>` / `<choose>` / `<foreach>` をそれぞれ書けるか
- [ ] TypeHandler が必要になる場面と実装の2メソッド
- [ ] `@Transactional` の境界をどこに置くか、ロールバックされない例外は何か
- [ ] 「更新件数0で在庫不足」のイディオムの意味
- [ ] `Invalid bound statement (not found)` の原因3つ
- [ ] SQLログから発行SQL・バインド値・件数を読み取れるか

---

## 詰まったときの参照先

| 状況 | 見る場所 |
|---|---|
| 書き方を思い出したい | <http://localhost:8080/learn>(早見表)、[06-mybatis-spec.md](06-mybatis-spec.md) |
| エラーが出た | [06-mybatis-spec.md](06-mybatis-spec.md) の「6.9 よくあるエラーと原因」、[../README.md](../README.md) の「6. よくあるエラーと対処」 |
| SQL が想定と違う | 起動ターミナルのログ(`==> Preparing` / `==> Parameters` / `<== Total`) |
| DB の中身を見たい | H2 コンソール <http://localhost:8080/h2-console> |
| 環境が動かない | [08-setup-operations.md](08-setup-operations.md) |
| 壊してしまった | `data` フォルダを削除して再起動(`schema.sql` / `data.sql` が再実行される) |
