package com.example.scm.domain;

/**
 * 出荷ステータス。
 *
 * DB には "10" "20" のようなコード値で入っている。
 * MyBatis 標準の EnumTypeHandler は enum 名(DRAFT など)で入出力するため、
 * コード値に合わせるには独自 TypeHandler が必要になる。
 * → {@link com.example.scm.typehandler.ShipmentStatusTypeHandler}
 */
public enum ShipmentStatus {

    /** 登録直後。在庫はまだ引き当てていない。 */
    DRAFT("10", "登録済"),
    /** 在庫引当済み。在庫テーブルから数量を減算した状態。 */
    ALLOCATED("20", "引当済"),
    /** 出荷完了。 */
    SHIPPED("30", "出荷済"),
    /** 取消。引当済だった場合は在庫を戻す。 */
    CANCELLED("90", "取消");

    private final String code;
    private final String label;

    ShipmentStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }

    public String getLabel() { return label; }

    /** DB のコード値から enum へ。未知のコードは例外にして早く気づけるようにする。 */
    public static ShipmentStatus fromCode(String code) {
        for (ShipmentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知の出荷ステータスコードです: " + code);
    }
}
