# Cloud Storage:Reborn / 云存储：涅槃

Minecraft 1.21.1 的 NeoForge 迁移版，模组 ID 为 `cloudstorage_reborn`。
版本：1.0.0。作者：Oritong（Original author:Alexthe668, Yosh）。迁移源码基于用户提供的 CloudStorage 源码。

## 安装

- Minecraft：1.21.1。
- NeoForge：21.1.203 或更高的 21.1.x 版本。
- Java：21。
- 前置：Citadel 2.7.1（1.21.1），使用用户提供的已编译 JAR。

把发布 JAR 与 `citadel-2.7.1-1.21.1.jar` 一起放入游戏实例的 `mods` 文件夹。客户端与服务器均需要安装。
配置文件：`config/cloudstorage_reborn-common.toml`。

## 迁移内容

- 更新 NeoForge 注册、事件、网络载荷、客户端渲染与物品自动化接口。
- 更新 1.21.1 物品数据组件，保留气球染色、静电/战利品状态、伙伴数据及吹云器燃料/模式。
- 更新云库存储和扩容的序列化，并修复缓存的自动化接口在扩容后的库存引用。
- 更新配方、进度、战利品、唱片、世界生成与村庄建筑资源。
- 更新新版 Citadel 指南书数据与渲染，并修复书页背景绘制顺序及中文正文换行。
- 同步修改所有注册项、资源、建筑模板、语言键、网络标识与存档数据名至新模组 ID。
- 移除 Alex’s Mobs 联动资源；本模组不依赖 Alex’s Mobs。

新模组 ID 不会自动替换旧存档中的 `cloudstorage:*` 注册项；旧模组世界数据也不自动转移到新 ID。

## 构建与验证

在项目目录使用 Java 21 运行：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat -PclientSmokeTest runClient
```

产物位于 `build/libs/cloudstorage_reborn-1.0.0-1.21.1-neoforge.jar`。本地 Citadel 编译依赖放在 `libs/`。首次构建需要下载 Gradle/NeoForge/Minecraft 依赖。

已通过编译和 4 项服务器 GameTest：云库存档/扩容、静电云箱自动化与实体创建、染色/气球箭/物品状态/唱片/20 个进度及伙伴性格判定/战利品、私人云箱菜单排序/搜索/滚动与 11 个建筑模板加载。客户端已完成中文名称、全部物品图标/模型、7 页指南书数据及指南书首页渲染检查。

开发测试类与空测试建筑均从发布 JAR 中排除。验证未覆盖长期生存流程与实际多人联机。
