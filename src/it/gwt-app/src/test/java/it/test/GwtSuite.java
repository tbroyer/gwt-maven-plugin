package it.test;

import it.test.client.AppTest;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

public class GwtSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for app");
    suite.addTestSuite(AppTest.class);
    return suite;
  }
}
