package sqoilerplate

import scalikejdbc._

/**
 * A class allowing objects to be mapped to a SQL DB.
 *
 * Created by j.coffey on 26/09/14.
 */
abstract class SQLMappable[T](val tableAliasName: String)(implicit val poolName: Symbol) extends SQLSyntaxSupport[T] {

  val tableAlias = syntax(tableAliasName)

  def resultSet2Model: WrappedResultSet => T

  def model2NamedValues: T => Seq[(SQLSyntax, Any)]

  override def connectionPoolName = poolName

  /**
   * SQL for generating the table representing this model.
   * @return
   */
  protected def createTableSql: SQLSyntax

  /**
   * Creates the table as defined by the [[SQLMappable.createTableSql]] method.
   * @return
   */
  def createTable(implicit session: DBSession = NamedAutoSession(connectionPoolName)) = {
    sql"$createTableSql".execute().apply()
  }

  /**
   * Inserts the object into the database.
   * @param obj the object to insert
   */
  def insert(obj: T)(implicit session: DBSession = NamedAutoSession(connectionPoolName)): Unit =
    insertSQL(obj).toSQL.executeUpdate().apply()

  /**
   * Fetches all objects in the table in whichever order the DB sees fit.
   * @return A list of objects.
   */
  def fetchAll(implicit session: DBSession = NamedAutoSession(connectionPoolName)) = {
    fetchSQLRoot.toSQL.map(rs => resultSet2Model(rs)).list().apply()
  }

  /**
   * Provides the count of objects in the table.
   * @return
   */
  def count(implicit session: DBSession = NamedAutoSession(connectionPoolName)) = {
    countSQLRoot.toSQL.map(rs => rs.long(1)).single().apply().get
  }

  /**
   * An insert fragment requiring a session.
   * @param obj the object to insert
   * @return
   */
  private[sqoilerplate] def insertSQL(obj: T): InsertSQLBuilder = scalikejdbc.insert.into(this).namedValues(model2NamedValues(obj): _*)

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
 */
trait FetchableEntities[ID, T <: Identifiable[ID]] { self: SQLMappable[T] =>

  /**
   * Fetches an object with the supplied key.
   * @param id the object's id
   * @return
   */
  def fetch(id: ID)(implicit session: DBSession = NamedAutoSession(connectionPoolName)): Option[T] = {
    fetchSQLRoot.where.eq(column.id, id).toSQL.map(rs => resultSet2Model(rs)).single().apply()
  }

  /**
   * Determines the existence of an object in the table.
   * @param id the object's id
   * @return
   */
  def exists(id: Long)(implicit session: DBSession = NamedAutoSession(connectionPoolName)) = {
    countSQLRoot.where.eq(column.id, id).toSQL
      .map(rs => rs.long(1))
      .single().apply().get > 0
  }
}

/**
 * For use with tables that generate their own keys.
 * 
 * NOTE: key types must be [[Long]]s
 * 
 * @tparam T the model's type
 */
trait AutoGeneratingIds[T <: Identifiable[Long]] { self: SQLMappable[T] =>

  /**
   * An insert that will return a valid key for the inserted object.
   * 
   * @param obj the object to insert
   * @return the key for use in later fetching
   */
  def insertAndReturnId(obj: T)(implicit session: DBSession = NamedAutoSession(connectionPoolName)): Long = {
    insertSQL(obj).toSQL.updateAndReturnGeneratedKey().apply()
  }
}

trait Identifiable[K] {
  def id: K
}