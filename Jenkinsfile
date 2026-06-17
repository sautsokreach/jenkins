pipeline {
    agent any

    parameters {
        choice(
            name: 'DEPLOY_MODE',
            choices: ['blue-green', 'canary', 'rollback'],
            description: 'blue-green: full cutover | canary: 10% traffic then manual approval | rollback: revert to previous slot'
        )
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    environment {
        NEXUS_URL  = 'http://nexus:8081'
        BLUE_PORT  = '8090'
        GREEN_PORT = '8091'
        SLOT_FILE  = '/var/jenkins_home/active_slot'
    }

    stages {
        stage('Checkout') {
            when { expression { params.DEPLOY_MODE != 'rollback' } }
            steps {
                checkout scm
            }
        }

        stage('Build') {
            when { expression { params.DEPLOY_MODE != 'rollback' } }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            when { expression { params.DEPLOY_MODE != 'rollback' } }
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
            when { expression { params.DEPLOY_MODE != 'rollback' } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            when { expression { params.DEPLOY_MODE != 'rollback' } }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Publish to Nexus') {
            when { expression { params.DEPLOY_MODE != 'rollback' } }
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
                script {
                    def activeSlot   = sh(script: "cat ${SLOT_FILE} 2>/dev/null || echo 'blue'", returnStdout: true).trim()
                    def inactiveSlot = activeSlot == 'blue' ? 'green' : 'blue'
                    def activePort   = activeSlot   == 'blue' ? env.BLUE_PORT : env.GREEN_PORT
                    def inactivePort = inactiveSlot == 'blue' ? env.BLUE_PORT : env.GREEN_PORT

                    if (params.DEPLOY_MODE == 'rollback') {
                        // inactive slot holds the previous version — switch back to it
                        echo "Rolling back: ${activeSlot} → ${inactiveSlot} (port ${inactivePort})"
                        setNginxFull(inactiveSlot, inactivePort)
                        sh "echo ${inactiveSlot} > ${SLOT_FILE}"
                        echo "Rollback complete. 100% traffic now on ${inactiveSlot}"
                        return
                    }

                    // ── Deploy new version to the inactive slot ──────────────────
                    echo "Deploying to ${inactiveSlot} slot (port ${inactivePort})"

                    sh "pkill -f 'app-${inactiveSlot}\\.jar' || true"
                    sh "sleep 3"
                    sh """
                        cp target/app.jar /home/ubuntu/app-${inactiveSlot}.jar
                        nohup java -jar /home/ubuntu/app-${inactiveSlot}.jar \
                            --server.port=${inactivePort} \
                            > /home/ubuntu/app-${inactiveSlot}.log 2>&1 &
                    """

                    // ── Health check: wait up to 60 s for port to open ──────────
                    echo "Waiting for ${inactiveSlot} to become healthy on port ${inactivePort}..."
                    sh "timeout 60 bash -c 'until (echo > /dev/tcp/localhost/${inactivePort}) 2>/dev/null; do sleep 2; done'"
                    echo "${inactiveSlot} is healthy"

                    if (params.DEPLOY_MODE == 'canary') {
                        // ── Canary: 10% to new slot, 90% stays on active ────────
                        echo "Canary: 10% → ${inactiveSlot}, 90% → ${activeSlot}"
                        setNginxCanary(activePort, inactivePort)

                        input message: "Canary live: 10% on ${inactiveSlot}. Promote to 100%?",
                              ok: 'Promote'
                    }

                    // ── Full cutover ─────────────────────────────────────────────
                    setNginxFull(inactiveSlot, inactivePort)
                    sh "echo ${inactiveSlot} > ${SLOT_FILE}"
                    echo "100% traffic on ${inactiveSlot}. Slot ${activeSlot} kept running for rollback."
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}

// ── Nginx helpers ─────────────────────────────────────────────────────────────

def setNginxFull(String slot, String port) {
    sh """docker exec nginx sh -c 'printf "upstream app_backend {\\n    server jenkins:${port};\\n}\\n" > /etc/nginx/conf.d/active.conf && nginx -s reload'"""
}

def setNginxCanary(String activePort, String newPort) {
    sh """docker exec nginx sh -c 'printf "upstream app_backend {\\n    server jenkins:${activePort} weight=9;\\n    server jenkins:${newPort} weight=1;\\n}\\n" > /etc/nginx/conf.d/active.conf && nginx -s reload'"""
}
