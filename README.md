# ClassroomNotes

第一条功能链的 Android 实现：

```text
输入课程
→ 调用系统相机或生成测试板书
→ 使用固定模拟数据生成笔记
→ 保存图片和 notes.json
→ 首页重新读取笔记
→ 打开笔记详情
```

## 当前阶段

- 使用 Kotlin 和 Android 传统 View 界面。
- 不依赖网络，不调用真实 AI。
- 首页拍完照后生成固定结构的模拟笔记。
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
```

