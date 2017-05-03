package it.test.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class Test implements EntryPoint {

  interface Binder extends UiBinder<Widget, Test> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @Override
  public void onModuleLoad() {
    RootPanel.get().add(BINDER.createAndBindUi(this));
  }
}
