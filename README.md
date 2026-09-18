# 物流管理(SCM)ミニWebアプリ ―― MyBatis 学習用

大手通信会社向け SCM / 物流管理システム案件の技術スタック
(**Java / Spring Framework / MyBatis / jQuery / JUnit**)を、
ローカルで一通り触って学べるようにした小さな Web アプリです。

**MyBatis のマッピングと DB とのやり取り**を重点的に学べるよう、
アノテーション方式・XML 方式・1:1 / 1:N マッピング・動的SQL・TypeHandler・
トランザクションを、すべて実際に動く形で入れてあります。

> 詳細な仕様は [`docs/`](docs/README.md) にまとめています
> (システム概要 / 機能 / 画面 / DB / API / MyBatisマッピング / テスト / 環境構築)。

---

## 1. 動かし方

### 必要なもの(このPCには構築済み)

| ツール | バージョン | 場所 |
|---|---|---|
| JDK | Temurin 21 | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Maven | 3.9.9 | `C:\Users\tky06\tools\apache-maven-3.9.9` |
| DB | H2(組込み・ファイルモード) | 別途インストール不要 |

`JAVA_HOME` / `MAVEN_HOME` / `Path` はユーザー環境変数に登録済みです。
**新しいターミナルを開いてから**以下を実行してください(登録前に開いたターミナルには反映されません)。

### 起動

```powershell
cd C:\Users\tky06\Documents\develope_app\SpringMyBatis

# 開発中はこれが手軽(Ctrl+C で停止)
mvn spring-boot:run

# jar を作って起動する場合
mvn clean package
java -jar target\scm-study-0.0.1-SNAPSHOT.jar
```

Maven が PATH に無い環境でも、同梱の Maven Wrapper が使えます: `.\mvnw.cmd spring-boot:run`

ブラウザで <http://localhost:8080> を開きます。

### テスト

```powershell
mvn test                                   # 全34件
mvn test -Dtest=ShipmentMapperTest         # クラス指定
mvn test -Dtest=ShipmentServiceTest#allocateRollsBackWhenStockIsShort   # メソッド指定
```

### DB を直接見る

H2 コンソール: <http://localhost:8080/h2-console>

**接続情報は入力済みの状態で開きます。そのまま Connect を押すだけです。**

| 項目 | 既定で入っている値 |
|---|---|
| Saved Settings | `SCM学習用DB (scm-study)` |
| JDBC URL | `jdbc:h2:file:./data/scmdb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1` |
| User Name | `sa` |
| Password | (空のまま) |

既定値は [`config/H2ConsoleConfig.java`](src/main/java/com/example/scm/config/H2ConsoleConfig.java) が
起動時に `data/.h2.server.properties` を生成することで設定しています。
`application.yml` の `spring.datasource.url` を変えれば、コンソール側の既定値も自動で追従します。

URL の `./data/scmdb` は相対パスですが、コンソールはアプリと同じ JVM で動くため
**アプリを起動したフォルダ基準**で解決されます(= アプリが使っているDBそのものに繋がる)。
`AUTO_SERVER=TRUE` のおかげで、アプリを起動したままでも接続できます。

データは `data/scmdb.mv.db` に永続化されます。
**初期状態に戻したいときは `data` フォルダごと削除**して起動し直してください
(`schema.sql` / `data.sql` が再実行されます)。

---

## 2. 画面

| URL | 内容 | 学べること |
|---|---|---|
| `/` | ダッシュボード | 集計SELECT、安全在庫割れ抽出(JOIN) |
| `/items` | 商品マスタ | **動的SQL**の検索、jQuery の Ajax 検索、登録/更新/削除 |
| `/stocks` | 在庫照会・入庫 | **association**(3テーブルJOIN)、Ajax での更新 |
| `/shipments` | 出荷指示一覧 | `<foreach>` の IN 句、`<choose>` の日付出し分け |
| `/shipments/new` | 出荷指示登録 | ヘッダ＋明細の一括INSERT、jQuery の明細行追加 |
| `/shipments/{id}` | 出荷指示詳細 | **collection**(1:N)、トランザクション(引当/取消) |
| `/learn` | MyBatis 早見表 | 書き方のまとめ(実装ファイルへの対応つき) |

### まず触ってみる流れ

