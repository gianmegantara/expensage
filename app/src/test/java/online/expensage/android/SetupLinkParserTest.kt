package online.expensage.android

import online.expensage.android.data.SetupLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupLinkParserTest {
  @Test
  fun parsesValidSetupLink() {
    val parsed = SetupLinkParser.parse(
      "https://expensage.online/android/setup?endpointUrl=https%3A%2F%2Fexpensage.online%2Fapi%2Fexpenses%2Fingest&accessKey=etk_123",
    )

    assertEquals("https://expensage.online/api/expenses/ingest", parsed.endpointUrl)
    assertEquals("etk_123", parsed.accessKey)
  }

  @Test
  fun rejectsLinkWithoutSetupPath() {
    assertThrows(IllegalArgumentException::class.java) {
      SetupLinkParser.parse(
        "https://expensage.online/wrong/path?endpointUrl=https%3A%2F%2Fexpensage.online%2Fapi%2Fexpenses%2Fingest&accessKey=etk_123",
      )
    }
  }

  @Test
  fun rejectsLinkWithoutAccessKey() {
    assertThrows(IllegalArgumentException::class.java) {
      SetupLinkParser.parse(
        "https://expensage.online/android/setup?endpointUrl=https%3A%2F%2Fexpensage.online%2Fapi%2Fexpenses%2Fingest",
      )
    }
  }
}
