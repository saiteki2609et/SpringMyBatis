package com.example.scm.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 出荷指示(ヘッダ)。
 *
 * details は 1:N の関連。MyBatis の XML では {@code <collection>} でまとめてマッピングする
 * (ShipmentMapper.xml の resultMap "shipmentWithDetails" を参照)。
 */
public class Shipment {

    private Integer id;

    /** 出荷番号。登録時にサービス層で採番する。 */
    private String shipmentNo;

    @NotBlank(message = "出荷先は必須です")
    private String customerName;

    @NotNull(message = "出荷元倉庫を選択してください")
    private Integer warehouseId;

    private ShipmentStatus status = ShipmentStatus.DRAFT;

    @NotNull(message = "出荷予定日は必須です")
    private LocalDate shipDate;

    private String remarks;
    private LocalDateTime createdAt;

    private Warehouse warehouse;

    @Valid
    @NotEmpty(message = "明細を1行以上入力してください")
    private List<ShipmentDetail> details = new ArrayList<>();

    /** 明細の合計数量(画面表示用)。 */
    public int getTotalQuantity() {
        return details.stream()
                .filter(d -> d.getQuantity() != null)
                .mapToInt(ShipmentDetail::getQuantity)
                .sum();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getShipmentNo() { return shipmentNo; }
    public void setShipmentNo(String shipmentNo) { this.shipmentNo = shipmentNo; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Integer getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public LocalDate getShipDate() { return shipDate; }
    public void setShipDate(LocalDate shipDate) { this.shipDate = shipDate; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public List<ShipmentDetail> getDetails() { return details; }
    public void setDetails(List<ShipmentDetail> details) { this.details = details; }
}
