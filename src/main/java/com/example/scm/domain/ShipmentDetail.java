package com.example.scm.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 出荷指示明細。1件の出荷指示に複数ぶら下がる(1:N の N 側)。 */
public class ShipmentDetail {

    private Integer id;
    private Integer shipmentId;

    @NotNull(message = "商品を選択してください")
    private Integer itemId;

    @NotNull(message = "数量は必須です")
    @Min(value = 1, message = "数量は1以上で入力してください")
    private Integer quantity;

    /** 明細に対する商品情報(XML の association でマッピング)。 */
    private Item item;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getShipmentId() { return shipmentId; }
    public void setShipmentId(Integer shipmentId) { this.shipmentId = shipmentId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
