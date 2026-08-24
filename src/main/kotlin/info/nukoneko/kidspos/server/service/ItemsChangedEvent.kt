package info.nukoneko.kidspos.server.service

/**
 * 商品マスタが変更されたことを表すイベント
 *
 * バーコードPDFのキャッシュを作り直すために使う。
 * BarcodePdfService は ItemService に依存しているため、逆向きの直接参照を避けてイベントで通知する。
 */
class ItemsChangedEvent(
    val itemId: Int,
)
