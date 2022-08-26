package info.nukoneko.kidspos.common

fun String.toAllEm(): String {
    return map { it.toEm() }.joinToString("")
}
