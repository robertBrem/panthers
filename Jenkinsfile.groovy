@Library('semantic_releasing') _

podTemplate(label: 'mypod', containers: [
        containerTemplate(name: 'docker', image: 'docker', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.0', command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'maven', image: 'maven:3.5.2-jdk-8', command: 'cat', ttyEnabled: true)
],
        volumes: [
                hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
        ]) {
    node('mypod') {

        stage('checkout & unit tests & build') {
            git url: 'https://github.com/robertbrem/panthers'
            container('maven') {
                sh 'mvn clean package'
            }
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
        }

        stage('build image & git tag & docker push') {
            env.VERSION = semanticReleasing()
            currentBuild.displayName = env.VERSION
            wrap([$class: 'BuildUser']) {
                currentBuild.description = "Started by: ${BUILD_USER} (${BUILD_USER_EMAIL})"
            }

            container('maven') {
                sh "mvn versions:set -DnewVersion=${env.VERSION}"
            }
            sh "git config user.email \"jenkins@khinkali.ch\""
            sh "git config user.name \"Jenkins\""
            sh "git tag -a ${env.VERSION} -m \"${env.VERSION}\""
            withCredentials([usernamePassword(credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/robertbrem/panthers.git --tags"
            }

            container('docker') {
                sh "docker build -t robertbrem/panthers:${env.VERSION} ."
                withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                    sh "docker login --username ${DOCKER_USERNAME} --password ${DOCKER_PASSWORD}"
                }
                sh "docker push robertbrem/panthers:${env.VERSION}"
            }
        }

        stage('deploy to test') {
            sh "sed -i -e 's~image: robertbrem/panthers:1.0.0~image: robertbrem/panthers:${env.VERSION}~' deployment.yml"
            container('kubectl') {
                sh "kubectl apply -f deployment.yml"
            }
        }
    }
}