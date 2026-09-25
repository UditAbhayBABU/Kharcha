package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UdhaarParty
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KHARCHA", appName)
  }

  @Test
  fun `udhaar party outstanding balance calculated correctly`() {
    val party = UdhaarParty(
      id = "p1",
      userId = "u1",
      name = "Ramesh",
      totalGiven = 5000.0,
      totalReceived = 2000.0
    )
    assertEquals(3000.0, party.outstandingBalance, 0.001)
  }
}
