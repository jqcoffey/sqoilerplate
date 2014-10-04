package sqoilerplate

import scalikejdbc._

/**
 * A class allowing objects to be mapped to a SQL DB.
 *
 * NOTE: Session handling is very basic (ie non-existent)!  You've been warned!
 *
 * Created by j.coffey on 26/09/14.
 */
abstract class SQLMappable[T](implicit val poolName: Symbol = 'default) extends SQLSyntaxSupport[T] {

  protected val tableAliasName: String

  protected def createTableSql: SQLSyntax

  protected def resultSet2Model(from: WrappedResultSet): T

  protected def model2NamedValues(from: T): Seq[(SQLSyntax, Any)]

  /**
   * Creates the table as defined by the [[SQLMappable.createTableSql]] method.
   * @return
   */
  def createTable = getDB localTx { implicit session =>
    sql"${createTableSql}".execute().apply()
  }

  /**
   * Inserts the object into the database.
   * @param obj
   * @return always None
   */
  def insert(obj: T): Option[Long] = getDB localTx { implicit session =>
    insertSQL(obj).toSQL.executeUpdate.apply()
    None
  }

  /**
   * Fetches all objects in the table in whichever order the DB sees fit.
   * @return A list of objects.
   */
  def fetchAll = getDB localTx { implicit session =>
    fetchSQLRoot.toSQL.map(rs => resultSet2Model(rs)).list.apply()
  }

  /**
   * Provides the count of objects in the table.
   * @return
   */
  def count: Long = getDB readOnly { implicit session =>
    countSQLRoot.toSQL.map(rs => rs.long(1)).single.apply().get
  }

  override def connectionPoolName = poolName

  protected def getDB = NamedDB(connectionPoolName)

  protected lazy val tableAlias = syntax(tableAliasName)

  /**
   * An insert fragment requiring a session.
   * @param obj
   * @return
   */
  protected def insertSQL(obj: T) = scalikejdbc.insert.into(this).namedValues(model2NamedValues(obj): _*)

  /**
   * A select * fragment requiring a session to which an optional predicate or ordering can be added.
   * @return
   */
  protected def fetchSQLRoot = select.from(this as tableAlias)

  /**
   * A select count(1) fragment requiring a session to which an optional predicate can be added.
   * @return
   */
  protected def countSQLRoot = select(sqls.count).from(this as tableAlias)
}

/**
 * A trait that allows retrieval of specific objects (eg via their primary key).
 *
 * NOTE: keys must be of type Long.
 *
 * @tparam T
 */
trait Fetchable[T <: Identifiable] extends SQLMappable[T] {

  /**
   * Fetches an object with the supplied id.
   * @param id
   * @return
   */
  def fetch(id: Long): Option[T] = getDB readOnly { implicit session =>
    fetchSQLRoot.where.eq(column.id, id).toSQL.map(rs => resultSet2Model(rs)).single.apply()
  }

  /**
   * Determines the existence of an object in the table.
   * @param id
   * @return
   */
  def exists(id: Long): Boolean = getDB readOnly { implicit session =>
    countSQLRoot.where.eq(column.id, id).toSQL
      .map(rs => rs.long(1))
      .single.apply().get > 0
  }
}

/**
 * For use with tables that generate their own keys.
 * @tparam T
 */
trait AutoGeneratingIds[T <: Identifiable] extends SQLMappable[T] {

  /**
   * An insert that should return a valid key for the inserted object.
   * @param obj
   * @return the key for use in later fetching
   */
  override def insert(obj: T) = getDB localTx {
    implicit session => Some(
      insertSQL(obj).toSQL.updateAndReturnGeneratedKey.apply()
    )
  }
}

trait Identifiable {
  def id: Long
}