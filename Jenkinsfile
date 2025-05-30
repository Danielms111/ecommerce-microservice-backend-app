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

        stage('Verify Tools') {
                            steps {
                                bat 'java -version'
                                bat 'mvn -version'
                                bat 'docker --version'
                                bat 'kubectl config current-context'
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
                 script {
                     ['user-service'].each {
                         bat "mvn test -pl ${it}"
                     }
                 }
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
        
        stage('Integration and e2e Tests - Development') {
            when {
                anyOf {
                    branch 'develop'
                    branch pattern: 'feature/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    echo "Running integration tests"
                    ['user-service', 'product-service'].each {
                        bat "mvn verify -pl ${it}"
                    }

                    echo "Running end-to-end tests"
                    bat "mvn verify -pl e2e-tests"
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
                    echo "Running integration tests"
                    ['user-service', 'product-service'].each {
                        bat "mvn verify -pl ${it}"
                    }
                }
            }
        }
        
         stage('Run Load Tests with Locust') {
            when {
                anyOf {
                    branch 'develop'
                    branch pattern: 'feature/.*', comparator: 'REGEXP'
                }
            }
            steps {
                script {
                    bat '''
                    echo  Levantando Locust para order-service...

                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      danielm11/locust:%IMAGE_TAG% ^
                      -f /mnt/test/order-service/locustfile.py ^
                      --host http://order-service-container:8300 ^
                      --headless -u 10 -r 2 -t 1m ^
                      --csv order-service-stats --csv-full-history

                    echo  Levantando Locust para payment-service...

                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      danielm11/locust:%IMAGE_TAG% ^
                      -f /mnt/test/payment-service/locustfile.py ^
                      --host http://payment-service-container:8400 ^
                      --headless -u 10 -r 1 -t 1m ^
                      --csv payment-service-stats --csv-full-history

                    echo  Levantando Locust para favourite-service...

                    docker run --rm --network ecommerce-test ^
                      -v "%CD%\\locust:/mnt" ^
                      -v "%CD%\\locust-results:/app" ^
                      danielm11/locust:%IMAGE_TAG% ^
                      -f /mnt/test/favourite-service/locustfile.py ^
                      --host http://favourite-service-container:8800 ^
                      --headless -u 10 -r 2 -t 1m ^
                      --csv favourite-service-stats --csv-full-history

                    echo  Pruebas completadas
                    '''
                }
            }
         }

         stage('Run Stress Tests with Locust') {
             when {
                 anyOf {
                     branch 'develop'
                     branch pattern: 'feature/.*', comparator: 'REGEXP'
                 }
             }
             steps {
                 script {
                     bat '''
                     echo  Levantando Locust para prueba de estrés...

                     docker run --rm --network ecommerce-test ^
                     -v "%CD%\\locust:/mnt" ^
                     -v "%CD%\\locust-results:/app" ^
                     danielm11/locust:%IMAGE_TAG% ^
                     -f /mnt/test/order-service/locustfile.py ^
                     --host http://order-service-container:8300 ^
                     --headless -u 50 -r 5 -t 1m ^
                     --csv order-service-stress --csv-full-history

                     docker run --rm --network ecommerce-test ^
                     -v "%CD%\\locust:/mnt" ^
                     -v "%CD%\\locust-results:/app" ^
                     danielm11/locust:%IMAGE_TAG% ^
                     -f /mnt/test/payment-service/locustfile.py ^
                     --host http://payment-service-container:8400 ^
                     --headless -u 50 -r 5 -t 1m ^
                     --csv payment-service-stress --csv-full-history

                     docker run --rm --network ecommerce-test ^
                     -v "%CD%\\locust:/mnt" ^
                     -v "%CD%\\locust-results:/app" ^
                     danielm11/locust:%IMAGE_TAG% ^
                     -f /mnt/test/favourite-service/locustfile.py ^
                     --host http://favourite-service-container:8800 ^
                     --headless -u 50 -r 5 -t 1m ^
                     --csv favourite-service-stress --csv-full-history

                     echo  Pruebas de estrés completadas
                     '''
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
    //kubectl wait --for=condition=ready pod -l app=zipkin --timeout=60s

    echo Deploying Service Discovery...
    kubectl apply -f k8s\\service-discovery\\
    //kubectl wait --for=condition=ready pod -l app=service-discovery --timeout=60s

    echo Deploying Cloud Config...
    kubectl apply -f k8s\\cloud-config\\
    //kubectl wait --for=condition=ready pod -l app=cloud-config --timeout=60s

    echo Deploying Api gateway...
    kubectl apply -f k8s\\api-gateway\\
    //kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=60s

    echo Deploying Favourite service...
    kubectl apply -f k8s\\favourite-service\\
    //kubectl wait --for=condition=ready pod -l app=favourite-service --timeout=60s

    echo Deploying Order service...
    kubectl apply -f k8s\\order-service\\
    //kubectl wait --for=condition=ready pod -l app=order-service --timeout=60s

    echo Deploying Payment service...
    kubectl apply -f k8s\\payment-service\\
    //kubectl wait --for=condition=ready pod -l app=payment-service --timeout=60s

    echo Deploying Product service...
    kubectl apply -f k8s\\product-service\\
    //kubectl wait --for=condition=ready pod -l app=product-service --timeout=60s

    echo Deploying Proxy client...
    kubectl apply -f k8s\\proxy-client\\
    //kubectl wait --for=condition=ready pod -l app=proxy-client --timeout=60s

    echo Deploying Shipping service...
    kubectl apply -f k8s\\shipping-service\\
    //kubectl wait --for=condition=ready pod -l app=shipping-service --timeout=60s

    echo Deploying User service...
    kubectl apply -f k8s\\user-service\\
    //kubectl wait --for=condition=ready pod -l app=user-service --timeout=60s

    """
}
