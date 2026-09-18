package com.example.scm.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

import com.example.scm.domain.Stock;

/** XML マッパー(association / JOIN / 更新件数判定)のテスト。 */
@MybatisTest
@AutoConfigureTestDatabase
@Sql(scripts = {"/schema.sql", "/data.sql"})
class StockMapperTest {

    private static final int ITEM_ROUTER = 1;     // ITM-1001
    private static final int WAREHOUSE_TOKYO = 1; // WH01

    @Autowired
    private StockMapper stockMapper;

    @Test
    @DisplayName("association: JOIN した商品・倉庫がネストしたオブジェクトに入る")
    void findAllMapsAssociations() {
        List<Stock> stocks = stockMapper.findAll();

        assertThat(stocks).isNotEmpty();
        Stock first = stocks.get(0);
        // 入れ子の Item / Warehouse は全項目がマッピングされること(一部だけ null にならない)
        assertThat(first.getItem()).isNotNull();
        assertThat(first.getItem().getItemCode()).isNotBlank();
        assertThat(first.getItem().getItemName()).isNotBlank();
        assertThat(first.getItem().getCategory()).isNotBlank();
        assertThat(first.getItem().getUnitPrice()).isNotNull();
        assertThat(first.getItem().getSafetyStock()).isNotNull();
        assertThat(first.getItem().getCreatedAt()).isNotNull();
        assertThat(first.getWarehouse()).isNotNull();
        assertThat(first.getWarehouse().getCode()).isNotBlank();
        assertThat(first.getWarehouse().getName()).isNotBlank();
        assertThat(first.getWarehouse().getAddress()).isNotBlank();
    }

    @Test
    @DisplayName("動的SQL: 倉庫IDが null なら全件、指定すれば絞り込まれる")
    void findByWarehouse() {
        List<Stock> all = stockMapper.findByWarehouse(null);
        List<Stock> tokyo = stockMapper.findByWarehouse(WAREHOUSE_TOKYO);

        assertThat(tokyo).isNotEmpty();
        assertThat(tokyo.size()).isLessThan(all.size());
        assertThat(tokyo).allSatisfy(s -> assertThat(s.getWarehouseId()).isEqualTo(WAREHOUSE_TOKYO));
    }

    @Test
    @DisplayName("在庫を減らせたときは更新件数1が返る")
    void decreaseSucceeds() {
        Stock before = stockMapper.findByItemAndWarehouse(ITEM_ROUTER, WAREHOUSE_TOKYO);

        int updated = stockMapper.decrease(ITEM_ROUTER, WAREHOUSE_TOKYO, 10);

        assertThat(updated).isEqualTo(1);
        Stock after = stockMapper.findByItemAndWarehouse(ITEM_ROUTER, WAREHOUSE_TOKYO);
        assertThat(after.getQuantity()).isEqualTo(before.getQuantity() - 10);
    }

    @Test
    @DisplayName("在庫不足のときは更新件数0が返り、数量は変化しない")
    void decreaseReturnsZeroWhenNotEnough() {
        Stock before = stockMapper.findByItemAndWarehouse(ITEM_ROUTER, WAREHOUSE_TOKYO);

        int updated = stockMapper.decrease(ITEM_ROUTER, WAREHOUSE_TOKYO, before.getQuantity() + 1);

        assertThat(updated).isZero();
        Stock after = stockMapper.findByItemAndWarehouse(ITEM_ROUTER, WAREHOUSE_TOKYO);
        assertThat(after.getQuantity()).isEqualTo(before.getQuantity());
    }

    @Test
    @DisplayName("存在しない在庫行を増やそうとすると更新件数0(INSERTへのフォールバック判定に使う)")
    void increaseReturnsZeroWhenRowMissing() {
        int updated = stockMapper.increase(ITEM_ROUTER, 999, 10);

        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("安全在庫割れの在庫だけを抽出できる")
    void findBelowSafetyStock() {
        List<Stock> shortages = stockMapper.findBelowSafetyStock();

        assertThat(shortages).isNotEmpty();
        assertThat(shortages).allSatisfy(s ->
                assertThat(s.getQuantity()).isLessThan(s.getItem().getSafetyStock()));
    }
}
