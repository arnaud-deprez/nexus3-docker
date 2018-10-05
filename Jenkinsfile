pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        disableConcurrentBuilds()
        timestamps()
    }

    agent { label 'gradle' }

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'nexus3-custom', description: 'Nexus 3 openshift service name')
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
        stage('Build') {
            steps {
                container('gradle') {
                    script {
                        openshift.withCluster() {
                            openshift.withProject('cicd') {
                                sh "gradle transformScriptToJson"
                                // create BuildConfig resources
                                sh "helm template --name ${params.SERVICE_NAME} --set nameOverride=${params.SERVICE_NAME} charts/openshift-build | oc apply -n cicd -f -"
                                //start build
                                def buildSelector = openshift.selector('bc', "${params.SERVICE_NAME}").startBuild('--from-dir=.')
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
        stage('Deploy') {
            steps {
                container('gradle') {
                    //TODO: ensure unique version with commit id or so
                    sh "helm template --name ${params.SERVICE_NAME} -f charts/openshift-build/values.yaml charts/nexus3 | oc apply -n cicd -f -"
                }
            }
        }
    }
}