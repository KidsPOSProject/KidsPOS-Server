package info.nukoneko.kidspos.server.logging

/**
 * 直近のログを保持するリングバッファ
 *
 * logback の appender から書き込まれ、画面表示用に読み出される。
 * appender は Spring 管理外で生成されるためシングルトンとして共有する。
 */
object LogBuffer {
    const val DEFAULT_CAPACITY = 500
    private const val MIN_CAPACITY = 1
    private const val MAX_CAPACITY = 10000

    private val lock = Any()
    private val entries = ArrayDeque<LogEntry>()
    private var capacity = DEFAULT_CAPACITY

    fun configureCapacity(newCapacity: Int) {
        synchronized(lock) {
            capacity = newCapacity.coerceIn(MIN_CAPACITY, MAX_CAPACITY)
            trimToCapacity()
        }
    }

    fun capacity(): Int = synchronized(lock) { capacity }

    fun add(entry: LogEntry) {
        synchronized(lock) {
            entries.addLast(entry)
            trimToCapacity()
        }
    }

    fun snapshot(): List<LogEntry> = synchronized(lock) { entries.toList() }

    fun size(): Int = synchronized(lock) { entries.size }

    fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private fun trimToCapacity() {
        while (entries.size > capacity) {
            entries.removeFirst()
        }
    }
}
