pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        BACKEND_IMAGE = 'agri-mortgage-loan-system'
        FRONTEND_IMAGE = 'agri-mortgage-loan-system-frontend'
        IMAGE_TAG = "${env.BUILD_NUMBER ?: 'latest'}"
        DOCKER_REGISTRY_URL = "${env.DOCKER_REGISTRY_URL ?: ''}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                dir('backend') {
                    sh 'mvn -B test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                dir('backend') {
                    sh 'mvn -B -DskipTests package'
                }
            }
        }

        stage('Frontend E2E') {
            steps {
                withEnv([
                        'CI=true',
                        'AGRI_MORTGAGE_MYSQL_ROOT_PASSWORD=agriRoot#2026',
                        'AGRI_MORTGAGE_DB_PASSWORD=agriDb#2026',
                        'AGRI_MORTGAGE_JWT_SECRET=agri-jwt-secret-for-ci-automation-2026',
                        'AGRI_MORTGAGE_ADMIN_PASSWORD=Admin#Agri2026',
                        'AGRI_MORTGAGE_OFFICER_PASSWORD=Officer#Agri2026',
                        'AGRI_MORTGAGE_REVIEWER_PASSWORD=Reviewer#Agri2026',
                        'AGRI_MORTGAGE_BORROWER_PASSWORD=Borrower#Agri2026',
                        'AGRI_MORTGAGE_FRONTEND_HOST_PORT=4400',
                        'AGRI_MORTGAGE_MYSQL_HOST_PORT=33062'
                ]) {
                    sh '''
                        set -euo pipefail
                        rm -rf frontend/playwright-report frontend/test-results
                        docker compose down -v --remove-orphans || true
                        docker compose up -d --build
                        ready=0
                        for attempt in $(seq 1 30); do
                          if curl -fsS http://127.0.0.1:8011/actuator/health/readiness >/dev/null && curl -fsS http://127.0.0.1:4400/ >/dev/null; then
                            ready=1
                            break
                          fi
                          sleep 5
                        done
                        if [ "$ready" -ne 1 ]; then
                          docker compose logs
                          exit 1
                        fi
                        docker run --rm --add-host=host.docker.internal:host-gateway \
                          -e CI=true \
                          -e PLAYWRIGHT_JUNIT_OUTPUT_NAME=test-results/e2e-results.xml \
                          -e AGRI_E2E_BASE_URL=http://host.docker.internal:4400 \
                          -e AGRI_E2E_API_BASE_URL=http://host.docker.internal:8011 \
                          -e AGRI_E2E_PASSWORD="$AGRI_MORTGAGE_ADMIN_PASSWORD" \
                          -v "$PWD/frontend:/work" \
                          -w /work \
                          mcr.microsoft.com/playwright:v1.59.1-noble \
                          sh -lc "npm ci && npx playwright test tests/golden-path.spec.ts --reporter=line,junit,html"
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'frontend/test-results/e2e-results.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'frontend/playwright-report/**,frontend/test-results/**'
                    sh 'docker compose down -v --remove-orphans || true'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -f backend/Dockerfile backend"
                sh "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -f frontend/Dockerfile frontend"
            }
        }

        stage('Docker Push') {
            when {
                expression { return env.DOCKER_REGISTRY_URL?.trim() }
            }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {
                    sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin ${DOCKER_REGISTRY_URL}"
                    sh "docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:latest"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:latest"
                    sh "docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:latest"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:latest"
                }
            }
        }

        stage('Kubernetes Apply') {
            when {
                expression { return env.KUBECONFIG?.trim() }
            }
            steps {
                sh 'kubectl apply -f k8s/00-namespace.yaml'
                sh 'kubectl apply -f k8s/01-configmap.yaml'
                sh 'kubectl apply -f k8s/02-secret.yaml'
                sh 'kubectl apply -f k8s/03-mysql.yaml'
                sh 'kubectl apply -f k8s/04-backend.yaml'
                sh 'kubectl apply -f k8s/05-frontend.yaml'
                sh 'kubectl apply -f k8s/06-ingress.yaml'
                sh 'kubectl rollout status deployment/agri-mortgage-backend -n agri-mortgage --timeout=180s'
                sh 'kubectl rollout status deployment/agri-mortgage-frontend -n agri-mortgage --timeout=180s'
            }
        }
    }
}
