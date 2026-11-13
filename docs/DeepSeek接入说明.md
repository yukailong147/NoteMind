# DeepSeek 接入说明

DeepSeek 已确认支持通过 OpenAI 兼容对话接口传入图片。本项目第一个真实模型使用：

```text
接口：https://api.deepseek.com/chat/completions
模型：deepseek-flash
图片格式：JPEG
图片传输：Base64 data URL
```

项目已经把接口地址和模型名称设为默认值。开发者只需要提供自己的 API Key。

## 两个模型的区别

| 能力 | deepseek-flash | deepseek-v4-pro |
|---|---|---|
| 图像理解 | 支持 | 不支持 |
| 板书图片整理 | 可以使用 | 不能直接使用 |
| 百万 tokens 输入价格 | 低 | 更高 |
| 百万 tokens 输出价格 | 低 | 更高 |
| 适合场景 | 图片整理、快速总结 | 已转成文字后的精细推理 |

因此第一版必须使用 `deepseek-flash`。以后增加 `deepseek-v4-pro` 时，只能把已经提取出的文字交给它，不能直接把课堂板书图片发给它。

官方页面显示的输入和输出价格会按时段变化，高峰时段价格更高。团队应先进行少量图片测试，确认效果后再批量使用。

## 第一步：创建 API Key

1. 打开 DeepSeek 开放平台。
2. 创建 API Key。
3. 检查账户余额，确认可以调用 `deepseek-flash`。
4. 不要把 API Key 发到聊天、群聊、代码仓库或截图里。

## 第二步：写入本机配置

打开：

```text
C:\Users\11469\Documents\Codex\2026-11-09\c-android\outputs\ClassroomNotes\local.properties
```

在文件末尾单独增加一行：

```properties
AI_API_KEY=你的 DeepSeek API Key
```

不需要再添加接口地址和模型名称，因为项目已经使用默认值。

只有确实需要临时更换模型时才增加：

```properties
AI_ENDPOINT=https://api.deepseek.com/chat/completions
AI_MODEL=deepseek-flash
```

`local.properties` 已写入 `.gitignore`，不会进入 Git。

## 第三步：重新同步和运行

1. 回到 Android Studio。
2. 点击 `File` → `Sync Project with Gradle Files`。
3. 点击 `Build` → `Rebuild Project`。
4. 启动模拟器或连接手机。
5. 运行应用。

应用会自动从模拟生成器切换到真实 DeepSeek。
应用已经开启 DeepSeek 的 JSON Output。如果模型没有遵守字段要求或返回内容无法解析，应用会进入失败状态，不会把不完整结果当作成功。

## 第四步：测试

建议按顺序测试：

1. 使用“测试：生成示例板书”。
2. 观察“整理中”状态。
3. 正常情况下，DeepSeek 会返回标题、摘要、知识点、问题和关键词。
4. 返回首页，确认笔记保存成功。
5. 重新打开应用，确认仍然存在。
6. 再使用一张真实手机拍摄的板书测试。

如果使用模拟器默认摄像头，得到的绿色虚拟场景图片虽然能上传，但不能验证真实板书识别效果。真实板书测试应使用手机。

## 常见错误

### HTTP 401

API Key 不正确或没有正确写入 `local.properties`。重新检查 Key，并重新 Sync、Rebuild。

### HTTP 402

账户余额不足。到 DeepSeek 平台检查余额和价格。

### HTTP 400

请求格式或图片内容有问题。保存应用显示的错误文字，不要多次重复请求。

### 返回 JSON 字段不完整

模型没有按约定输出结构化字段。应用会进入失败状态并保留原图，可以点击重试。

### 请求超时

检查网络是否可以访问 `api.deepseek.com`。当前超时设置为连接 20 秒、读取 60 秒。

## 图片大小

DeepSeek 当前 Base64 请求体上限为 48 MiB。应用会先把图片长边缩放到不超过 1600 像素，再以 JPEG 质量 82 压缩，通常远小于该限制。

支持格式包括 JPEG、PNG、GIF 和 WebP。本项目拍照后使用 JPEG。

## 当前为何只启用一个真实模型

官方模型表确认 `deepseek-flash` 支持图片输入，而 `deepseek-v4-pro` 不支持。第一个版本先使用 Flash 验证完整流程。等真实板书测试通过后，再为文字问答或二次整理加入 Pro 模型。

## 最终版本的安全限制

把 Key 写入 `local.properties` 只是开发和比赛准备阶段的做法。构建后的 APK 仍可能被逆向并提取 Key。

正式公开版本应通过一个很小的中转服务调用 DeepSeek，手机端只提交任务和模型编号，不保存 DeepSeek Key。

