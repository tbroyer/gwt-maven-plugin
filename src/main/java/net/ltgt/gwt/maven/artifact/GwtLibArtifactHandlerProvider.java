package net.ltgt.gwt.maven.artifact;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.artifact.handler.ArtifactHandler;

@Singleton
@Named("gwt-lib")
public class GwtLibArtifactHandlerProvider implements Provider<ArtifactHandler> {
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
      return "jar";
    }

    @Override
    public String getLanguage() {
      return "java";
    }

    @Override
    public String getPackaging() {
      return "gwt-lib";
    }

    @Override
    public boolean isAddedToClasspath() {
      return true;
    }

    @Override
    public boolean isIncludesDependencies() {
      return false;
    }
  };

  @Override
  public ArtifactHandler get() {
    return artifactHandler;
  }
}
