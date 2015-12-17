package it.test.client;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.GwtCompatible;

@AutoValue
@GwtCompatible(serializable = true)
public abstract class Processed2 {
  public static Processed2 create(String property) {
    return new AutoValue_Processed2(property);
  }

  public abstract String getProperty();
}
