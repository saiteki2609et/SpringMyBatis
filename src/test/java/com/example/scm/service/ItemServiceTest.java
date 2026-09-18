package com.example.scm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;
import com.example.scm.exception.BusinessException;

/**
 * 商品サービスのテスト。
 * ここでは ORDER BY のホワイトリスト(SQLインジェクション対策)の挙動も確認している。
 */
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional   // 各テストの更新はロールバックされる
class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Test
    @DisplayName("ソート未指定(null)でも既定の並び順で検索できる")
    void searchWithoutSort() {
        List<Item> items = itemService.search(new ItemSearchCriteria());

        assertThat(items).isNotEmpty();
        assertThat(items.get(0).getItemCode()).isEqualTo("ITM-1001");   // item_code 昇順
    }

    @Test
    @DisplayName("ホワイトリストに無いソート指定は既定値に倒される(SQLを組み立てさせない)")
    void searchWithUnknownSortFallsBack() {
        ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setSort("item_code; DROP TABLE item--");

        List<Item> items = itemService.search(criteria);

        assertThat(items).isNotEmpty();
        assertThat(items.get(0).getItemCode()).isEqualTo("ITM-1001");
    }

    @Test
    @DisplayName("単価の降順で並べ替えできる")
    void searchSortedByPrice() {
        ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setSort("price");

        List<Item> items = itemService.search(criteria);

        assertThat(items).isNotEmpty();
        assertThat(items.get(0).getUnitPrice())
                .isGreaterThanOrEqualTo(items.get(items.size() - 1).getUnitPrice());
    }

    @Test
    @DisplayName("商品コードが重複すると業務エラーになる")
    void createRejectsDuplicatedCode() {
        Item item = new Item();
        item.setItemCode("ITM-1001");
        item.setItemName("重複テスト");
        item.setCategory("テスト");
        item.setUnitPrice(new BigDecimal("100"));
        item.setSafetyStock(0);

        assertThatThrownBy(() -> itemService.create(item))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重複");
    }

    @Test
    @DisplayName("在庫から参照されている商品は削除できない(外部キー制約)")
    void deleteRejectsItemInUse() {
        Item inUse = itemService.findAll().get(0);

        assertThatThrownBy(() -> itemService.delete(inUse.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("使用中");
    }
}
