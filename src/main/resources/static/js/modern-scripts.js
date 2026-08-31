/**
 * Modern Scripts for KidsPOS System
 */

// Dark Mode Toggle
document.addEventListener('DOMContentLoaded', function() {
    const darkModeToggle = document.getElementById('darkModeToggle');
    const body = document.body;
    
    // Check for saved dark mode preference
    const darkMode = localStorage.getItem('darkMode');
    if (darkMode === 'enabled') {
        body.classList.add('dark-mode');
        updateDarkModeIcon(true);
    }
    
    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function(e) {
            e.preventDefault();
            body.classList.toggle('dark-mode');
            const isDark = body.classList.contains('dark-mode');
            localStorage.setItem('darkMode', isDark ? 'enabled' : 'disabled');
            updateDarkModeIcon(isDark);
        });
    }
    
    function updateDarkModeIcon(isDark) {
        const icon = darkModeToggle?.querySelector('i');
        if (icon) {
            icon.className = isDark ? 'fas fa-sun' : 'fas fa-moon';
        }
    }
});

// Sidebar Toggle for Mobile
document.addEventListener('DOMContentLoaded', function() {
    const sidebarToggle = document.createElement('button');
    sidebarToggle.className = 'btn btn-primary d-md-none sidebar-toggle';
    sidebarToggle.innerHTML = '<i class="fas fa-bars"></i>';
    sidebarToggle.style.position = 'fixed';
    sidebarToggle.style.top = '80px';
    sidebarToggle.style.left = '10px';
    sidebarToggle.style.zIndex = '1001';
    
    if (window.innerWidth < 768) {
        document.body.appendChild(sidebarToggle);
    }
    
    sidebarToggle.addEventListener('click', function() {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.classList.toggle('active');
        }
    });
});

// DataTables (opt-in: <table data-datatable>)
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

document.addEventListener('DOMContentLoaded', function() {
    if (typeof jQuery === 'undefined' || typeof jQuery.fn.DataTable === 'undefined') {
        return;
    }

    jQuery('table[data-datatable]').each(function() {
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

// Form Validation
document.addEventListener('DOMContentLoaded', function() {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!form.checkValidity()) {
                e.preventDefault();
                e.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });
});

// Delete confirmation (opt-in: <form data-confirm-delete="表示名">)
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('form[data-confirm-delete]').forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (form.dataset.confirmed === 'true') {
                return;
            }
            event.preventDefault();
            event.stopPropagation();

            const name = form.dataset.confirmDelete;
            const text = name + ' を削除します。この操作は取り消せません。';

            if (typeof Swal === 'undefined') {
                if (window.confirm(text)) {
                    form.dataset.confirmed = 'true';
                    form.submit();
                }
                return;
            }

            Swal.fire({
                title: '削除しますか？',
                text: text,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#64748b',
                confirmButtonText: '削除する',
                cancelButtonText: 'キャンセル'
            }).then(function(result) {
                if (result.isConfirmed) {
                    form.dataset.confirmed = 'true';
                    form.submit();
                }
            });
        });
    });
});

// Smooth Scroll
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
});

// Loading Spinner
function showLoadingSpinner() {
    const spinner = document.createElement('div');
    spinner.className = 'loading-overlay';
    spinner.innerHTML = '<div class="spinner-modern"></div>';
    spinner.style.position = 'fixed';
    spinner.style.top = '0';
    spinner.style.left = '0';
    spinner.style.width = '100%';
    spinner.style.height = '100%';
    spinner.style.background = 'rgba(0,0,0,0.5)';
    spinner.style.display = 'flex';
    spinner.style.alignItems = 'center';
    spinner.style.justifyContent = 'center';
    spinner.style.zIndex = '9999';
    document.body.appendChild(spinner);
}

function hideLoadingSpinner() {
    const spinner = document.querySelector('.loading-overlay');
    if (spinner) {
        spinner.remove();
    }
}

// Toast Notifications
function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toastContainer') || createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');
    
    const icon = getToastIcon(type);
    
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${icon} ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    
    const bsToast = new bootstrap.Toast(toast, {
        autohide: true,
        delay: 3000
    });
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', () => {
        toast.remove();
    });
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container position-fixed top-0 end-0 p-3';
    container.style.zIndex = '9999';
    document.body.appendChild(container);
    return container;
}

function getToastIcon(type) {
    const icons = {
        'success': '<i class="fas fa-check-circle"></i>',
        'danger': '<i class="fas fa-exclamation-circle"></i>',
        'warning': '<i class="fas fa-exclamation-triangle"></i>',
        'info': '<i class="fas fa-info-circle"></i>',
        'primary': '<i class="fas fa-bell"></i>'
    };
    return icons[type] || icons['info'];
}

// Number Format Helper
function formatCurrency(amount) {
    return new Intl.NumberFormat('ja-JP', {
        style: 'currency',
        currency: 'JPY'
    }).format(amount);
}

// Date Format Helper
function formatDate(date) {
    return new Intl.DateTimeFormat('ja-JP', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(new Date(date));
}

// Confirm Dialog Helper
function confirmAction(title, text, confirmText = '確認', cancelText = 'キャンセル') {
    return Swal.fire({
        title: title,
        text: text,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#6366f1',
        cancelButtonColor: '#64748b',
        confirmButtonText: confirmText,
        cancelButtonText: cancelText
    });
}

// Export functions for use in other scripts
window.kidsPOS = {
    showLoadingSpinner,
    hideLoadingSpinner,
    showToast,
    formatCurrency,
    formatDate,
    confirmAction
};

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
