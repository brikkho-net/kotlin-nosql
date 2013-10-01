package kotlin.sql

import java.util.ArrayList

open class Column<C, T: Table>(val table: T, val name: String, val columnType: ColumnType, val _nullable: Boolean, val length: Int) : Field<C>() {
    fun equals(other: Expression): Op {
        return EqualsOp(this, other)
    }

    fun equals(other: C): Op {
        return EqualsOp(this, LiteralOp(other))
    }

    fun like(other: String): Op {
        return LikeOp(this, LiteralOp(other))
    }

    override fun toSQL(): String {
        return Session.get().fullIdentity(this);
    }

    fun toString(): String {
        return "${table.tableName}.$name"
    }

    fun foreignKey(reference: Column<C, *>) : FKColumn<C, T> {
        (table.tableColumns as ArrayList<Column<*, T>>).remove(this)
        val column = FKColumn<C, T>(table, name, columnType, length, reference)
        (table.tableColumns as ArrayList<Column<*, T>>).add(column)
        return column
    }

    fun primaryKey(): PKColumn<C, T> {
        (table.tableColumns as ArrayList<Column<*, T>>).remove(this)
        val column = PKColumn<C, T>(table, name, columnType, length)
        (table.tableColumns as ArrayList<Column<*, T>>).add(column)
        (table.primaryKeys as ArrayList<PKColumn<*, T>>).add(column)
        return column
    }

    fun <C2> plus(c: Column<C2, T>): Template2<T, C, C2> {
        return Template2(table, this, c) as Template2<T, C, C2>
    }

    fun <A1, A2, T2: Table> plus(template: FKTemplate<T, A1, T2, A2>): TemplateFKTemplate<T, C, A1, T2, A2> {
        return TemplateFKTemplate<T, C, A1, T2, A2>(table, this, template.c1, template.t2, template.c2) as TemplateFKTemplate<T, C, A1, T2, A2>
    }

    fun <A1, A2, T2: Table> join(column: Column<A2, T2>, on: FKColumn<A1, T>): TemplateFKTemplateL<T, C, A1?, T2, A2?> {
        return TemplateFKTemplateL<T, C, A1?, T2, A2?>(table, this, on as FKColumn<A1?, T>, column.table, column as Column<A2?, T2>) as TemplateFKTemplateL<T, C, A1?, T2, A2?>
    }

    fun <A1, A2, T2: Table> leftJoin(template: FKTemplate<T, A1, T2, A2>): TemplateFKTemplateL<T, C, A1?, T2, A2?> {
        return TemplateFKTemplateL<T, C, A1?, T2, A2?>(table, this, template.c1 as FKColumn<A1?, T>, template.t2, template.c2 as Column<A2?, T2>) as TemplateFKTemplateL<T, C, A1?, T2, A2?>
    }
}

class TemplateFKTemplate<T1: Table, A1, B1, T2: Table, A2>(val t1: T1, val a1: Column<A1, T1>, val b1: Column<B1, T1>, val t2: T2, val a2: Column<A2, T2>) {
    fun forEach(statement: (row: Pair<A1, A2>) -> Unit) {
        Query<Pair<A1, A2>>(Session.get(), array(a1, a2)).from(t1).join(t2).forEach(statement)
    }
}

class TemplateFKTemplateL<T1: Table, A1, B1, T2: Table, A2>(val t1: T1, val a1: Column<A1, T1>, val b1: FKColumn<B1, T1>, val t2: T2, val a2: Column<A2, T2>) {
    fun forEach(statement: (row: Pair<A1, A2>) -> Unit) {
        Query<Pair<A1, A2>>(Session.get(), array(a1, a2)).from(t1).leftJoin(b1).forEach(statement)
    }
}

fun <T: Table, C, T2: Table, C2> Column<C, T>.times(c2: Column<C2, T2>): Template1t1<T, C, T2, C2> {
    return Template1t1<T, C, T2, C2>(table, this, c2.table, c2)
}


fun <C, T : Table> Column<C, T>.nullable(): Column<C?, T> {
    (table.tableColumns as ArrayList<Column<*, T>>).remove(this)
    val column = (Column<C?, T>(table, name, columnType, true, length)) as Column<C?, T>
    (table.tableColumns as ArrayList<Column<*, T>>).add(column)
    return column
}

fun <C, T : Table> FKColumn<C, T>.nullable(): FKColumn<C?, T> {
    (table.tableColumns as ArrayList<Column<*, T>>).remove(this)
    val column = FKColumn<C, T>(table, name, columnType, length, reference) as FKColumn<C?, T>
    (table.tableColumns as ArrayList<Column<*, T>>).add(column)
    return column
}

val Column<*, *>.isNull: Op
    get() {
        return IsNullOp(this)
    }

fun Column<*, *>.equals(other: Expression): Op {
    return EqualsOp(this, other)
}

open class PKColumn<C, T: Table>(table: T, name: String, columnType: ColumnType, length: Int) : Column<C, T>(table, name, columnType, false, length) {
}

fun <T:Table> PKColumn<Int, T>.auto(): GeneratedPKColumn<Int, T> {
    (table.tableColumns as ArrayList<Column<*, T>>).remove(this)
    (table.primaryKeys as ArrayList<PKColumn<*, T>>).remove(this)
    val column = GeneratedPKColumn<Int, T>(table, name, columnType, length)
    (table.tableColumns as ArrayList<Column<*, T>>).add(column)
    (table.primaryKeys as ArrayList<PKColumn<*, T>>).add(column)
    return column
}

fun <T:Table> Column<Int, T>.auto(): GeneratedColumn<Int, T> {
    (table.tableColumns as ArrayList<Column<*, T>>).remove(this)
    (table.primaryKeys as ArrayList<PKColumn<*, T>>).remove(this)
    val column = GeneratedColumn<Int, T>(table, name, columnType, length)
    (table.tableColumns as ArrayList<Column<*, T>>).add(column)
    (table.primaryKeys as ArrayList<Column<*, T>>).add(column)
    return column
}

open class FKColumn<C, T: Table>(table: T, name: String, columnType: ColumnType, length: Int, val reference: Column<C, *>) : Column<C, T>(table, name, columnType, true, length) {
    fun <T2: Table, A2, B2, C2> times(template: Template3<T2, A2, B2, C2>): FKTemplate3<T, C, T2, A2, B2, C2> {
        return FKTemplate3(table, this, template.table, template.a, template.b, template.c) as FKTemplate3<T, C, T2, A2, B2, C2>
    }

    fun <T2: Table, A2, B2> times(template: Template2<T2, A2, B2>): FKTemplate2<T, C, T2, A2, B2> {
        return FKTemplate2(table, this, template.table, template.a, template.b) as FKTemplate2<T, C, T2, A2, B2>
    }
}

fun <T: Table, C, T2: Table, C2> FKColumn<C, T>.times(c: Column<C2, T2>): FKTemplate<T, C, T2, C2> {
    return FKTemplate<T, C, T2, C2>(table, this, c.table, c) as FKTemplate<T, C, T2, C2>
}

class FKTemplate<T1: Table, C1, T2: Table, C2>(val t1: T1, val c1: FKColumn<C1, T1>, val t2: T2, val c2: Column<C2, T2>) {
}

class GeneratedPKColumn<C, T: Table>(table: T, name: String, columnType: ColumnType, length: Int) : PKColumn<C, T>(table, name, columnType, length), GeneratedValue<C> {
}

class GeneratedColumn<C, T: Table>(table: T, name: String, columnType: ColumnType, length: Int) : Column<C, T>(table, name, columnType, false, length), GeneratedValue<C> {
}