package com.example.scm.domain;

import java.time.LocalDateTime;

/**
 * 在庫。商品×倉庫で1レコード。
 *
 * item / warehouse は別テーブルの情報を1件ぶらさげたもので、
 * MyBatis の XML では {@code <association>} でマッピングしている
 * (StockMapper.xml を参照)。
 */
public class Stock {

    private Integer id;
    private Integer itemId;
    private Integer warehouseId;
    private Integer quantity;
    private LocalDateTime updatedAt;

    private Item item;
    private Warehouse warehouse;

    /** 安全在庫を割っているか(画面の警告表示用)。 */
    public boolean isBelowSafetyStock() {
        return item != null && item.getSafetyStock() != null
                && quantity != null && quantity < item.getSafetyStock();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
}
