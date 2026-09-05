const DATATABLE_LANGUAGE = {
    emptyTable: 'データがありません',
    info: '_TOTAL_ 件中 _START_ - _END_ 件を表示',
    infoEmpty: '0 件',
    infoFiltered: '(全 _MAX_ 件から絞り込み)',
    lengthMenu: '_MENU_ 件ずつ表示',
    loadingRecords: '読み込み中...',
    processing: '処理中...',
    search: '検索:',
    searchPlaceholder: 'キーワード',
    zeroRecords: '一致するデータがありません',
    paginate: {
        first: '最初',
        last: '最後',
        next: '次',
        previous: '前'
    }
};

document.addEventListener('DOMContentLoaded', function () {
    if (typeof jQuery === 'undefined' || typeof jQuery.fn.DataTable === 'undefined') {
        return;
    }

    jQuery('table[data-datatable]').each(function () {
        const table = jQuery(this);
        if (jQuery.fn.DataTable.isDataTable(table)) {
            return;
        }

        table.DataTable({
            language: DATATABLE_LANGUAGE,
            // 並び順はサーバー側で意味のある順に整えてあるため、初期状態では崩さない
            order: [],
            pageLength: 25,
            lengthMenu: [[25, 50, 100, -1], ['25', '50', '100', 'すべて']],
            columnDefs: [
                { targets: 'no-sort', orderable: false, searchable: false }
            ]
        });
    });
});

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });
});

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form[data-confirm-delete]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            const text = form.dataset.confirmDelete + ' を削除します。この操作は取り消せません。';
            if (!window.confirm(text)) {
                event.preventDefault();
                event.stopPropagation();
            }
        });
    });
});

// サーバーはイントラネットに閉じていて NTP に届かず、Raspberry Pi は RTC を持たないため
// 電源を入れるたびに時刻が巻き戻る。管理画面を開くだけで時刻が合うよう、
// ブラウザの時刻を全リクエストで申告する
(function reportClientTime() {
    const HEADER = 'X-Client-Time';

    const originalFetch = window.fetch;
    if (typeof originalFetch === 'function') {
        window.fetch = function (input, init) {
            try {
                const request = new Request(input, init);
                request.headers.set(HEADER, String(Date.now()));
                return originalFetch.call(this, request);
            } catch (e) {
                return originalFetch.call(this, input, init);
            }
        };
    }

    const originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function (body) {
        try {
            this.setRequestHeader(HEADER, String(Date.now()));
        } catch (e) {
            // 送信直前でヘッダーを足せない状態なら申告を諦めて通常どおり送る
        }
        return originalSend.call(this, body);
    };

    document.addEventListener('DOMContentLoaded', function () {
        fetch('/api/system/time', { headers: { 'X-Client-Time': String(Date.now()) } }).catch(function () {});
    });
})();
