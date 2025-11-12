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
                        $class: 'GitHubPRTrigger',
                        spec: '* * * * *',  // 轮询间隔
                        triggerPhrase: '.*', // 触发短语
                        onlyTriggerPhrase: false,
                        githubHubConfig: [id: 'github'], // 配置的 GitHub server ID
                        permitAll: false,
                        autoCloseFailed: true,
                        allowMembersOfWhitelistedOrgsAsAdmin: true,
                        orgsList: [],
                        whiteListTargetBranches: [],
                        blackListTargetBranches: [],
                        blackListLabels: [],
                        whiteListLabels: [],
                        adminList: '',
                        cron: '* * * * *',
                        useGitHubHooks: true  // 使用 webhook 而不是轮询
                ]
        ])
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