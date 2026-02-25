#!/usr/bin/env groovy

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
