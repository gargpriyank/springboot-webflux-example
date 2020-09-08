@Library('jenkins-library') _

properties([
        parameters([
                choice(choices: ["POC"], description: "Please select an environment to deploy", name: "DeploymentEnv"),
                string(defaultValue: "512Mi", description: "Please enter the memory limit of the application", name: "MemoryLimit", trim: true),
                string(defaultValue: "2", description: "Please enter the number of application instances to run", name: "NumOfPods", trim: true),
                string(defaultValue: "de.icr.io", description: "Please enter the docker registry URL", name: "ContainerRegistryURL", trim: true),
                string(defaultValue: "infordata_poc_ir", description: "Please enter the docker repository name", name: "ContainerRegistryNamespace",
                        trim: true)
        ])
])

def appName = "springboot-webflux"
def imageTag = "1-0.0.${currentBuild.number}"
def namespace = "dev"
def template = "deploy-template.yaml"
def deployEnv = "${params.DeploymentEnv}"
def memLimit = "${params.MemoryLimit}"
def numOfPods = "${params.NumOfPods}"
def dockerRegistryURL = "${params.ContainerRegistryURL}"
def dockerRepo = "${params.ContainerRegistryNamespace}"

pipeline {

    agent {
        label 'maven'
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    stages {
        stage('Initialize') {
            steps {
                gitCheckout(gitURL: 'https://github.com/gargpriyank/springboot-webflux-example.git', gitBranch: 'master')
            }
        }
        stage('Build app jar') {
            steps {
                buildJarMaven(mvnArgs: '-DskipTests')
            }
        }
        stage('Build and push docker image') {
            steps {
                buildPushDockerImage(appName: "$appName", imageTag: "$imageTag", dockerRegistry: "$dockerRegistryURL", dockerRepo: "$dockerRepo")
            }
        }
    }
    post {
        always {
            postScript(dockerRegistry: "$dockerRegistryURL")
        }
    }
}
