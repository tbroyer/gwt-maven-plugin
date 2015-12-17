package it.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class AppTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "it.test.Test";
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
