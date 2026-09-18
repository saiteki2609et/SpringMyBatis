package com.example.scm.web.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.scm.domain.Stock;
import com.example.scm.service.StockService;

/** 在庫 API。一覧の絞り込みと入庫(数量加算)を jQuery から呼ぶ。 */
@RestController
@RequestMapping("/api/stocks")
public class StockApiController {

    private final StockService stockService;

    public StockApiController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<Stock> list(@RequestParam(required = false) Integer warehouseId) {
        return stockService.findByWarehouse(warehouseId);
    }

    /** 入庫。JSON ボディ {"itemId":1,"warehouseId":1,"quantity":10} を受け取る。 */
    @PostMapping("/receive")
    public List<Stock> receive(@RequestBody ReceiveRequest request) {
        stockService.receive(request.itemId(), request.warehouseId(), request.quantity());
        return stockService.findByWarehouse(request.warehouseId());
    }

    /** リクエストボディの入れ物には record が使える(Java 16 以降)。 */
    public record ReceiveRequest(Integer itemId, Integer warehouseId, int quantity) {
    }
}
