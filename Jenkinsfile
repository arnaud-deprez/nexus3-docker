pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        disableConcurrentBuilds()
        timestamps()
    }

    agent { label 'gradle' }

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'nexus3', description: 'Nexus 3 openshift service name')
        string(name: 'VERSION', defaultValue: '3.13.0', description: 'Version used for based sonatype/nexus3 image')
        string(name: 'VOLUME_CAPACITY', defaultValue: '10Gi', description: 'Volume capacity of the PVC for Nexus 3 storage')
    }

    stages {
        stage('Process resources') {
            steps {
                checkout scm
                container('gradle') {
                    sh "gradle -Pci=true clean transformScriptToJson"
                }
            }
        }
        stage('Build and Deploy') {
            steps {
                container('gradle') {
                    // create a build dir context
                    sh "rm -rf oc-build && mkdir -p oc-build"
                    sh "cp -R Dockerfile build docker oc-build/"
                    //create and start build
                    script {
                        openshift.withCluster() {
                            openshift.withProject('cicd') {
                                // create resources
                                sh "helm upgrade --install ${params.SERVICE_NAME} charts/nexus3"
                                //start build
                                def buildSelector = openshift.selector('bc', "${params.SERVICE_NAME}-docker").startBuild('--from-dir=oc-build')
                                buildSelector.logs('-f')
                                timeout(5) {
                                    buildSelector.untilEach(1) {
                                        return it.object().status.phase == "Complete"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //TODO: build and then deploy
    }
}