package com.example.scm.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;
import com.example.scm.exception.BusinessException;
import com.example.scm.service.ItemService;

import jakarta.validation.Valid;

/** 商品マスタ画面。一覧の絞り込みは jQuery(Ajax) から /api/items を呼ぶ。 */
@Controller
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", itemService.search(new ItemSearchCriteria()));
        model.addAttribute("categories", itemService.findCategories());
        return "items";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("item", new Item());
        model.addAttribute("mode", "create");
        return "item-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("item", itemService.findById(id));
        model.addAttribute("mode", "edit");
        return "item-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Item item, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            return "item-form";
        }
        try {
            itemService.create(item);
        } catch (BusinessException e) {
            model.addAttribute("mode", "create");
            model.addAttribute("errorMessage", e.getMessage());
            return "item-form";
        }
        redirect.addFlashAttribute("message", "商品を登録しました: " + item.getItemCode());
        return "redirect:/items";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @Valid @ModelAttribute Item item,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        item.setId(id);
        if (binding.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "item-form";
        }
        try {
            itemService.update(item);
        } catch (BusinessException e) {
            model.addAttribute("mode", "edit");
            model.addAttribute("errorMessage", e.getMessage());
            return "item-form";
        }
        redirect.addFlashAttribute("message", "商品を更新しました: " + item.getItemCode());
        return "redirect:/items";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            itemService.delete(id);
            redirect.addFlashAttribute("message", "商品を削除しました");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/items";
    }
}
