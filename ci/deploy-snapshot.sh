if [ "$TRAVIS_REPO_SLUG" == "tbroyer/gwt-maven-plugin" ] && \
   [ "$TRAVIS_JDK_VERSION" == "openjdk11" ] && \
   [ "$GWT_VERSION" == "2.9.0" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then

  mvn -s ci/settings.xml clean source:jar deploy -Dmaven.test.skip=true -Dinvoker.skip=true
fi
