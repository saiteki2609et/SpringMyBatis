package com.example.scm.web;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.scm.domain.Shipment;
import com.example.scm.domain.ShipmentSearchCriteria;
import com.example.scm.domain.ShipmentStatus;
import com.example.scm.exception.BusinessException;
import com.example.scm.mapper.WarehouseMapper;
import com.example.scm.service.ItemService;
import com.example.scm.service.ShipmentService;

import jakarta.validation.Valid;

/** 出荷指示画面。 */
@Controller
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final WarehouseMapper warehouseMapper;
    private final ItemService itemService;

    public ShipmentController(ShipmentService shipmentService, WarehouseMapper warehouseMapper,
                              ItemService itemService) {
        this.shipmentService = shipmentService;
        this.warehouseMapper = warehouseMapper;
        this.itemService = itemService;
    }

    /** 一覧＋検索。検索条件は @ModelAttribute で受け取り、そのまま動的SQLへ渡す。 */
    @GetMapping
    public String list(@ModelAttribute("criteria") ShipmentSearchCriteria criteria, Model model) {
        model.addAttribute("shipments", shipmentService.search(criteria));
        model.addAttribute("warehouses", warehouseMapper.findAll());
        model.addAttribute("allStatuses", ShipmentStatus.values());
        return "shipments";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Shipment shipment = new Shipment();
        shipment.setShipDate(LocalDate.now());
        model.addAttribute("shipment", shipment);
        prepareFormModel(model);
        return "shipment-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Shipment shipment, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            prepareFormModel(model);
            return "shipment-form";
        }
        try {
            shipmentService.create(shipment);
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            prepareFormModel(model);
            return "shipment-form";
        }
        redirect.addFlashAttribute("message", "出荷指示を登録しました: " + shipment.getShipmentNo());
        return "redirect:/shipments/" + shipment.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("shipment", shipmentService.findById(id));
        return "shipment-detail";
    }

    /** 引当・出荷・取消・削除。同じ処理は /api 経由でも呼べる(jQuery 用)。 */
    @PostMapping("/{id}/{action}")
    public String changeStatus(@PathVariable Integer id, @PathVariable String action,
                               RedirectAttributes redirect) {
        try {
            switch (action) {
                case "allocate" -> {
                    shipmentService.allocate(id);
                    redirect.addFlashAttribute("message", "在庫を引き当てました");
                }
                case "ship" -> {
                    shipmentService.ship(id);
                    redirect.addFlashAttribute("message", "出荷済にしました");
                }
                case "cancel" -> {
                    shipmentService.cancel(id);
                    redirect.addFlashAttribute("message", "取消しました");
                }
                case "delete" -> {
                    shipmentService.delete(id);
                    redirect.addFlashAttribute("message", "削除しました");
                    return "redirect:/shipments";
                }
                default -> redirect.addFlashAttribute("errorMessage", "不正な操作です: " + action);
            }
        } catch (BusinessException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shipments/" + id;
    }

    private void prepareFormModel(Model model) {
        model.addAttribute("warehouses", warehouseMapper.findAll());
        model.addAttribute("items", itemService.findAll());
    }
}
