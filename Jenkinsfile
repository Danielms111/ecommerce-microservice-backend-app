pipeline {
    agent any
    
    tools {
        maven 'MVN'
        jdk 'JDK_11'
    }
    
    environment {
        DOCKERHUB_CREDENTIALS = credentials('password')
        ENVIRONMENT = determinateEnvironment()
        IMAGE_TAG = "${ENVIRONMENT}-${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "Building branch: ${env.BRANCH_NAME}"
                    echo "Environment: ${ENVIRONMENT}"
                    echo "Image tag: ${IMAGE_TAG}"
                }
            }
        }
        
        stage('Build Maven') {
            steps {
                bat 'java -version'
                bat 'mvn clean compile'
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo 'Skipping unit tests for now - will be implemented later'
                // TODO: Implement unit tests
                // bat 'mvn test'
            }
        }
        
        stage('Package') {
            steps {
                bat 'mvn package -DskipTests'
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'api-gateway', 'cloud-config', 'favourite-service', 'order-service',
                        'payment-service', 'product-service', 'proxy-client',
                        'service-discovery', 'shipping-service', 'user-service'
                    ]
                    
                    for (service in services) {
                        bat "docker build -t danielm11/${service}:${IMAGE_TAG} .\\${service}"
                        
                        // También crear tag latest para la rama master/main
                        if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main') {
                            bat "docker tag danielm11/${service}:${IMAGE_TAG} danielm11/${service}:latest"
                        }
                    }
                }
            }
        }
        
        stage('Push Images to DockerHub') {
            steps {
                script {
                    def services = [
                        'api-gateway', 'cloud-config', 'favourite-service', 'order-service',
                        'payment-service', 'product-service', 'proxy-client',
                        'service-discovery', 'shipping-service', 'user-service'
                    ]
                    
                    withCredentials([string(credentialsId: 'password', variable: 'credential')]) {
                        bat "docker login -u danielm11 -p %credential%"
                        
                        for (service in services) {
                            bat "docker push danielm11/${service}:${IMAGE_TAG}"
                            
                            // Push latest solo para master/main
                            if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main') {
                                bat "docker push danielm11/${service}:latest"
                            }
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Development') {
            when {
                anyOf {
                    branch 'develop'
                    branch pattern: 'feature/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    echo "Deploying to Development Environment"
                    deployToEnvironment('dev', IMAGE_TAG)
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                anyOf {
                    branch 'master'
                    branch 'main'
                    branch pattern: 'release/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    echo "Deploying to Staging Environment"
                    deployToEnvironment('stage', IMAGE_TAG)
                }
            }
        }
        
        stage('Integration Tests - Development') {
            when {
                anyOf {
                    branch 'develop'
                    branch pattern: 'feature/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    echo "Integration tests will be implemented later for Development"
                    // runIntegrationTests('dev')
                }
            }
        }
        
        stage('Integration Tests - Staging') {
            when {
                anyOf {
                    branch 'master'
                    branch 'main'
                    branch pattern: 'release/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    echo "Integration tests will be implemented later for Staging"
                    // runIntegrationTests('stage')
                    
                    // Pruebas adicionales para staging
                    echo "End-to-end tests will be implemented later"
                    // runE2ETests('stage')
                }
            }
        }
        
        stage('Performance Tests - Staging Only') {
            when {
                anyOf {
                    branch 'master'
                    branch 'main'
                }
            }
            steps {
                script {
                    echo "Performance tests will be implemented later for Staging"
                    // runPerformanceTests('stage')
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Cleanup
                bat 'docker system prune -f'
            }
        }
        success {
            echo "Pipeline completed successfully for ${env.BRANCH_NAME}"
        }
        failure {
            echo "Pipeline failed for ${env.BRANCH_NAME}"
            emailext (
                subject: "Pipeline Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Pipeline failed for branch ${env.BRANCH_NAME}. Check console output at ${env.BUILD_URL}",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}

def determinateEnvironment() {
    if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('release/')) {
        return 'stage'
    } else if (env.BRANCH_NAME == 'develop' || env.BRANCH_NAME.startsWith('feature/')) {
        return 'dev'
    } else {
        return 'dev'
    }
}

def deployToEnvironment(environment, imageTag) {
    bat """
    echo Verifying kubectl context for ${environment}...
    kubectl config current-context
    
    echo Applying common configuration for ${environment}...
    kubectl apply -f k8s\\common-config.yaml
    
    echo Deploying services to ${environment}...
    """

    bat """
    echo Deploying Zipkin...
    kubectl apply -f k8s\\zipkin\\
    kubectl wait --for=condition=ready pod -l app=zipkin --timeout=300s

    echo Deploying Service Discovery...
    kubectl apply -f k8s\\service-discovery\\
    kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=300s

    echo Deploying Cloud Config...
    kubectl apply -f k8s\\cloud-config\\
    kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=300s

    echo Deploying Api gateway...
    kubectl apply -f k8s\\api-gateway\\
    kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=300s

    echo Deploying Favourite service...
    kubectl apply -f k8s\\favourite-service\\
    kubectl wait --for=condition=ready pod -l app=favourite-service --timeout=300s

    echo Deploying Order service...
    kubectl apply -f k8s\\order-service\\
    kubectl wait --for=condition=ready pod -l app=order-service --timeout=300s

    echo Deploying Payment service...
    kubectl apply -f k8s\\payment-service\\
    kubectl wait --for=condition=ready pod -l app=payment-service --timeout=300s

    echo Deploying Product service...
    kubectl apply -f k8s\\product-service\\
    kubectl wait --for=condition=ready pod -l app=product-service --timeout=300s

    echo Deploying Proxy client...
    kubectl apply -f k8s\\proxy-client\\
    kubectl wait --for=condition=ready pod -l app=proxy-client --timeout=300s

    echo Deploying Shipping service...
    kubectl apply -f k8s\\shipping-service\\
    kubectl wait --for=condition=ready pod -l app=shipping-service --timeout=300s

    echo Deploying User service...
    kubectl apply -f k8s\\user-service\\
    kubectl wait --for=condition=ready pod -l app=user-service --timeout=300s

    """
}

def runIntegrationTests(environment) {
    echo "Integration tests placeholder for ${environment} environment"
    // TODO: Implement integration tests
    // bat "mvn test -Dtest=**/*IntegrationTest -Dspring.profiles.active=${environment}"
}

def runE2ETests(environment) {
    echo "E2E tests placeholder for ${environment} environment"
    // TODO: Implement E2E tests
    // bat "mvn test -Dtest=**/*E2ETest -Dspring.profiles.active=${environment}"
}

def runPerformanceTests(environment) {
    echo "Performance tests placeholder for ${environment} environment"
    // TODO: Implement performance tests
    // bat "mvn test -Dtest=**/*PerformanceTest -Dspring.profiles.active=${environment}"
}