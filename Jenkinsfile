pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        disableConcurrentBuilds()
        timestamps()
    }

    agent { label 'gradle' }

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'nexus3', description: 'Nexus 3 openshift service name')
        string(name: 'VERSION', defaultValue: 'latest', description: 'Version used for based sonatype/nexus3 image')
        string(name: 'VOLUME_CAPACITY', defaultValue: '10Gi', description: 'Volume capacity of the PVC for Nexus 3 storage')
    }

    stages {
        stage('Process resources') {
            steps {
                checkout scm
                sh "gradle -Pci=true clean transformScriptToJson"
            }
        }
        stage('Build Image') {
            steps {
                // create a build dir context
                sh "rm -rf oc-build && mkdir -p oc-build"
                sh "cp -R Dockerfile build docker oc-build/"
                //create and start build
                script {
                    openshift.withCluster() {
                        openshift.withProject('cicd') {
                            def template = readYaml file: 'openshift/nexus3-persistent-template.yml'
                            def resources = openshift.process(template, "-p", "SERVICE_NAME=${params.SERVICE_NAME}", "-p", "NEXUS_VERSION=${params.VERSION}", "-p", "VOLUME_CAPACITY=${params.VOLUME_CAPACITY}")
                            def buildCfg = openshift.apply(resources).narrow('bc')
                            def buildSelector = buildCfg.startBuild('--from-dir=oc-build')
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
        stage('Deploy') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('cicd') {
                            openshift.selector('dc', params.SERVICE_NAME).rollout().latest()
                            // TODO: replace it when https://github.com/openshift/jenkins-client-plugin/issues/84 will be solved
                            openshiftVerifyDeployment depCfg: params.SERVICE_NAME, namespace: 'cicd'
                        }
                    }
                }
            }
        }
    }
}