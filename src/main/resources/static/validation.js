// バリデーション関数

/**
 * IPアドレスのバリデーション
 * IPv4形式（例: 192.168.1.1）をチェック
 */
function validateIPAddress(ip) {
    const ipv4Pattern = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    return ipv4Pattern.test(ip);
}

/**
 * バーコードのバリデーション
 * 数字のみで構成され、8-13桁（EAN-8, EAN-13, JAN, UPCなど）
 */
function validateBarcode(barcode) {
    // 空文字は許可
    if (!barcode || barcode.length === 0) return true;

    // 数字のみチェック
    const numericPattern = /^\d+$/;
    if (!numericPattern.test(barcode)) return false;

    // 長さチェック（8-13桁）
    return barcode.length >= 8 && barcode.length <= 13;
}

/**
 * フォームバリデーションの初期化
 */
function initializeValidation() {
    // 店舗追加フォーム
    const storeForm = document.querySelector('form[action="/stores"]');
    if (storeForm) {
        const printerUriInput = storeForm.querySelector('input[name="printerUri"]');
        if (printerUriInput) {
            // IPアドレスフィールドのバリデーション
            printerUriInput.addEventListener('blur', function() {
                const value = this.value.trim();
                if (value && !validateIPAddress(value)) {
                    this.classList.add('is-invalid');
                    showValidationError(this, '有効なIPアドレスを入力してください（例: 192.168.1.100）');
                } else {
                    this.classList.remove('is-invalid');
                    removeValidationError(this);
                }
            });

            // フォーム送信時のバリデーション
            storeForm.addEventListener('submit', function(e) {
                const value = printerUriInput.value.trim();
                if (value && !validateIPAddress(value)) {
                    e.preventDefault();
                    printerUriInput.classList.add('is-invalid');
                    showValidationError(printerUriInput, '有効なIPアドレスを入力してください（例: 192.168.1.100）');
                    printerUriInput.focus();
                }
            });
        }
    }

    // アイテム追加フォーム
    const itemForm = document.querySelector('form[action="/items"]');
    if (itemForm) {
        const barcodeInput = itemForm.querySelector('input[name="barcode"]');
        if (barcodeInput) {
            // バーコードフィールドのバリデーション
            barcodeInput.addEventListener('blur', function() {
                const value = this.value.trim();
                if (value && !validateBarcode(value)) {
                    this.classList.add('is-invalid');
                    showValidationError(this, 'バーコードは8〜13桁の数字で入力してください');
                } else {
                    this.classList.remove('is-invalid');
                    removeValidationError(this);
                }
            });

            // フォーム送信時のバリデーション
            itemForm.addEventListener('submit', function(e) {
                const value = barcodeInput.value.trim();
                if (value && !validateBarcode(value)) {
                    e.preventDefault();
                    barcodeInput.classList.add('is-invalid');
                    showValidationError(barcodeInput, 'バーコードは8〜13桁の数字で入力してください');
                    barcodeInput.focus();
                }
            });
        }
    }
}

/**
 * エラーメッセージを表示
 */
function showValidationError(input, message) {
    // 既存のエラーメッセージを削除
    removeValidationError(input);

    // エラーメッセージを作成
    const errorDiv = document.createElement('div');
    errorDiv.className = 'invalid-feedback';
    errorDiv.textContent = message;

    // inputの後に挿入
    input.parentNode.insertBefore(errorDiv, input.nextSibling);
}

/**
 * エラーメッセージを削除
 */
function removeValidationError(input) {
    const existingError = input.parentNode.querySelector('.invalid-feedback');
    if (existingError) {
        existingError.remove();
    }
}

// ページロード時に初期化
document.addEventListener('DOMContentLoaded', initializeValidation);