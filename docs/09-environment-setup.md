# 09. 環境構築手順書

まっさらな Windows PC で、このアプリをビルド・起動できる状態にするまでの手順書です。
上から順に実行し、各 STEP の**確認**が想定どおりになってから次へ進んでください。

- 所要時間: 約 20〜30 分(ネットワーク速度による。依存ライブラリの初回ダウンロードで大半を占める)
- 完成状態: <http://localhost:8080> でアプリが表示され、`mvn test` が 36 件すべて成功する
- 設定値の意味や運用については [08-setup-operations.md](08-setup-operations.md) を参照

## 0. 全体像

```
STEP 1  JDK 21 のインストール              ← 必須
STEP 2  環境変数(JAVA_HOME / Path)の設定   ← 必須
STEP 3  Maven の準備                       ← ルートA(Wrapper) / ルートB(本体) を選択
STEP 4  ソースコードの取得                  ← 必須
STEP 5  ビルド                             ← 必須
STEP 6  テスト実行                          ← 必須(構築が正しいことの確認)
STEP 7  起動と動作確認                      ← 必須
STEP 8  VS Code のセットアップ              ← 任意(開発する場合)
```

### Maven の2つのルート

| ルート | 内容 | 向いているケース |
|---|---|---|
| **A: Maven Wrapper のみ**(推奨) | リポジトリ同梱の `mvnw.cmd` が Maven 本体を自動ダウンロードする。追加インストール不要 | このプロジェクトだけ動かせればよい |
| B: Maven 本体をインストール | `mvn` コマンドをどこでも使えるようにする | 他の Java プロジェクトも扱う |

以降のコマンドは、ルート A なら `mvn` を `.\mvnw.cmd` に読み替えてください。

## 1. 前提条件

| 項目 | 要件 |
|---|---|
| OS | Windows 10 / 11(本手順書は Windows 11 Pro で検証) |
| 権限 | JDK インストール時に管理者権限(UAC の承認)が必要 |
| ネットワーク | インターネット接続必須(JDK・Maven・依存ライブラリのダウンロード) |
| ディスク空き | 約 1.5 GB(JDK 約 300MB、Maven 約 10MB、依存ライブラリ `~/.m2` 約 300MB) |
| ポート | 8080 が空いていること(使用中の場合は STEP 7 の補足を参照) |
| ターミナル | PowerShell(Windows Terminal または VS Code の統合ターミナル) |

> DB(H2)は依存ライブラリとして同梱されるため、**個別のインストールは不要**です。

### 事前確認

すでに入っているものを確認します。

```powershell
java -version      # JDK
mvn -v             # Maven
git --version      # Git
code --version     # VS Code
```

`java` と `mvn` が「認識されていません」と出れば未インストールです(これからの手順で入れます)。
Git が無い場合は先に `winget install --id Git.Git` でインストールしてください。

---

## STEP 1. JDK 21 のインストール

