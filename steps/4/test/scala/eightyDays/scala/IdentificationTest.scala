package eightyDays.scala

import org.scalatest.WordSpec

class IdentificationTest extends WordSpec {
  "A new identification" must {
    "has a unique number" in {
      val out = Identification()
      assert(out.number.toString.length > 0)
      assert(out.number !== Identification().number)
    }
  }
}