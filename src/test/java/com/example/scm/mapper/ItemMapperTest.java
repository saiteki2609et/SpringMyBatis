package com.example.scm.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;

/**
 * 【Mapper 単体テスト】
 *
 * <p>{@code @MybatisTest} は DataSource と Mapper だけを起動する軽量なテスト
 * (Controller や Service は起動しない)。デフォルトで各テストは
 * トランザクション内で実行され、終了時にロールバックされるので後片付け不要。
 *
 * <p>{@code @AutoConfigureTestDatabase} で接続先をインメモリDBに差し替え、
 * {@code @Sql} でスキーマと初期データを流し込んでいる
 * (開発用の data/scmdb.mv.db を壊さないため)。
 */
@MybatisTest
@AutoConfigureTestDatabase
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ItemMapperTest {

    @Autowired
    private ItemMapper itemMapper;

    @Test
    @DisplayName("全件取得できる")
    void findAll() {
        List<Item> items = itemMapper.findAll();
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).getItemCode()).isNotBlank();
    }

    @Test
    @DisplayName("スネークケースのカラムがキャメルケースのプロパティにマッピングされる")
    void mapUnderscoreToCamelCase() {
        Item item = itemMapper.findByItemCode("ITM-1001");

        assertThat(item).isNotNull();
        assertThat(item.getItemName()).isEqualTo("光回線ルーター RX-100");   // item_name  -> itemName
        assertThat(item.getUnitPrice()).isEqualByComparingTo("18500.00");    // unit_price -> unitPrice
        assertThat(item.getSafetyStock()).isEqualTo(30);                     // safety_stock -> safetyStock
        assertThat(item.getCreatedAt()).isNotNull();                         // created_at -> createdAt
    }

    @Test
    @DisplayName("動的SQL: キーワードを指定すると LIKE 条件が追加される")
    void searchByKeyword() {
        ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setKeyword("ルーター");

        List<Item> items = itemMapper.search(criteria, "item_code");

        assertThat(items).isNotEmpty();
        assertThat(items).allSatisfy(i -> assertThat(i.getItemName()).contains("ルーター"));
    }

    @Test
    @DisplayName("動的SQL: 条件を何も指定しなければ WHERE 句ごと消えて全件返る")
    void searchWithoutCriteria() {
        List<Item> all = itemMapper.findAll();

        List<Item> items = itemMapper.search(new ItemSearchCriteria(), "item_code");

        assertThat(items).hasSameSizeAs(all);
    }

    @Test
    @DisplayName("動的SQL: カテゴリと価格帯を組み合わせるとAND条件になる")
    void searchByCategoryAndPrice() {
        ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setCategory("ネットワーク機器");
        criteria.setMinPrice(new BigDecimal("10000"));
        criteria.setMaxPrice(new BigDecimal("20000"));

        List<Item> items = itemMapper.search(criteria, "unit_price DESC");

        assertThat(items).isNotEmpty();
        assertThat(items).allSatisfy(i -> {
            assertThat(i.getCategory()).isEqualTo("ネットワーク機器");
            assertThat(i.getUnitPrice()).isBetween(new BigDecimal("10000"), new BigDecimal("20000"));
        });
    }

    @Test
    @DisplayName("INSERT すると採番されたIDが引数のオブジェクトに書き戻される(useGeneratedKeys)")
    void insertSetsGeneratedId() {
        Item item = new Item();
        item.setItemCode("ITM-9999");
        item.setItemName("テスト商品");
        item.setCategory("テスト");
        item.setUnitPrice(new BigDecimal("1000"));
        item.setSafetyStock(5);

        int inserted = itemMapper.insert(item);

        assertThat(inserted).isEqualTo(1);
        assertThat(item.getId()).isNotNull();
        assertThat(itemMapper.findById(item.getId()).getItemName()).isEqualTo("テスト商品");
    }

    @Test
    @DisplayName("UPDATE / DELETE は更新件数が返る")
    void updateAndDelete() {
        Item item = new Item();
        item.setItemCode("ITM-8888");
        item.setItemName("削除予定商品");
        item.setCategory("テスト");
        item.setUnitPrice(new BigDecimal("500"));
        item.setSafetyStock(1);
        itemMapper.insert(item);

        item.setItemName("更新後の名前");
        assertThat(itemMapper.update(item)).isEqualTo(1);
        assertThat(itemMapper.findById(item.getId()).getItemName()).isEqualTo("更新後の名前");

        assertThat(itemMapper.deleteById(item.getId())).isEqualTo(1);
        assertThat(itemMapper.findById(item.getId())).isNull();
    }
}
