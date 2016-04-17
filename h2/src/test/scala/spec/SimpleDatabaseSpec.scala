//package spec
//
//import org.scalarelational._
//import org.scalarelational.column.Column
//import org.scalarelational.h2.H2Database
//import org.scalarelational.h2.column.types.{IntType, VarCharType}
//import org.scalarelational.table.Table
//import org.scalatest.{Matchers, WordSpec}
//
//class SimpleDatabaseSpec extends WordSpec with Matchers {
//  "Simple Database" should {
//    val db = SimpleDatabase
//
//    "create the tables" in {
//      db.withSession { implicit session =>
//        db.createTables().exec()
//      }
//    }
//    "insert a record" in {
//      db.withSession { implicit session =>
//        db.insert(db.animals.name("Dog")).exec()
//      }
//    }
//    "query back a record" when {
//      "as a tuple" in {
//        db.withSession { implicit session =>
//          val q = select(db.animals.*) from db.animals
//          val rs = q.exec()
//          rs.head should equal((1, "Dog"))
//          rs.tail should equal(Nil)
//        }
//      }
//      "as metadata" in {
//        db.withSession { implicit session =>
//          val q = select(db.animals.*) from db.animals
//          val rsm = q.exec().meta
//          val row = rsm.head
//          row(db.animals.id) should equal(1)
//          row(db.animals.name) should equal("Dog")
//          rsm.tail should equal(Nil)
//        }
//      }
//      "mapping" in {
//        db.withSession { implicit session =>
//          val q = select(db.animals.*) from db.animals
//          val rs = q.exec().map(Animal)
//          val row = rs.head
//          row should equal(Animal(1, "Dog"))
//          rs.tail should equal(Nil)
//        }
//      }
//    }
//  }
//}
//
//object SimpleDatabase extends H2Database {
//  val animals: Animals = table[Animals]("animals")
//}
//
//trait Animals extends Table {
//  val id: Column[Int] = IntType.primaryKey.autoIncrement
//  val name: Column[String] = VarCharType.unique.ignoreCase
//}
//
//case class Animal(id: Int, name: String)