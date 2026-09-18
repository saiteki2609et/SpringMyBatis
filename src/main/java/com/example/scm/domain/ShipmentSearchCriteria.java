package com.example.scm.domain;

import java.time.LocalDate;
import java.util.List;

/** 出荷指示の検索条件。&lt;foreach&gt; と &lt;choose&gt; の練習用にステータスを複数指定できるようにしている。 */
public class ShipmentSearchCriteria {

    private String customerName;
    private Integer warehouseId;
    private List<ShipmentStatus> statuses;
    private LocalDate shipDateFrom;
    private LocalDate shipDateTo;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) {
        this.customerName = (customerName == null || customerName.isBlank()) ? null : customerName.trim();
    }

    public Integer getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }

    public List<ShipmentStatus> getStatuses() { return statuses; }
    public void setStatuses(List<ShipmentStatus> statuses) {
        this.statuses = (statuses == null || statuses.isEmpty()) ? null : statuses;
    }

    public LocalDate getShipDateFrom() { return shipDateFrom; }
    public void setShipDateFrom(LocalDate shipDateFrom) { this.shipDateFrom = shipDateFrom; }

    public LocalDate getShipDateTo() { return shipDateTo; }
    public void setShipDateTo(LocalDate shipDateTo) { this.shipDateTo = shipDateTo; }
}
