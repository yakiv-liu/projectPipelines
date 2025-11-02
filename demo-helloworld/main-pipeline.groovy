@Library('jenkins-pipeline-library@v1.0.0')_

pipeline {
    agent {
        label 'centos-slave'  // 这个项目指定在centos-slave上运行
    }
    
    parameters {
        string(
            name: 'PROJECT_NAME',
            defaultValue: 'demo-helloworld',
            description: '项目名称'
        )
        choice(
            name: 'DEPLOY_ENV',
            choices: ['staging', 'pre-prod', 'prod'],
            description: '选择部署环境'
        )
        booleanParam(
            name: 'ROLLBACK',
            defaultValue: false,
            description: '是否执行回滚'
        )
        string(
            name: 'ROLLBACK_VERSION',
            defaultValue: '',
            description: '回滚版本号（格式: timestamp或tag）'
        )
        booleanParam(
            name: 'IS_RELEASE',
            defaultValue: false,
            description: '是否为正式发布版本'
        )
        string(
            name: 'EMAIL_RECIPIENTS',
            defaultValue: '251934304@qq.com',
            description: '邮件接收人（多个用逗号分隔）'
        )
    }
    
    stages {
        stage('Run Main Pipeline') {
            steps {
                script {
                    // 调用共享库的主流水线，传递用户选择的参数
                    mainPipeline([
                        projectName: params.PROJECT_NAME,
                        org: 'yakiv-liu',
                        repo: 'demo-helloworld',
                        agentLabel: 'centos-slave',
                        defaultEmail: params.EMAIL_RECIPIENTS,
                        environments: ['staging', 'pre-prod', 'prod'],
                        // 传递用户选择的参数给共享库
                        deployEnv: params.DEPLOY_ENV,
                        rollback: params.ROLLBACK,
                        rollbackVersion: params.ROLLBACK_VERSION,
                        isRelease: params.IS_RELEASE
                    ])
                }
            }
        }
    }
}
