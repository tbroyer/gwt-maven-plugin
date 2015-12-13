package it.testlib.client;

import com.google.gwt.core.client.GWT;

public class SuperSourced {
  public static boolean isSuperSourced() {
    GWT.create(Super.class);
    return true;
  }
}
