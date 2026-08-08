pipeline {
    agent any

    environment {
        NETWORK = 'lightalm-net'
        PGVOLUME = 'lightalm-pgdata'
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

                // 이전 LightALM 배포 컨테이너 정리 (재배포 시)
                sh 'docker rm -f lightalm-frontend lightalm-backend lightalm-postgres || true'

                sh "docker network create ${NETWORK} || true"
                sh "docker volume create ${PGVOLUME} || true"

                // postgres, backend는 전용 네트워크 안에서만 통하는 별칭(postgres/backend)을 부여
                // -> 다른 팀 컨테이너와 이름이 겹쳐도 이 네트워크 밖에는 영향 없음, nginx.conf의 proxy_pass http://backend:8080 과 매칭
                sh """
                    docker run -d --name lightalm-postgres --network ${NETWORK} --network-alias postgres \
                        -e POSTGRES_DB=lightalm -e POSTGRES_USER=lightalm -e POSTGRES_PASSWORD=lightalm \
                        -v ${PGVOLUME}:/var/lib/postgresql/data \
                        postgres:15
                """

                sh '''
                    for i in $(seq 1 30); do
                        docker exec lightalm-postgres pg_isready -U lightalm -d lightalm && break
                        sleep 2
                    done
                '''

                sh """
                    docker run -d --name lightalm-backend --network ${NETWORK} --network-alias backend \
                        -e DB_HOST=postgres -e DB_PORT=5432 -e DB_NAME=lightalm \
                        -e DB_USER=lightalm -e DB_PASSWORD=lightalm \
                        lightalm-backend:latest
                """

                // frontend만 호스트 8888로 노출 (기존 리버스 프록시 경로 재사용)
                sh """
                    docker run -d --name lightalm-frontend --network ${NETWORK} \
                        -p 8888:80 \
                        lightalm-frontend:latest
                """
            }
        }
    }
}
