pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        APP_NAME = 'agri-mortgage-loan-system'
        IMAGE_TAG = 'latest'
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
        }

        stage('Package') {
            steps {
                dir('backend') {
                    sh 'mvn -B -DskipTests package'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t agri-mortgage-loan-system:latest -f backend/Dockerfile backend'
            }
        }

        stage('Docker Push') {
            when {
                expression { return env.DOCKER_REGISTRY_URL?.trim() }
            }
            steps {
                sh 'echo "Push to registry is configured via Jenkins credentials in the target environment."'
            }
        }

        stage('Kubernetes Apply') {
            when {
                expression { return env.KUBECONFIG?.trim() }
            }
            steps {
                sh 'kubectl apply -f k8s/'
            }
        }
    }
}
