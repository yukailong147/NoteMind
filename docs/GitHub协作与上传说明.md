# GitHub 协作与上传说明

## 最重要的一条

不要把 `local.properties`、DeepSeek API Key 或包含真实 Key 的 APK 上传到 GitHub。

本项目的 `.gitignore` 已经排除：

```text
local.properties
app/build
.gradle
dist/*.apk
```

提交前仍要检查：

```powershell
git status
git diff --cached
```

如果看到 `local.properties` 或以 `sk-` 开头的密钥，立即停止提交。

## 第一次创建共享仓库

1. 登录 GitHub，点击右上角 `+`，选择 `New repository`。
2. 填写仓库名称，例如 `ClassroomNotes`。
3. 选择 `Private`，暂时只允许队友查看。
4. 不要勾选自动创建 README、`.gitignore` 或 License。
5. 点击 `Create repository`。
6. 进入仓库的 `Settings` → `Collaborators`，邀请队友的 GitHub 账号。

保持仓库为空很重要。本机已经有完整的 Git 提交历史，如果 GitHub 端也创建 README，首次上传会发生冲突。

## 第一次上传本机代码

在 Android Studio 底部的 `Terminal` 中运行。把仓库地址换成自己的地址：

```powershell
git remote add origin https://github.com/你的用户名/ClassroomNotes.git
git branch -M main
git push -u origin main
```

三条命令的含义：

- `git remote add origin ...`：告诉本机代码要上传到哪个 GitHub 仓库。
- `git branch -M main`：把本地主分支改名为 GitHub 常用的 `main`。
- `git push -u origin main`：第一次上传，并记住以后默认上传到这里。

如果提示 `remote origin already exists`，说明已经配置过远端，不要重复添加。可以查看地址：

```powershell
git remote -v
```

## 队友第一次获取代码

队友先安装 Git 和 Android Studio，然后运行：

```powershell
git clone https://github.com/你的用户名/ClassroomNotes.git
```

在 Android Studio 中选择：

```text
File → Open → 刚才克隆出来的 ClassroomNotes 文件夹
```

等待 Gradle Sync 完成即可运行。没有 API Key 时，应用会使用模拟 AI，不影响界面和流程测试。

需要真实 DeepSeek 的队友在自己的 `local.properties` 中配置：

```properties
AI_API_KEY=自己的Key
AI_ENDPOINT=https://api.deepseek.com/chat/completions
AI_IMAGE_MODEL=deepseek-flash
```

每个人使用自己的 Key，不要把 Key 发进群，也不要把配置后的文件提交到 Git。

## 日常协作流程

`main` 分支应保持可运行。每个人开发新功能时创建自己的分支：

```powershell
git pull
git checkout -b feature-note-export
```

完成修改后：

```powershell
git status
git add .
git commit -m "feat: add note export"
git push -u origin feature-note-export
```

然后到 GitHub 页面创建 Pull Request，让另一名队友检查后再合并。不要所有人都直接向 `main` 推送。

## 上传 APK

APK 是安装包，不建议反复提交进源码目录。推荐使用 GitHub 的 Release：

1. 打开共享仓库首页。
2. 点击右侧 `Releases` 或 `Create a new release`。
3. Tag 填写 `v1.0.0`。
4. 标题填写 `ClassroomNotes v1.0.0`。
5. 把 `ClassroomNotes-v1.0-demo.apk` 拖入附件区域。
6. 点击 `Publish release`。

队友从 Release 下载 APK 即可安装；需要改代码时再克隆源码。

## 禁止使用的命令

以下命令会覆盖或丢失队友的工作，新成员不要使用：

```text
git push --force
git reset --hard
git clean -fd
```

遇到冲突时先截图或复制完整报错，不要自行删除 `.git` 文件夹。
