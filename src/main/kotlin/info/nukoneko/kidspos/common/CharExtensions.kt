package info.nukoneko.kidspos.common

fun Char.toEm(): String {
    return when (this) {
        '0' -> "０"
        '1' -> "１"
        '2' -> "２"
        '3' -> "３"
        '4' -> "４"
        '5' -> "５"
        '6' -> "６"
        '7' -> "７"
        '8' -> "８"
        '9' -> "９"
        '(' -> "（"
        ')' -> "）"
        ' ' -> "　"
        else -> Character.toString(this)
    }
}