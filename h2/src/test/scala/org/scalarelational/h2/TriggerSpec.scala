package org.scalarelational.h2

import org.scalarelational.column.property.{AutoIncrement, PrimaryKey}
import org.scalarelational.h2.trigger.TriggerType
import org.scalarelational.table.Table
import org.scalatest.{Matchers, WordSpec}


class TriggerSpec extends WordSpec with Matchers {
  var inserted = 0
  var updated = 0
  var deleted = 0
  var selected = 0

  "Triggers" should {
    import TriggerTestDatastore._

    "create tables" in {
      session {
        create(triggerTest)
      }
    }
    "add a trigger" in {
      trigger.on {
        case evt => evt.triggerType match {
          case TriggerType.Insert => inserted += 1
          case TriggerType.Update => updated += 1
          case TriggerType.Delete => deleted += 1
          case TriggerType.Select => selected += 1
        }
      } shouldNot equal(null)
    }
    "validate no trigger has been invoked" in {
      inserted should equal(0)
      updated should equal(0)
      deleted should equal(0)
      selected should equal(0)
    }
    "insert a record to fire a trigger" in {
      session {
        val result = insert(triggerTest.name("Test1")).result
        result should equal (1)
      }
    }
    "validate that one insert was triggered" in {
      inserted should equal(1)
      updated should equal(0)
      deleted should equal(0)
      selected should equal(0)
    }
    "update a record to fire a trigger" in {
      session {
        exec(update(triggerTest.name("Test2")) where triggerTest.id === Some(1)) should equal(1)
      }
    }
    "validate that one update was triggered" in {
      inserted should equal(1)
      updated should equal(1)
      deleted should equal(0)
      selected should equal(0)
    }
    "select a record to fire a select trigger" in {
      session {
        val results = (select(triggerTest.*) from triggerTest).result.toList
        results.size should equal(1)
      }
    }
    "validate that another update was triggered" in {
      inserted should equal(1)
      updated should equal(1)
      deleted should equal(0)
      selected should equal(1)
    }
    "delete a record to fire a trigger" in {
      session {
        exec(delete(triggerTest) where triggerTest.id === Some(1)) should equal(1)
      }
    }
    "validate that one delete was triggered" in {
      inserted should equal(1)
      updated should equal(1)
      deleted should equal(1)
      selected should equal(1)
    }
  }
}

object TriggerTestDatastore extends H2Datastore(mode = H2Memory("trigger_test")) {
  object triggerTest extends Table("trigger_test", Triggers.All) {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
  }
}