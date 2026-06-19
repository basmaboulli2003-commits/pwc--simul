
pipeline {
    agent any
 
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }
 
    environment {
        // Configurable values
        KIND_CLUSTER  = 'microservices-cluster'
        K8S_NAMESPACE = 'microservices'
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
 
        // --- Checkmarx config (nom identique à chaque run = un seul projet) ---
        CX_PROJECT = 'pwc-simul'
        CX_BRANCH  = 'main'
    }
 
    stages {
 
        // STEP 1 — Checkout source
        stage('Checkout') {
            steps {
                echo '=== Fetching source code ==='
                checkout scm
            }
        }
 
        // STEP 2 — SAST scan (shift-left : on scanne le code AVANT de builder/déployer)
        stage('SAST - Checkmarx') {
            steps {
                echo '=== Running Checkmarx SAST scan ==='
                withCredentials([string(credentialsId: 'cx-api-key', variable: 'CX_APIKEY')]) {
                    sh '''
                        cx scan create \
                          --project-name "${CX_PROJECT}" \
                          --branch "${CX_BRANCH}" \
                          -s "${WORKSPACE}" \
                          --scan-types "sast"
                    '''
                    // --- Pour activer le break-build (gate de sécurité) ---
                    // Une fois que tu connais ton baseline de vulnérabilités, ajoute
                    // ce flag à la commande ci-dessus pour casser le build au-delà du seuil :
                    //   --threshold "sast-high=1;sast-medium=10"
                    // (signifie : >=1 High OU >=10 Medium => le build échoue)
                }
            }
        }
 
        // STEP 3 — Build common-lib first (other services depend on it)
        stage('Build common-lib') {
            steps {
                echo '=== Installing common-lib to local repo ==='
                dir('common-lib') {
                    sh 'mvn clean install -DskipTests -Dgpg.skip=true'
                }
            }
        }
 
        // STEP 4 — Build all microservices in parallel
        stage('Build Microservices') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'identity-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'email-service'
                    ]
                    def builds = [:]
                    services.each { svc ->
                        builds[svc] = {
                            dir(svc) {
                                echo "=== Building ${svc} ==="
                                sh 'mvn clean package -DskipTests'
                            }
                        }
                    }
                    parallel builds
                }
            }
        }
 
        // STEP 5 — Build Docker images
        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'identity-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'email-service'
                    ]
                    services.each { svc ->
                        echo "=== Docker build: ${svc}:${IMAGE_TAG} ==="
                        dir(svc) {
                            sh "docker build -t ${svc}:${IMAGE_TAG} -t ${svc}:latest ."
                        }
                    }
                }
            }
        }
 
        // STEP 6 — Load images into KIND
        stage('Load Images into KIND') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'identity-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'email-service'
                    ]
                    services.each { svc ->
                        echo "=== Loading ${svc}:${IMAGE_TAG} into kind cluster ==="
                        sh "kind load docker-image ${svc}:${IMAGE_TAG} --name ${KIND_CLUSTER}"
                    }
                }
            }
        }
 
        // STEP 7 — Deploy to KIND
        stage('Deploy to Kubernetes') {
            steps {
                echo '=== Rolling out new images to all deployments ==='
                script {
                    def services = [
                        'api-gateway',
                        'identity-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'email-service'
                    ]
                    services.each { svc ->
                        sh """
                            kubectl set image deployment/${svc} \
                                ${svc}=${svc}:${IMAGE_TAG} \
                                -n ${K8S_NAMESPACE} || \
                            echo 'Deployment ${svc} not found, skipping'
                        """
                    }
                }
            }
        }
 
        // STEP 8 — Wait for pods to be ready
        stage('Verify Deployment') {
            steps {
                echo '=== Waiting for rollouts to complete ==='
                script {
                    def services = [
                        'api-gateway',
                        'identity-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'email-service'
                    ]
                    services.each { svc ->
                        sh """
                            kubectl rollout status deployment/${svc} \
                                -n ${K8S_NAMESPACE} \
                                --timeout=120s || \
                            echo 'Rollout check failed for ${svc}'
                        """
                    }
                    sh "kubectl get pods -n ${K8S_NAMESPACE}"
                }
            }
        }
 
    }
 
    post {
        success {
            echo '✅ Pipeline succeeded — Build + Deploy complete'
        }
        failure {
            echo '❌ Pipeline failed — check logs above'
        }
        always {
            echo '=== Cleaning workspace ==='
            cleanWs()
        }
    }
}
 