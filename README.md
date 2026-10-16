# Cloud Storage:Reborn

**云存储：涅槃** · Minecraft 1.21.1 · NeoForge · v1.0.3

[中文](#中文) | [English](#english)

## 中文

把箱子绑上气球，送到天上，再用同色气球连接云箱。你的东西就存到云里了。

这是 [Cloud Storage](https://github.com/Alex-the-666/CloudStorage) 的 1.21.1 NeoForge 迁移版，由 Oritong 维护。原模组由 Alexthe668 和 Yosh 制作。气球、云箱、天空生物、气球伙伴和村庄气球商人都保留了，Alex's Mobs 联动已移除。

### 怎么玩

普通气球对应私人云库存储，颜色相同的气球连接同一份库存。把更多箱子送上去，就能继续扩容。普通云箱像末影箱一样，只能由对应玩家访问。

想共享库存，就用静电气球和静电云箱，它们连接同色公共库存。普通云箱和静电云箱都可以在上传漏斗后接入自动化。云箱需要能看到天空，地下和下界无法使用。

模组里还保留了气球箭、气球伙伴、吹云器和天空建筑。第一次玩可以先合成指南书，里面有配方和玩法说明。

### 云端工作台

工作台也能绑上气球送上天。第一次上传会为对应的云库存解锁合成区，左侧随之出现功能栏和工作台按钮。多余的工作台作为物品存入云端，放不下就销毁，不会扩容或重复解锁。普通气球解锁自己的同色库存，静电气球解锁同色公共库存。

点击工作台按钮，就会在云库存和背包之间展开一个 3×3 合成区，再点一次收起。窗口高度不够时，会先减少云库存显示的行数，剩下的物品仍可滚动查看。合成格里的材料默认会保留，关闭界面、换一个连接同一库存的云箱或重新进入世界，都可以继续使用。公共库存的合成格也由使用同色库存的玩家共享。

合成区右侧有三个开关，默认都关闭，设置随这份云库存保存：

- **自动清除**：关闭云箱时，把合成格里的材料放回云库存。只能放回一部分时，其余材料留在原来的合成格里。
- **自动拉取**：合成后从云库存补充材料，可以连续 Shift 合成。优先使用原材料；同一个配方中、不会改变产物及其数据的替代材料也可以补入。只使用已经存入云端的材料，不会发起其他配方的自动合成。
- **存入云端**：关闭时，Shift 合成优先放进背包，背包装不下再放云端；开启后则先放云端，云端装不下再放背包。两边加起来也放不下完整产物时，就会停止合成，保留材料。

普通点击合成结果仍会拿到鼠标上。每次 Shift 点击最多执行 64 次合成，避免材料较多时一次操作占用太长时间。不需要安装 AE2。

开启超越维度联动时，已解锁功能方块的云箱会保留云端终端，方便使用合成区；原有的后台转存仍按配置运行，合成格里的材料不会被转存。

### 漏斗与自动化

把漏斗绑上气球送上天，就能为对应的云库存解锁 IO 功能。普通气球解锁自己的同色库存，静电气球解锁同色公共库存。上传一次就够了，多余的漏斗作为物品存入云端，放不下就销毁，也不会增加物品槽位。漏斗里装着的物品会一起存入云端，放不下的退回背包；玩家离线时则掉在上传位置。

解锁后，左侧会出现漏斗按钮。点击它，在云库存和背包之间展开面配置；各功能面板可以互相切换。各格对应云箱的正面、左右、背面、顶部和底部，有相邻方块时显示它的图标，没有方块时留空。颜色表示这个面的用途：

- **红色**：仅输入。
- **蓝色**：仅输出，由相邻漏斗或管道提取。
- **紫色**：允许输入和输出。
- **灰色**：禁止 IO。

左键切到下一个模式，右键切回上一个模式，Shift 左键直接禁用这个面。**顶部固定禁止 IO，不能修改。** 右下角是批量配置按钮：各面模式一致时，左键或右键一起切换；模式不一致时先全部禁用，Shift 点击也会全部禁用。顶部不参与切换。

面配置同时作用于物品和流体接口。存物品仍需上传箱子，存流体仍需上传储罐。使用 NeoForge 标准接口，不需要安装 Mekanism。IO 设置保存在每个实体云箱上，同一库存的不同云箱可以有不同配置，默认各面都禁用。更新前已经使用自动化的静电云箱，也需要上传漏斗并重新设置允许存取的面。

普通云箱会使用**最后修改 IO 设置的玩家在这个箱子上绑定的库存**，不会因为其他玩家走近而切换；玩家离线后仍然有效，区块需要保持加载。静电云箱使用其连接的公共库存。

右侧的**收集掉落物**开关默认关闭，红色为关闭，绿色为开启。开启后会把云箱正上方的掉落物放入云库存，装不下的部分留在原地。每 8 tick 检查一次，每次最多处理 32 个掉落物实体。收集不需要开启顶部 IO，也不会收集侧面或下方的物品。自动化与收集都遵循云箱原有的天空和上方留空条件。

### 云端熔炉

熔炉也可以绑上气球上传。每个熔炉增加一路并行，最多 64 路，彼此可以同时处理不同配方。上传数量按私人玩家与气球颜色区分，静电气球则为同色公共库存增加熔炉。第 65 个开始，熔炉会作为物品存入云端，放不下就销毁。

左侧熔炉按钮会展开输入、燃料和输出区。槽位随上传数量自动增加，已有材料不会因升级移位：

| 上传总数 | 并行数 | 输入格 | 燃料格 | 输出格 |
| --- | --- | --- | --- | --- |
| 1～15 | 按实际数量 | 1 | 1 | 1 |
| 16～31 | 按实际数量 | 2 | 2 | 2 |
| 32～63 | 按实际数量 | 4 | 4 | 4 |
| 64 | 64 | 9 | 9 | 9 |

每一路按原版熔炉配方计时，单独消耗燃料；例如两个熔炉可以同时处理两个原料，燃烧中的煤炭会继续用于后续批次。多路共享可见槽位，原料完成前仍留在输入区，输出区满了就暂停。手动燃料支持原版及模组提供的有效熔炉燃料，熔岩桶会留下桶，烧干湿海绵也可以把燃料区的单个空桶变成水桶。

右侧三个开关默认都关闭，红色为关、绿色为开，随这份云库存保存：

- **自动输出**：将产物送到云端，放不下的留在输出槽，腾出空间后继续。
- **自动寻找燃料**：从云端补充 `#cloudstorage:fuel` 标签内的有效燃料，默认只有煤炭和煤炭块。数据包可通过 `data/cloudstorage/tags/item/fuel.json` 扩展标签。
- **输入熔炼**：需要上传漏斗，并在实际云箱上启用对应输入面。该面输入的可熔炼物品优先进入熔炉，放不下的部分以及其他物品照常存入云端；两边都满时拒收，不会销毁。即使没有上传普通箱子，也能直接向熔炉输入。顶部仍禁止 IO。

自动输出和找燃料每 8 tick 检查一次。Shift 点击背包里的原料或燃料可以快速放入当前熔炉；Shift 点击熔炉内的物品优先取回背包，背包装不下再放云端。手动取出产物会获得累计熔炼经验。材料、燃烧时间和进度会随世界保存，关闭界面后继续工作；同一份公共库存的熔炉由玩家共享，不会因打开多个终端而重复加速。熔炉本身不增加云端物品容量。

### 流体存储

云箱现在也能存水、熔岩和模组流体。物品和流体显示在同一个终端列表中，共用搜索框、排序和滚动，不需要切换页面。搜索也支持流体名称、`@模组ID` 和 `#标签`，客户端装有 JEC 时可以用拼音匹配。

先把桶或流体容器拿到鼠标上。在终端列表区域右键，会把容器里的流体倒入云库存；左键点击已存的流体，会用它装填容器。每次点击处理一个容器，叠放的空桶填满后会放回背包，背包满了则掉在玩家脚边。普通桶每次需要完整的 1,000 mB，装不下或不够一桶时不会消耗容器；支持部分装填的模组容器按实际可转移量处理。鼠标悬停可查看流体名称、数量和总容量。拿着装满的容器悬停在物品或空格上时，会分别提示「左键点击：存入 水桶」「右键点击：存入 水」。左键可以直接保存容器物品；也可以在背包里用 Shift 点击存入。

按住 **Shift** 会固定已有条目的位置，数量仍会实时更新。取完的条目暂时保留在原位；相同物品重新存入时会回填原来的显示位置，即使后台槽位改变，也不会不断追加重复的 0 数量条目。多组相同物品分别匹配，不会重复显示同一组。新的物品和流体只要符合搜索条件，就会立即追加到列表末尾，可以直接继续存取。松开 Shift 后再按当前排序整理列表。Shift 点击物品会将其转移到背包。

流体空间需要单独上传储罐。**每上传一个储罐，增加 1 个流体种类额度，并增加该储罐实际大小的容量。** 多种流体共用总容量；带有不同数据的流体会分别占用种类额度。箱子只增加物品槽位，普通箱子是 27 格。只上传储罐、没有物品空间时，也能打开云箱存取流体。

目前已支持 Modern Industrialization 的铁、青铜、钢、铝、不锈钢、钛和量子储罐，以及 Mekanism 的基础、高级、精英和终极流体储罐。给储罐绑上气球，像上传箱子一样送上天即可；罐内已有的流体也会一起存入。容量读取储罐的实际数值，量子储罐的大容量和已有流体不会被普通整数范围截断。MI 和 Mekanism 都是可选联动，不需要为了使用云存储安装它们。

流体功能默认开启，可在 `config/cloudstorage_reborn-common.toml` 中关闭：

```toml
[storage]
fluid_storage = true
```

改成 `false` 后，储罐上传、终端及管道流体存取和向超越维度转存流体都会停用，已上传的容量、种类额度和流体数据会保留。私人流体库存按玩家 UUID 和气球颜色区分，静电库存按公共颜色区分，随世界保存。上传漏斗并配置云箱对应的面后，也可以通过流体管道存取。

可上传储罐由方块标签 **`cloudstorage:fluid_storage`** 决定。数据包可通过 `data/cloudstorage/tags/block/fluid_storage.json` 追加其他模组的储罐；方块需要提供标准流体能力，目前支持空罐或只含一种流体的储罐。内置的七种 MI 储罐和四种 Mekanism 储罐都是可选标签项，不装对应模组也能正常加载。Mekanism 储罐上传时，输入/输出槽中的物品会随货物保存；云库存没有物品槽位时，多出的物品会退回玩家背包或掉落在脚边。

之前测试版由箱子赠送的流体容量不再生效。已有流体会保留，可以取出；需要上传储罐获得容量和种类额度后才能继续正常存入。

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

### Mouse Tweaks

客户端安装 Mouse Tweaks 后，按住 Shift 和鼠标左键拖过物品格，即可在云库存与背包之间快速转移物品。终端接入 Mouse Tweaks 的自定义界面接口，由它处理拖动手势，并遵循它的左键拖动设置；不需要在服务器安装 Mouse Tweaks。流体格不参与物品拖动，滚轮仍用于滚动终端列表。未安装 Mouse Tweaks 时，普通点击和 Shift 点击照常使用。

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

Linux 或 macOS 使用 `./gradlew build`。产物位于 `build/libs/cloudstorage_reborn-1.0.3-1.21.1-neoforge.jar`。Citadel 不会打进成品 JAR，运行游戏时仍需单独安装。

开发测试可以运行 `gradlew.bat runGameTestServer` 和 `gradlew.bat -PclientSmokeTest runClient`。后者会检查物品模型和指南书，保存截图后自动退出。

超越维度的联动测试可通过 `-PbeyondDimensionsTestJar=本地JAR路径` 加载它的运行库，运行 `runGameTestServer`，或加上 `-PbeyondDimensionsSmokeTest runClient` 检查两个存储界面的同步与渲染。目前使用超越维度 0.7.30 完成了实际存取测试；联动接口也按仓库中的 0.8.0-alpha.1 源码核对过。

终端 GUI 的开发检查使用 `gradlew.bat -PfluidSmokeTest runClient`，会通过实际点击验证物品和流体存取、叠放空桶、混合搜索、滚动和 Shift 排列锁定，保存截图后退出。

储罐联动测试可用 `-PneoForgeVersion=21.1.244 -PmodernIndustrializationTestMods=本地模组目录 -PmekanismTestJar=本地Mekanism的JAR路径 runGameTestServer` 加载 MI 和 Mekanism，验证各级储罐的实际上传和存档读写。`-PtankSmokeTest runClient` 会检查没有物品槽位的流体终端、桶的实际存取和配置开关同步。

Mouse Tweaks 的开发检查可用 `-PmouseTweaksTestJar=本地JAR路径 -PmouseTweaksSmokeTest runClient`，会通过它的实际自定义界面处理器验证坐标识别、双向快速转移和连续请求。编译使用仓库内的独立 API 接口源码，运行时由 Mouse Tweaks 提供接口，成品不包含 Mouse Tweaks 或它的接口类。

工作台检查使用 `-PcraftingSmokeTest runClient`；IO 检查使用 `-PioSmokeTest runClient`，通过实际点击验证面切换、顶部锁定、批量配置、相邻方块显示、掉落物收集和面板切换，保存截图后退出。

熔炉检查使用 `-PfurnaceSmokeTest runClient`，会检查四档槽位、界面内升级、实际 Shift 存取、跨配方熔炼、自动输出和开关同步，并保存不同尺寸的界面截图。

目前基础测试集有 41 项服务器 GameTest，覆盖存档、容量、容器存取、菜单、配方、工作台、IO 权限、真实漏斗传输、满仓掉落物收集、熔炉并行与燃料消耗、进度保存和重复上传等。客户端另有搜索、Shift 排列、渲染和交互检查；MI、Mekanism、Mouse Tweaks 及超越维度的联动可按上面的参数加载本地模组后单独验证。长期生存流程和实际多人联机还没有完整测试。发现问题可以到 [Issues](https://github.com/oritong/CloudStorage-Reborn/issues) 留下复现步骤和日志。

### 作者与协议

维护与迁移：**Oritong**。原作者：**Alexthe668、Yosh**。

本项目采用 **GNU Lesser General Public License v3.0（LGPL-3.0-only）**。协议全文见 [LICENSE](LICENSE)。Citadel 是独立依赖，遵循它自己的许可协议。仅用于编译的 Mouse Tweaks API 接口源码采用 BSD-3-Clause，协议见 [src/mouseTweaksApi/LICENSE](src/mouseTweaksApi/LICENSE)。

## English

Tie a balloon to a chest, send it into the sky, then put a balloon of the same color on a Cloud Chest. That's your cloud storage.

This is a Minecraft 1.21.1 NeoForge port of [Cloud Storage](https://github.com/Alex-the-666/CloudStorage), maintained by Oritong. The original mod was made by Alexthe668 and Yosh. Balloons, Cloud Chests, sky mobs, Balloon Buddies and village balloon salesmen are still here. The Alex's Mobs integration has been removed.

### Playing

Regular balloons connect to your private cloud inventory. Matching colors share the same storage, and sending up more chests gives you more space. A regular Cloud Chest is personal, much like an ender chest.

For shared storage, use static balloons and Static Cloud Chests. These connect to a public inventory of the same color. Both regular and Static Cloud Chests support automation after uploading a hopper. Cloud Chests need access to the sky, so they won't work underground or in the Nether.

Balloon arrows, Balloon Buddies, the Cloud Blower and sky structures are also included. Craft the guide book for recipes and more details.

### Cloud Crafting

Tie a balloon to a crafting table and send it up. The first upload unlocks a crafting area for that cloud inventory and adds a workbench button in a new sidebar on the left. Extra workbenches are stored as items in the cloud, or discarded when full; they do not expand storage or unlock more grids. Regular balloons unlock your personal inventory of that color; static balloons unlock the shared inventory.

Click the workbench button to expand a 3×3 crafting grid between cloud storage and your inventory. Click again to collapse it. When the screen is too short, the terminal shows fewer storage rows and keeps the rest accessible by scrolling. Ingredients stay in the grid by default, even after closing the screen, opening another chest connected to the same cloud, or reloading the world. Players using the same public cloud also share its crafting grid.

The three switches on the right start disabled and are saved with the cloud inventory:

- **Auto clear** returns ingredients to cloud storage when you close the chest. Anything that does not fit stays in its original crafting slot.
- **Auto refill** pulls ingredients from cloud storage after crafting, including between Shift crafts. It prefers the original ingredients and also accepts alternatives that keep the same recipe and exact output. It uses existing materials only; it does not craft missing ingredients through other recipes.
- **Store in cloud** changes the Shift crafting destination. Off: inventory first, cloud storage for overflow. On: cloud storage first, inventory for overflow. Crafting stops if neither destination has enough combined room for a complete result, leaving the ingredients intact.

An ordinary click still picks the result up on your cursor. Each Shift click performs at most 64 crafts to keep large batches from holding up the server. AE2 is not required.

With Beyond Dimensions integration enabled, clouds with an unlocked functional block keep the cloud terminal so the crafting grid remains accessible. Background transfers still follow the configured interval and leave crafting ingredients alone.

### Hoppers And Automation

Send a hopper up with a balloon to unlock IO for that cloud inventory. Regular balloons unlock your personal inventory of that color; static balloons unlock the shared inventory. One hopper is enough. Extra hoppers are stored as items in the cloud, or discarded when full; they do not add item slots. Items inside the hopper travel with it. Overflow returns to your inventory, or drops at the upload location if you are offline.

A hopper button then appears in the left sidebar. Click it to open the side configuration panel between cloud storage and your inventory. The functional panels share this space. The six squares represent the chest's front, left, right, back, top and bottom faces. Each shows the adjacent block's icon, or stays empty when there is no block. Colors indicate the allowed operations:

- **Red**: input only.
- **Blue**: output only, extracted by a connected hopper or pipe.
- **Purple**: input and output.
- **Gray**: disabled.

Left-click advances the mode, right-click goes back, and Shift-left-click disables a face. **The top face is always disabled.** The lower-right button configures all other faces at once: left/right-click cycles matching modes, mixed modes reset to disabled, and Shift-click disables all of them. The top face stays unchanged.

These settings apply to both item and fluid connections through standard NeoForge capabilities. Item storage still needs uploaded chests; fluid storage needs uploaded tanks. Mekanism is not required. Settings belong to each physical Cloud Chest, so chests connected to the same cloud can have different configurations. All faces start disabled. Existing automated Static Cloud Chests also need a hopper upload and configured faces after updating.

A regular Cloud Chest uses **the linked inventory of the player who last changes its IO settings**. Other players approaching the chest do not change the automation target. It continues working while that player is offline, as long as the chunk is loaded. A Static Cloud Chest uses its shared inventory.

The **Collect dropped items** switch on the right starts off: red means disabled, green means enabled. It stores items dropped directly above the chest and leaves any overflow in the world. Collection runs every 8 ticks and processes up to 32 item entities per pass. It works independently of the disabled top IO face and ignores items beside or below the chest. Both automation and collection follow the chest's usual sky access and clear space requirements.

### Cloud Furnaces

Send furnaces up with balloons to add parallel smelting. Each furnace adds one independent lane, up to 64, and lanes can process different recipes at the same time. Regular balloons add furnaces to your personal cloud of that color; static balloons add them to the shared cloud. From the 65th upload onward, extra furnaces are stored as items, or discarded if the cloud is full.

The furnace button opens input, fuel and output groups. Uploading more furnaces automatically expands these groups without moving existing ingredients:

| Uploaded furnaces | Parallel lanes | Input slots | Fuel slots | Output slots |
| --- | --- | --- | --- | --- |
| 1–15 | Actual count | 1 | 1 | 1 |
| 16–31 | Actual count | 2 | 2 | 2 |
| 32–63 | Actual count | 4 | 4 | 4 |
| 64 | 64 | 9 | 9 | 9 |

Each lane uses ordinary furnace recipes and consumes fuel independently. Two furnaces can process two ingredients at once, and burning fuel remains available for later batches. Lanes share the visible slots. Ingredients stay in the input until cooking finishes; full outputs pause processing. Manual fuel accepts valid vanilla and modded furnace fuels. Lava buckets leave an empty bucket, which wet sponge smelting can turn into a water bucket.

The three switches on the right start disabled and are saved with the cloud inventory. Red means off; green means on:

- **Auto output** moves results into cloud storage. Overflow stays in the output slots until space becomes available.
- **Find fuel** refills valid fuels tagged `#cloudstorage:fuel` from cloud storage. The default tag contains coal and coal blocks. Data packs can extend `data/cloudstorage/tags/item/fuel.json`.
- **Smelt incoming items** requires an uploaded hopper and an enabled input face on the physical chest. Incoming smeltable items enter the furnace first. Any remainder and other items go into cloud storage. Full destinations reject the transfer without discarding anything. This also works without uploaded item chests. The top face remains disabled.

Automatic output and fuel refills run every 8 ticks. Shift-click ingredients or fuel in your inventory to insert them into the open furnace. Shift-click furnace items to take them into your inventory, with cloud storage accepting overflow. Taking results manually awards accumulated smelting experience. Ingredients, burning fuel and progress survive world saves, and processing continues after closing the screen. Players using the same public cloud share its furnace; opening more terminals does not speed it up. Furnaces do not add cloud item capacity.

### Fluid Storage

Cloud Chests can now store water, lava and modded fluids. Items and fluids share one terminal list, search box, sort control and scrollbar. Searches include fluid names, `@modid` and `#tag`, with pinyin matching when JEC is installed on the client.

Pick up a bucket or fluid container with your cursor. Right-click in the terminal grid to empty it into the cloud, or left-click a stored fluid to fill the container. Each click handles one container. When filling stacked buckets, the filled bucket goes into your inventory, or drops at your feet if the inventory is full. Regular buckets need a full 1,000 mB; insufficient space or fluid leaves them unchanged. Modded containers that support partial transfers move only what fits. Hover over the grid for fluid names, amounts and total capacity. With a filled container over an item or empty slot, a two-line tooltip distinguishes storing the container with left-click from emptying its fluid with right-click. You can also Shift-click a filled container from your inventory to store it as an item.

Hold **Shift** to keep existing entries in place while their counts update. Depleted entries stay in their original positions. Returning the same item restores its previous display position, even if it occupies a different cloud slot, so repeated transfers do not accumulate stale zero-count entries. Multiple stacks are matched separately without displaying the same stack twice. New items and fluids that match your search appear at the end immediately and remain accessible. Release Shift to sort the list again. Shift-click an item to move it into your inventory.

Fluid space comes from uploaded tanks. **Each tank adds one fluid type slot and its actual capacity.** All stored fluids share the total capacity; fluids with different data count as separate types. Chests add item slots only: 27 for a regular chest. You can open a Cloud Chest with just fluid space, even if you have never uploaded an item chest.

Modern Industrialization's iron, bronze, steel, aluminum, stainless steel, titanium and quantum tanks, plus Mekanism's basic, advanced, elite and ultimate fluid tanks, are supported. Tie a balloon to a tank and send it up just like a chest. Any fluid inside comes with it. Capacity is read from the actual tank, including quantum capacities and contents beyond the ordinary integer range. MI and Mekanism are optional integrations.

Fluid storage is enabled by default. Its switch is in `config/cloudstorage_reborn-common.toml`:

```toml
[storage]
fluid_storage = true
```

Setting it to `false` disables tank uploads, terminal and pipe fluid transfers, and fluid transfers to Beyond Dimensions. Uploaded capacity, type slots and saved fluids are preserved. Private fluid storage follows the player UUID and balloon color; static storage follows its shared color. Both are saved with the world. Fluid pipes can connect after uploading a hopper and enabling the appropriate chest faces.

The block tag **`cloudstorage:fluid_storage`** controls which tanks can be uploaded. Data packs can add other tanks through `data/cloudstorage/tags/block/fluid_storage.json`. A tank must expose a standard fluid capability; empty tanks and tanks containing a single fluid type are supported. The seven MI entries and four Mekanism entries are optional, so the tag loads without either mod installed. Items in Mekanism tank input/output slots travel with the upload; when a tank-only cloud has no item slots, excess items return to the owner or drop nearby.

Fluid capacity previously granted by chests in test builds is no longer available. Existing fluids are preserved and can be withdrawn. Upload tanks to gain the capacity and type slots needed for further deposits.

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

### Mouse Tweaks

With Mouse Tweaks installed on the client, hold Shift and the left mouse button while dragging across item entries to transfer them between the cloud and your inventory. The terminal uses Mouse Tweaks' custom screen interface, so Mouse Tweaks handles gestures and its left-drag settings apply. It is not needed on the server. Fluid entries are excluded from item dragging, and the wheel still scrolls the terminal list. Ordinary clicks and Shift-clicks work without Mouse Tweaks.

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

On Windows, run `.\gradlew.bat build`. The output is `build/libs/cloudstorage_reborn-1.0.3-1.21.1-neoforge.jar`. Citadel is not bundled into that JAR and must be installed separately to play.

For development checks, run `./gradlew runGameTestServer` and `./gradlew -PclientSmokeTest runClient`. The client check renders the item models and guide book, saves screenshots, then exits.

To check the integration, supply `-PbeyondDimensionsTestJar=/path/to/local.jar` and run `runGameTestServer`, or add `-PbeyondDimensionsSmokeTest runClient` to check synchronization and rendering of both storage screens. Actual storage operations have been tested with Beyond Dimensions 0.7.30. The bridge's API signatures were also checked against the supplied 0.8.0-alpha.1 source.

Run `./gradlew -PfluidSmokeTest runClient` for the terminal GUI check. It performs real clicks to check item and fluid transfers, stacked buckets, mixed searches, scrolling and Shift-locked ordering, saves screenshots, then exits.

Use `-PneoForgeVersion=21.1.244 -PmodernIndustrializationTestMods=/path/to/mods -PmekanismTestJar=/path/to/Mekanism.jar runGameTestServer` to load local MI and Mekanism dependencies and check actual uploads and saves for all supported tank tiers. `-PtankSmokeTest runClient` checks a terminal with zero item slots, actual bucket transfers and configuration synchronization.

For the Mouse Tweaks check, use `-PmouseTweaksTestJar=/path/to/local.jar -PmouseTweaksSmokeTest runClient`. It checks coordinate mapping, rapid requests and transfers in both directions through the actual custom screen handler. The compile-only API source is included in the repository; the runtime interface comes from Mouse Tweaks. Neither Mouse Tweaks nor its interface class is bundled into the mod JAR.

Use `-PcraftingSmokeTest runClient` for the crafting check. `-PioSmokeTest runClient` performs real clicks to check face modes, the fixed top face, batch configuration, adjacent block icons, dropped item collection and panel switching. Both save screenshots and exit.

Use `-PfurnaceSmokeTest runClient` to check all four slot tiers, live upgrades, actual Shift transfers, parallel recipes, automatic output and switch synchronization. It also saves screenshots at different GUI sizes.

The base suite contains 41 server GameTests covering saves, capacity, container transfers, menus, recipes, crafting, IO permissions, real hopper transfers, collection with full storage, furnace parallelism and fuel use, saved progress, and duplicate uploads. Separate client checks cover searches, Shift ordering, rendering and interaction. The parameters above load local mods for MI, Mekanism, Mouse Tweaks and Beyond Dimensions integration checks. Long survival sessions and real multiplayer have not been fully tested yet. Please report problems in [Issues](https://github.com/oritong/CloudStorage-Reborn/issues) with steps to reproduce and relevant logs.

### Credits And License

Port and maintenance: **Oritong**. Original authors: **Alexthe668, Yosh**.

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0-only)**. See [LICENSE](LICENSE) for the full license text. Citadel is a separate dependency covered by its own license. The compile-only Mouse Tweaks API interface source is covered by BSD-3-Clause; see [src/mouseTweaksApi/LICENSE](src/mouseTweaksApi/LICENSE).