1. `/shipments/new` で出荷指示を作る(明細を2行以上入れる)
2. 詳細画面で **「在庫引当」** → 在庫が減り、ステータスが「引当済」になる
3. `/stocks` で在庫が減っていることを確認
4. わざと在庫数を超える数量で出荷指示を作り、引当してみる
   → 「在庫が不足しています」と出て、**1行目の在庫も減っていない**(=ロールバック)
5. 起動しているターミナルに、実際に発行された SQL とバインド値が出ているのを確認する

---

## 3. プロジェクト構成

```
src/main/java/com/example/scm/
├── ScmStudyApplication.java        起動クラス
├── domain/                         ドメイン(Item, Stock, Shipment, ShipmentDetail, ShipmentStatus...)
├── mapper/                         ★ MyBatis の Mapper インタフェース
│   ├── ItemMapper.java               アノテーション方式(@Select/@Insert/<script>動的SQL)
│   ├── WarehouseMapper.java          一番シンプルな形
│   ├── StockMapper.java              XML方式(SQLは StockMapper.xml)
│   └── ShipmentMapper.java           XML方式(SQLは ShipmentMapper.xml)
├── typehandler/
│   └── ShipmentStatusTypeHandler.java  ★ enum ⇔ DBコード値("10"/"20")の変換
├── service/                        業務ロジック＋@Transactional
│   ├── ItemService.java              ORDER BY のホワイトリスト検証
│   ├── StockService.java             入庫(UPDATE→0件ならINSERT)
│   └── ShipmentService.java        ★ 出荷指示の登録・引当・出荷・取消
├── web/                            画面用Controller(Thymeleaf)
│   └── api/                        REST API(jQuery から呼ぶ)
└── exception/BusinessException.java

src/main/resources/
├── application.yml                 ★ MyBatis 設定(mapper-locations, TypeHandler, ログ)
├── schema.sql / data.sql           起動時のDDL・初期データ(何度実行してもOK)
├── mapper/
│   ├── StockMapper.xml             ★ resultMap / association / <sql><include>
│   └── ShipmentMapper.xml          ★ collection(1:N) / foreach / choose
├── templates/                      Thymeleaf のHTML
└── static/js/app.js                jQuery(Ajax)

src/test/java/com/example/scm/
├── mapper/   ItemMapperTest / StockMapperTest / ShipmentMapperTest   (@MybatisTest)
├── service/  ItemServiceTest / ShipmentServiceTest                   (@SpringBootTest)
└── web/      ItemApiControllerTest                                   (@WebMvcTest)
```

### テーブル

```
warehouse (倉庫)          item (商品)
     │                       │
     └──────┬────────────────┘
            │
          stock (在庫: 商品×倉庫でユニーク)

shipment (出荷指示ヘッダ) ──1:N── shipment_detail (明細) ──N:1── item
```

---

## 4. MyBatis 学習ガイド(どこを読むか)

読む順番のおすすめです。アプリを起動したまま、該当画面を触りながら読むと早いです。

### ステップ1: アノテーション方式 ― `mapper/ItemMapper.java`

- `@Select` / `@Insert` / `@Update` / `@Delete`
- `#{}`(プレースホルダ)と `${}`(文字列埋め込み)の違い ← **面接でも頻出**
- `@Options(useGeneratedKeys = true, keyProperty = "id")` で採番されたIDを受け取る
- `@Param` による複数引数の名前付け
- `<script>` を使ったアノテーション内の動的SQL

### ステップ2: XML 方式と resultMap ― `resources/mapper/StockMapper.xml`

- namespace はインタフェースの FQCN と**完全一致**
- `<resultMap>` の `<id>` と `<result>`
- `<association>`(多対一)と `columnPrefix` による列名衝突の回避
- `<sql>` / `<include>` による SQL の使い回し
- `UPDATE ... WHERE quantity >= #{quantity}` で**更新件数0 = 在庫不足**と判定するテクニック

### ステップ3: 1対多と動的SQL ― `resources/mapper/ShipmentMapper.xml`

- `<collection>` でヘッダに明細をぶら下げる(`<id>` の書き忘れが定番の不具合)
- `<where>` / `<if>` / `<choose>` / `<foreach>`
- `<foreach>` による複数行INSERT
- JOIN で1回のSQLにまとめる = **N+1問題**を避ける

