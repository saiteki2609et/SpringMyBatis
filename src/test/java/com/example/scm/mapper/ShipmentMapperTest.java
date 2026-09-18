package com.example.scm.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.example.scm.domain.Shipment;
import com.example.scm.domain.ShipmentDetail;
import com.example.scm.domain.ShipmentSearchCriteria;
import com.example.scm.domain.ShipmentStatus;
import com.example.scm.domain.StatusCount;

/** collection(1:N) / foreach / TypeHandler のテスト。 */
@MybatisTest
@AutoConfigureTestDatabase
@Sql(scripts = {"/schema.sql", "/data.sql"})
class ShipmentMapperTest {

    @Autowired
    private ShipmentMapper shipmentMapper;

    /** SQL の実行結果を生で確認したいときは JdbcTemplate が手軽(@MybatisTest でも使える)。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer firstShipmentId() {
        return jdbcTemplate.queryForObject("SELECT MIN(id) FROM shipment", Integer.class);
    }

    @Test
    @DisplayName("collection: ヘッダ1件に明細N件がぶら下がる")
    void findByIdLoadsDetails() {
        Shipment shipment = shipmentMapper.findById(firstShipmentId());

        assertThat(shipment).isNotNull();
        assertThat(shipment.getShipmentNo()).isNotBlank();
        assertThat(shipment.getWarehouse()).isNotNull();
        assertThat(shipment.getDetails()).isNotEmpty();
        // 明細の中の商品(入れ子の association)までマッピングされている
        assertThat(shipment.getDetails().get(0).getItem()).isNotNull();
        assertThat(shipment.getDetails().get(0).getItem().getItemName()).isNotBlank();
    }

    @Test
    @DisplayName("TypeHandler: DBのコード値('30')が enum に変換される")
    void typeHandlerConvertsCodeToEnum() {
        Shipment shipment = shipmentMapper.findById(firstShipmentId());
        String rawStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM shipment WHERE id = ?", String.class, shipment.getId());

        assertThat(rawStatus).isEqualTo(shipment.getStatus().getCode());
        assertThat(shipment.getStatus()).isInstanceOf(ShipmentStatus.class);
    }

    @Test
    @DisplayName("動的SQL: ステータス複数指定は IN 句になる(foreach)")
    void searchByStatuses() {
        ShipmentSearchCriteria criteria = new ShipmentSearchCriteria();
        criteria.setStatuses(List.of(ShipmentStatus.DRAFT, ShipmentStatus.ALLOCATED));

        List<Shipment> shipments = shipmentMapper.search(criteria);

        assertThat(shipments).isNotEmpty();
        assertThat(shipments).allSatisfy(s ->
                assertThat(s.getStatus()).isIn(ShipmentStatus.DRAFT, ShipmentStatus.ALLOCATED));
    }

    @Test
    @DisplayName("動的SQL: 条件なしなら全件返る")
    void searchWithoutCriteria() {
        List<Shipment> shipments = shipmentMapper.search(new ShipmentSearchCriteria());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shipment", Integer.class);
        assertThat(shipments).hasSize(count);
    }

    @Test
    @DisplayName("ヘッダINSERTでIDが採番され、明細はforeachで一括INSERTできる")
    void insertShipmentWithDetails() {
        Shipment shipment = new Shipment();
        shipment.setShipmentNo("SH-29991231-001");
        shipment.setCustomerName("テスト商事");
        shipment.setWarehouseId(1);
        shipment.setStatus(ShipmentStatus.DRAFT);
        shipment.setShipDate(LocalDate.of(2999, 12, 31));

        shipmentMapper.insertShipment(shipment);
        assertThat(shipment.getId()).isNotNull();

        ShipmentDetail d1 = new ShipmentDetail();
        d1.setItemId(1);
        d1.setQuantity(3);
        ShipmentDetail d2 = new ShipmentDetail();
        d2.setItemId(4);
        d2.setQuantity(7);

        int inserted = shipmentMapper.insertDetails(shipment.getId(), List.of(d1, d2));

        assertThat(inserted).isEqualTo(2);
        Shipment saved = shipmentMapper.findById(shipment.getId());
        assertThat(saved.getDetails()).hasSize(2);
        assertThat(saved.getTotalQuantity()).isEqualTo(10);
        // enum はコード値で保存されている
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM shipment WHERE id = ?",
                String.class, shipment.getId())).isEqualTo("10");
    }

    @Test
    @DisplayName("ステータス更新とステータス別件数の集計")
    void updateStatusAndCount() {
        Integer id = firstShipmentId();

        shipmentMapper.updateStatus(id, ShipmentStatus.CANCELLED);

        assertThat(shipmentMapper.findById(id).getStatus()).isEqualTo(ShipmentStatus.CANCELLED);

        List<StatusCount> counts = shipmentMapper.countByStatus();
        assertThat(counts).isNotEmpty();
        assertThat(counts).anySatisfy(c -> {
            assertThat(c.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
            assertThat(c.getCount()).isPositive();
        });
    }

    @Test
    @DisplayName("出荷番号の最大値をプレフィックス指定で取得できる(採番用)")
    void findMaxShipmentNo() {
        String max = shipmentMapper.findMaxShipmentNo("SH-20260901-");

        assertThat(max).isEqualTo("SH-20260901-001");
        assertThat(shipmentMapper.findMaxShipmentNo("SH-19000101-")).isNull();
    }
}
