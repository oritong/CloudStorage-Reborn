# Cloud Storage:Reborn

**云存储：涅槃** · Minecraft 1.21.1 · NeoForge · v1.0.2

[中文](#中文) | [English](#english)

## 中文

把箱子绑上气球，送到天上，再用同色气球连接云箱。你的东西就存到云里了。

这是 [Cloud Storage](https://github.com/Alex-the-666/CloudStorage) 的 1.21.1 NeoForge 迁移版，由 Oritong 维护。原模组由 Alexthe668 和 Yosh 制作。气球、云箱、天空生物、气球伙伴和村庄气球商人都保留了，Alex's Mobs 联动已移除。

### 怎么玩

普通气球对应私人云库存储，颜色相同的气球连接同一份库存。把更多箱子送上去，就能继续扩容。普通云箱像末影箱一样，只能由对应玩家访问。

想共享库存或接入漏斗，就用静电气球和静电云箱。它们连接公共库存，也支持自动化。云箱需要能看到天空，地下和下界无法使用。

模组里还保留了气球箭、气球伙伴、吹云器和天空建筑。第一次玩可以先合成指南书，里面有配方和玩法说明。

### 流体存储

云箱现在也能存水、熔岩和模组流体。物品和流体显示在同一个终端列表中，共用搜索框、排序和滚动，不需要切换页面。搜索也支持流体名称、`@模组ID` 和 `#标签`，客户端装有 JEC 时可以用拼音匹配。

先把桶或流体容器拿到鼠标上。在终端列表区域右键，会把容器里的流体倒入云库存；左键点击已存的流体，会用它装填容器。每次点击处理一个容器，叠放的空桶填满后会放回背包，背包满了则掉在玩家脚边。普通桶每次需要完整的 1,000 mB，装不下或不够一桶时不会消耗容器；支持部分装填的模组容器按实际可转移量处理。鼠标悬停可查看流体名称、数量和总容量。拿着装满的容器悬停在物品或空格上时，会分别提示「左键点击：存入 水桶」「右键点击：存入 水」。左键可以直接保存容器物品；也可以在背包里用 Shift 点击存入。

按住 **Shift** 会固定已有条目的位置，数量仍会实时更新。取完的条目暂时保留在原位，新存入且符合搜索条件的物品和流体会立即追加到列表末尾，可以直接继续存取。松开 Shift 后再按当前排序整理列表。Shift 点击物品会将其转移到背包。

流体与物品使用各自的容量。每上传一个普通箱子，除了 27 个物品槽位，还会增加 27,000 mB（27 桶）流体容量；多种流体共用这份流体容量，最多保存 256 种流体及其数据变体。私人流体库存沿用玩家 UUID 和气球颜色，静电流体库存沿用公共颜色，都会随世界保存。现阶段通过 GUI 存取，还没有加入流体管道接口。

容器交互参考了 AE2 的处理方式，使用 NeoForge 的标准流体能力接口实现。**不需要安装 AE2**；其他模组的容器只要提供标准流体能力，也可以使用。

### 安装

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.203 或更新的 21.1.x |
| Java | 21 |
| Citadel | 2.7.1，适用于 Minecraft 1.21.1 |

把本模组 JAR 和 `citadel-2.7.1-1.21.1.jar` 放入游戏实例的 `mods` 文件夹。联机时，客户端和服务器都需要安装这两个模组。

模组 ID 是 `cloudstorage_reborn`，配置文件是 `config/cloudstorage_reborn-common.toml`。

如果客户端装了 Just Enough Characters（JEC），云箱里也可以用全拼或拼音首字母搜索。比如中文语言下，输入 `zuanshi` 或 `zs` 都能找到钻石。匹配遵循 JEC 的拼音设置；`@模组ID` 和 `#标签` 搜索照常使用。JEC 是可选联动，只需客户端安装，服务器不需要它。

### 超越维度联动（WIP）

如果也安装了 Beyond Dimensions（超越维度），可以让私人云库存使用自己的维度网络。它是可选联动，默认关闭。在 `config/cloudstorage_reborn-common.toml` 中开启：

```toml
[integration]
beyond_dimensions_storage = true # WIP
beyond_dimensions_transfer_interval_seconds = 5 # 1～60 秒
```

联机时由服务器配置决定是否开启；双方都需要安装超越维度才能使用它的存储界面。先在超越维度里开通自己的网络，并将它设为主网络。云箱仍需要气球、已上传的云空间和原有的天空条件。

开启后，玩家在线时，已有库存和新上传的物品默认每 5 秒尝试转存一次，可通过 `beyond_dimensions_transfer_interval_seconds` 调整为 1～60 秒（按 20 tick/秒计）。打开普通云箱时仍会立即转存，不受后台间隔影响。不同颜色的私人云库存会汇入同一个个人主网络，普通云箱打开这个网络的存储界面，可以直接存取物品。存储容量由超越维度决定，上传箱子增加的云槽位不会给维度网络扩容。

私人云库存中的流体也会按同一间隔转入个人主网络，可在超越维度界面中存取。网络装不下的流体会留在原云库存，对应云箱会打开普通界面供你取回。关闭联动后，已经转存的流体仍留在维度网络中。

只有自己拥有的主网络会接收物品。没有网络、主网络属于别人或容量不足时，未转存的物品留在云库存里；对应云箱会打开原来的界面，方便取回剩余物品。静电公共库存、战利品库存和其他玩家的私人库存不参与转存。

把开关改回 `false` 就会恢复普通云存储。已经转存的物品仍在原维度网络里，需要从超越维度取出；关闭开关、换主网络或卸载超越维度都不会把它们自动搬回来。切换个人主网络后，之后的物品会存入新的主网络。

**旧存档说明：** 这个版本使用新的模组 ID，不会自动替换旧存档中的 `cloudstorage:*` 方块、物品和实体，也不会自动迁移旧模组的云库存档。

### 从源码构建

需要 JDK 21。仓库的 `libs/` 已包含构建使用的 Citadel 2.7.1，首次构建会下载 Gradle、NeoForge 和 Minecraft 依赖。

```powershell
# Windows，在项目目录运行
.\gradlew.bat build
```

Linux 或 macOS 使用 `./gradlew build`。产物位于 `build/libs/cloudstorage_reborn-1.0.2-1.21.1-neoforge.jar`。Citadel 不会打进成品 JAR，运行游戏时仍需单独安装。

开发测试可以运行 `gradlew.bat runGameTestServer` 和 `gradlew.bat -PclientSmokeTest runClient`。后者会检查物品模型和指南书，保存截图后自动退出。

超越维度的联动测试可通过 `-PbeyondDimensionsTestJar=本地JAR路径` 加载它的运行库，运行 `runGameTestServer`，或加上 `-PbeyondDimensionsSmokeTest runClient` 检查两个存储界面的同步与渲染。目前使用超越维度 0.7.30 完成了实际存取测试；联动接口也按仓库中的 0.8.0-alpha.1 源码核对过。

终端 GUI 的开发检查使用 `gradlew.bat -PfluidSmokeTest runClient`，会通过实际点击验证物品和流体存取、叠放空桶、混合搜索、滚动和 Shift 排列锁定，保存截图后退出。

目前通过了编译、12 项服务器 GameTest 和客户端渲染检查，覆盖云库存档与扩容、流体保存与容器存取、自动化、云箱菜单、客户端搜索结果同步、可选维度网络转存、配方、进度和建筑模板。长期生存流程和实际多人联机还没有完整测试。发现问题可以到 [Issues](https://github.com/oritong/CloudStorage-Reborn/issues) 留下复现步骤和日志。

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

### Fluid Storage

Cloud Chests can now store water, lava and modded fluids. Items and fluids share one terminal list, search box, sort control and scrollbar. Searches include fluid names, `@modid` and `#tag`, with pinyin matching when JEC is installed on the client.

Pick up a bucket or fluid container with your cursor. Right-click in the terminal grid to empty it into the cloud, or left-click a stored fluid to fill the container. Each click handles one container. When filling stacked buckets, the filled bucket goes into your inventory, or drops at your feet if the inventory is full. Regular buckets need a full 1,000 mB; insufficient space or fluid leaves them unchanged. Modded containers that support partial transfers move only what fits. Hover over the grid for fluid names, amounts and total capacity. With a filled container over an item or empty slot, a two-line tooltip distinguishes storing the container with left-click from emptying its fluid with right-click. You can also Shift-click a filled container from your inventory to store it as an item.

Hold **Shift** to keep existing entries in place while their counts update. Depleted entries stay in their original positions. Newly stored items and fluids that match your search appear at the end immediately and remain accessible. Release Shift to sort the list again. Shift-click an item to move it into your inventory.

Items and fluids have separate capacities. Each uploaded regular chest adds 27 item slots and 27,000 mB of fluid space. Different fluids share that space, with up to 256 fluid types and data variants. Private storage follows the same player UUID and balloon color as items; static storage follows its shared color. Fluids are saved with the world. This first implementation supports GUI transfers; fluid pipes are not connected yet.

The container interaction follows AE2's approach and uses NeoForge's standard fluid capabilities. **AE2 is not required.** Containers from other mods work when they provide the standard fluid capability.

### Installation

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.203 or a newer 21.1.x version |
| Java | 21 |
| Citadel | 2.7.1 for Minecraft 1.21.1 |

Put this mod's JAR and `citadel-2.7.1-1.21.1.jar` in your instance's `mods` folder. Both mods are needed on the client and server when playing multiplayer.

The mod ID is `cloudstorage_reborn`. Settings are in `config/cloudstorage_reborn-common.toml`.

With Just Enough Characters (JEC) installed on the client, Cloud Chest searches also accept pinyin and initials. For example, with Chinese item names, `zuanshi` or `zs` will find diamonds. Matching follows your JEC settings, and `@modid` and `#tag` searches still work as usual. JEC is optional and only needed on the client; the server does not need it.

### Beyond Dimensions Integration (WIP)

If you also use Beyond Dimensions, your private clouds can feed into your own dimensional network. This is optional and disabled by default. Enable it in `config/cloudstorage_reborn-common.toml`:

```toml
[integration]
beyond_dimensions_storage = true # WIP
beyond_dimensions_transfer_interval_seconds = 5 # 1-60 seconds
```

In multiplayer, the server controls this setting. Both sides need Beyond Dimensions to use its storage screen. Create your own network in Beyond Dimensions and make it your primary network first. Cloud Chests still need a balloon, uploaded cloud space and the usual sky access.

While you are online, existing cloud items and new uploads are transferred every 5 seconds by default. Set `beyond_dimensions_transfer_interval_seconds` to any value from 1 to 60 seconds (measured at 20 ticks per second). Opening a regular Cloud Chest still attempts a transfer immediately, regardless of the background interval. All of your private balloon colors feed the same personal primary network. Regular Cloud Chests open that network's storage screen so you can deposit and withdraw items there. Beyond Dimensions controls its capacity; uploading more chests does not expand the dimensional network.

Private cloud fluids also transfer into your primary network on the same schedule and can be accessed through Beyond Dimensions. Any fluid the network cannot accept stays in cloud storage, and that chest opens its ordinary screen so you can retrieve it. Turning the integration off leaves already transferred fluids in the dimensional network.

Only a primary network you own can receive your items. If you have no network, your primary network belongs to someone else, or it cannot accept everything, the remaining items stay in cloud storage. A chest with remaining items opens its ordinary screen so you can retrieve them. Public/static clouds, loot clouds and other players' private inventories are excluded.

Set the option back to `false` to return to ordinary cloud storage. Items already transferred stay in their original dimensional network and must be retrieved through Beyond Dimensions. Disabling the option, switching networks or removing Beyond Dimensions does not move them back. Once you switch to another personal primary network, future transfers go there.

**Existing worlds:** this port uses a new mod ID. It does not automatically replace blocks, items or entities registered under `cloudstorage:*`, or import the original mod's saved cloud inventories.

### Building

Use JDK 21. The Citadel 2.7.1 dependency is included in `libs/`. The first build downloads Gradle, NeoForge and Minecraft dependencies.

```sh
# Linux / macOS, from the project directory
./gradlew build
```

On Windows, run `.\gradlew.bat build`. The output is `build/libs/cloudstorage_reborn-1.0.2-1.21.1-neoforge.jar`. Citadel is not bundled into that JAR and must be installed separately to play.

For development checks, run `./gradlew runGameTestServer` and `./gradlew -PclientSmokeTest runClient`. The client check renders the item models and guide book, saves screenshots, then exits.

To check the integration, supply `-PbeyondDimensionsTestJar=/path/to/local.jar` and run `runGameTestServer`, or add `-PbeyondDimensionsSmokeTest runClient` to check synchronization and rendering of both storage screens. Actual storage operations have been tested with Beyond Dimensions 0.7.30. The bridge's API signatures were also checked against the supplied 0.8.0-alpha.1 source.

Run `./gradlew -PfluidSmokeTest runClient` for the terminal GUI check. It performs real clicks to check item and fluid transfers, stacked buckets, mixed searches, scrolling and Shift-locked ordering, saves screenshots, then exits.

The port passes compilation, twelve server GameTests and client rendering checks. These cover cloud inventory saves and expansion, fluid persistence and container transfers, automation, chest menus, client search result synchronization, optional dimensional network transfers, recipes, advancements and structure templates. Long survival sessions and real multiplayer have not been fully tested yet. Please report problems in [Issues](https://github.com/oritong/CloudStorage-Reborn/issues) with steps to reproduce and relevant logs.

### Credits And License

Port and maintenance: **Oritong**. Original authors: **Alexthe668, Yosh**.

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0-only)**. See [LICENSE](LICENSE) for the full license text. Citadel is a separate dependency covered by its own license.
