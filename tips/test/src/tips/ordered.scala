package tips

import utest._

/**
  * Created by ikhoon on 07/04/2018.
  */
class OrderedTest extends TestSuite {

  // 값을 비교하고 싶다.

  val tests = Tests {

    case class Version(major: Int, minor: Int, patch: Int)
        extends Ordered[Version] {
      def compare(that: Version): Int =
        if (major > that.major) 1
        else if (major == that.major && minor > that.minor) 1
        else if (major == that.major && minor == that.minor && patch > that.patch)
          1
        else if (major == that.major && minor == that.minor && patch == that.patch)
          0
        else -1
    }

    'version_check - {
      assert((Version(1, 1, 1) < Version(1, 1, 1)) == false)
      assert((Version(1, 10, 1) > Version(0, 0, 1)) == true)
      assert((Version(10, 9, 3) <= Version(0, 0, 1)) == false)
      assert((Version(10, 9, 3) >= Version(0, 0, 1)) == true)
    }

  }
}