### 1-1. インストール

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
```

- 途中で UAC(ユーザーアカウント制御)のダイアログが出たら「はい」を押します。
  **応答しないとインストールが進まず、途中で止まったように見える**ので注意してください。
- 完了まで数分かかります。

### 1-2. winget が使えない場合

<https://adoptium.net/temurin/releases/?version=21> から
「Windows / x64 / JDK / .msi」をダウンロードして実行します。

### 確認

```powershell
Get-ChildItem "C:\Program Files\Eclipse Adoptium"
```

`jdk-21.0.x.x-hotspot` のようなフォルダが表示されれば成功です。

> この時点では `java -version` はまだ通りません(Path 未設定のため)。STEP 2 で設定します。

---

## STEP 2. 環境変数の設定

Temurin の MSI は `JAVA_HOME` や `Path` を自動設定しません。手動で設定します。

### 2-1. 設定

PowerShell に以下をそのまま貼り付けて実行します(JDK のパスは自動検出します)。

```powershell
$jdk = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory |
        Where-Object { $_.Name -like "jdk-21*" } | Select-Object -First 1).FullName
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdk, "User")

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($userPath -notlike "*$jdk\bin*") {
    [Environment]::SetEnvironmentVariable("Path", ($userPath.TrimEnd(';') + ";$jdk\bin").TrimStart(';'), "User")
}
Write-Output "JAVA_HOME = $jdk"
```

### 2-2. ターミナルを開き直す

> **重要**: 環境変数は**新しく開いたターミナルにしか反映されません。**
> 実行中の PowerShell / VS Code は、いったん閉じてから開き直してください。
> 「設定したのに `java` が見つからない」の原因はほぼこれです。

### 確認

新しいターミナルで:

```powershell
java -version
```

```
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS, mixed mode, sharing)
```

`21.` で始まるバージョンが表示されれば成功です。

---

## STEP 3. Maven の準備

### ルート A: Maven Wrapper を使う(推奨・追加インストール不要)

リポジトリに `mvnw.cmd` が同梱されているため、**この STEP は不要**です。STEP 4 へ進んでください。
初回実行時に Maven 3.9.9 が `%USERPROFILE%\.m2\wrapper\dists` へ自動ダウンロードされます。

> Wrapper は内部で `powershell` を使って Maven をダウンロードします。
> PowerShell が実行できない環境ではルート B を選んでください。

### ルート B: Maven 本体をインストールする

**winget には Apache Maven のパッケージがありません**(`Apache.Maven` は存在しない)。
公式 zip を展開して配置します。

```powershell
# 1) ダウンロードして展開(ユーザーフォルダ配下なので管理者権限不要)
$tools = "$env:USERPROFILE\tools"
New-Item -ItemType Directory $tools -Force | Out-Null
$zip = "$env:TEMP\apache-maven-3.9.9-bin.zip"
Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile $zip
Expand-Archive -Path $zip -DestinationPath $tools -Force

# 2) 環境変数へ追加
$mvn = "$tools\apache-maven-3.9.9"
[Environment]::SetEnvironmentVariable("MAVEN_HOME", $mvn, "User")
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($userPath -notlike "*$mvn\bin*") {
    [Environment]::SetEnvironmentVariable("Path", ($userPath.TrimEnd(';') + ";$mvn\bin").TrimStart(';'), "User")
}
```

### 確認

ターミナルを開き直してから:

```powershell
mvn -v
```

```
Apache Maven 3.9.9
Maven home: C:\Users\<ユーザー名>\tools\apache-maven-3.9.9
Java version: 21.0.12.1, vendor: Eclipse Adoptium, ...
```

`Java version` が 21 になっていることもあわせて確認してください。

---

## STEP 4. ソースコードの取得

```powershell
cd $env:USERPROFILE\Documents
git clone https://github.com/saiteki2609et/SpringMyBatis.git
cd SpringMyBatis
```

すでにローカルにある場合は最新化します。

```powershell
cd <プロジェクトのパス>
git pull
```

### 確認

```powershell
Get-ChildItem
```

`pom.xml` / `src` / `docs` / `mvnw.cmd` が存在すれば成功です。

---

## STEP 5. ビルド

プロジェクトのフォルダで実行します。

```powershell
mvn clean package
```

ルート A(Wrapper)の場合:

```powershell
.\mvnw.cmd clean package
```

- **初回は依存ライブラリのダウンロードで数分かかります**(2回目以降は数十秒)。
- ダウンロードされたライブラリは `%USERPROFILE%\.m2\repository` にキャッシュされます。

### 確認

```
[INFO] BUILD SUCCESS
```

と表示され、`target\scm-study-0.0.1-SNAPSHOT.jar` が生成されていれば成功です。

```powershell
Get-ChildItem target\*.jar
```

---

## STEP 6. テスト実行

環境が正しく構築できたかを、テストで確認します。

```powershell
mvn test
```

### 確認

```
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

