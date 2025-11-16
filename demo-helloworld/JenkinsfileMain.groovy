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
                choice(name: 'BUILD_MODE', choices: ['full-pipeline', 'build-only', 'deploy-only'], description: '''构建模式选择：
• full-pipeline: 完整流水线（构建+部署）- 自动生成版本号
• build-only: 仅构建（会推送Docker镜像到仓库）- 自动生成版本号  
• deploy-only: 仅部署（需要从下方选择部署版本）'''),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),

                // ========== 修改：直接在脚本中实现数据库连接 ==========
                [
                        $class: 'CascadeChoiceParameter',
                        choiceType: 'PT_SINGLE_SELECT',
                        name: 'DEPLOY_VERSION',
                        referencedParameters: 'BUILD_MODE,PROJECT_NAME',
                        script: [
                                $class: 'GroovyScript',
                                script: [
                                        script: '''
                                            import groovy.sql.Sql
                                            import java.sql.DriverManager
                                            import java.net.URLClassLoader
                                            import java.io.File
                                            
                                            // 只有当选择deploy-only模式时才显示版本列表
                                            if (BUILD_MODE != "deploy-only") {
                                                return ["请选择deploy-only模式以显示版本列表"]
                                            }
                                            
                                            try {
                                                // 数据库连接配置 - 使用您的实际配置
                                                def dbUrl = "jdbc:postgresql://192.168.233.8:5432/jenkins_deployments"
                                                def dbUser = "sonar"
                                                def dbPassword = "sonar123"
                                                def driverClassName = "org.postgresql.Driver"
                                                
                                                // ========== 参照 DatabaseTools.groovy 的驱动加载方式 ==========
                                                def driverInstance = null
                                                
                                                try {
                                                    // 首先尝试直接加载（如果已经加载过）
                                                    driverInstance = Class.forName(driverClassName).newInstance()
                                                } catch (ClassNotFoundException e) {
                                                    // 驱动类未找到，从已知路径加载
                                                    def driverPath = "/tmp/jenkins-libs/postgresql.jar"
                                                    
                                                    // 检查文件是否存在
                                                    def driverFile = new File(driverPath)
                                                    if (!driverFile.exists()) {
                                                        return ["驱动文件不存在: ${driverPath}"]
                                                    }
                                                    
                                                    try {
                                                        // 使用URLClassLoader动态加载
                                                        def urlClassLoader = new URLClassLoader(
                                                            [driverFile.toURI().toURL()] as URL[],
                                                            this.class.classLoader
                                                        )
                                                        
                                                        driverInstance = urlClassLoader.loadClass(driverClassName).newInstance()
                                                        
                                                    } catch (Exception ex) {
                                                        return ["从已知路径加载驱动失败: " + ex.message]
                                                    }
                                                }
                                                
                                                if (!driverInstance) {
                                                    return ["无法加载数据库驱动"]
                                                }
                                                
                                                // 建立连接
                                                def connection = null
                                                try {
                                                    // 尝试通过驱动实例建立连接
                                                    def props = new Properties()
                                                    props.setProperty("user", dbUser)
                                                    props.setProperty("password", dbPassword)
                                                    
                                                    connection = driverInstance.connect(dbUrl, props)
                                                    if (connection == null) {
                                                        // 备选方案：尝试注册到DriverManager
                                                        DriverManager.registerDriver(driverInstance)
                                                        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
                                                    }
                                                } catch (Exception e) {
                                                    return ["数据库连接失败: " + e.message]
                                                }
                                                
                                                if (!connection) {
                                                    return ["无法建立数据库连接"]
                                                }
                                                
                                                def sql = new Sql(connection)
                                                
                                                // 查询最近10个成功构建的版本
                                                def query = """
                                                    SELECT version, build_timestamp 
                                                    FROM build_records 
                                                    WHERE project_name = ? AND build_status = 'SUCCESS'
                                                    ORDER BY build_timestamp DESC 
                                                    LIMIT 10
                                                """
                                                def versions = sql.rows(query, [PROJECT_NAME])
                                                sql.close()
                                                connection.close()
                                                
                                                if (versions.empty) {
                                                    return ["暂无可用版本，请先执行构建"]
                                                }
                                                
                                                // 返回版本列表，格式：版本号 (构建时间)
                                                return versions.collect { row -> 
                                                    def time = new Date(row.build_timestamp.time).format("MM-dd HH:mm")
                                                    "${row.version} (${time})"
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

// 辅助方法：从选择项中提取纯版本号
def extractVersionFromChoice(choiceValue) {
    if (!choiceValue) return ""

    // ========== 修正正则表达式语法错误 ==========
    // 处理格式："20241120143025 (11-20 14:30)"
    def matcher = choiceValue =~ /^(\d+)\s*\(/
    if (matcher.find()) {
        return matcher[0][1]
    }
    return choiceValue
}

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
        deployVersion: extractVersionFromChoice(params.DEPLOY_VERSION),
        skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
        appPort: 8085,
        environmentHosts: [
                staging: [host: '192.168.233.8'],
                'pre-prod': [host: '192.168.233.9'],
                prod: [host: '192.168.233.10']
        ]
])