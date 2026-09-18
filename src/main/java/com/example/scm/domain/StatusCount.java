package com.example.scm.domain;

/** 出荷ステータス別の件数(ダッシュボード表示用)。 */
public class StatusCount {

    private ShipmentStatus status;
    private int count;

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getLabel() { return status == null ? "" : status.getLabel(); }
}
