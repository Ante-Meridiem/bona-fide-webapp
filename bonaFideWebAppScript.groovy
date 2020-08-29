def getReleaseConfirmation() {
  slackSend channel: "#bona-fide-production-deployment", message: "Deployment Started for Service: ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.JENKINS_BUILD_URL}|Open>)",teamDomain: 'bona-fide-co', tokenCredentialId: 'slackIntegrationIdForSendingNotification'
  def inputMessage = "Please provide the release version for Bona Fide Web App"
  getBuildVersion()
  timeout(time: 30, unit: 'MINUTES') {
    buildVersion = input(id: 'buildVersion', message: inputMessage, parameters: [[$class: 'TextParameterDefinition', defaultValue: env.BUILD_VERSION, description: 'Build Version', name: 'Release Version']])
  }
}

def getBuildVersion() {
  git credentialsId: 'bona-fide',
  url: 'git@github.com:Ante-Meridiem/Bona-Fide-Web-App.git'
  def masterCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
  def currentDate = sh(returnStdout: true, script: 'date +%Y-%m-%d').trim()
  env.BUILD_VERSION = currentDate + "-" + masterCommit
}

def fecthJarAndDockerFile() {
  def fetchErrorMessage = 'Error while fetching the Jar and dockerfile'
  try {
    sh label: 'createTargetDirectory',
    script: '''sudo mkdir -p target'''
    sh label: 'copyJarFile',
    script: '''sudo cp /home/ec2-user/Bona-Fide-Web-App/bona-fide-web-app.jar target'''
    sh label: 'copyDockerFile',
    script: 'sudo cp /home/ec2-user/Bona-Fide-Web-App/Dockerfile .'
  }
  catch(Exception e) {
    error "${fetchErrorMessage} ${e.getMessage()}"
  }
}

def buildDockerImage() {
  def dockerImgBuildError = 'Error while creating docker image'
  try {
    sh "docker build -f Dockerfile -t docker4bonafide/${buildVersion} ."
  }
  catch(Exception e) {
    error "${dockerImgBuildError} ${e.getMessage()}"
  }
}

def pushDockerImage() {
  def dockerImagePushError = 'Error while pushing docker image'
  withCredentials([string(credentialsId: 'docker-hub-password-bona-fide', variable: 'bonaFideDockerHubPassword')]) {
    sh "docker login -u docker4bonafide -p ${bonaFideDockerHubPassword}"
  }
  try {
    sh "docker push docker4bonafide/${buildVersion}:latest"
  }
  catch(Exception e) {
    error "${dockerImagePushError} ${e.getMessage()}"
  }
}

def stopRunningContainer() {
  def stoppingContainerErrorMessage = 'Error occured while stopping running container'
  def renamingContainerErrorMessage = 'Error occured while renaming container'
  sshagent(['bonaFideDeploymentAccess']) {
    final String currentImageId = sh(script: 'ssh -o StrictHostKeyChecking=no ec2-user@13.126.97.24 docker ps -q -f name="^bona_fide_web_app_container$"', returnStdout: true)
         if(!currentImageId.isEmpty()){
             echo 'Stopping Current Container'
             try{
		sh 'ssh -o StrictHostKeyChecking=no ec2-user@13.126.97.24 docker stop bona_fide_web_app_container '
             }
             catch(Exception e){
                error "${stoppingContainerErrorMessage} ${e.getMessage()}"
             }
	     echo 'Renaming Current Container '
             try{
                sh 'ssh -o StrictHostKeyChecking=no ec2-user@13.126.97.24 docker rename bona_fide_web_app_container bona_fide_web_app_container_old '
             }
             catch(Exception e){
                 error "${renamingContainerErrorMessage} ${e.getMessage()}" 
             }
	     echo 'Renamed bona_fide_web_app_container to bona_fide_web_app_container_old '
         }
     }
}

def runContainer(){
    def dockerContainerRunError = 'Error while running the container '
    def dockerRunCommand = "docker run -d -p 9000:9000 --name bona_fide_web_app_container docker4bonafide/${buildVersion}"
    sshagent(['bonaFideDeploymentAccess ']) {
        try{
            sh "ssh -o StrictHostKeyChecking=no ec2-user@13.126.97.24 ${dockerRunCommand}"
        }
        catch(Exception e){
            error "${dockerContainerRunError} ${e.getMessage()}"
        }
        echo 'Waiting for a minute...'
        sleep 59
    }
}

def performHealthCheck(){
    def httpResponseStatus = "HTTP/1.1 200"
    def deploymentFailureMessage = 'Deplyoment Unsuccessfull...Please have a look '
    final String url = 'http://ec2-13-126-97-24.ap-south-1.compute.amazonaws.com:9000/index.html'
    final String response = sh(script: "curl -Is $url | head -1", returnStdout: true).trim()
    if (response == "${httpResponseStatus}") {
      APPLICATION_RUNNING_STATUS = true
    }
    else {
      error "${deploymentFailureMessage}"
    }
  }

def performCleanSlateProtocol() {
    def imageRemovalErrorMessage = 'Error while removing images'
    def containerRemovalErrorMessage = 'Error while removing containers'
    if (APPLICATION_RUNNING_STATUS == true) {
	    try{
	    	sh 'docker image prune -a -f'
	    }
	    catch(Exception e){
	    	echo "${imageRemovalErrorMessage} ${e.getMessage()}"
	    }
      sshagent(['bonaFideDeploymentAccess']) {      
	try{
		sh 'ssh -o StrictHostKeyChecking=no ec2-user@13.126.97.24 docker system prune -f'
		sh 'ssh -o StrictHostKeyChecking=no ec2-user@13.126.97.24 docker image prune -a -f'
		echo 'Successfully removed all non-functional containers' 
	}
	catch(Exception e){
		echo "${containerRemovalErrorMessage} ${e.getMessage()}"
	}		
      }
    }
    dir('/var/lib/jenkins/workspace') {
	try{
		sh(script:'sudo rm -rf Bona-Fide-Web-App')
    		sh(script:'sudo rm -rf Bona-Fide-Web-App@tmp')
	}
	catch(Exception e){
		echo 'Error while deleting the working directory'
	}
    }
}

return this
