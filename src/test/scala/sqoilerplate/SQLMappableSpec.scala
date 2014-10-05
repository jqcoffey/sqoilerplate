package sqoilerplate

import org.scalatest._
import scalikejdbc._
import scalikejdbc.config.DBs

/**
 * Created by j.coffey on 29/09/14.
 */
class SQLMappableSpec extends FlatSpec {

  // change the connection pool to our test pool
  // TODO: test this explicitly!
  implicit val poolName = 'test

  DBs.setup(poolName)

  // TODO: find out why this is not being pulled in from application.conf.
  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)


  "A SQLMappable" should "insert data into a table" in {
    Foo.createTable
    assert(0 == Foo.count)

    Foo.insert(new Foo("bar"))
    assert(1 == Foo.count)

    assert("bar" == Foo.fetchAll(0).value)
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

    val fooId = FooWithAutoId.insert(foo)
    assert(FooWithAutoId.exists(fooId.get))
    assert("bar" == FooWithAutoId.fetch(fooId.get).get.value)
  }

  case class Foo(value: String)

  object Foo extends SQLMappable[Foo] {
    override protected val tableAliasName = "foo"

    override protected def createTableSql =
      sqls"create table foo (value varchar(128))"

    override protected def model2NamedValues(from: Foo) = Seq(column.value -> from.value)

    override protected def resultSet2Model(from: WrappedResultSet) = new Foo(from.get(column.value))
  }

  case class FooWithId(id: Long, value: String) extends Identifiable

  object FooWithId extends Fetchable[FooWithId] {
    override protected val tableAliasName: String = "foo_with_id"

    override protected def model2NamedValues(from: FooWithId) = Seq(
      column.id -> from.id,
      column.value -> from.value
    )

    override protected def resultSet2Model(from: WrappedResultSet) = new FooWithId(
      from.get(column.id),
      from.get(column.value)
    )

    override protected def createTableSql =
      sqls"create table foo_with_id (id bigint, value varchar(128))"
  }

  case class FooWithAutoId(id: Long = 0, value: String) extends Identifiable

  object FooWithAutoId extends Fetchable[FooWithAutoId] with AutoGeneratingIds[FooWithAutoId] {
    override protected val tableAliasName = "foo_with_auto_id"

    override protected def model2NamedValues(from: FooWithAutoId) = Seq(
      // id column is excluded since it is auto-generated
      column.value -> from.value
    )

    override protected def resultSet2Model(from: WrappedResultSet) = new FooWithAutoId(
      from.get(column.id),
      from.get(column.value)
    )

    override protected def createTableSql =
      sqls"create table foo_with_auto_id (id bigint not null primary key auto_increment, value varchar(128))"
  }
}
