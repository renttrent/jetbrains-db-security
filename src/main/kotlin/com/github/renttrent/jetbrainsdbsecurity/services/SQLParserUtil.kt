package com.github.renttrent.jetbrainsdbsecurity.services

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement

class SQLParserUtil {
    fun isValid(sql: String): Statement? {
        return try {
            val parsed = CCJSqlParserUtil.parse(sql)
            parsed
        } catch (e: Exception) {
            null
        }
    }
}