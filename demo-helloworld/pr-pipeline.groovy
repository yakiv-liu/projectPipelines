@Library('jenkins-pipeline-library@master')_

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }
    
    parameters {
        string(
            name: 'PROJECT_NAME',
            defaultValue: 'demo-helloworld',
            description: '项目名称'
        )
        string(
            name: 'EMAIL_RECIPIENTS',
            defaultValue: '251934304@qq.com',
            description: '邮件接收人（多个用逗号分隔）'
        )
    }
    
    stages {
        stage('Run PR Pipeline') {
            steps {
                script {
                    // 调用共享库的PR流水线
                    prPipeline([
                        projectName: params.PROJECT_NAME,
                        org: 'yakiv-liu',
                        repo: 'demo-helloworld',
                        agentLabel: 'docker-jnlp-slave',
                        defaultBranch: 'main',
                        defaultEmail: params.EMAIL_RECIPIENTS
                    ])
                }
            }
        }
    }
}
