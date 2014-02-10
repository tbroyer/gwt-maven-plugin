package it.testlib;

public class Other implements Runnable {
  private final TestLib testLib;
  private final StringBuilder sb;

  public Other(TestLib testLib, StringBuilder sb) {
    this.testLib = testLib;
    this.sb = sb;
  }

  @Override
  public void run() {
    testLib.doSomeThing(sb);
  }
}