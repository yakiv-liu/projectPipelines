@Library('jenkins-pipeline-library@master')_

def isPR = env.CHANGE_ID != null

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'PROJECT_REPO_URL', defaultValue: 'git@github.com:yakiv-liu/demo-helloworld.git', description: '项目代码仓库 URL'),
                string(name: 'PROJECT_BRANCH', defaultValue: 'master', description: '项目代码分支（默认：master）'),
                choice(name: 'BUILD_MODE', choices: ['full-pipeline', 'build-only', 'deploy-only'], description: '''构建模式选择：
• full-pipeline: 完整流水线（构建+部署）- 自动生成版本号
• build-only: 仅构建（会推送Docker镜像到仓库）- 自动生成版本号  
• deploy-only: 仅部署（需要从下方选择部署版本）'''),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),

                // ========== 核心修改：使用 Active Choices Reactive Parameter ==========
                [
                        $class: 'CascadeChoiceParameter',
                        choiceType: 'PT_SINGLE_SELECT',
                        name: 'DEPLOY_VERSION',
                        referencedParameters: 'BUILD_MODE,PROJECT_NAME',
                        script: [
                                $class: 'GroovyScript',
                                script: [
                                        script: '''
                                // 只有当选择deploy-only模式时才显示版本列表
                                if (BUILD_MODE != "deploy-only") {
                                    return ["请选择deploy-only模式以显示版本列表"]
                                }
                                
                                try {
                                    // 使用共享库中的数据库工具类
                                    def configLoader = new org.yakiv.Config(steps)
                                    def dbTools = new org.yakiv.DatabaseTools(steps, env, configLoader)
                                    
                                    def recentVersions = dbTools.getRecentBuildVersions(PROJECT_NAME, 10)
                                    
                                    if (recentVersions.empty) {
                                        return ["暂无可用版本，请先执行构建"]
                                    }
                                    
                                    // 返回版本列表，格式：版本号 (构建时间)
                                    return recentVersions.collect { version -> 
                                        def time = new Date(version.build_timestamp.time).format("MM-dd HH:mm")
                                        "${version.version} (${time})"
                                    }
                                    
                                } catch (Exception e) {
                                    return ["加载版本失败: " + e.message]
                                }
                            ''',
                                        fallbackScript: 'return ["加载版本列表失败，请检查数据库连接"]'
                                ]
                        ]
                ],

                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）'),
        ])
])

// 调用共享库，传递所有必要配置
mainPipeline([
        projectName: params.PROJECT_NAME,
        projectRepoUrl: params.PROJECT_REPO_URL,
        projectBranch: params.PROJECT_BRANCH,
        org: 'yakiv-liu',
        repo: 'demo-helloworld',
        agentLabel: 'docker-jnlp-slave',
        defaultEmail: params.EMAIL_RECIPIENTS,
        deployEnv: params.DEPLOY_ENV,
        buildMode: params.BUILD_MODE,
        deployVersion: extractVersionFromChoice(params.DEPLOY_VERSION), // 提取纯版本号
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
        appPort: 8085,
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])

// ========== 辅助方法：从选择项中提取纯版本号 ==========
def extractVersionFromChoice(choiceValue) {
    if (!choiceValue) return ""

    // 处理格式："20241120143025 (11-20 14:30)"
    def matcher = choiceValue =~ /^(\d+)\s*\(/
    if (matcher.find()) {
        return matcher[0][1]
    }
    return choiceValue
}