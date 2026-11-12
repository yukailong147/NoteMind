# ClassroomNotes

当前版本的 Android 实现：

```text
输入课程
→ 调用系统相机或生成测试板书
→ 保存待整理草稿
→ 使用模拟或真实 AI 生成笔记
→ 保存图片和 notes.json
→ 显示待整理、整理中、成功或失败
→ 失败后保留草稿并允许重试
→ 重启后重新读取
```

## 当前阶段

- 使用 Kotlin 和 Android 传统 View 界面。
- 没有 AI 配置时使用模拟生成器，不要求联网。
- 配置接口地址、模型和密钥后自动使用真实 AI。
- 拍完照先保存草稿，再进入整理状态。
- 网络失败时保留原图、草稿和错误信息。
- 图片保存在应用私有目录 `files/photos`。
- 笔记保存在应用私有目录 `files/notes.json`。
- 重启应用后可以再次读取。

## 运行方法

1. 用 Android Studio 打开本目录。
2. 等待 Gradle Sync 完成。
3. 启动模拟器 `Medium_Phone_API_37.0`。
4. 点击运行按钮。
5. 点击“测试：生成示例板书”可以在没有相机的情况下验证保存流程。

## 目录说明

- `MainActivity.kt`：首页、拍照、示例图片和笔记列表。
- `NoteDetailActivity.kt`：笔记详情和图片显示。
- `NoteItem.kt`：一条笔记的数据结构。
- `NoteStore.kt`：把笔记保存为 JSON，并在启动时读回。
- `activity_main.xml`：首页布局。
- `activity_note_detail.xml`：详情页布局。
- `item_note.xml`：列表中的单条笔记卡片。

## 代码讲解

详细运行步骤、验收方法、关键代码和数据流见：

```text
docs/第一条功能链说明.md
docs/AI接入与状态说明.md
```

