package net.ltgt.gwt.maven.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

import javax.inject.Provider;

public abstract class AbstractLifecycleMappingProvider implements Provider<LifecycleMapping> {

  private static final String DEFAULT_LIFECYCLE_KEY = "default";

  private final Lifecycle defaultLifecycle;
  private final LifecycleMapping lifecycleMapping;

  public AbstractLifecycleMappingProvider() {
    this.defaultLifecycle = new Lifecycle();
    this.defaultLifecycle.setId(DEFAULT_LIFECYCLE_KEY);
    this.defaultLifecycle.setLifecyclePhases(loadMapping());

    this.lifecycleMapping = new LifecycleMapping() {
      @Override
      public Map<String, Lifecycle> getLifecycles() {
        return Collections.singletonMap(DEFAULT_LIFECYCLE_KEY, defaultLifecycle);
      }

      @SuppressWarnings("deprecation")
      @Override
      public List<String> getOptionalMojos(String lifecycle) {
        return null;
      }

      @SuppressWarnings("deprecation")
      @Override
      public Map<String, String> getPhases(String lifecycle) {
        if (DEFAULT_LIFECYCLE_KEY.equals(lifecycle)) {
          return defaultLifecycle.getPhases();
        } else {
          return null;
        }
      }
    };
  }

  private Map<String, LifecyclePhase> loadMapping() {
    Properties properties = new Properties();
    try (InputStream inputStream = getClass().getResourceAsStream(getClass().getSimpleName() + ".properties")) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    HashMap<String, LifecyclePhase> result = new HashMap<>();
    for (String phase : properties.stringPropertyNames()) {
      result.put(phase, new LifecyclePhase(properties.getProperty(phase)));
    }
    return result;
  }

  @Override
  public LifecycleMapping get() {
    return lifecycleMapping;
  }
}
