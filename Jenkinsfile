pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'ACTION',
            choices: ['Deploy', 'Revert'],
            description: 'Deploy new build or Revert to previous deployment'
        )
        string(
            name: 'BRANCH',
            defaultValue: 'main',
            description: 'Only for Deploy: branch to build (e.g. main, develop). Ignored when Revert is selected — leave as is.',
            trim: true
        )
    }

    environment {
        DEPLOY_DIR = '/var/www/build'
        PREV_DIR = '/var/www/build/previous'
        PREV_JAR = '/var/www/build/previous/previous.jar'
        SERVICE_NAME = 'tinyurl-backend.service'
    }

    stages {
        stage('Validate') {
            when {
                expression { params.ACTION == 'Deploy' }
            }
            steps {
                script {
                    def branch = (params.BRANCH ?: '').trim()
                    if (!branch) {
                        error "BRANCH cannot be empty. Please specify a branch (e.g. main, develop)."
                    }
                    if (!branch.matches(/^[a-zA-Z0-9\/_.-]+$/)) {
                        error "BRANCH contains invalid characters: ${branch}"
                    }
                }
            }
        }

        stage('Clone') {
            when {
                expression { params.ACTION == 'Deploy' }
            }
            steps {
                script {
                    def branch = (params.BRANCH ?: 'main').trim()
                    git url: 'https://github.com/vickyjsr/backend-tiny-url.git',
                        branch: branch,
                        credentialsId: env.GIT_CREDENTIALS_ID ?: ''
                }
            }
        }

        stage('Build') {
            when {
                expression { params.ACTION == 'Deploy' }
            }
            steps {
                sh './gradlew clean bootJar --no-daemon -x test'
                sh '''
                    set -e
                    if ! ls build/libs/*.jar 1>/dev/null 2>&1; then
                        echo "ERROR: Build did not produce any JAR in build/libs/"
                        exit 1
                    fi
                    echo "JAR(s) found in build/libs/"
                '''
                echo "Build successful (branch: ${params.BRANCH})."
            }
        }

        stage('Deploy') {
            when {
                expression { params.ACTION == 'Deploy' }
            }
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
                echo "Deploying branch: ${params.BRANCH} to ${env.DEPLOY_DIR}"
                sh """
                    set -e
                    mkdir -p '${env.DEPLOY_DIR}' '${env.PREV_DIR}'
                    # Backup current JAR before overwriting (for Revert)
                    CURRENT_JAR=\$(ls -1 ${env.DEPLOY_DIR}/*.jar 2>/dev/null | head -1)
                    if [ -n "\$CURRENT_JAR" ] && [ -f "\$CURRENT_JAR" ]; then
                        cp "\$CURRENT_JAR" '${env.PREV_JAR}'
                        echo "Backed up previous JAR for revert."
                    fi
                    # Deploy new JAR(s)
                    cp build/libs/*.jar '${env.DEPLOY_DIR}/'
                    echo "Deployed \$(ls ${env.DEPLOY_DIR}/*.jar) to ${env.DEPLOY_DIR}"
                """
                sh "sudo systemctl restart ${env.SERVICE_NAME}"
                sh """
                    set -e
                    echo "Waiting for service to become active (up to 60s)..."
                    for i in \$(seq 1 12); do
                        if sudo systemctl is-active --quiet ${env.SERVICE_NAME}; then
                            echo "Service is active after \$((i*5))s."
                            break
                        fi
                        [ \$i -eq 12 ] && { echo "ERROR: Service did not become active within 60s."; exit 1; }
                        sleep 5
                    done
                    echo "Soak check: waiting 45s to ensure service stays up..."
                    sleep 45
                    if sudo systemctl is-active --quiet ${env.SERVICE_NAME}; then
                        echo "Service still running after soak. Deploy verified."
                    else
                        echo "ERROR: Service died during soak period (e.g. crash after 30-60s)."
                        exit 1
                    fi
                """
                echo "Deploy complete. Service restarted and verified."
            }
        }

        stage('Revert') {
            when {
                expression { params.ACTION == 'Revert' }
            }
            steps {
                echo "Reverting to previous deployment from ${env.PREV_DIR}"
                sh """
                    set -e
                    if [ ! -f '${env.PREV_JAR}' ]; then
                        echo "ERROR: No previous JAR found at ${env.PREV_JAR}. Run a Deploy first to create a backup."
                        exit 1
                    fi
                    mkdir -p '${env.DEPLOY_DIR}'
                    # Remove current JAR(s) and restore previous
                    rm -f ${env.DEPLOY_DIR}/*.jar
                    cp '${env.PREV_JAR}' '${env.DEPLOY_DIR}/'
                    echo "Restored previous JAR to ${env.DEPLOY_DIR}"
                """
                sh "sudo systemctl restart ${env.SERVICE_NAME}"
                sh """
                    set -e
                    echo "Waiting for service to become active (up to 60s)..."
                    for i in \$(seq 1 12); do
                        if sudo systemctl is-active --quiet ${env.SERVICE_NAME}; then
                            echo "Service is active after \$((i*5))s."
                            break
                        fi
                        [ \$i -eq 12 ] && { echo "ERROR: Service did not become active within 60s."; exit 1; }
                        sleep 5
                    done
                    echo "Soak check: waiting 45s to ensure service stays up..."
                    sleep 45
                    if sudo systemctl is-active --quiet ${env.SERVICE_NAME}; then
                        echo "Service still running after soak. Revert verified."
                    else
                        echo "ERROR: Service died during soak period."
                        exit 1
                    fi
                """
                echo "Revert complete. Service verified."
            }
        }
    }

    post {
        success {
            echo "Pipeline finished successfully."
        }
        failure {
            echo "Pipeline failed. Check the stage logs above."
        }
    }
}
