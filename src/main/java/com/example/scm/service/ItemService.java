package com.example.scm.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;
import com.example.scm.exception.BusinessException;
import com.example.scm.mapper.ItemMapper;

/**
 * 商品マスタのサービス。
 *
 * <p>MyBatis の Mapper はここから呼ぶ(Controller から直接呼ばない)のが基本構成。
 * トランザクション境界もサービス層に置く。
 */
@Service
@Transactional(readOnly = true)
public class ItemService {

    /**
     * ORDER BY に渡してよいカラムのホワイトリスト。
     * 画面から来た文字列をそのまま {@code ${}} に入れると SQL インジェクションになるため、
     * 必ずこのマップを経由して「アプリが用意した文字列」だけを SQL に渡す。
     */
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "code", "item_code",
            "name", "item_name",
            "price", "unit_price DESC",
            "category", "category, item_code");

    private static final String DEFAULT_SORT_COLUMN = "item_code";

    private final ItemMapper itemMapper;

    public ItemService(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    public List<Item> search(ItemSearchCriteria criteria) {
        return itemMapper.search(criteria, toOrderByColumn(criteria.getSort()));
    }

    /**
     * 画面から来たソート指定を、SQL に埋め込んでよいカラム名へ変換する。
     * ホワイトリストに無い値・未指定はすべて既定のカラムに倒す。
     * (Map.of() は null キーを渡すと NPE になるため、null は先に弾く)
     */
    private String toOrderByColumn(String sort) {
        if (sort == null) {
            return DEFAULT_SORT_COLUMN;
        }
        return SORT_COLUMNS.getOrDefault(sort, DEFAULT_SORT_COLUMN);
    }

    public List<Item> findAll() {
        return itemMapper.findAll();
    }

    public Item findById(Integer id) {
        Item item = itemMapper.findById(id);
        if (item == null) {
            throw new BusinessException("商品が見つかりません (id=" + id + ")");
        }
        return item;
    }

    public List<String> findCategories() {
        return itemMapper.findCategories();
    }

    @Transactional
    public Item create(Item item) {
        if (itemMapper.findByItemCode(item.getItemCode()) != null) {
            throw new BusinessException("商品コードが重複しています: " + item.getItemCode());
        }
        // insert 後、採番された id が item にセットされている(useGeneratedKeys)
        itemMapper.insert(item);
        return item;
    }

    @Transactional
    public Item update(Item item) {
        Item current = findById(item.getId());
        Item duplicated = itemMapper.findByItemCode(item.getItemCode());
        if (duplicated != null && !duplicated.getId().equals(current.getId())) {
            throw new BusinessException("商品コードが重複しています: " + item.getItemCode());
        }
        itemMapper.update(item);
        return item;
    }

    @Transactional
    public void delete(Integer id) {
        try {
            itemMapper.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 在庫や出荷明細から参照されている商品は消せない(外部キー制約)
            throw new BusinessException("在庫または出荷明細で使用中のため削除できません");
        }
    }
}
