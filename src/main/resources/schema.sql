-- =====================================================================
-- 物流管理(SCM)学習用スキーマ
-- IF NOT EXISTS を付けているので、起動のたびに実行されてもデータは消えない。
-- 作り直したいときは data/ フォルダを削除してから起動する。
-- =====================================================================

CREATE TABLE IF NOT EXISTS warehouse (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    code    VARCHAR(10)  NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL,
    address VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS item (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    item_code    VARCHAR(20)   NOT NULL UNIQUE,
    item_name    VARCHAR(100)  NOT NULL,
    category     VARCHAR(50)   NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    safety_stock INT           NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    item_id      INT NOT NULL,
    warehouse_id INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock UNIQUE (item_id, warehouse_id),
    CONSTRAINT fk_stock_item      FOREIGN KEY (item_id)      REFERENCES item(id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS shipment (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    shipment_no   VARCHAR(20)  NOT NULL UNIQUE,
    customer_name VARCHAR(100) NOT NULL,
    warehouse_id  INT          NOT NULL,
    -- 区分値はコードで保持(10:登録済 20:引当済 30:出荷済 90:取消)
    status        VARCHAR(2)   NOT NULL,
    ship_date     DATE         NOT NULL,
    remarks       VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipment_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

CREATE TABLE IF NOT EXISTS shipment_detail (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    shipment_id INT NOT NULL,
    item_id     INT NOT NULL,
    quantity    INT NOT NULL,
    CONSTRAINT fk_detail_shipment FOREIGN KEY (shipment_id) REFERENCES shipment(id) ON DELETE CASCADE,
    CONSTRAINT fk_detail_item     FOREIGN KEY (item_id)     REFERENCES item(id)
);
