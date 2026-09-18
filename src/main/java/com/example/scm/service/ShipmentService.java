package com.example.scm.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.scm.domain.Item;
import com.example.scm.domain.Shipment;
import com.example.scm.domain.ShipmentDetail;
import com.example.scm.domain.ShipmentSearchCriteria;
import com.example.scm.domain.ShipmentStatus;
import com.example.scm.domain.StatusCount;
import com.example.scm.exception.BusinessException;
import com.example.scm.mapper.ItemMapper;
import com.example.scm.mapper.ShipmentMapper;
import com.example.scm.mapper.StockMapper;

/**
 * 出荷指示のサービス。このアプリの業務ロジックの中心。
 *
 * <p>学習ポイント:
 * <ul>
 *   <li>{@code @Transactional} を付けたメソッド全体が1トランザクション。
 *       途中で RuntimeException が出れば、それまでの INSERT / UPDATE はすべてロールバックされる。</li>
 *   <li>複数テーブル(shipment / shipment_detail / stock)の更新を1つの業務処理としてまとめている。</li>
 *   <li>在庫の減算は「UPDATE の件数が 0 なら在庫不足」で判定する(StockMapper.decrease)。</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ShipmentService {

    private static final DateTimeFormatter NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ShipmentMapper shipmentMapper;
    private final StockMapper stockMapper;
    private final ItemMapper itemMapper;

    public ShipmentService(ShipmentMapper shipmentMapper, StockMapper stockMapper, ItemMapper itemMapper) {
        this.shipmentMapper = shipmentMapper;
        this.stockMapper = stockMapper;
        this.itemMapper = itemMapper;
    }

    public List<Shipment> search(ShipmentSearchCriteria criteria) {
        return shipmentMapper.search(criteria);
    }

    public Shipment findById(Integer id) {
        Shipment shipment = shipmentMapper.findById(id);
        if (shipment == null) {
            throw new BusinessException("出荷指示が見つかりません (id=" + id + ")");
        }
        return shipment;
    }

    public List<StatusCount> countByStatus() {
        return shipmentMapper.countByStatus();
    }

    /**
     * 出荷指示の登録。ヘッダ1件＋明細N件を1トランザクションで INSERT する。
     * この時点では在庫は動かさない(ステータスは DRAFT)。
     */
    @Transactional
    public Shipment create(Shipment shipment) {
        List<ShipmentDetail> details = mergeSameItems(shipment.getDetails());
        if (details.isEmpty()) {
            throw new BusinessException("明細を1行以上入力してください");
        }
        validateItemsExist(details);

        shipment.setDetails(details);
        shipment.setStatus(ShipmentStatus.DRAFT);
        shipment.setShipmentNo(nextShipmentNo(shipment.getShipDate()));

        shipmentMapper.insertShipment(shipment);          // ← ここで id が採番される
        shipmentMapper.insertDetails(shipment.getId(), details); // foreach でまとめて INSERT
        return shipment;
    }

    /**
     * 在庫引当。明細の数量だけ在庫を減らし、ステータスを ALLOCATED にする。
     * 1行でも在庫不足なら例外 → トランザクション全体がロールバックされ、
     * 「一部だけ在庫が減った」という中途半端な状態にはならない。
     */
    @Transactional
    public Shipment allocate(Integer id) {
        Shipment shipment = findById(id);
        if (shipment.getStatus() != ShipmentStatus.DRAFT) {
            throw new BusinessException("引当できるのは「登録済」の出荷指示のみです(現在: "
                    + shipment.getStatus().getLabel() + ")");
        }

        for (ShipmentDetail detail : shipment.getDetails()) {
            int updated = stockMapper.decrease(detail.getItemId(), shipment.getWarehouseId(), detail.getQuantity());
            if (updated == 0) {
                String itemName = detail.getItem() != null ? detail.getItem().getItemName()
                        : String.valueOf(detail.getItemId());
                throw new BusinessException("在庫が不足しています: " + itemName
                        + " (要求数 " + detail.getQuantity() + ")");
            }
        }

        shipmentMapper.updateStatus(id, ShipmentStatus.ALLOCATED);
        shipment.setStatus(ShipmentStatus.ALLOCATED);
        return shipment;
    }

    /** 出荷確定。引当済みのものだけ出荷済みにできる。 */
    @Transactional
    public Shipment ship(Integer id) {
        Shipment shipment = findById(id);
        if (shipment.getStatus() != ShipmentStatus.ALLOCATED) {
            throw new BusinessException("出荷できるのは「引当済」の出荷指示のみです(現在: "
                    + shipment.getStatus().getLabel() + ")");
        }
        shipmentMapper.updateStatus(id, ShipmentStatus.SHIPPED);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        return shipment;
    }

    /** 取消。引当済みだった場合は在庫を戻す。 */
    @Transactional
    public Shipment cancel(Integer id) {
        Shipment shipment = findById(id);
        if (shipment.getStatus() == ShipmentStatus.SHIPPED) {
            throw new BusinessException("出荷済の出荷指示は取消できません");
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new BusinessException("すでに取消済です");
        }
        if (shipment.getStatus() == ShipmentStatus.ALLOCATED) {
            for (ShipmentDetail detail : shipment.getDetails()) {
                stockMapper.increase(detail.getItemId(), shipment.getWarehouseId(), detail.getQuantity());
            }
        }
        shipmentMapper.updateStatus(id, ShipmentStatus.CANCELLED);
        shipment.setStatus(ShipmentStatus.CANCELLED);
        return shipment;
    }

    /**
     * 削除。引当済みなら在庫を戻してから消す(明細は ON DELETE CASCADE でも消えるが明示的に削除する)。
     *
     * <p>出荷済は出荷実績として残す必要があるため削除させない。
     * 誤登録を取り下げたい場合は取消({@link #cancel(Integer)})を使う。
     */
    @Transactional
    public void delete(Integer id) {
        Shipment shipment = findById(id);
        if (shipment.getStatus() == ShipmentStatus.SHIPPED) {
            throw new BusinessException("出荷済の出荷指示は削除できません");
        }
        if (shipment.getStatus() == ShipmentStatus.ALLOCATED) {
            for (ShipmentDetail detail : shipment.getDetails()) {
                stockMapper.increase(detail.getItemId(), shipment.getWarehouseId(), detail.getQuantity());
            }
        }
        shipmentMapper.deleteDetailsByShipmentId(id);
        shipmentMapper.deleteById(id);
    }

    /** 出荷番号を "SH-yyyyMMdd-001" 形式で採番する。 */
    private String nextShipmentNo(LocalDate shipDate) {
        String prefix = "SH-" + shipDate.format(NO_DATE_FORMAT) + "-";
        String max = shipmentMapper.findMaxShipmentNo(prefix);
        int seq = 1;
        if (max != null && max.length() > prefix.length()) {
            seq = Integer.parseInt(max.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%03d", seq);
    }

    /** 同じ商品が複数行に分かれて入力された場合は1行にまとめる。 */
    private List<ShipmentDetail> mergeSameItems(List<ShipmentDetail> details) {
        Map<Integer, ShipmentDetail> merged = new LinkedHashMap<>();
        for (ShipmentDetail detail : details) {
            if (detail == null || detail.getItemId() == null
                    || detail.getQuantity() == null || detail.getQuantity() <= 0) {
                continue; // 画面の空行はスキップ
            }
            ShipmentDetail exists = merged.get(detail.getItemId());
            if (exists == null) {
                merged.put(detail.getItemId(), detail);
            } else {
                exists.setQuantity(exists.getQuantity() + detail.getQuantity());
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void validateItemsExist(List<ShipmentDetail> details) {
        for (ShipmentDetail detail : details) {
            Item item = itemMapper.findById(detail.getItemId());
            if (item == null) {
                throw new BusinessException("存在しない商品が指定されています (id=" + detail.getItemId() + ")");
            }
        }
    }
}
