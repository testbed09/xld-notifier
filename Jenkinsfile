// TODO : look at putting notification in share library if works i.e. @Library('bitwiseman-shared@blog/declarative/notifications')
//************* notification snippet *********************
emailNotifications = 'evdb@x-idra.de'
notificationSent = false

def sendNotification(buildChanged)
{
    if (notificationSent)
    {
        return
    }
    notificationSent = true

    if (currentBuild.currentResult == 'SUCCESS')
    {
        // notify users when the build is back to normal
        mail to: emailNotifications,
            subject: "Build fixed: ${currentBuild.fullDisplayName}",
            body: "The build is back to normal ${env.BUILD_URL}"
    }
    else if ((currentBuild.currentResult == 'FAILURE') && buildChanged)
    {
        // notify users when the Pipeline first fails
        mail to: emailNotifications,
            subject: "Build failed: ${currentBuild.fullDisplayName}",
            body: "Something went wrong with ${env.BUILD_URL}"
    }
    else if ((currentBuild.currentResult == 'FAILURE'))
    {
        // notify users when they check into a broken build
        mail to: emailNotifications,
            subject: "Build failed (again): ${currentBuild.fullDisplayName}",
            body: "Something is still wrong with ${env.BUILD_URL}"
    }
}
//***************** end *****************

def releaseTag = null
def isReleaseBuild = false
def versionSuffix = "+build.${env.BUILD_ID}"

pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '60', artifactNumToKeepStr: '60', daysToKeepStr: '60', artifactDaysToKeepStr: '60'))
    }
    //TODO: Obtain from maven in future.
    environment {
        projectKey = 'xld-notifier-XLR-test'
        projectName = 'xld-notifier-XLR-test'
        projectVersion = '0.2'
        gitTag = sh(returnStdout: true, script: "git tag --contains").trim()
    }
    agent any
    stages {
        stage('Preparation') { // for display purposes
            steps{
            	script {
            		sh "env"

					if (env.gitTag != null) {
	            		
	            		// we are in a tag and its in the format "RELEASE/something"
	            		if (env.gitTag.startsWith("RELEASE/") ) {
	            			sh "echo In RELEASE tag"
	            			isReleaseBuild = true
	            			versionSuffix = "-RELEASE+build.${env.BUILD_ID}"
	            		}
	            		
	            		checkout scm: [$class: 'GitSCM', userRemoteConfigs: [
	            			[url: "$GIT_URL", credentialsId: '563fccd2-3676-41c9-bb63-e4c2fc5fa292']
	            		], branches: [
	            			[name: "refs/tags/${gitTag}"]
	            			]
	            		],poll: false
	            		
	            		sh "env"
	            		
	// 	                git( 
	// 	                	credentialsId: '563fccd2-3676-41c9-bb63-e4c2fc5fa292',
	// 	                	url: "$GIT_URL",
	// 	                	branch: "refs/tags/${gitTag}"
	// 	                )
	
	            		 
	            	} else {
	            	
	            		sh "echo In $GIT_BRANCH branch"
	            		sh "env"
		                git( 
		                	credentialsId: '563fccd2-3676-41c9-bb63-e4c2fc5fa292',
		                	url: "$GIT_URL",
		                	branch: "$GIT_BRANCH"
		                )
	            	}
	            }
            }
        }
        stage('Build') {
            steps {
                    script {
                        // Get the Maven tool.
                        def mvnHome = tool 'Default, 3.5.2'

						releaseTag = sh(returnStdout: true, script: "git tag --contains").trim()
						if (releaseTag != null && releaseTag.startsWith("RELEASE/")) {
	            			sh "echo In RELEASE tag"
	            			isReleaseBuild = true
	            			versionSuffix = "-RELEASE+build.${env.BUILD_ID}"
						}

                    	sh "mvn -Dversion.suffix=${versionSuffix} clean test package dependency:copy-dependencies"
                    }
            }
        }
        stage('SonarQube') {
            when {
            	branch 'master'
            }
            steps {
                    script {

                        def scannerHome = tool 'Default, 3.0.3'

                        // Send to SonarQube for analysis
                        withSonarQubeEnv('Default, 5.6.6') {
                            sh "${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=\"${env.projectKey}\" \
                            -Dsonar.projectName=\"${env.projectName}\" \
                            -Dsonar.projectVersion=\"${env.projectVersion}\""
                        }

                    }
        	}
        }
	    stage('Quality Gate') { // This is outside the stages block, so shouldn't tie up the agent.
            when { 
            	branch 'sdlkjsdfsdklfdlkfjklfj'
            	//branch 'master' 
            }	    
	        steps {
	            timeout(time: 1, unit: 'HOURS') { // Just in case something goes wrong, pipeline will be killed after a timeout
	                script {
	                    def scannerHome = tool 'Default, 3.0.3'
	                    
                        // waitForQualityGate seems to be broken. I think it is waiting
                        // for a webhook from sonar, but our sonar is too old.
                        // Therefore, just wait about 20 seconds here so that when
                        // waitForQualityGate is called, we have a result already. Yuck!
                       	sh 'sleep 20'

	                    qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
	                    if (qg.status != 'OK') {
	                        error "Pipeline aborted due to quality gate failure: ${qg.status}"
	                    }
	                }
	            }
	        }
	    }
        stage('Deliver to Nexus') {
        	when {
        		expression { isReleaseBuild == true }
        	}
            steps {
                script {
                    // Get the Maven tool.
                    def mvnHome = tool 'Default, 3.5.2'
                    sh "mvn install -Dversion.suffix=${versionSuffix} -DskipTests -Djacoco.skip -P release-install"
                }
            }
        }
	}
    post {
        always {
            //junit 'target/surefire-reports/TEST-*.xml'
        }
        success {
            //archive 'target/*.jar'
            //archive 'target/delivery.zip'
        }
        changed {
            sendNotification buildChanged:true
        }
        failure {
            sendNotification buildChanged:false
        }
   }
}
