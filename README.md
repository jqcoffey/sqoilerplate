sqoilerplate
============

Sqoilerplate saves you from writing redundant code to use RDBMs.  It's built on top of scalikejdbc and is explicitly not an ORM (it does not manage relations between objects!).

This is barebones and probably has bugs.  You've been warned.

Setup a class:
```
case class Foo(id: Long, value: String) extends Identifiable

object Foo extends Fetchable[Foo] {
  override protected val tableAliasName = "foo"

  override protected def model2NamedValues(from: Foo) = Seq(
    column.id -> from.id,
    column.value -> from.value
  )

  override protected def resultSet2Model(from: WrappedResultSet) = new Foo(
    from.get(column.id),
    from.get(column.value)
  )

  override protected def createTableSql =
    sqls"create table foo (id bigint, value varchar(128))"
}
```

Ensure the proper config in resources/application.conf:
```
# JDBC settings
db.test.driver="org.h2.Driver"
db.test.url="jdbc:h2:mem:test"
db.test.user="sa"
db.test.password=""

# Connection Pool settings
db.test.poolInitialSize=5
db.test.poolMaxSize=7
db.test.poolConnectionTimeoutMillis=1000
db.test.poolValidationQuery="select 1 as one"
db.test.poolFactoryName="commons-dbcp"
```

And now use it
```
implicit val poolName: 'test

DBs.setup(poolName)

Foo.insert(new Foo(1, "bar"))

val foo = Foo.fetch(1)

foo.value = "baz"

Foo.update(foo)
```

Sqoilerplate is built on top of scalikejdbc and has taken some inspiration from Skinny ORM (thanks fellas).

Links of interest:
* For config details, look here: [http://scalikejdbc.org/documentation/configuration.html](http://scalikejdbc.org/documentation/configuration.html)
* To add your own fetch methods, look here: [http://scalikejdbc.org/documentation/query-dsl.html](http://scalikejdbc.org/documentation/query-dsl.html)
* For another way to avoid writing ORM code, look here: [http://scalikejdbc.org/documentation/reverse-engineering.html](http://scalikejdbc.org/documentation/reverse-engineering.html)
* For a "real" ORM, look here: [http://skinny-framework.org/documentation/orm.html](http://skinny-framework.org/documentation/orm.html)
