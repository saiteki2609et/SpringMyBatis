package com.example.scm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.scm.domain.Stock;
import com.example.scm.exception.BusinessException;
import com.example.scm.mapper.StockMapper;

/** 在庫のサービス。 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockMapper stockMapper;

    public StockService(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    public List<Stock> findByWarehouse(Integer warehouseId) {
        return stockMapper.findByWarehouse(warehouseId);
    }

    public List<Stock> findBelowSafetyStock() {
        return stockMapper.findBelowSafetyStock();
    }

    /** 入庫。在庫レコードが無ければ作る。 */
    @Transactional
    public void receive(Integer itemId, Integer warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("入庫数は1以上で指定してください");
        }
        int updated = stockMapper.increase(itemId, warehouseId, quantity);
        if (updated == 0) {
            Stock stock = new Stock();
            stock.setItemId(itemId);
            stock.setWarehouseId(warehouseId);
            stock.setQuantity(quantity);
            stockMapper.insert(stock);
        }
    }
}
