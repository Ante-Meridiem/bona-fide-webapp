//Jenkinsfile for Bona-Fide-User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

node{
	stage('RELEASE CONFIRMATION'){
	  def inputMessage = "Please provide the RELEASE VERSION for Bona Fide Web App"
	  doScmCheckoutAndGetBuildVersion()
		timeout(time: 30, unit: 'MINUTES') {
		      buildVersion = input(id: 'buildVersion', message: inputMessage, parameters: [
			    [$class: 'TextParameterDefinition', defaultValue: env.BUILD_VERSION , description: 'Build Version', name: 'Release Version']])
		}
	}

	stage('CLEAN BUILD'){
		    def mvnHome = tool name: 'Maven', type: 'maven'
		    def mvnCmd = "${mvnHome}\\bin\\mvn"
		    bat "${mvnCmd} clean package"
	}

	stage('DOCKER IMAGE BUILD'){
			bat "docker build -f Dockerfile -t talk2linojoy/bona-fide-web-app/${buildVersion} ."
	}

	/*stage('DOCKER IMAGE PUSH'){
			withCredentials([string(credentialsId: 'docker-hub-password', variable: 'dockerHubPassword')]) {
				bat "docker login -u talk2linojoy -p ${dockerHubPassword}"	
			}
			bat "docker push talk2linojoy/bona-fide-web-app/${buildVersion}"
	}

	stage('STOPPING RUNNING CONTAINER'){
			script{
				final String currentImageId = bat(script: 'docker ps -q --filter name="bona_fide_web_app_container" --filter status=running',returnStdout: true)
				if(!currentImageId.isEmpty()){
					echo 'Stopping Current Container'
					bat 'docker stop bona_fide_web_app_container'
					echo 'Stopping Container : bona_fide_web_app_container'
					echo 'Renaming Current Container'
					bat 'docker rename bona_fide_web_app_container bona_fide_web_app_container_old'
					echo 'Renamed bona_fide_web_app_container to bona_fide_web_app_container_old'
				}
			}
	}*/
	
	stage('DOCKER CONTAINER RUN'){
		bat "docker run -d -p 9000:9000 --name bona_fide_web_app_container talk2linojoy/bona_fide_web_app/${buildVersion}"
		echo 'Waiting for a minute...' 
		sleep 59
	}
	
	/*stage('DOCKER CONTAINER HEALTH CHECK'){
		  script {
		    final String url = 'http://ec2-13-235-2-41.ap-south-1.compute.amazonaws.com:9000/bona-fide-web-app/build/version'
		    final String response = bat(script: "curl -Is $url | head -1", returnStdout: true).trim()
		    if(response == "HTTP/1.1 200"){
		      final String dockerImageId = bat(script: 'docker ps -q --filter name="^bona_fide_web_app_container$"',returnStdout: true)  
		      if(!dockerImageId.isEmpty()){
			bat 'docker rm bona_fide_web_app_container_old'
			echo 'Successfully removed the previous container' 
			}
			echo "Deployment Successfull,Application Bona Fide Web App is up and running in port 9000 with build version ${buildVersion}"
		    }
		    else{
		      echo 'Deployment Unsuccessfull,please have a look!!!'
		      }
		  }
	}*/

}
def doScmCheckoutAndGetBuildVersion(){
  	git credentialsId: 'bona-fide', url: 'https://github.com/Ante-Meridiem/Bona-Fide-Web-App.git'
	def masterCommit = bat(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
	def now = LocalDateTime.now()
	def currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
	env.BUILD_VERSION = currentDate + "-" + masterCommit
}
