# 08. 環境構築・運用

本書は**構築済み環境の設定値と運用手順**をまとめた参照資料です。
新しい PC でゼロから構築する場合は、STEP 形式の
[09-environment-setup.md(環境構築手順書)](09-environment-setup.md) を使ってください。

## 8.1 前提環境

| ツール | バージョン | 構築済みの場所(この PC) |
|---|---|---|
| JDK | Temurin 21 | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Maven | 3.9.9 | `C:\Users\tky06\tools\apache-maven-3.9.9` |
| DB | H2(組込み) | 依存ライブラリとして同梱。個別インストール不要 |

環境変数はユーザー環境変数に登録済み。**登録後に開いたターミナル**から有効になる。

| 変数 | 値 |
|---|---|
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| `MAVEN_HOME` | `C:\Users\tky06\tools\apache-maven-3.9.9` |
| `Path` | 上記2つの `\bin` を追加 |

### 別環境で構築する場合

手順の詳細は [09-environment-setup.md](09-environment-setup.md) を参照。要点は以下のとおり。

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK
# Maven は winget に無いため公式 zip を展開して PATH に追加する
# もしくは同梱の Maven Wrapper を使う(Maven 本体の準備が不要)
.\mvnw.cmd -v
```

## 8.2 ビルドと起動

```powershell
cd C:\Users\tky06\Documents\develope_app\SpringMyBatis

# 開発中の起動(Ctrl+C で停止)
mvn spring-boot:run

# jar を作って起動
mvn clean package
java -jar target\scm-study-0.0.1-SNAPSHOT.jar

# テスト
mvn test
```

Maven Wrapper を使う場合は `mvn` を `.\mvnw.cmd` に置き換える。

起動後: <http://localhost:8080>

### 停止方法

| 起動方法 | 停止 |
|---|---|
| ターミナルでフォアグラウンド起動 | `Ctrl + C` |
| バックグラウンド起動 | 下記のポート指定で停止する |

```powershell
# ポート 8080 を掴んでいるプロセスだけを止める
Get-NetTCPConnection -LocalPort 8080 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

> **`Get-Process java | Stop-Process` は使わないこと。**
> VS Code の Java 言語サーバー(`redhat.java` が起動する JVM)まで巻き込んで終了させてしまい、
> 拡張機能が不正な状態になることがある(コマンドの二重登録エラーなど)。

## 8.3 設定値一覧(application.yml)

| 設定キー | 値 | 説明 |
|---|---|---|
| `server.port` | 8080 | 待ち受けポート。競合する場合はここを変更 |
| `spring.datasource.url` | `jdbc:h2:file:./data/scmdb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1` | 起動フォルダ基準の相対パス |
| `spring.datasource.username` / `password` | `sa` / (空) | ― |
| `spring.sql.init.mode` | `always` | 起動のたびに `schema.sql` / `data.sql` を実行 |
| `spring.h2.console.enabled` | `true` | H2 コンソールの有効化 |
| `spring.h2.console.path` | `/h2-console` | コンソールのURL |
| `spring.h2.console.settings.web-allow-others` | `false` | 他ホストからの接続を禁止 |
| `spring.thymeleaf.cache` | `false` | テンプレートを都度読み込み(開発用) |
| `spring.mvc.format.date` | `iso` | `<input type="date">` の値を `LocalDate` にバインド |
| `mybatis.*` | [06-mybatis-spec.md](06-mybatis-spec.md) 参照 | MyBatis の設定 |
| `logging.level.com.example.scm.mapper` | `debug` | 発行SQL・バインド値・取得件数を出力 |

### URL パラメータの意味

| パラメータ | 意味 |
|---|---|
| `AUTO_SERVER=TRUE` | アプリ起動中でも他プロセス(外部ツール)から同じ DB に接続できる |
| `DB_CLOSE_DELAY=-1` | 最後の接続が閉じても DB を閉じない |

## 8.4 H2 コンソール

<http://localhost:8080/h2-console>

**接続情報は入力済みの状態で開く。そのまま Connect を押すだけでよい。**

