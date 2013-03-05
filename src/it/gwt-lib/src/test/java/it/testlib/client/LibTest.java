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
}