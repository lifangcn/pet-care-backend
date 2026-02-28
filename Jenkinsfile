pipeline {
    agent any

    stages {
        stage('Deploy') {
            steps {
                sh 'docker compose -f /opt/petcare/infra/docker-compose.app.yml up -d'
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "等待服务启动..."
                    sleep 90
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
