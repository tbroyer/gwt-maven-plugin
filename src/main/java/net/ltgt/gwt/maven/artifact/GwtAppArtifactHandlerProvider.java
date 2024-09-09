package net.ltgt.gwt.maven.artifact;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.artifact.handler.ArtifactHandler;

@Singleton
@Named("gwt-app")
public class GwtAppArtifactHandlerProvider implements Provider<ArtifactHandler> {
  private final ArtifactHandler artifactHandler = new ArtifactHandler() {
    @Override
    public String getClassifier() {
      return null;
    }

    @Override
    public String getDirectory() {
      return null;
    }

    @Override
    public String getExtension() {
      return "war";
    }

    @Override
    public String getLanguage() {
      return "java";
    }

    @Override
    public String getPackaging() {
      return "gwt-app";
    }

    @Override
    public boolean isAddedToClasspath() {
      return false;
    }

    @Override
    public boolean isIncludesDependencies() {
      return true;
    }
  };

  @Override
  public ArtifactHandler get() {
    return artifactHandler;
  }
}
