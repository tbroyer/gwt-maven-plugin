                                                                 JenkinsFile                                                                       Modified  
pipeline {

  agent any

  options {

    buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '5', daysToKeepStr: '', numToKeepStr: '5')

  }

  stages {

    stage('Print Java') {

      steps {

        sh '''
               java -version

          '''
}
}
stage('cat README') {
 when {
branch "branch1"
}
steps {
 sh '''
cat README.md
'''

}
}
}
}
