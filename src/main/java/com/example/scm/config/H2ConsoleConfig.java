package com.example.scm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * H2 コンソールのログイン画面に、このアプリのDB接続情報を既定値として表示させる設定。
 *
 * <p>H2 コンソールは起動時に {@code .h2.server.properties} という設定ファイルを読み、
 * そこに書かれた接続先を「Saved Settings」の選択肢＋入力欄の初期値として表示する。
 * 置き場所は WebServlet の初期化パラメータ {@code properties}(H2 の {@code -properties} 引数)で指定でき、
 * 未指定だとユーザーのホームフォルダを見にいくため、既定値が {@code jdbc:h2:~/test} のままになってしまう。
 *
 * <p>そこでこのクラスでは
 * <ol>
 *   <li>{@code spring.datasource.url} と同じ接続情報を書いた設定ファイルをプロジェクト内に生成し</li>
 *   <li>Spring Boot が自動登録する H2 コンソール(Bean 名 {@code h2Console})に、その置き場所を教える</li>
 * </ol>
 * ということをしている。application.yml の接続先を変えれば、コンソールの既定値も自動で追従する。
 *
 * <p>ポイントは {@link BeanPostProcessor} を使っていること。
 * 組込み Tomcat はコンテキストのリフレッシュ途中(他の Bean の生成より前)に起動し、
 * その時点でサーブレットの登録情報が読み取られてしまう。
 * 普通の {@code @Configuration} のコンストラクタで初期化パラメータを足しても間に合わないため、
 * 「h2Console という Bean が作られた瞬間」に割り込める BeanPostProcessor で設定している。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
public class H2ConsoleConfig {

    private static final Logger log = LoggerFactory.getLogger(H2ConsoleConfig.class);

    /** Spring Boot が登録する H2 コンソールの Bean 名。 */
    private static final String H2_CONSOLE_BEAN_NAME = "h2Console";

    /** 設定ファイルの置き場所(アプリを起動したフォルダからの相対)。DBファイルと同じ data フォルダに置く。 */
    private static final Path SETTINGS_DIR = Path.of("data");

    /** ファイル名は H2 側で固定。 */
    private static final String SETTINGS_FILE_NAME = ".h2.server.properties";

    /** ログイン画面の「Saved Settings」に表示される名前。 */
    private static final String SETTING_NAME = "SCM学習用DB (scm-study)";

    /**
     * BeanPostProcessor を定義する @Bean メソッドは static にする。
     * (static でないと、この設定クラス自体が他の Bean より早く生成されてしまい警告が出る)
     */
    @Bean
    static BeanPostProcessor h2ConsoleDefaultConnectionCustomizer(Environment environment) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (H2_CONSOLE_BEAN_NAME.equals(beanName) && bean instanceof ServletRegistrationBean<?> registration) {
                    configure(registration, environment);
                }
                return bean;
            }
        };
    }

    private static void configure(ServletRegistrationBean<?> registration, Environment environment) {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username", "sa");
        if (url == null) {
            return;
        }
        try {
            Path settingsDir = writeSettingsFile(url, username);
            // この初期化パラメータが H2 の -properties 引数として渡される
            registration.addInitParameter("properties", settingsDir.toString());
            log.info("H2コンソールの既定接続を設定しました: {}", url);
        } catch (IOException e) {
            // 既定値が出ないだけでアプリは動くので、起動は止めない
            log.warn("H2コンソールの既定接続の設定に失敗しました。URLは手入力してください。", e);
        }
    }

    /**
     * {@code .h2.server.properties} を書き出す。
     * 1行の書式は {@code 連番=表示名|ドライバ|JDBC URL|ユーザー名}。
     * コンソール画面で「Save」した他の設定を消さないよう、既存ファイルを読んでから 0 番だけ差し替える。
     */
    private static Path writeSettingsFile(String url, String username) throws IOException {
        Path dir = SETTINGS_DIR.toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path file = dir.resolve(SETTINGS_FILE_NAME);

        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            }
        }
        properties.setProperty("0", String.join("|", SETTING_NAME, "org.h2.Driver", url, username));

        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "Spring Boot が起動時に生成(H2コンソールの既定接続先)");
        }
        return dir;
    }
}
