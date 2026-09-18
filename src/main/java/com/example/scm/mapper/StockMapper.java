package com.example.scm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.scm.domain.Stock;

/**
 * 在庫 Mapper ―― 【XML方式】の例。SQL は resources/mapper/StockMapper.xml。
 *
 * <p>XML のファイル名・namespace とこのインタフェースの FQCN を一致させるのが基本ルール
 * (namespace="com.example.scm.mapper.StockMapper")。
 * mybatis.mapper-locations で XML の置き場所を指定している(application.yml)。
 *
 * <p>学習ポイント:
 * <ul>
 *   <li>{@code <resultMap>} + {@code <association>} で JOIN 結果を入れ子オブジェクトに詰める</li>
 *   <li>{@code <sql>} / {@code <include>} で SQL の共通部分を使い回す</li>
 *   <li>UPDATE の戻り値(更新件数)を使った在庫引当のチェック</li>
 * </ul>
 */
@Mapper
public interface StockMapper {

    /** 在庫一覧(商品・倉庫を JOIN して1回のSQLで取得)。 */
    List<Stock> findAll();

    /** 倉庫で絞り込み。warehouseId が null なら全件(動的SQL)。 */
    List<Stock> findByWarehouse(@Param("warehouseId") Integer warehouseId);

    Stock findByItemAndWarehouse(@Param("itemId") Integer itemId,
                                @Param("warehouseId") Integer warehouseId);

    /** 安全在庫を下回っている在庫。 */
    List<Stock> findBelowSafetyStock();

    /**
     * 在庫を減らす(引当)。
     * WHERE に {@code quantity >= #{quantity}} を入れているので、
     * 在庫不足のときは更新件数 0 が返る = アプリ側で在庫不足と判定できる。
     * 「SELECT で残数を確認してから UPDATE」より競合に強い書き方。
     *
     * @return 更新件数(1 なら成功、0 なら在庫不足 or 対象なし)
     */
    int decrease(@Param("itemId") Integer itemId,
                 @Param("warehouseId") Integer warehouseId,
                 @Param("quantity") int quantity);

    /** 在庫を増やす(入庫・取消時の戻し)。 */
    int increase(@Param("itemId") Integer itemId,
                 @Param("warehouseId") Integer warehouseId,
                 @Param("quantity") int quantity);

    /** 在庫レコードが無い商品×倉庫の組み合わせ用。 */
    int insert(Stock stock);
}
