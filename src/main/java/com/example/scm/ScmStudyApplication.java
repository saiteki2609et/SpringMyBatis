package com.example.scm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 物流管理(SCM)学習用アプリのエントリポイント。
 *
 * MyBatis の Mapper インタフェースは {@code @MapperScan} を書かなくても
 * mybatis-spring-boot-starter がこのクラスのパッケージ配下から
 * {@code @Mapper} 付きインタフェースを探して DI 登録してくれる。
 */
@SpringBootApplication
public class ScmStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScmStudyApplication.class, args);
    }
}
