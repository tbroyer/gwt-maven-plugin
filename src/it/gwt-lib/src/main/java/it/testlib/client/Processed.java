package it.testlib.client;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.GwtCompatible;

@AutoValue
@GwtCompatible(serializable = true)
public abstract class Processed {
  public static Processed create(String property) {
    return new AutoValue_Processed(property);
  }

  public abstract String getProperty();
}
