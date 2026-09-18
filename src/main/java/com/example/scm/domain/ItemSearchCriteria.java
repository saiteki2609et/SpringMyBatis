package com.example.scm.domain;

import java.math.BigDecimal;

/**
 * 商品検索条件。MyBatis の動的SQL(&lt;if&gt; / &lt;where&gt;)の入力になる。
 * 値が null のプロパティは検索条件から外れる、という作りにしてある。
 */
public class ItemSearchCriteria {

    private String keyword;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    /** "name" | "price" | "code" のいずれか。想定外の値は SQL に渡さない(SQLインジェクション対策)。 */
    private String sort;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = emptyToNull(keyword); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = emptyToNull(category); }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = emptyToNull(sort); }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
