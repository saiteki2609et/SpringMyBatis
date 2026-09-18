package com.example.scm.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 商品マスタ。
 *
 * DB のカラムは item_code / unit_price のようなスネークケースだが、
 * application.yml の {@code mybatis.configuration.map-underscore-to-camel-case: true}
 * によって itemCode / unitPrice へ自動マッピングされる。
 */
public class Item {

    private Integer id;

    @NotBlank(message = "商品コードは必須です")
    private String itemCode;

    @NotBlank(message = "商品名は必須です")
    private String itemName;

    @NotBlank(message = "カテゴリは必須です")
    private String category;

    @NotNull(message = "単価は必須です")
    @DecimalMin(value = "0", message = "単価は0以上で入力してください")
    private BigDecimal unitPrice;

    @NotNull(message = "安全在庫は必須です")
    @Min(value = 0, message = "安全在庫は0以上で入力してください")
    private Integer safetyStock;

    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Integer getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
