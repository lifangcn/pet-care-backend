pipeline {
    agent any

    environment {
        APP_DIR = '/opt/petcare/app-build'
        INFRA_DIR = '/opt/petcare/infra'
    }

    stages {
        stage('Checkout') {
            steps {
                dir("${env.APP_DIR}") {
                    // 清理旧代码
                    sh 'rm -rf * || true'
                    // Jenkins 已经从 Gitea 拉取了代码
                }
            }
        }

        stage('Build') {
            steps {
                dir("${env.APP_DIR}") {
                    sh 'mvn clean package -DskipTests -q'
                }
            }
        }

        stage('Deploy') {
            steps {
                dir("${env.INFRA_DIR}") {
                    sh 'docker compose -f docker-compose.app.yml up -d --build'
                }
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "等待服务启动..."
                    sleep 30
                    curl -f http://localhost:8080/actuator/health || exit 1
                    curl -f http://localhost:8081/actuator/health || exit 1
                    echo "✅ 健康检查通过！"
                '''
            }
        }
    }

    post {
        success {
            echo '✅ 部署成功！'
            echo "Core API: http://ubuntu001:8080"
            echo "AI API:  http://ubuntu001:8081"
        }
        failure {
            echo '❌ 部署失败，请检查日志'
        }
    }
}
