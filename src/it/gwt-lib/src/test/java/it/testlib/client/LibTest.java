package it.testlib.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class LibTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "it.testlib.TestLib";
  }

  public void testShouldRun() {
    GWT.log("Hello world!");
  }

  public void testSuperSource() {
    assertTrue(SuperSourced.isSuperSourced());
  }

  public void testProcessed() {
    assertEquals("foo", Processed.create("foo").getProperty());
    assertEquals("bar", Processed2.create("bar").getProperty());
  }
}
