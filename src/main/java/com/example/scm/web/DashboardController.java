package com.example.scm.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.scm.service.ShipmentService;
import com.example.scm.service.StockService;

/** トップ画面(ダッシュボード)。 */
@Controller
public class DashboardController {

    private final StockService stockService;
    private final ShipmentService shipmentService;

    public DashboardController(StockService stockService, ShipmentService shipmentService) {
        this.stockService = stockService;
        this.shipmentService = shipmentService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("shortages", stockService.findBelowSafetyStock());
        model.addAttribute("statusCounts", shipmentService.countByStatus());
        return "index";
    }

    /** MyBatis の書き方まとめページ(静的なガイド)。 */
    @GetMapping("/learn")
    public String learn() {
        return "learn";
    }
}
