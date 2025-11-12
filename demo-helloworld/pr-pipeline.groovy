@Library('jenkins-pipeline-library@master')_

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
                // PR 流水线特定参数
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）'),
                choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
        ]),
        pipelineTriggers([
                [
                        $class: 'GitHubPushTrigger',
                        adminlist: '',
                        allowWhiteList: false,
                        branchRestriction: '',  // PR 不限制分支
                        cron: '',
                        triggerForBranch: false,
                        triggerForPr: true,     // 启用 PR 触发
                        whiteList: ''
                ]
        ]),
        // 添加 GitHub 项目配置
        [
                $class: 'GitHubProjectProperty',
                projectUrlStr: 'https://github.com/yakiv-liu/demo-helloworld/',
                displayName: ''
        ]
])

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    stages {
        stage('Run PR Pipeline') {
            steps {
                script {
                    // 调用共享库的PR流水线
                    prPipeline([
                            // 基础配置
                            projectName: params.PROJECT_NAME,
                            org: 'yakiv-liu',
                            repo: 'demo-helloworld',
                            agentLabel: 'docker-jnlp-slave',
                            defaultBranch: 'main',
                            defaultEmail: params.EMAIL_RECIPIENTS,

                            // PR 特定配置
                            skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
                            scanIntensity: params.SCAN_INTENSITY
                    ])
                }
            }
        }
    }
}