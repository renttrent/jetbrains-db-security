package com.github.renttrent.jetbrainsdbsecurity.services

import net.sf.jsqlparser.parser.CCJSqlParserUtil

class SQLParserUtil {
    fun isValid(sql: String): Boolean {
        return try {
            val parsed = CCJSqlParserUtil.parse(sql)
            true
        } catch (e: Exception) {
            false
        }
    }
}