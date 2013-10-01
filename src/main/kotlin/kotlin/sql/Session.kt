package kotlin.sql

import java.sql.Connection
import org.h2.engine.Session
import java.sql.Driver
import java.util.regex.Pattern
import java.sql.Statement
import java.sql.ResultSet

open class Session (val connection: Connection, val driver: Driver) {
    val identityQuoteString = connection.getMetaData()!!.getIdentifierQuoteString()!!
    val extraNameCharacters = connection.getMetaData()!!.getExtraNameCharacters()!!
    val identifierPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$")

    fun query(sql: String): PlainQuery {
        return PlainQuery(Session.get(), sql)
    }

    fun <A> select(a: Field<A>): Query<A> {
        return Query(this, array(a))
    }

    fun count(column: Column<*, *>): Count {
        return Count(column)
    }

    fun <A, B> select(a: Field<A>, b: Field<B>): Query<Pair<A, B>> {
        return Query(this, array(a, b))
    }

    fun <A, B, C> select(a: Field<A>, b: Field<B>, c: Field<C>): Query<Triple<A, B, C>> {
        return Query<Triple<A, B, C>>(this, array(a, b, c))
    }

    /*fun <A, B> select(a: Column2<A, B>): Query<Pair<A, B>> {
        return Query(this, array(a.a, a.b))
    }

    fun <A, B, C> select(a: Column3<A, B, C>): Query<Triple<A, B, C>> {
        return Query(this, array(a.a, a.b, a. c))
    }*/

    fun delete(table: Table): DeleteQuery {
        return DeleteQuery(this, table)
    }

    fun identity(table: Table): String {
        return if (identifierPattern.matcher(table.tableName).matches())
            table.tableName else "$identityQuoteString${table.tableName}$identityQuoteString"
    }

    fun fullIdentity(column: Column<*, *>): String {
        return (if (identifierPattern.matcher(column.table.tableName).matches())
            column.table.tableName else "$identityQuoteString${column.table.tableName}$identityQuoteString") + "." +
        (if (identifierPattern.matcher(column.name).matches())
            column.name else "$identityQuoteString${column.name}$identityQuoteString")
    }

    fun identity(column: Column<*, *>): String {
        return if (identifierPattern.matcher(column.name).matches())
            column.name else "$identityQuoteString${column.name}$identityQuoteString"
    }

    fun foreignKey(foreignKey: FKColumn<*, *>): String {
        return when (driver.getClass().getName()) {
            "com.mysql.jdbc.Driver", "oracle.jdbc.driver.OracleDriver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver", "org.postgresql.Driver",
            "org.h2.Driver" -> {
                "ALTER TABLE ${identity(foreignKey.table)} ADD CONSTRAINT ${identity(foreignKey)} FOREIGN KEY (${identity(foreignKey)}) REFERENCES ${identity(foreignKey.reference.table)}(${identity(foreignKey.table.primaryKeys[0])})"
            }
            else -> throw UnsupportedOperationException("Unsupported driver: " + driver.getClass().getName())
        }
    }

    fun <T: Table> T.insert(columns: T.() -> Array<Pair<Column<*, T>, *>>): InsertQuery<T> {
        return insert(columns() as Array<Pair<Column<*, T>, *>>)
    }

    /*fun <T: Table> T.insert(column: Pair<Column<*, T>, *>): InsertQuery<T> { // TODO
        return insert(array(column) as Array<Pair<Column<*, *>, *>>)
    }*/

    fun <T: Table> T.insert(columns: Array<Pair<Column<*, T>, *>>): InsertQuery<T> {
        val table = columns[0].component1().table
        var sql = StringBuilder("INSERT INTO ${identity(table)}")
        var c = 0
        sql.append(" (")
        for (column in columns) {
            sql.append(identity(column.component1()))
            c++
            if (c < columns.size) {
                sql.append(", ")
            }
        }
        sql.append(") ")
        c = 0
        sql.append("VALUES (")
        for (column in columns) {
            when (column.component1().columnType) {
                ColumnType.STRING -> sql.append("'" + column.component2() + "'")
                else -> sql.append(column.component2())
            }
            c++
            if (c < columns.size) {
                sql.append(", ")
            }
        }
        sql.append(") ")
        println("SQL: " + sql.toString())
        val statement = connection.createStatement()!!
        statement.executeUpdate(sql.toString(), Statement.RETURN_GENERATED_KEYS)
        return InsertQuery(this, statement)
    }


    fun autoIncrement(column: Column<*, *>): String {
        return when (driver.getClass().getName()) {
            "com.mysql.jdbc.Driver", /*"oracle.jdbc.driver.OracleDriver",*/
            "com.microsoft.sqlserver.jdbc.SQLServerDriver", /*"org.postgresql.Driver",*/
            "org.h2.Driver" -> {
                "AUTO_INCREMENT"
            }
            else -> throw UnsupportedOperationException("Unsupported driver: " + driver.getClass().getName())
        }
    }

    class object {
        val threadLocale = ThreadLocal<Session>()

        fun get(): Session {
            return threadLocale.get()!!
        }
    }
}
