@Library('jenkins-pipeline-library@master')_

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                booleanParam(name: 'ROLLBACK', defaultValue: false, description: '是否回滚'),
                string(name: 'ROLLBACK_VERSION', defaultValue: '', description: '回滚版本号'),
                booleanParam(name: 'IS_RELEASE', defaultValue: false, description: '正式发布'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人')
        ])
])

// 直接调用共享库，不包装在 pipeline 块中
mainPipeline([
        projectName: params.PROJECT_NAME,
        deployEnv: params.DEPLOY_ENV,
        rollback: params.ROLLBACK.toBoolean(),
        rollbackVersion: params.ROLLBACK_VERSION,
        isRelease: params.IS_RELEASE.toBoolean(),
        defaultEmail: params.EMAIL_RECIPIENTS,

        // 项目特定配置
        appPort: 8080,
        environmentHosts: [
                staging: [host: '192.168.233.10'],
                'pre-prod': [host: '192.168.233.11'],
                prod: [host: '192.168.233.12']
        ]
])