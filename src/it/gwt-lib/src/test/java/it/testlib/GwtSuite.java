package it.testlib;

import it.testlib.client.LibTest;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

public class GwtSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for lib");
    suite.addTestSuite(LibTest.class);
    return suite;
  }
}