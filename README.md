# Cloud Storage:Reborn

**云存储：涅槃** · Minecraft 1.21.1 · NeoForge · v1.0.0

[中文](#中文) | [English](#english)

## 中文

把箱子绑上气球，送到天上，再用同色气球连接云箱。你的东西就存到云里了。

这是 [Cloud Storage](https://github.com/Alex-the-666/CloudStorage) 的 1.21.1 NeoForge 迁移版，由 Oritong 维护。原模组由 Alexthe668 和 Yosh 制作。气球、云箱、天空生物、气球伙伴和村庄气球商人都保留了，Alex's Mobs 联动已移除。

### 怎么玩

普通气球对应私人云库存储，颜色相同的气球连接同一份库存。把更多箱子送上去，就能继续扩容。普通云箱像末影箱一样，只能由对应玩家访问。

想共享库存或接入漏斗，就用静电气球和静电云箱。它们连接公共库存，也支持自动化。云箱需要能看到天空，地下和下界无法使用。

模组里还保留了气球箭、气球伙伴、吹云器和天空建筑。第一次玩可以先合成指南书，里面有配方和玩法说明。

### 安装

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.203 或更新的 21.1.x |
| Java | 21 |
| Citadel | 2.7.1，适用于 Minecraft 1.21.1 |

把本模组 JAR 和 `citadel-2.7.1-1.21.1.jar` 放入游戏实例的 `mods` 文件夹。联机时，客户端和服务器都需要安装这两个模组。

模组 ID 是 `cloudstorage_reborn`，配置文件是 `config/cloudstorage_reborn-common.toml`。

**旧存档说明：** 这个版本使用新的模组 ID，不会自动替换旧存档中的 `cloudstorage:*` 方块、物品和实体，也不会自动迁移旧模组的云库存档。

### 从源码构建

需要 JDK 21。仓库的 `libs/` 已包含构建使用的 Citadel 2.7.1，首次构建会下载 Gradle、NeoForge 和 Minecraft 依赖。

```powershell
# Windows，在项目目录运行
.\gradlew.bat build
```

Linux 或 macOS 使用 `./gradlew build`。产物位于 `build/libs/cloudstorage_reborn-1.0.0-1.21.1-neoforge.jar`。Citadel 不会打进成品 JAR，运行游戏时仍需单独安装。

开发测试可以运行 `gradlew.bat runGameTestServer` 和 `gradlew.bat -PclientSmokeTest runClient`。后者会检查物品模型和指南书，保存截图后自动退出。

目前通过了编译、4 项服务器 GameTest 和客户端渲染检查，覆盖云库存档与扩容、自动化、云箱菜单、配方、进度和建筑模板。长期生存流程和实际多人联机还没有完整测试。发现问题可以到 [Issues](https://github.com/oritong/CloudStorage-Reborn/issues) 留下复现步骤和日志。

### 作者与协议

维护与迁移：**Oritong**。原作者：**Alexthe668、Yosh**。

本项目采用 **GNU Lesser General Public License v3.0（LGPL-3.0-only）**。协议全文见 [LICENSE](LICENSE)。Citadel 是独立依赖，遵循它自己的许可协议。

## English

Tie a balloon to a chest, send it into the sky, then put a balloon of the same color on a Cloud Chest. That's your cloud storage.

This is a Minecraft 1.21.1 NeoForge port of [Cloud Storage](https://github.com/Alex-the-666/CloudStorage), maintained by Oritong. The original mod was made by Alexthe668 and Yosh. Balloons, Cloud Chests, sky mobs, Balloon Buddies and village balloon salesmen are still here. The Alex's Mobs integration has been removed.

### Playing

Regular balloons connect to your private cloud inventory. Matching colors share the same storage, and sending up more chests gives you more space. A regular Cloud Chest is personal, much like an ender chest.

For shared storage and hopper access, use static balloons and Static Cloud Chests. These connect to a public inventory and support automation. Cloud Chests need access to the sky, so they won't work underground or in the Nether.

Balloon arrows, Balloon Buddies, the Cloud Blower and sky structures are also included. Craft the guide book for recipes and more details.

### Installation

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.203 or a newer 21.1.x version |
| Java | 21 |
| Citadel | 2.7.1 for Minecraft 1.21.1 |

Put this mod's JAR and `citadel-2.7.1-1.21.1.jar` in your instance's `mods` folder. Both mods are needed on the client and server when playing multiplayer.

The mod ID is `cloudstorage_reborn`. Settings are in `config/cloudstorage_reborn-common.toml`.

**Existing worlds:** this port uses a new mod ID. It does not automatically replace blocks, items or entities registered under `cloudstorage:*`, or import the original mod's saved cloud inventories.

### Building

Use JDK 21. The Citadel 2.7.1 dependency is included in `libs/`. The first build downloads Gradle, NeoForge and Minecraft dependencies.

```sh
# Linux / macOS, from the project directory
./gradlew build
```

On Windows, run `.\gradlew.bat build`. The output is `build/libs/cloudstorage_reborn-1.0.0-1.21.1-neoforge.jar`. Citadel is not bundled into that JAR and must be installed separately to play.

For development checks, run `./gradlew runGameTestServer` and `./gradlew -PclientSmokeTest runClient`. The client check renders the item models and guide book, saves screenshots, then exits.

The port passes compilation, four server GameTests and client rendering checks. These cover cloud inventory saves and expansion, automation, chest menus, recipes, advancements and structure templates. Long survival sessions and real multiplayer have not been fully tested yet. Please report problems in [Issues](https://github.com/oritong/CloudStorage-Reborn/issues) with steps to reproduce and relevant logs.

### Credits And License

Port and maintenance: **Oritong**. Original authors: **Alexthe668, Yosh**.

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0-only)**. See [LICENSE](LICENSE) for the full license text. Citadel is a separate dependency covered by its own license.
