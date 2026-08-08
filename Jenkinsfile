pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'lightalm'
    }

    stages {
        stage('Checkout') {
            steps {
                // Gitea 저장소에서 소스 코드 동기화
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // docker-compose.yml에 정의된 postgres/backend/frontend 이미지 빌드
                sh 'docker compose -f docker-compose.yml -f docker-compose.prod.yml build'
            }
        }

        stage('Deploy') {
            steps {
                // 기존(구) 단일 컨테이너 배포가 남아있으면 정리 (alm.ondalprincess.synology.me -> :8888 프록시 포트 확보)
                sh 'docker stop factorysolution-alm || true'
                sh 'docker rm factorysolution-alm || true'
                // 신규 스택 기동: frontend가 :8888로 노출되어 기존 리버스 프록시 경로를 그대로 사용
                sh 'docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d'
            }
        }
    }
}
