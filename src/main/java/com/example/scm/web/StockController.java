package com.example.scm.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.scm.mapper.WarehouseMapper;
import com.example.scm.service.ItemService;
import com.example.scm.service.StockService;

/** 在庫照会画面。絞り込み・入庫は jQuery(Ajax) から /api/stocks を呼ぶ。 */
@Controller
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;
    private final WarehouseMapper warehouseMapper;
    private final ItemService itemService;

    public StockController(StockService stockService, WarehouseMapper warehouseMapper, ItemService itemService) {
        this.stockService = stockService;
        this.warehouseMapper = warehouseMapper;
        this.itemService = itemService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("stocks", stockService.findByWarehouse(null));
        model.addAttribute("warehouses", warehouseMapper.findAll());
        model.addAttribute("items", itemService.findAll());
        return "stocks";
    }
}
