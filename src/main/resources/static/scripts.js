// KidsPOS Modern Scripts
window.onload = function () {
    // Initialize DataTables with modern configuration
    $('.table').DataTable({
        scrollY: "400px",
        scrollCollapse: true,
        paging: true,
        pageLength: 10,
        language: {
            search: "検索:",
            lengthMenu: "_MENU_ 件表示",
            info: "_START_ - _END_ / _TOTAL_ 件",
            paginate: {
                first: "最初",
                last: "最後",
                next: "次",
                previous: "前"
            },
            zeroRecords: "データがありません",
            emptyTable: "テーブルにデータがありません"
        },
        dom: '<"row"<"col-sm-6"l><"col-sm-6"f>>rtip'
    });
    
    // Add fade-in animation
    document.querySelectorAll('.card-modern').forEach(function(card, index) {
        setTimeout(function() {
            card.style.opacity = '0';
            card.style.animation = 'fadeIn 0.5s ease forwards';
        }, index * 100);
    });
};
