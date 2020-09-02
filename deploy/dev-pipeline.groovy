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

    agent any
    tools{
        maven 'mvn'
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    stages {
        stage('Build app jar') {
            steps {
                buildJarMaven(mvnArgs: '-DskipTests=false')
                script{
                    sh "oc process -f build-template.yaml -pNAME=springboot-example -pIMAGE_REGISTRY=de.icr.io -pIMAGE_REPO=infordata_poc_ir -pIMAGE_TAG=latest | oc apply -f -"
                }
            }
        }
    }
}
