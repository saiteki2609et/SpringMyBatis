package com.example.scm.web.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scm.domain.Shipment;
import com.example.scm.domain.StatusCount;
import com.example.scm.exception.BusinessException;
import com.example.scm.service.ShipmentService;

/** 出荷指示 API。ステータス操作を jQuery(Ajax)から実行する。 */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentApiController {

    private final ShipmentService shipmentService;

    public ShipmentApiController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /** ヘッダ＋明細をまとめた JSON が返る(collection マッピングの結果を目で確認できる)。 */
    @GetMapping("/{id}")
    public Shipment findById(@PathVariable Integer id) {
        return shipmentService.findById(id);
    }

    @GetMapping("/status-counts")
    public List<StatusCount> statusCounts() {
        return shipmentService.countByStatus();
    }

    @PostMapping("/{id}/{action}")
    public Shipment changeStatus(@PathVariable Integer id, @PathVariable String action) {
        return switch (action) {
            case "allocate" -> shipmentService.allocate(id);
            case "ship" -> shipmentService.ship(id);
            case "cancel" -> shipmentService.cancel(id);
            default -> throw new BusinessException("不正な操作です: " + action);
        };
    }
}
