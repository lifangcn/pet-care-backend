pipeline {
    agent any

    stages {
        stage('Sync Code') {
            steps {
                sh '''
                    # 同步代码到构建目录
                    rsync -av --delete $WORKSPACE/modules/pet-care-core/ /opt/petcare/app/modules/pet-care-core/
                    rsync -av --delete $WORKSPACE/modules/pet-care-ai/ /opt/petcare/app/modules/pet-care-ai/
                    rsync -av --delete $WORKSPACE/modules/pet-care-common/ /opt/petcare/app/modules/pet-care-common/ || true
                    echo "✅ 代码已同步到构建目录"
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose -f /opt/petcare/infra/docker-compose.app.yml up -d --build'
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "等待服务启动..."
                    sleep 45
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
