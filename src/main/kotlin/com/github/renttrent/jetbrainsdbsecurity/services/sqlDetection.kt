package com.github.renttrent.jetbrainsdbsecurity.services

enum class WarningSeverity {
    OK,
    WEAK,
    STRONG
}

fun isValidSqlNaive(input: String): Boolean {
    val regxOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    val sqlPattern = """(WITH\s+\w+\s+AS\s+\((?:[^)(]+|\((?:[^)(]+|\([^)(]*\))*\))*\)\s*,?\s*)*(SELECT\s+.+?\s+FROM\s+[\w\.]+(?:\s*,\s*[\w\.]+)*(?:\s+WHERE\s+.+?)?|INSERT\s+INTO\s+[\w\.]+\s*\(.+?\)\s*VALUES\s*\(.+?\)|UPDATE\s+[\w\.]+\s+SET\s+.+?(?:\s+WHERE\s+.+?)?|DELETE\s+FROM\s+[\w\.]+\s+WHERE\s+.+?)""".toRegex(regxOptions)

    return sqlPattern.matches(input.trim())
}

fun isSqlVulnerable(input: String): WarningSeverity {
    val regxOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    val valuePattern = """((<)|(>)|(<=)|(>=)|(=)|(==)|(!=)|(<>)|(IS)|(IS NOT)|(I?LIKE)|(~))\s+'?(%|(\{\w+\}))""".toRegex(regxOptions)
    val bracesPattern = """(?<!\\)\{.*?\}""".toRegex(regxOptions)

    if (valuePattern.find(input.trim()) != null) return WarningSeverity.STRONG
    if (bracesPattern.find(input.trim()) != null) return WarningSeverity.WEAK
    return WarningSeverity.OK
}

