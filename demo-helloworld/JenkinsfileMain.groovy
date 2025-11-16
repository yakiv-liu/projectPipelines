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
                // ========== 修改点1：增加 deploy-only 模式，更新描述 ==========
                choice(name: 'BUILD_MODE', choices: ['full-pipeline', 'build-only', 'deploy-only'],
                        description: '''构建模式选择：
                        • full-pipeline: 完整流水线（构建+部署）
                        • build-only: 仅构建（不需要填写部署相关参数）
                        • deploy-only: 仅部署（需要填写部署版本）'''),
                choice(name: 'DEPLOY_ENV', choices: ['staging', 'pre-prod', 'prod'], description: '部署环境'),
                // ========== 修改点2：移除回滚相关参数 ==========
                // booleanParam(name: 'ROLLBACK', defaultValue: false, description: '是否回滚'),
                // string(name: 'ROLLBACK_VERSION', defaultValue: '', description: '回滚版本号'),
                string(name: 'DEPLOY_VERSION', defaultValue: '', description: '部署版本号（仅在deploy-only模式下需要填写，格式如：20241120123045）'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
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
        // ========== 修改点4：移除回滚相关配置 ==========
        // rollback: params.ROLLBACK.toBoolean(),
        // rollbackVersion: params.ROLLBACK_VERSION,
        buildMode: params.BUILD_MODE,
        // ========== 修改点5：传递部署版本参数 ==========
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