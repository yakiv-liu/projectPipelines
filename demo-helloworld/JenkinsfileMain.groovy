@Library('jenkins-pipeline-library@master')_

def isPR = env.CHANGE_ID != null
print "change_id的值是：${env.CHANGE_ID}"
print "ispr的值: ${isPR}"
print "branch name is: ${env.BRANCH_NAME}"

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'PROJECT_REPO_URL', defaultValue: 'git@github.com:yakiv-liu/demo-helloworld.git', description: '项目代码仓库 URL'),
                string(name: 'PROJECT_BRANCH', defaultValue: 'master', description: '项目代码分支（默认：master）'),
                // ========== 修改点1：更新构建模式描述 ==========
                choice(name: 'BUILD_MODE', choices: ['full-pipeline', 'build-only', 'deploy-only'], description: '''构建模式选择：
• full-pipeline: 完整流水线（构建+部署）- 自动生成版本号
• build-only: 仅构建（会推送Docker镜像到仓库）- 自动生成版本号  
• deploy-only: 仅部署（需要从下方选择部署版本）'''),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
                // ========== 修改点2：使用正确的 Active Choices 参数语法 ==========
                [
                        $class: 'CascadeChoiceParameter',
                        choiceType: 'PT_SINGLE_SELECT',
                        name: 'DEPLOY_VERSION',
                        referencedParameters: 'BUILD_MODE',
                        script: [
                                $class: 'GroovyScript',
                                script: [
                                        script: '''
                                import groovy.sql.Sql
                                
                                // 如果是deploy-only模式，从数据库加载版本
                                if (BUILD_MODE == "deploy-only") {
                                    try {
                                        // 数据库连接配置 - 需要根据您的环境调整
                                        def dbUrl = "jdbc:postgresql://192.168.233.8:5432/jenkins_deployments"
                                        def dbUser = "sonar"
                                        def dbPassword = "sonar123"
                                        def driver = "org.postgresql.Driver"
                                        
                                        // 加载驱动
                                        Class.forName(driver)
                                        def sql = Sql.newInstance(dbUrl, dbUser, dbPassword, driver)
                                        
                                        // 查询最近10个成功构建的版本
                                        def versions = sql.rows("""
                                            SELECT version 
                                            FROM build_records 
                                            WHERE project_name = ? AND build_status = 'SUCCESS'
                                            ORDER BY build_timestamp DESC 
                                            LIMIT 10
                                        """, ['demo-helloworld'])
                                        
                                        sql.close()
                                        
                                        // 返回版本列表
                                        def versionList = versions.collect { it.version }
                                        return versionList ?: ["暂无可用版本"]
                                        
                                    } catch (Exception e) {
                                        return ["数据库连接失败: " + e.message]
                                    }
                                } else {
                                    return ["请选择deploy-only模式以显示版本列表"]
                                }
                            ''',
                                        fallbackScript: 'return ["加载版本列表失败"]'
                                ]
                        ]
                ],
                // === 控制是否跳过依赖检查 ===
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）'),
        ])
])

// 调用共享库，传递所有必要配置
mainPipeline([
        // 基础配置
        projectName: params.PROJECT_NAME,
        projectRepoUrl: params.PROJECT_REPO_URL,
        projectBranch: params.PROJECT_BRANCH,
        org: 'yakiv-liu',
        repo: 'demo-helloworld',
        agentLabel: 'docker-jnlp-slave',
        defaultEmail: params.EMAIL_RECIPIENTS,
        deployEnv: params.DEPLOY_ENV,
        buildMode: params.BUILD_MODE,
        // ========== 修改点3：传递部署版本参数 ==========
        deployVersion: params.DEPLOY_VERSION,
        // === 传递跳过依赖检查参数 ===
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
        // 项目特定配置
        appPort: 8085,
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])