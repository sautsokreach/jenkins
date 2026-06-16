pipeline {
    agent any

    environment {
        NEXUS_URL = 'http://172.31.10.73:8081'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Publish to Nexus') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    script {
                        writeFile file: 'maven-settings.xml', text: """
<settings>
  <servers>
    <server>
      <id>nexus-snapshots</id>
      <username>${env.NEXUS_USER}</username>
      <password>${env.NEXUS_PASS}</password>
    </server>
    <server>
      <id>nexus-releases</id>
      <username>${env.NEXUS_USER}</username>
      <password>${env.NEXUS_PASS}</password>
    </server>
  </servers>
</settings>
"""
                    }
                    sh "mvn deploy -DskipTests -s maven-settings.xml -Denv.NEXUS_URL=${NEXUS_URL}"
                }
            }
            post {
                always {
                    sh 'rm -f maven-settings.xml'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    pkill -f app.jar || true
                    nohup java -jar target/app.jar > /home/ubuntu/app.log 2>&1 &
                    echo "App started"
                '''
            }
        }
    }

    post {
        success {
            echo 'Build and deploy successful!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
