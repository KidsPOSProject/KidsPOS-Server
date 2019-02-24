package info.nukoneko.kidspos.common

abstract class Commander {
    private var commands: ByteArray = ByteArray(0)

    fun writeBytes(vararg byte: Byte) {
        writeByteArray(byte)
    }

    fun writeByteArray(byte: ByteArray) {
        commands = ByteArray(commands.size + byte.size) {
            if (it < commands.size) {
                commands[it]
            } else {
                byte[it - commands.size]
            }
        }
    }

    fun build(): ByteArray = commands
}