package com.example.scm.exception;

/** 業務エラー(在庫不足など)。画面にメッセージとして出す想定。 */
public class BusinessException extends RuntimeException {

    /**
     * 例外は Serializable なので、宣言しないと
     * 「The serializable class ... does not declare a static final serialVersionUID field」
     * という警告が出る。値は任意だが、直列化の互換性を意識して固定値を明示しておく。
     */
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
