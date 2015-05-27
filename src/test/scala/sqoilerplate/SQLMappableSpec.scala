package sqoilerplate

import org.scalatest._
import scalikejdbc._
import scalikejdbc.config.DBs

object DBSettings {
  // change the connection pool to our test pool
  implicit val poolName = 'test

  DBs.setup(poolName)
}

/**
 * Specs for SQLMappable.
 */
class SQLMappableSpec extends FlatSpec {

  // TODO: find out why this is not being pulled in from application.conf.
  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)

  "A SQLMappable" should "insert data into a table" in {
    Foo.createTable
    assert(0 == Foo.count)

    Foo.insert(new Foo("bar"))
    assert(1 == Foo.count)

    assert("bar" == Foo.fetchAll().head.value)
  }

  "A Fetchable SQLMappable" should "be able to be fetched by its id" in {
    FooWithId.createTable
    assert(0 == FooWithId.count)

    FooWithId.insert(new FooWithId(1, "bar"))
    assert(1 == FooWithId.count)

    assert("bar" == FooWithId.fetch(1).get.value)
  }

  "A Mappable with AutoGeneratingIds" should "provide an id with which the inserted object can be retrieved" in {
    FooWithAutoId.createTable
    assert(0 == FooWithAutoId.count)

    val foo = new FooWithAutoId(value = "bar")
    assert(0 == foo.id)

    val fooId = FooWithAutoId.insertAndReturnId(foo)
    assert(FooWithAutoId.exists(fooId))
    assert("bar" == FooWithAutoId.fetch(fooId).get.value)
  }

}

import DBSettings._

/**
 * For testing mapping of non-identifiable objects.
 *
 * @param value
 */
case class Foo(value: String)

object Foo extends SQLMappable[Foo](tableAliasName = "foo") {
  override protected def createTableSql = sqls"create table foo (value varchar(128))"

  override def model2NamedValues = { from: Foo => Seq(column.value -> from.value) }

  override def resultSet2Model = { from: WrappedResultSet => new Foo(from.get(column.value)) }
}

/**
 * For testing the insertion of identifiable and thus fetchable objects.
 *
 * @param id
 * @param value
 */
case class FooWithId(id: Long, value: String) extends Identifiable[Long]

object FooWithId extends SQLMappable[FooWithId](tableAliasName = "foo_with_id") with FetchableEntities[Long, FooWithId] {

  implicit def model2NamedValues = { from: FooWithId =>
    Seq(column.id -> from.id, column.value -> from.value)
  }

  implicit def resultSet2Model = { from: WrappedResultSet =>
    new FooWithId(from.get(column.id), from.get(column.value))
  }

  override protected def createTableSql =
    sqls"create table foo_with_id (id bigint, value varchar(128))"

}

/**
 * For testing the insertion of identifiable and fetchable objects that produce their own ids.
 *
 * @param id
 * @param value
 */
case class FooWithAutoId(id: Long = 0, value: String) extends Identifiable[Long]

object FooWithAutoId extends SQLMappable[FooWithAutoId](tableAliasName = "foo_with_auto_id")
with FetchableEntities[Long, FooWithAutoId] with AutoGeneratingIds[FooWithAutoId] {

  override def model2NamedValues = { from: FooWithAutoId =>
    Seq(
      // id column is excluded since it is auto-generated
      column.value -> from.value
    )
  }

  override def resultSet2Model = { from: WrappedResultSet =>
    new FooWithAutoId(from.get(column.id), from.get(column.value))
  }

  override protected def createTableSql =
    sqls"create table foo_with_auto_id (id bigint not null primary key auto_increment, value varchar(128))"
}
