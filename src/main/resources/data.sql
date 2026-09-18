-- =====================================================================
-- 初期データ。
-- 何度実行しても増えないように「まだ1件も無いときだけ INSERT する」書き方にしている。
--
-- ポイント: ID を明示せず AUTO_INCREMENT に採番させること。
--   明示的に id を指定して INSERT すると、H2 の採番カウンタが進まないまま
--   データだけが入るため、後から画面で登録したときに
--   「主キー違反(PRIMARY KEY ON PUBLIC.ITEM(ID))」が発生する。
-- =====================================================================

INSERT INTO warehouse (code, name, address)
SELECT * FROM (
  SELECT 'WH01' AS code, '東京物流センター' AS name, '東京都江東区新木場1-1-1' AS address UNION ALL
  SELECT 'WH02', '大阪物流センター', '大阪府大阪市住之江区南港1-2-3' UNION ALL
  SELECT 'WH03', '福岡デポ',         '福岡県福岡市東区香椎浜4-5-6'
) src
WHERE NOT EXISTS (SELECT 1 FROM warehouse);

INSERT INTO item (item_code, item_name, category, unit_price, safety_stock)
SELECT * FROM (
  SELECT 'ITM-1001' AS item_code, '光回線ルーター RX-100' AS item_name, 'ネットワーク機器' AS category,
         18500.00 AS unit_price, 30 AS safety_stock UNION ALL
  SELECT 'ITM-1002', 'ONU 一体型ルーター RX-200', 'ネットワーク機器', 24800.00, 20 UNION ALL
  SELECT 'ITM-1003', 'Wi-Fi 6 中継機 EX-50',      'ネットワーク機器',  7980.00, 40 UNION ALL
  SELECT 'ITM-2001', 'LANケーブル CAT6A 5m',      'ケーブル',          1280.00, 200 UNION ALL
  SELECT 'ITM-2002', '光ファイバケーブル 10m',     'ケーブル',          3200.00, 100 UNION ALL
  SELECT 'ITM-3001', 'SIMカード(nano)',           'モバイル',           500.00, 500 UNION ALL
  SELECT 'ITM-3002', 'モバイルルーター MR-30',     'モバイル',         29800.00, 15 UNION ALL
  SELECT 'ITM-4001', 'STB セットトップボックス',   '映像機器',         15800.00, 25 UNION ALL
  SELECT 'ITM-4002', 'リモコン RC-01',             '映像機器',          1800.00, 60 UNION ALL
  SELECT 'ITM-5001', 'AC アダプタ 12V',            '周辺機器',          2200.00, 80
) src
WHERE NOT EXISTS (SELECT 1 FROM item);

-- 在庫。商品コード・倉庫コードで引き当てるので、ID の値に依存しない
-- 数量は「下のサンプル出荷指示を処理し終えた後の状態」にしてある。
-- 出荷済(30)・引当済(20)の伝票の明細分は、すでに在庫から引かれている前提。
--   SH-20260901-001(出荷済): ITM-1001 x10, ITM-2001 x50 を WH01 から引当済
--   SH-20260910-001(引当済): ITM-1002 x5,  ITM-3001 x100 を WH02 から引当済
--   SH-20260915-001(登録済): 未引当のため在庫は減っていない
INSERT INTO stock (item_id, warehouse_id, quantity)
SELECT i.id, w.id, src.quantity
FROM (
  SELECT 'ITM-1001' AS item_code, 'WH01' AS wh_code, 110 AS quantity UNION ALL  -- 120 - 10(出荷済)
  SELECT 'ITM-1001', 'WH02', 45   UNION ALL
  SELECT 'ITM-1002', 'WH01', 18   UNION ALL
  SELECT 'ITM-1002', 'WH02', 55   UNION ALL  -- 60 - 5(引当済)
  SELECT 'ITM-1003', 'WH01', 230  UNION ALL
  SELECT 'ITM-1003', 'WH03', 12   UNION ALL
  SELECT 'ITM-2001', 'WH01', 800  UNION ALL  -- 850 - 50(出荷済)
  SELECT 'ITM-2001', 'WH02', 420  UNION ALL
  SELECT 'ITM-2002', 'WH01', 95   UNION ALL
  SELECT 'ITM-3001', 'WH02', 1100 UNION ALL  -- 1200 - 100(引当済)
  SELECT 'ITM-3002', 'WH01', 8    UNION ALL
  SELECT 'ITM-3002', 'WH03', 22   UNION ALL
  SELECT 'ITM-4001', 'WH02', 40   UNION ALL
  SELECT 'ITM-4002', 'WH02', 35   UNION ALL
  SELECT 'ITM-5001', 'WH01', 70
) src
JOIN item      i ON i.item_code = src.item_code
JOIN warehouse w ON w.code      = src.wh_code
WHERE NOT EXISTS (SELECT 1 FROM stock);

-- 出荷指示のサンプル(status は 10:登録済 20:引当済 30:出荷済 90:取消)
INSERT INTO shipment (shipment_no, customer_name, warehouse_id, status, ship_date, remarks)
SELECT src.shipment_no, src.customer_name, w.id, src.status, src.ship_date, src.remarks
FROM (
  SELECT 'SH-20260901-001' AS shipment_no, '株式会社アルファ通信' AS customer_name, 'WH01' AS wh_code,
         '30' AS status, DATE '2026-09-01' AS ship_date, '定期補充' AS remarks UNION ALL
  SELECT 'SH-20260910-001', 'ベータ電設株式会社', 'WH02', '20', DATE '2026-09-10', NULL UNION ALL
  SELECT 'SH-20260915-001', 'ガンマ商事株式会社', 'WH01', '10', DATE '2026-09-15', '午前着指定'
) src
JOIN warehouse w ON w.code = src.wh_code
WHERE NOT EXISTS (SELECT 1 FROM shipment);

INSERT INTO shipment_detail (shipment_id, item_id, quantity)
SELECT s.id, i.id, src.quantity
FROM (
  SELECT 'SH-20260901-001' AS shipment_no, 'ITM-1001' AS item_code, 10 AS quantity UNION ALL
  SELECT 'SH-20260901-001', 'ITM-2001', 50  UNION ALL
  SELECT 'SH-20260910-001', 'ITM-1002', 5   UNION ALL
  SELECT 'SH-20260910-001', 'ITM-3001', 100 UNION ALL
  SELECT 'SH-20260915-001', 'ITM-1003', 12
) src
JOIN shipment s ON s.shipment_no = src.shipment_no
JOIN item     i ON i.item_code   = src.item_code
WHERE NOT EXISTS (SELECT 1 FROM shipment_detail);
