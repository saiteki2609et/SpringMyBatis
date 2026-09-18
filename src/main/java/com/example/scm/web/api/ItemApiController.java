package com.example.scm.web.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;
import com.example.scm.service.ItemService;

/**
 * 商品検索 API。jQuery の $.getJSON から呼ばれる。
 * 画面を再読み込みせずに MyBatis の動的SQLの結果を確認できる。
 */
@RestController
@RequestMapping("/api/items")
public class ItemApiController {

    private final ItemService itemService;

    public ItemApiController(ItemService itemService) {
        this.itemService = itemService;
    }

    /** クエリパラメータ(keyword, category, minPrice, maxPrice, sort)がそのまま検索条件になる。 */
    @GetMapping
    public List<Item> search(@ModelAttribute ItemSearchCriteria criteria) {
        return itemService.search(criteria);
    }
}
