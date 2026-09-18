/* ===================================================================
 * 画面まわりの jQuery。
 * サーバとのやり取りはすべて /api 配下の JSON API(Spring の @RestController)。
 * 返ってくる JSON は MyBatis がマッピングしたオブジェクトそのもの。
 * =================================================================== */
$(function () {

  // ------------------------------------------------------------------
  // 共通: 数値のカンマ編集 / HTML エスケープ
  // ------------------------------------------------------------------
  function comma(value) {
    if (value === null || value === undefined) return '';
    return Number(value).toLocaleString('ja-JP');
  }

  function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return $('<div>').text(value).html();
  }

  /** API がエラー(400/500)を返したときのメッセージを取り出す */
  function apiMessage(xhr, fallback) {
    if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
      return xhr.responseJSON.message;
    }
    return fallback;
  }

  // ==================================================================
  // 商品マスタ画面: 検索(GET /api/items)
  // ==================================================================
  if ($('#itemTable').length) {

    function renderItems(items) {
      var $tbody = $('#itemTable tbody').empty();
      if (items.length === 0) {
        $tbody.append('<tr><td colspan="6" class="muted">該当する商品がありません。</td></tr>');
      }
      $.each(items, function (_, item) {
        var html = '<tr>' +
          '<td>' + escapeHtml(item.itemCode) + '</td>' +
          '<td>' + escapeHtml(item.itemName) + '</td>' +
          '<td>' + escapeHtml(item.category) + '</td>' +
          '<td class="num">' + comma(item.unitPrice) + '</td>' +
          '<td class="num">' + comma(item.safetyStock) + '</td>' +
          '<td>' +
            '<a class="btn btn-sub btn-sm" href="/items/' + item.id + '/edit">編集</a> ' +
            '<form class="inline-form" action="/items/' + item.id + '/delete" method="post"' +
            ' onsubmit="return confirm(\'削除しますか?\');">' +
            '<button type="submit" class="btn-danger btn-sm">削除</button></form>' +
          '</td>' +
        '</tr>';
        $tbody.append(html);
      });
      $('#resultCount').text('(' + items.length + ' 件)');
    }

    function searchItems() {
      // 空文字のパラメータも送るが、サーバ側(ItemSearchCriteria)で null 扱いにしている
      var params = {
        keyword: $('#keyword').val(),
        category: $('#category').val(),
        minPrice: $('#minPrice').val(),
        maxPrice: $('#maxPrice').val(),
        sort: $('#sort').val()
      };
      $('#searchInfo').text('検索中...');
      $.getJSON('/api/items', params)
        .done(function (items) {
          renderItems(items);
          $('#searchInfo').text('GET /api/items?' + $.param(params));
        })
        .fail(function (xhr) {
          $('#searchInfo').text(apiMessage(xhr, '検索に失敗しました'));
        });
    }

    $('#btnSearch').on('click', searchItems);
    $('#keyword').on('keydown', function (e) {
      if (e.key === 'Enter') { e.preventDefault(); searchItems(); }
    });
    $('#btnClear').on('click', function () {
      $('#keyword, #minPrice, #maxPrice').val('');
      $('#category').val('');
      $('#sort').val('code');
      searchItems();
    });
  }

  // ==================================================================
  // 在庫照会画面: 絞り込み(GET /api/stocks) / 入庫(POST /api/stocks/receive)
  // ==================================================================
  if ($('#stockTable').length) {

    function renderStocks(stocks) {
      var $tbody = $('#stockTable tbody').empty();
      if (stocks.length === 0) {
        $tbody.append('<tr><td colspan="7" class="muted">在庫データがありません。</td></tr>');
      }
      $.each(stocks, function (_, s) {
        var below = s.quantity < s.item.safetyStock;
        var updated = s.updatedAt ? String(s.updatedAt).replace('T', ' ').substring(0, 16) : '';
        $tbody.append('<tr>' +
          '<td>' + escapeHtml(s.warehouse.name) + '</td>' +
          '<td>' + escapeHtml(s.item.itemCode) + '</td>' +
          '<td>' + escapeHtml(s.item.itemName) + '</td>' +
          '<td>' + escapeHtml(s.item.category) + '</td>' +
          '<td class="num' + (below ? ' shortage' : '') + '">' + comma(s.quantity) + '</td>' +
          '<td class="num">' + comma(s.item.safetyStock) + '</td>' +
          '<td>' + escapeHtml(updated) + '</td>' +
        '</tr>');
      });
    }

    function loadStocks() {
      var warehouseId = $('#warehouseFilter').val();
      $.getJSON('/api/stocks', warehouseId ? { warehouseId: warehouseId } : {})
        .done(renderStocks)
        .fail(function (xhr) {
          $('#receiveInfo').text(apiMessage(xhr, '在庫の取得に失敗しました'));
        });
    }

    $('#warehouseFilter').on('change', loadStocks);
    $('#btnReload').on('click', loadStocks);

    $('#btnReceive').on('click', function () {
      var payload = {
        itemId: Number($('#receiveItem').val()),
        warehouseId: Number($('#receiveWarehouse').val()),
        quantity: Number($('#receiveQty').val())
      };
      $.ajax({
        url: '/api/stocks/receive',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload)
      }).done(function (stocks) {
        // 入庫した倉庫の在庫が返ってくるので、フィルタもその倉庫に合わせて描画する
        $('#warehouseFilter').val(String(payload.warehouseId));
        renderStocks(stocks);
        $('#receiveInfo').text('入庫しました(数量 ' + payload.quantity + ')。UPDATE stock SET quantity = quantity + ? が実行されています。');
      }).fail(function (xhr) {
        $('#receiveInfo').text(apiMessage(xhr, '入庫に失敗しました'));
      });
    });
  }

  // ==================================================================
  // 出荷指示 詳細画面: ステータス操作(POST /api/shipments/{id}/{action})
  // ==================================================================
  var $actionPanel = $('#actionPanel');
  if ($actionPanel.length) {
    var shipmentId = $actionPanel.data('shipment-id');
    var statusClasses = 'badge-10 badge-20 badge-30 badge-90';

    $('.js-action').on('click', function () {
      var action = $(this).data('action');
      $('#ajaxError, #ajaxOk').hide();

      $.ajax({
        url: '/api/shipments/' + shipmentId + '/' + action,
        type: 'POST'
      }).done(function (shipment) {
        // 返ってきた JSON の status は enum 名("ALLOCATED" など)
        var labels = { DRAFT: '登録済', ALLOCATED: '引当済', SHIPPED: '出荷済', CANCELLED: '取消' };
        var codes = { DRAFT: '10', ALLOCATED: '20', SHIPPED: '30', CANCELLED: '90' };
        $('#statusBadge')
          .removeClass(statusClasses)
          .addClass('badge-' + codes[shipment.status])
          .text(labels[shipment.status]);
        $('#ajaxOk').text('処理しました。ステータス: ' + labels[shipment.status]).show();
      }).fail(function (xhr) {
        // 在庫不足などの業務エラーは ApiExceptionHandler が 400 + message で返す
        $('#ajaxError').text(apiMessage(xhr, '処理に失敗しました')).show();
      });
    });
  }

  // ==================================================================
  // 出荷指示 作成画面: 明細行の追加・削除(添字を振り直す)
  // ==================================================================
  if ($('#detailTable').length) {

    function renumberRows() {
      $('#detailBody tr.detail-row').each(function (index) {
        $(this).find('select, input').each(function () {
          var name = $(this).attr('name');
          if (!name) return;
          $(this).attr('name', name.replace(/details\[\d+\]/, 'details[' + index + ']'));
        });
      });
    }

    function addRow() {
      var index = $('#detailBody tr.detail-row').length;
      var html = $('#detailRowTemplate').html().replace(/__INDEX__/g, index);
      $('#detailBody').append(html);
    }

    $('#btnAddRow').on('click', addRow);

    $('#detailBody').on('click', '.btn-remove-row', function () {
      $(this).closest('tr').remove();
      renumberRows();
    });

    // 初期表示で明細が0行なら1行だけ用意しておく
    if ($('#detailBody tr.detail-row').length === 0) {
      addRow();
    }
  }
});
