pipeline {
    agent any

    environment {
        // Jenkins 工作空间代码路径
        WORKSPACE = '${WORKSPACE}'
        INFRA_DIR = '/opt/petcare/infra'
    }

    stages {
        stage('Build') {
            steps {
                // Jenkins 已从 Gitea 拉取代码到工作空间
                sh 'cd ${WORKSPACE} && mvn clean package -DskipTests -q'
            }
        }

        stage('Copy Jars') {
            steps {
                // 复制编译好的 jar 包到部署目录
                sh '''
                    cp ${WORKSPACE}/modules/pet-care-core/target/pet-care-core-1.0-SNAPSHOT.jar /opt/petcare/infra/modules/pet-care-core/target/
                    cp ${WORKSPACE}/modules/pet-care-ai/target/pet-care-ai-1.0-SNAPSHOT.jar /opt/petcare/infra/modules/pet-care-ai/target/
                    echo "JAR 包已复制到部署目录"
                '''
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
