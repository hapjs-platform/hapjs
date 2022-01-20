pipeline {
    options {
        timeout(time: 1, unit: 'HOURS')
    }
    agent {
        docker {
            image 'gengjiawen/android-ndk:qa-2020-07-31'
            args '-v /gradle/.gradle:/root/.gradle -v $HOME/.npm:/root/.npm'
        }
    }
    stages {
        stage('Build and Test Android') {
            steps {
                sh 'npx envinfo'
                sh 'git clean -xdf'
                sh 'git log -6 --oneline'
                sh 'cd $WORKSPACE/mockup/platform/android && ./gradlew --no-daemon clean assembleDebug assembleRelease'
                archiveArtifacts 'mockup/platform/android/app/build/outputs/apk/**'
            }
        }
        stage('Build and Test Frontend') {
            steps {
                sh 'npx envinfo'
                sh 'cd $WORKSPACE/core/framework && npm install && npm run build && npm run test:suite'
            }
        }
        stage('Quality Check') {
            steps {
                sh 'cd $WORKSPACE/mockup/platform/android && ./gradlew --no-daemon spotbugs lintPhoneDebug lintTvDebug'
            }
        }
        stage('Analysis Report') {
            steps {
                script {
                    spotbugs = scanForIssues tool: spotBugs(pattern: 'mockup/platform/android/build/reports/spotbugs*.xml'), 
                        filters: [includePackage('org.hapjs.*'), excludeFile('R.java')]
                }
                publishIssues name: 'Spotbugs', issues: [spotbugs]

                script {
                    androidLint = scanForIssues tool: androidLintParser(pattern: 'mockup/platform/android/build/reports/lint-results*.xml')
                }
                publishIssues name: 'Android Lint', issues: [androidLint]

                publishIssues id: 'analysis', name: 'All Issues', issues: [spotbugs, androidLint], failedNewHigh: 1
            }
        }
    }
}
