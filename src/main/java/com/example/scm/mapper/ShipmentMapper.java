package com.example.scm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.scm.domain.Shipment;
import com.example.scm.domain.ShipmentDetail;
import com.example.scm.domain.ShipmentSearchCriteria;
import com.example.scm.domain.ShipmentStatus;
import com.example.scm.domain.StatusCount;

/**
 * 出荷指示 Mapper ―― 【XML方式・1:N とバッチINSERT】の例。
 * SQL は resources/mapper/ShipmentMapper.xml。
 *
 * <p>学習ポイント:
 * <ul>
 *   <li>{@code <collection>} でヘッダ1件に明細N件をぶら下げる</li>
 *   <li>{@code <foreach>} で「IN句」と「複数行INSERT」を組み立てる</li>
 *   <li>{@code <choose>/<when>/<otherwise>}、{@code <trim>} などの動的SQL</li>
 *   <li>enum の TypeHandler が効いていること(status に "20" が入る)</li>
 * </ul>
 */
@Mapper
public interface ShipmentMapper {

    /** ヘッダ＋明細＋商品名まで1回のSQLで取得(collection マッピング)。 */
    Shipment findById(@Param("id") Integer id);

    /** 検索条件つき一覧(ヘッダのみ)。 */
    List<Shipment> search(@Param("criteria") ShipmentSearchCriteria criteria);

    /** ステータス別件数。 */
    List<StatusCount> countByStatus();

    /** 出荷番号の最大値(採番用)。該当なしなら null。 */
    String findMaxShipmentNo(@Param("prefix") String prefix);

    /** ヘッダ登録。採番された id は引数の shipment にセットされる。 */
    int insertShipment(Shipment shipment);

    /** 明細をまとめて1文で INSERT(foreach)。 */
    int insertDetails(@Param("shipmentId") Integer shipmentId,
                      @Param("details") List<ShipmentDetail> details);

    int updateStatus(@Param("id") Integer id,
                     @Param("status") ShipmentStatus status);

    int deleteDetailsByShipmentId(@Param("shipmentId") Integer shipmentId);

    int deleteById(@Param("id") Integer id);
}
