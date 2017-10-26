pipeline {
    agent { label 'gradle' }

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'nexus3', description: 'Nexus 3 openshift service name')
        string(name: 'VERSION', defaultValue: 'latest', description: 'Version used for based sonatype/nexus3 image')
        string(name: 'VOLUME_CAPACITY', defaultValue: '10Gi', description: 'Volume capacity of the PVC for Nexus 3 storage')
    }

    stages {
        stage('Process resources') {
            steps {
                git url: "https://github.com/arnaud-deprez/nexus3-docker.git", branch: "master"
                sh "gradle -Pci=true clean transformScriptToJson"
            }
        }
        stage('Build Image') {
            steps {
                // create a build dir context
                sh "rm -rf oc-build && mkdir -p oc-build"
                sh "cp -R Dockerfile build docker oc-build/"
                //create and start build
                sh "oc process -f openshift/nexus3-persistent-template.yml -p SERVICE_NAME=${params.SERVICE_NAME} -p NEXUS_VERSION=${params.VERSION} -p VOLUME_CAPACITY=${params.VOLUME_CAPACITY} | oc apply -n cicd -f -"
                sh "oc start-build ${params.SERVICE_NAME}-docker --from-dir=oc-build -n cicd"
                openshiftVerifyBuild bldCfg: "${params.SERVICE_NAME}-docker", waitTime: "20", waitUnit: "min"
            }
        }
        stage('Deploy') {
            steps {
                sh "oc rollout latest dc/${params.SERVICE_NAME} -n cicd"
                openshiftVerifyDeployment depCfg: params.SERVICE_NAME
            }
        }
    }
}