### ステップ4: TypeHandler ― `typehandler/ShipmentStatusTypeHandler.java`

区分値をコード(`"10"`, `"20"`)で持つレガシーDBと Java の enum をつなぐ方法。
`application.yml` の `mybatis.type-handlers-package` で自動登録されます。

### ステップ5: トランザクション ― `service/ShipmentService.java`

- `@Transactional` の境界はサービス層
- RuntimeException でロールバック(検査例外ではロールバックされない)
- 複数テーブル(shipment / shipment_detail / stock)の更新を1つの業務処理にまとめる

### ステップ6: テスト ― `src/test/java`

| アノテーション | 起動範囲 | 用途 |
|---|---|---|
| `@MybatisTest` | Mapper + DataSource のみ | Mapper の SQL とマッピングの確認。毎回ロールバック |
| `@SpringBootTest` | アプリ全体 | Service の業務ロジック・トランザクションの確認 |
| `@WebMvcTest` | Web 層のみ | Controller の入出力。Service は `@MockBean` |

`ShipmentServiceTest#allocateRollsBackWhenStockIsShort` は、
**あえてテストをトランザクションで包まずに**「本当にロールバックされたか」を検証しています。

---

## 5. 手を動かす課題

やさしい順です。詰まったら既存の似た実装を真似るのが早道です。

1. **商品検索に「安全在庫を下回っているものだけ」チェックボックスを足す**
   → `ItemSearchCriteria` に項目追加 → `ItemMapper#search` の `<if>` を1つ足す
2. **在庫一覧をカテゴリでも絞り込めるようにする**
   → `StockMapper.xml` の `findByWarehouse` に `<if>` を追加
3. **出荷指示に「出荷先の郵便番号」を追加する**
   → `schema.sql`(カラム追加) → ドメイン → XML の resultMap と INSERT → 画面
4. **出荷指示の明細を後から編集できるようにする**
   → `deleteDetailsByShipmentId` + `insertDetails` を1トランザクションで
5. **一覧にページングを入れる**
   → `LIMIT #{limit} OFFSET #{offset}` と件数取得SELECT、または `PageHelper` の導入を調べてみる
6. **`<collection>` を `select=` 属性(遅延ロード)に書き換えて、SQLログの本数を比べる**
   → N+1問題を体感する

---

## 6. よくあるエラーと対処

| エラー | 原因 |
|---|---|
| `Invalid bound statement (not found)` | ①XMLの namespace がインタフェースのFQCNと違う ②`<select id>` とメソッド名が違う ③`mybatis.mapper-locations` の指定漏れ |
| プロパティが null のまま | カラム別名とプロパティ名の不一致。`map-underscore-to-camel-case` の設定も確認 |
| 1:N の明細が1件しか入らない/重複する | resultMap に `<id>` が無い |
| `Parameter 'xxx' not found` | 引数が2つ以上あるのに `@Param` が無い |
| `PRIMARY KEY violation` が登録時に出る | 初期データで id を直接指定すると採番カウンタが進まない(→ `data.sql` は採番に任せる書き方にしてあります) |
| ポート8080が使用中 | `application.yml` の `server.port` を変更、または既存プロセスを停止 |

SQL が想定通りか迷ったら、起動ログを見てください。
`logging.level.com.example.scm.mapper: debug` により、
発行SQL・バインド値・取得件数がすべて出力されます。

```
==>  Preparing: SELECT id, item_code, ... FROM item WHERE category = ? AND unit_price >= ?
==> Parameters: ネットワーク機器(String), 10000(BigDecimal)
<==      Total: 3
```

---

## 7. 実案件との違い(把握しておくとよい点)

- 実案件では **Spring MVC + JSP を Tomcat に war デプロイ**する構成も多い(本アプリは Spring Boot + Thymeleaf)。
  Controller / Service / Mapper の3層構造と MyBatis の書き方自体は共通です。
- DB は Oracle / PostgreSQL などが一般的。SQL の方言(シーケンス、`ROWNUM`、`||` 連結など)は差異が出ます。
- 認証・認可(Spring Security)、排他制御(更新日時によるオプティミスティックロック)、
  帳票出力、バッチ(Spring Batch)などは本アプリには含めていません。
