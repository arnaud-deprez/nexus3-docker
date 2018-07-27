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
                sh "gradle -Pci=true clean transformScriptToJson"
            }
        }
        stage('Build and Deploy') {
            steps {
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
        //TODO: add a way to control deployment
        /*stage('Deploy') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('cicd') {
                            openshift.selector('deploy', params.SERVICE_NAME).rollout().latest()
                            // TODO: replace it when https://github.com/openshift/jenkins-client-plugin/issues/84 will be solved
                            openshift.selector('deploy', params.SERVICE_NAME).withEach { deploy ->
                                timeout(time: 10, unit: 'MINUTES') {
                                    deploy.untilEach(1) {
                                        it.rollout().status().out.contains('successfully rolled out')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }*/
    }
}