#!/usr/bin/env groovy

def call(Map config) {
    echo "========== DEPLOY TO ${config.environment.toUpperCase()} =========="
    
    sshagent(credentials: [config.sshCredentialId]) {
        sh  """
            echo "=== Creating remote directory ==="
            ssh -o StrictHostKeyChecking=no ${SSH_USER}@${config.host} "mkdir -p /tmp/database"

            echo "=== Copying SQL files to staging server ==="
            scp -o StrictHostKeyChecking=no \
                src/main/resources/database/create.sql \
                src/main/resources/database/data.sql \
                ${SSH_USER}@${config.host}:/tmp/database/

            echo "=== Starting the containers ==="
            ssh -o StrictHostKeyChecking=no ${SSH_USER}@${config.host} "              

                echo "=== Checking existing MySQL container ===" &&
                if docker ps -a | grep -q ${DB_CONTAINER_NAME}; then
                    echo "MySQL container exists, checking if running..." &&
                    if ! docker ps | grep -q ${DB_CONTAINER_NAME}; then
                        echo "Starting existing MySQL container..." &&
                        docker start ${DB_CONTAINER_NAME}
                    else
                        echo "MySQL container already running"
                    fi
                else
                    echo "=== Starting MySQL container ===" &&
                    docker run -d \
                        --name ${DB_CONTAINER_NAME} \
                        -p ${DB_PORT}:3306 \
                        -e MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD} \
                        -e MYSQL_DATABASE=${DB_NAME} \
                        mysql:8.0 &&
                    echo "Waiting for MySQL to be ready..." &&
                    sleep 30
                fi &&

                echo "=== Getting Docker bridge IP ===" &&
                DOCKER_IP=\\\$(docker inspect -f "{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}" ${DB_CONTAINER_NAME}) &&
                echo \"MySQL IP: \\\$DOCKER_IP\" &&
                
                echo "=== Executing SQL scripts ===" &&
                docker exec -i ${DB_CONTAINER_NAME} mysql -u root -p${DB_ROOT_PASSWORD} < /tmp/database/create.sql &&
                docker exec -i ${DB_CONTAINER_NAME} mysql -u root -p${DB_ROOT_PASSWORD} ${DB_NAME} < /tmp/database/data.sql &&

                echo "=== Pulling new image ===" &&
                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG} &&

                echo "=== Stopping old container ===" &&
                docker stop ${APP_NAME} || true &&
                docker rm ${APP_NAME} || true &&

                echo "=== Starting application container ===" &&
                docker run -d \
                    --name ${APP_NAME} \
                    -p ${APP_PORT}:${CONTAINER_PORT} \
                    -e SPRING_PROFILES_ACTIVE=${config.springProfile} \
                    ${DOCKER_IMAGE}:${DOCKER_TAG} &&
                sleep 15
            "
        """
    }
}


def testEnvironment(Map config) {
    echo "========== TESTING ENVIRONMENT =========="
    
    sshagent(credentials: [config.sshCredentialId]) {
        sh """
            ssh -o StrictHostKeyChecking=no ${SSH_USER}@${config.host} '
                echo "=== Checking containers ===" &&
                docker ps | grep -E "${APP_NAME}|${DB_CONTAINER_NAME}" &&
                
                echo "=== Cleanup ===" &&
                rm -rf /tmp/database &&
                
                echo "=== Test completed ==="
            '
        """
    }
}