| 項目 | 既定値 |
|---|---|
| Saved Settings | `SCM学習用DB (scm-study)` |
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:./data/scmdb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1` |
| User Name | `sa` |
| Password | (空) |

### 既定値の仕組み

`com.example.scm.config.H2ConsoleConfig` が起動時に以下を行う。

1. `spring.datasource.url` の値を使って `data/.h2.server.properties` を生成する
   (書式: `0=表示名|ドライバ|JDBC URL|ユーザー名`)
2. Spring Boot が自動登録した H2 コンソールのサーブレット(Bean 名 `h2Console`)に、
   初期化パラメータ `properties`(= H2 の `-properties` 引数)でその置き場所を渡す

これにより、`application.yml` の接続先を変更すればコンソール側の既定値も自動で追従する。

> **実装上の注意**
> 組込み Tomcat はコンテキスト初期化の途中で起動し、その時点でサーブレット登録情報を読み取る。
> 通常の `@Configuration` のコンストラクタで初期化パラメータを追加しても**間に合わない**ため、
> `h2Console` Bean の生成時に割り込む `BeanPostProcessor` で設定している。

### 相対パスが機能する理由

H2 コンソールはアプリと同じ JVM で動作するため、`./data/scmdb` は
**アプリを起動したフォルダ**を基準に解決される(= アプリが使っている DB そのものに接続される)。

## 8.5 データのリセット

```powershell
# アプリを停止してから
Remove-Item data -Recurse -Force
```

次回起動時に `schema.sql` / `data.sql` が実行され、初期データが再生成される。

| 削除されるファイル | 内容 |
|---|---|
| `data/scmdb.mv.db` | DB 本体 |
| `data/scmdb.lock.db` | ロックファイル(起動中のみ存在) |
| `data/.h2.server.properties` | H2 コンソールの接続設定(起動時に再生成される) |

`data/` は `.gitignore` の対象のため、Git には含まれない。

## 8.6 ディレクトリ構成

```
SpringMyBatis/
├── docs/                       仕様書(本書)
├── src/main/java/              アプリケーションコード
├── src/main/resources/
│   ├── application.yml         設定
│   ├── schema.sql / data.sql   DDL / 初期データ
│   ├── mapper/                 MyBatis の XML マッパー
│   ├── templates/              Thymeleaf テンプレート
│   └── static/                 CSS / JavaScript(jQuery 同梱)
├── src/test/java/              テストコード
├── data/                       H2 の DB ファイル(Git 管理外・自動生成)
├── target/                     ビルド成果物(Git 管理外)
├── pom.xml                     Maven ビルド定義
├── mvnw / mvnw.cmd / .mvn/     Maven Wrapper
└── README.md                   セットアップと学習ガイド
```

## 8.7 トラブルシュート

| 症状 | 原因 / 対処 |
|---|---|
| `java`/`mvn` が見つからない | 環境変数登録後に開いたターミナルを使う。または `.\mvnw.cmd` を使う |
| `mvn package` が `Unable to rename ... .jar.original` で失敗 | アプリが起動中で jar がロックされている。先に停止する |
| VS Code で `couldn't create connection to server` / `command 'sts.java...' already exists` | Spring Boot Tools(`vmware.vscode-spring-boot`)と `redhat.java` のコマンド二重登録。Spring Boot Tools を撤去するか無効化する(Java の定義ジャンプ等は `redhat.java` 側の機能なので影響なし) |
| ポート 8080 が使用中 | `application.yml` の `server.port` を変更、または既存プロセスを停止 |
| H2 コンソールで `Database "C:/Users/xxx/test" not found` | JDBC URL が H2 の初期値 `jdbc:h2:~/test` のまま。8.4 の URL に置き換える |
| `Invalid bound statement (not found)` | [06-mybatis-spec.md](06-mybatis-spec.md) の「よくあるエラー」を参照 |
| 起動時に主キー違反 | `data.sql` で ID を直接指定していないか確認(採番カウンタが進まないため) |
| 画面の文字が化ける | ファイルが UTF-8 で保存されているか確認(特に新規作成した `.md` / `.sql`) |
| SQL が意図どおりか分からない | 起動ログの `==> Preparing:` / `==> Parameters:` / `<== Total:` を確認 |

## 8.8 リポジトリ

| 項目 | 値 |
|---|---|
| リモート | <https://github.com/saiteki2609et/SpringMyBatis> |
| 既定ブランチ | `main` |
| Git 管理外 | `target/`、`data/`、IDE 設定ファイル、`.mvn/wrapper/maven-wrapper.jar` |
