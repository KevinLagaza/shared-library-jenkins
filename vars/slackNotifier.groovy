#!/usr/bin/env groovy

def call(String buildResult) {
    if (buildResult == "SUCCESS") {
        slackSend(
            color: "good",
            message: "✅ CONGRATULATION: Job ${env.JOB_NAME} build #${env.BUILD_NUMBER} was successful! ${env.BUILD_URL}"
        )
    } else if (buildResult == "FAILURE") {
        slackSend(
            color: "danger",
            message: "❌ BAD NEWS: Job ${env.JOB_NAME} build #${env.BUILD_NUMBER} failed! ${env.BUILD_URL}"
        )
    } else if (buildResult == "UNSTABLE") {
        slackSend(
            color: "warning",
            message: "⚠️ WARNING: Job ${env.JOB_NAME} build #${env.BUILD_NUMBER} was unstable! ${env.BUILD_URL}"
        )
    } else {
        slackSend(
            color: "danger",
            message: "❓ UNKNOWN: Job ${env.JOB_NAME} build #${env.BUILD_NUMBER} result unclear! ${env.BUILD_URL}"
        )
    }
}