36 件すべて成功すれば、DB(H2)・MyBatis・Spring がすべて正しく動作しています。

> テストは**インメモリDB**で実行されるため、開発用のデータ(`data` フォルダ)には影響しません。

---

## STEP 7. 起動と動作確認

### 7-1. 起動

開発中はこちらが手軽です(ソースを直接実行。停止は `Ctrl + C`)。

```powershell
mvn spring-boot:run
```

ビルド済みの jar から起動する場合:

```powershell
java -jar target\scm-study-0.0.1-SNAPSHOT.jar
```

以下が表示されれば起動完了です。

```
Tomcat started on port 8080 (http) with context path '/'
Started ScmStudyApplication in 3.305 seconds
```

### 7-2. 動作確認

| # | 確認内容 | 手順 | 期待結果 |
|---|---|---|---|
| 1 | 画面が表示される | ブラウザで <http://localhost:8080> | ダッシュボードが表示され、安全在庫割れの一覧が出る |
| 2 | 初期データが入っている | 「商品マスタ」を開く | 商品が 10 件表示される |
| 3 | Ajax が動く | 商品マスタでキーワード「ルーター」を検索 | 画面遷移せずに一覧が絞り込まれる |
| 4 | SQL ログが出る | 起動中のターミナルを見る | `==> Preparing:` `==> Parameters:` `<== Total:` が出力される |
| 5 | DB に接続できる | <http://localhost:8080/h2-console> を開き Connect | 接続情報は入力済み。テーブル一覧が表示される |
| 6 | 更新処理が動く | 出荷指示を新規登録し「在庫引当」 | ステータスが「引当済」になり、在庫が減る |

### 7-3. ポート 8080 が使用中の場合

```powershell
# 使用中のプロセスを確認
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Get-Process -Id <上で表示されたPID>
```

停止できない場合は、別のポートで起動します。

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

### 7-4. 停止

| 起動方法 | 停止方法 |
|---|---|
| ターミナルで起動 | `Ctrl + C` |
| バックグラウンド起動 | `Get-Process java \| Stop-Process` |

---

## STEP 8. VS Code のセットアップ(任意)

### 8-1. 拡張機能のインストール

```powershell
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
```

| 拡張機能 | 内容 |
|---|---|
| Extension Pack for Java | Java 言語サポート、デバッガ、Maven、テストランナー、IntelliSense |
| Spring Boot Extension Pack | Spring Boot の起動・プロパティ補完・Bean 一覧 |

インストール後、VS Code を再起動します。

### 8-2. プロジェクトを開く

```powershell
code C:\Users\<ユーザー名>\Documents\develope_app\SpringMyBatis
```

初回はワークスペースの解析(Java projects import)が走ります。完了まで1〜2分待ちます。

### 8-3. 起動・デバッグ

| 操作 | 方法 |
|---|---|
| 実行 | `ScmStudyApplication.java` を開き、`main` メソッド上の **Run** をクリック |
| デバッグ | 同じく **Debug** をクリック。ブレークポイントで止まる |
| テスト実行 | テストクラスの左側の再生ボタン、またはテストエクスプローラー |

### 8-4. 文字化け対策

ソース・テンプレート・SQL はすべて UTF-8 です。VS Code の設定(`settings.json`)に以下を入れておくと確実です。

```json
{
  "files.encoding": "utf8",
  "files.autoGuessEncoding": false
}
```

> 既存ファイルを開いて日本語が化ける場合は、右下のエンコーディング表示から
> 「Reopen with Encoding → UTF-8」を選びます。

---

## 構築完了チェックリスト

