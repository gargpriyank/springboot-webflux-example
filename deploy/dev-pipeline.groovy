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

def githubURL = "https://github.com/gargpriyank/springboot-webflux-example.git"
def githubBranch = "master"
def appName = "springboot-webflux-example"
def imageTag = "1.0.0.${currentBuild.number}"
def namespace = "dev"
def memLimit = "${params.MemoryLimit}"
def numOfPods = "${params.NumOfPods}"
def dockerRegistryURL = "${params.ContainerRegistryURL}"
def dockerRepo = "${params.ContainerRegistryNamespace}"
def osClusterURL = "https://c100-e.eu-de.containers.cloud.ibm.com:32563"
def dockerFilePath = "deploy/Dockerfile"
def deployEnv = "${params.DeploymentEnv}"

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
                gitCheckout(gitURL: "$githubURL", gitBranch: "$githubBranch")
                loginOS(clusterURL: "$osClusterURL")
            }
        }
        stage('Build and push app image') {
            steps {
                processOSTemplate(project: "$namespace", templateFullPath: 'deploy/build-template.yaml', appName: "$appName", dockerRegistry:
                        "$dockerRegistryURL", dockerRepo: "$dockerRepo", imageTag: "$imageTag", githubURL: "$githubURL",
                        githubBranch: "$githubBranch")
                script {
                    sleep(300)
                }
            }
        }
        stage('Deploy app') {
            steps {
                processOSTemplate(project: "$namespace", templateFullPath: 'deploy/deploy-template.yaml', appName: "$appName", dockerRegistry:
                        "$dockerRegistryURL", dockerRepo: "$dockerRepo", memLimit: "$memLimit", replicas: "$numOfPods", imageTag: "$imageTag")
            }
        }
    }
    post {
        always {
            postScript(dockerRegistry: "$dockerRegistryURL")
        }
    }
}
