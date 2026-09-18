package com.example.scm.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;
import com.example.scm.service.ItemService;
import com.example.scm.web.api.ItemApiController;

/**
 * 【Controller 単体テスト】
 * {@code @WebMvcTest} は Web 層だけを起動する。DB には一切つながらないので、
 * Service は {@code @MockBean} で差し替えて「入出力のかたち」だけを検証する。
 */
@WebMvcTest(ItemApiController.class)
class ItemApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Test
    @DisplayName("検索APIが商品一覧のJSONを返す")
    void searchReturnsJson() throws Exception {
        Item item = new Item();
        item.setId(1);
        item.setItemCode("ITM-1001");
        item.setItemName("光回線ルーター RX-100");
        item.setCategory("ネットワーク機器");
        item.setUnitPrice(new BigDecimal("18500.00"));
        item.setSafetyStock(30);
        given(itemService.search(any(ItemSearchCriteria.class))).willReturn(List.of(item));

        mockMvc.perform(get("/api/items").param("keyword", "ルーター"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].itemCode").value("ITM-1001"))
                .andExpect(jsonPath("$[0].safetyStock").value(30));
    }
}
