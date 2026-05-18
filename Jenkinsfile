pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
        // Vérifie que 'CX_CLI' est le nom exact dans Administrer Jenkins > Tools
        'com.checkmarx.jenkins.CxScanConfig' 'CX_CLI' 
    }

    environment {
        CLUSTER_NAME = 'microservices-cluster'
        NAMESPACE    = 'microservices'
    }

    stages {
        // ÉTAPE 1 : Récupérer le code source (Indispensable avant le scan)
        stage('Checkout') {
            steps {
                echo '=== Récupération du code GitHub ==='
                checkout scm
            }
        }

        // ÉTAPE 2 : Scanner le code avec Checkmarx
        stage('Security Scan (Checkmarx)') {
            steps {
                echo '=== Analyse de sécurité avec Checkmarx ==='
                checkmarxASTScanner(
                    projectName: 'springboot-kafka-microservices-intern',
                    serverUrl: 'https://eu.ast.checkmarx.net/', 
                    credentialsId: 'checkmarx-credstesr'
                )
            }
        }

        stage('Build common-lib') {
            steps {
                dir('common-lib') {
                    sh 'mvn clean install -DskipTests -Dgpg.skip=true'
                }
            }
        }

        stage('Build Services') {
            steps {
                script {
                    def services = [
                        'service-registry',
                        'api-gateway',
                        'identity-service',
                        'order-service',
                        'payment-service',
                        'product-service',
                        'email-service'
                    ]
                    services.each { svc ->
                        echo "=== Build: ${svc} ==="
                        dir(svc) {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Docker Build Backend') {
            steps {
                echo '=== Build images Docker backend ==='
                sh 'docker-compose -p myapp build'
            }
        }

        stage('Docker Build Frontend') {
            steps {
                echo '=== Build image Docker frontend ==='
                dir('mon-projet') {
                    sh 'docker build -t springboot-kafka-microservices/frontend:latest .'
                }
            }
        }

        stage('Update Kubeconfig') {
            steps {
                echo '=== Mise a jour kubeconfig KIND ==='
                sh """
                    kind export kubeconfig \
                        --name ${CLUSTER_NAME} \
                        --kubeconfig /var/jenkins_home/.kube/config
                """
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline reussi — app disponible sur http://localhost'
        }
        failure {
            echo '❌ Pipeline echoue'
        }
    }
}
