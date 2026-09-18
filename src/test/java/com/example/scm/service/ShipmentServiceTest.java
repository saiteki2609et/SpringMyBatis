package com.example.scm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.scm.domain.Shipment;
import com.example.scm.domain.ShipmentDetail;
import com.example.scm.domain.ShipmentStatus;
import com.example.scm.domain.Stock;
import com.example.scm.exception.BusinessException;
import com.example.scm.mapper.StockMapper;

/**
 * 【サービスの結合テスト】
 *
 * <p>あえてテストクラスに {@code @Transactional} を付けていない。
 * テストをトランザクションで包んでしまうと「業務処理がロールバックされたかどうか」を
 * 確認できないため(サービス内の更新がテストのトランザクションから見えてしまう)。
 * 実際にコミットさせ、DB の値で検証している。
 *
 * <p>{@code @AutoConfigureTestDatabase} で接続先をインメモリDBに差し替えているので、
 * 開発用の data/scmdb.mv.db は汚れない。
 */
@SpringBootTest
@AutoConfigureTestDatabase
class ShipmentServiceTest {

    private static final int WAREHOUSE_TOKYO = 1;
    private static final int ITEM_CABLE = 4;     // ITM-2001 LANケーブル: 東京に 850 個
    private static final int ITEM_MOBILE = 7;    // ITM-3002 モバイルルーター: 東京に 8 個

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private StockMapper stockMapper;

    private int quantityOf(int itemId) {
        Stock stock = stockMapper.findByItemAndWarehouse(itemId, WAREHOUSE_TOKYO);
        return stock == null ? 0 : stock.getQuantity();
    }

    private Shipment newShipment(List<ShipmentDetail> details) {
        Shipment shipment = new Shipment();
        shipment.setCustomerName("テスト通信株式会社");
        shipment.setWarehouseId(WAREHOUSE_TOKYO);
        shipment.setShipDate(LocalDate.of(2026, 10, 1));
        shipment.setDetails(new ArrayList<>(details));
        return shipment;
    }

    private ShipmentDetail detail(int itemId, int quantity) {
        ShipmentDetail d = new ShipmentDetail();
        d.setItemId(itemId);
        d.setQuantity(quantity);
        return d;
    }

    @Test
    @DisplayName("登録すると出荷番号が採番され、明細ごと保存される(在庫はまだ動かない)")
    void createSavesHeaderAndDetails() {
        int before = quantityOf(ITEM_CABLE);

        Shipment created = shipmentService.create(newShipment(List.of(detail(ITEM_CABLE, 5))));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getShipmentNo()).startsWith("SH-20261001-");
        assertThat(created.getStatus()).isEqualTo(ShipmentStatus.DRAFT);

        Shipment saved = shipmentService.findById(created.getId());
        assertThat(saved.getDetails()).hasSize(1);
        assertThat(saved.getDetails().get(0).getItem().getItemCode()).isEqualTo("ITM-2001");
        assertThat(quantityOf(ITEM_CABLE)).isEqualTo(before);   // 登録時点では在庫は減らない
    }

    @Test
    @DisplayName("同じ商品を複数行で入力すると1行にまとめられる")
    void createMergesSameItems() {
        Shipment created = shipmentService.create(
                newShipment(List.of(detail(ITEM_CABLE, 3), detail(ITEM_CABLE, 4))));

        Shipment saved = shipmentService.findById(created.getId());
        assertThat(saved.getDetails()).hasSize(1);
        assertThat(saved.getDetails().get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("引当すると在庫が減り、ステータスが引当済になる")
    void allocateDecreasesStock() {
        int before = quantityOf(ITEM_CABLE);
        Shipment created = shipmentService.create(newShipment(List.of(detail(ITEM_CABLE, 10))));

        Shipment allocated = shipmentService.allocate(created.getId());

        assertThat(allocated.getStatus()).isEqualTo(ShipmentStatus.ALLOCATED);
        assertThat(quantityOf(ITEM_CABLE)).isEqualTo(before - 10);
    }

    @Test
    @DisplayName("在庫不足なら例外になり、先に処理された明細の在庫も元に戻る(ロールバック)")
    void allocateRollsBackWhenStockIsShort() {
        int cableBefore = quantityOf(ITEM_CABLE);
        int mobileBefore = quantityOf(ITEM_MOBILE);

        // 1行目(ケーブル)は引当可能、2行目(モバイルルーター)は在庫を超える数量
        Shipment created = shipmentService.create(newShipment(List.of(
                detail(ITEM_CABLE, 5),
                detail(ITEM_MOBILE, mobileBefore + 1))));

        assertThatThrownBy(() -> shipmentService.allocate(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("在庫が不足");

        // 1行目の減算もロールバックされ、どちらの在庫も変わっていない
        assertThat(quantityOf(ITEM_CABLE)).isEqualTo(cableBefore);
        assertThat(quantityOf(ITEM_MOBILE)).isEqualTo(mobileBefore);
        assertThat(shipmentService.findById(created.getId()).getStatus()).isEqualTo(ShipmentStatus.DRAFT);
    }

    @Test
    @DisplayName("引当済を取消すると在庫が戻る")
    void cancelRestoresStock() {
        int before = quantityOf(ITEM_CABLE);
        Shipment created = shipmentService.create(newShipment(List.of(detail(ITEM_CABLE, 20))));
        shipmentService.allocate(created.getId());
        assertThat(quantityOf(ITEM_CABLE)).isEqualTo(before - 20);

        Shipment cancelled = shipmentService.cancel(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(quantityOf(ITEM_CABLE)).isEqualTo(before);
    }

    @Test
    @DisplayName("登録済のまま出荷しようとするとエラーになる(ステータス遷移のチェック)")
    void cannotShipBeforeAllocation() {
        Shipment created = shipmentService.create(newShipment(List.of(detail(ITEM_CABLE, 1))));

        assertThatThrownBy(() -> shipmentService.ship(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("引当済");
    }

    @Test
    @DisplayName("引当 → 出荷 の順なら出荷済にできる")
    void allocateThenShip() {
        Shipment created = shipmentService.create(newShipment(List.of(detail(ITEM_CABLE, 2))));
        shipmentService.allocate(created.getId());

        Shipment shipped = shipmentService.ship(created.getId());

        assertThat(shipped.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThatThrownBy(() -> shipmentService.cancel(created.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("明細が空なら登録できない")
    void createRejectsEmptyDetails() {
        assertThatThrownBy(() -> shipmentService.create(newShipment(List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("明細");
    }
}
