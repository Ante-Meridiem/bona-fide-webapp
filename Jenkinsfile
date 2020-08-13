//BONA-FIDE-WEB-APP
node{

	stage('RELEASE CONFIRMATION'){
		def inputMessage = "Please provide the RELEASE VERSION for Bona Fide Web App"
		getBuildVersion()
        	timeout(time: 30, unit: 'MINUTES') {
            		buildVersion = input(id: 'buildVersion', message: inputMessage, parameters: [
                    	[$class: 'TextParameterDefinition', defaultValue: env.BUILD_VERSION , description: 'Build Version', name: 'Release Version']])
        	}
	}
	
	stage('GETTING JAR FROM BUILD'){
		sh label: '', script: '''sudo mkdir -p target'''
		sh label: '', script: '''sudo cp /home/ec2-user/Bona-Fide-Web-App/target/bona-fide-web-app.jar target'''
		sh label: '', script: 'sudo cp /home/ec2-user/Bona-Fide-Web-App/Dockerfile .'
	}
	
    	stage('DOCKER IMAGE BUILD'){
		sh "docker build -f Dockerfile -t talk2linojoy/bona-fide-web-app/${buildVersion} ."
	}
	
	/*stage('DOCKER IMAGE PUSH'){
		withCredentials([string(credentialsId: 'docker-hub-password', variable: 'dockerHubPassword')]) {
			sh "docker login -u talk2linojoy -p ${dockerHubPassword}"	
		}
		sh "docker push talk2linojoy/bona-fide-web-app/${buildVersion}"
	}*/
	
	stage('STOPPING RUNNING CONTAINER'){
		script{
			final String currentImageId = sh(script: 'docker ps -q --filter name="^bona_fide_web_app_container$"',returnStdout: true)
			if(!currentImageId.isEmpty()){
				echo 'Stopping Current Container'
				sh 'docker stop bona_fide_web_app_container'
				echo 'Stopping Container : bona_fide_web_app_container'
				echo 'Renaming Current Container'
				sh 'docker rename bona_fide_web_app_container bona_fide_web_app_container_old'
				echo 'Renamed bona_fide_web_app_container to bona_fide_web_app_container_old'
			}
		}	
	}
	
	stage('DOCKER CONTAINER RUN'){
		sh "docker run -d -p 9000:9000 --name bona_fide_web_app_container talk2linojoy/bona-fide-web-app/${buildVersion}"
		echo 'Waiting for a minute...' 
		sleep 59
	}
	
	stage('HEALTH CHECK'){
		script {
            final String url = 'http://ec2-13-235-2-41.ap-south-1.compute.amazonaws.com:9000/index.html'
            final String response = sh(script: "curl -Is $url | head -1", returnStdout: true).trim()
			if(response == "HTTP/1.1 200"){
				final String dockerImageId = sh(script: 'docker ps -q --filter name="^bona_fide_web_app_container_old$"',returnStdout: true)  
				if(!dockerImageId.isEmpty()){
					sh 'docker rm bona_fide_web_app_container_old'
					echo 'Successfully removed the previous container'
                    sh 'docker rmi -f $(docker inspect bona_fide_web_app_container_old --format=\'{{.Image}}\')'
                    echo 'Successfully removed the previous container Image Id'
				}
				echo "Deployment Successfull,Application Bona Fide Web App is up and running in port 9000 with build version ${buildVersion}"
			}
			else{
				echo 'Deployment Unsuccessfull Please check!!!'
			}
        }
	}
	
}

def getBuildVersion(){
	git credentialsId: 'bona-fide', url: 'git@github.com:Ante-Meridiem/Bona-Fide-Web-App.git'
	def masterCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
	def currentDate = sh(returnStdout: true, script: 'date +%Y-%m-%d').trim()
	env.BUILD_VERSION = currentDate + "-" + masterCommit
}
