package org.scalarelational.h2

import java.io.File

import org.scalatest.{Matchers, WordSpec}


class ConnectionModeSpec extends WordSpec with Matchers {
  "H2ConnectionMode" when {
    "creating an embedded connection" should {
      "create a path-only URL" in {
        H2Embedded(new File("/test")).url should equal("jdbc:h2:file:/test")
      }
      "create a URL with one option" in {
        H2Embedded(new File("/test"), AutoServer).url should equal("jdbc:h2:file:/test;AUTO_SERVER=TRUE")
      }
      "create a URL with multiple options" in {
        H2Embedded(new File("/test"), CloseDelay(123), AutoServer, AutoServerPort(456)).url should equal("jdbc:h2:file:/test;DB_CLOSE_DELAY=123;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=456")
      }
    }
  }
}