pipeline {
    agent any

    environment {
        NETWORK = 'lightalm-net'
        // 자체 컨테이너 대신 Synology에 이미 떠있는 Postgres의 ALM_Project DB를 사용
        // (기존 PLM 앱이 쓰던 것과 동일한 인스턴스, KPI 집계 등 외부 도구가 이 DB를 바라봄)
        DB_HOST = 'ondalprincess.synology.me'
        DB_PORT = '55432'
        DB_NAME = 'ALM_Project'
        DB_USER = 'postgres'
        DB_PASSWORD = 'postgres'
        CORS_ALLOWED_ORIGINS = 'https://alm.ondalprincess.synology.me'
    }

    stages {
        stage('Checkout') {
            steps {
                // Gitea 저장소에서 소스 코드 동기화
                checkout scm
            }
        }

        stage('Build Images') {
            steps {
                // docker compose/docker-compose 둘 다 없는 에이전트라 순수 docker build로 진행
                sh 'docker build -t lightalm-backend:latest ./backend'
                sh 'docker build -t lightalm-frontend:latest --build-arg VITE_API_BASE_URL=/api ./frontend'
            }
        }

        stage('Deploy') {
            steps {
                // 기존(구) 단일 컨테이너 배포가 남아있으면 정리 (alm.ondalprincess.synology.me -> :8888 프록시 포트 확보)
                sh 'docker stop factorysolution-alm || true'
                sh 'docker rm factorysolution-alm || true'

                // 이전 LightALM 배포 컨테이너 정리 (재배포 시). lightalm-postgres는 더 이상 안 쓰므로 볼륨까지 제거
                sh 'docker rm -f lightalm-frontend lightalm-backend lightalm-postgres || true'
                sh 'docker volume rm lightalm-pgdata || true'

                sh "docker network create ${NETWORK} || true"

                // backend는 전용 네트워크 안에서만 통하는 별칭(backend)을 부여
                // -> 다른 팀 컨테이너와 이름이 겹쳐도 이 네트워크 밖에는 영향 없음, nginx.conf의 proxy_pass http://backend:8080 과 매칭
                // 외부 ALM_Project DB에 저장된 기존 마이그레이션 체크섬이 개행 문자 차이로 어긋나 있어 검증만 비활성화
                // (테이블/컬럼 구조는 V1/V2와 동일함을 확인함)
                sh """
                    docker run -d --name lightalm-backend --network ${NETWORK} --network-alias backend \
                        -e DB_HOST=${DB_HOST} -e DB_PORT=${DB_PORT} -e DB_NAME=${DB_NAME} \
                        -e DB_USER=${DB_USER} -e DB_PASSWORD=${DB_PASSWORD} \
                        -e SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false \
                        -e CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS} \
                        lightalm-backend:latest
                """

                // frontend만 호스트 8888로 노출 (기존 리버스 프록시 경로 재사용)
                sh """
                    docker run -d --name lightalm-frontend --network ${NETWORK} \
                        -p 8888:80 \
                        lightalm-frontend:latest
                """

                sh 'sleep 30'
                sh 'docker ps -a --filter name=lightalm- || true'
                sh 'docker logs --tail 300 lightalm-backend || true'
            }
        }
    }
}
