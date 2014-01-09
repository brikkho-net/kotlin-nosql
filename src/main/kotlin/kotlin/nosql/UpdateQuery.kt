package kotlin.nosql

import java.sql.Connection
import java.util.LinkedHashMap

class UpdateQuery<T: AbstractTableSchema>(val table: T, val where: Op) {
    val values = LinkedHashMap<AbstractColumn<*, T, *>, Any>()

    fun <C> set(column: AbstractColumn<C, T, *>, value: C) {
        if (values containsKey column) {
            throw RuntimeException("$column is already initialized")
        }
        values[column] = value
    }
}