| | 確認項目 | 確認コマンド / 方法 |
|---|---|---|
| ☐ | JDK 21 が使える | `java -version` が 21.x |
| ☐ | JAVA_HOME が設定されている | `echo $env:JAVA_HOME` |
| ☐ | Maven が使える | `mvn -v`(または `.\mvnw.cmd -v`) |
| ☐ | ソースが取得できている | `Get-ChildItem` で `pom.xml` がある |
| ☐ | ビルドが通る | `mvn clean package` で BUILD SUCCESS |
| ☐ | テストが全件通る | `mvn test` で Tests run: 36, Failures: 0, Errors: 0 |
| ☐ | アプリが起動する | <http://localhost:8080> が表示される |
| ☐ | DB に接続できる | H2 コンソールでテーブルが見える |
| ☐ | SQL ログが出る | 起動ターミナルに `==> Preparing:` が出る |

すべて ☐ が埋まれば構築完了です。
学習の進め方は [README](../README.md) と [MyBatis 早見表](http://localhost:8080/learn) を参照してください。

---

## よくあるつまずき

| 症状 | 原因 | 対処 |
|---|---|---|
| `java` / `mvn` が「認識されていません」 | 環境変数の設定前に開いたターミナルを使っている | ターミナルを閉じて開き直す。VS Code も再起動 |
| `winget install Apache.Maven` が「パッケージが見つかりません」 | winget に Apache Maven は登録されていない | STEP 3 のルート A(Wrapper)かルート B(zip 展開)を使う |
| JDK のインストールが途中で止まる | UAC ダイアログが応答待ちになっている | 画面のダイアログで「はい」を押す |
| `mvnw.cmd` が `'powershell' is not recognized` | PATH から PowerShell が外れている | `C:\Windows\System32\WindowsPowerShell\v1.0` を PATH に含める、またはルート B |
| ビルドが `Could not transfer artifact` で失敗 | プロキシ環境でリポジトリに到達できない | 下の「プロキシ環境の場合」を参照 |
| 起動時に `Port 8080 was already in use` | 他プロセスが 8080 を使用中 | STEP 7-3 を参照 |
| 画面の日本語が化ける | ファイルが UTF-8 以外で保存された | STEP 8-4 を参照 |
| `Invalid bound statement (not found)` | MyBatis の設定・記述ミス | [06-mybatis-spec.md](06-mybatis-spec.md) の「よくあるエラー」を参照 |
| テストだけ失敗する | 中途半端なビルド成果物が残っている | `mvn clean test` を実行 |

### プロキシ環境の場合

`%USERPROFILE%\.m2\settings.xml` を作成し、以下を記述します。

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <proxies>
    <proxy>
      <id>corporate-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy.example.co.jp</host>
      <port>8080</port>
      <username>ユーザー名</username>
      <password>パスワード</password>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

---

## クリーンアップ・再構築

| 目的 | 手順 |
|---|---|
| ビルド成果物を消す | `mvn clean`(`target` フォルダが削除される) |
| DB を初期状態に戻す | アプリ停止後に `Remove-Item data -Recurse -Force`。次回起動時に初期データが再生成される |
| 依存ライブラリを再取得する | `Remove-Item $env:USERPROFILE\.m2\repository -Recurse -Force` の後、再ビルド(数分かかる) |
| 環境を完全に削除する | `winget uninstall EclipseAdoptium.Temurin.21.JDK`、`$env:USERPROFILE\tools\apache-maven-3.9.9` を削除、環境変数 `JAVA_HOME` / `MAVEN_HOME` / `Path` の追記分を削除 |

## 参考: 検証済みの構成

本手順書は以下の構成で動作確認しています。

| 項目 | バージョン |
|---|---|
| OS | Windows 11 Pro 10.0.26200 |
| JDK | Eclipse Temurin 21.0.12.1+1 (LTS) |
| Maven | 3.9.9(本体・Wrapper とも) |
| Git | 2.51.0.windows.1 |
| VS Code | 1.137.0 |
| Spring Boot | 3.3.5 |
| MyBatis | mybatis-spring-boot-starter 3.0.3 |
| H2 | 2.2.224 |
