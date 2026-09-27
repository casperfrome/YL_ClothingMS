# 警告与兼容性检查记录

检查日期：2026-09-26。范围：本项目源码、配置、实际依赖树、迁移后的应用与 MySQL 日志、现有测试记录，以及独立的严格 Java 编译检查。本文只列出发现和建议，未修改业务代码、依赖或运行配置。

当前没有发现阻断项目运行的兼容性故障。迁移验收中的 18 项测试全部通过，58 次接口和静态资源请求成功；11 张表、395 条记录在应用启动前后核对一致。这些结果证明已覆盖路径可用，不代表所有数据库语法、浏览器和部署环境都获得了完整兼容性保证。

实际连接参数已迁移至不纳入 Git 的 `config/local.yml`。首次使用请从项目根目录复制 `config/local.example.yml` 为 `config/local.yml` 并填写密码，再从项目根目录运行 Maven 或 JAR；同名环境变量可覆盖本地配置。本文保留历史诊断结论，不公开实际密码。

## 环境与检查方法

| 组件 | 实际版本 / 状态 |
|---|---|
| Java / Maven | Oracle JDK 25.0.2 / Maven 3.9.12 |
| Spring Boot / Framework | 3.5.16 / 6.2.19 |
| MyBatis Starter / MyBatis | 3.0.5 / 3.5.19 |
| MySQL Server / Connector/J | 9.7.2 / 9.7.0 |
| Flyway Core / MySQL 模块 | 均为 11.7.2 |
| Mockito / Byte Buddy | 5.17.0 / 1.17.8，仅测试使用 |
| 图片插件 | TwelveMonkeys WebP 3.12.0 |
| 数据库 | mysql_9_7_2，YL_260924；旧容器 friendly_bohr 保留 |

本次执行了 `mvn dependency:tree`、`mvn -o validate`，并使用现有测试依赖类路径对全部主程序和测试 Java 源码执行独立的 `javac -Xlint:all -proc:none --release 25 -encoding UTF-8`。严格编译成功，仅出现下表中的 serial 警告，输出保存到独立目录，没有替换运行中的 JAR。没有重新运行会写入数据库的集成测试，测试结论引用同日迁移验收记录。

## 一、已观察到的警告或输出问题

| 编号 / 优先级 | 内容与依据 | 影响 | 建议解决方案 |
|---|---|---|---|
| W1 / 中 | Flyway 11.7.2：`MySQL 9.7 is newer than this version of Flyway`，日志声称最高已测试版本为 8.1。应用启动日志第 22 行、构建日志第 51 行。 | 当前 V1–V3 校验、空测试库初始化和业务测试通过；后续迁移的 SQL 解析、元数据读取及新版本行为仍有未经验证的风险。不是导入失败。 | 查明候选版本对 MySQL 9.7 的明确支持，再让 `flyway-core` 与 `flyway-mysql` 同版本升级；在独立库验证全新迁移、已有库升级、历史校验和完整测试。不要删除历史表、修改旧迁移或关闭校验来消除提示。官方当前 MySQL 页面仍只列出验证版本 5.7、8.0、9.4，不能承诺“升级最新版一定解决”。[Flyway 官方说明](https://documentation.red-gate.com/fd/mysql-277579322.html) |
| W2 / 中 | 测试 JVM：`A Java agent has been loaded dynamically`，加载的是 `byte-buddy-agent-1.17.8.jar`；随后提示未来默认禁止动态加载。构建日志第 67–70 行。 | 当前测试可通过；未来收紧动态代理加载的 JDK 下，Mockito 内联 Mock 可能失效。只在测试路径出现，不是业务数据库故障。 | 按 Mockito 文档，在 Surefire 的测试 JVM 启动参数中显式配置 Mockito 的 `-javaagent`，并同步 IDE 测试启动配置。保留其他已有 argLine；若使用 `@{argLine}`，先定义空默认值。`-XX:+EnableDynamicAgentLoading` 只是显式允许现有行为，不是首选长期处理。[Mockito 5.17 文档](https://javadoc.io/static/org.mockito/mockito-core/5.17.0/org.mockito/org/mockito/Mockito.html) |
| W3 / 低 | 测试 JVM：`Sharing is only supported for boot loader classes because bootstrap classpath has been appended`。构建日志第 66 行。 | 测试代理追加启动类路径后，部分 CDS 类数据共享优化不可用；不改变接口或数据正确性。 | 可保留。如果要求测试输出无此提示，可仅在测试 JVM 使用 `-Xshare:off`，代价是关闭该 JVM 的 CDS。配置显式 agent 后仍可能出现此提示，不能把 W2 与 W3 当成同一个开关解决。 |
| W4 / 低 | 严格编译发现：`BusinessException` 没有 `serialVersionUID`。位置：`src/main/java/com/yl/clothing/exception/BusinessException.java:3`。 | RuntimeException 可序列化；未来如果跨版本保存/传输 Java 序列化异常对象，默认 UID 变化会影响反序列化。当前 HTTP 层把异常转换为 JSON 错误响应，不受影响。 | 增加 `@java.io.Serial private static final long serialVersionUID = 1L;`。这是源代码整洁性修正，不需要改变异常处理逻辑。 |
| W5 / 中（跨平台时） | MySQL `[MY-010159]`：数据目录所在文件系统不区分大小写，自动设置 `lower_case_table_names=2`。当前查询值确为 2。 | 当前源、目标一致，迁移正常。将来迁往大小写敏感的 Linux 存储时，库表名比较规则可能改变；直接复制 Windows 数据目录存在配置不匹配风险。 | 统一库表名大小写，优先使用 SQL 逻辑导出/导入；在初始化新实例前决定并验证大小写设置，检查应用 SQL 和迁移脚本。不要在现有数据目录上随意切换该设置。[MySQL 官方说明](https://dev.mysql.com/doc/refman/9.7/en/identifier-case-sensitivity.html) |
| W6 / 低（本机） | MySQL `[MY-010068]`：`CA certificate ca.pem is self signed`。 | 默认自签名 CA 并不等于服务器不能使用 TLS；但客户端需要明确信任。当前 JDBC 设置 `useSSL=false`，这张证书没有为应用连接提供加密保护。 | 本机演示可记录保留；远程连接应配置受信任证书、正确的证书主机名和客户端信任链，使用 `sslMode=VERIFY_IDENTITY`。仅换证书而不修改 JDBC 配置，不能保护当前连接。[Connector/J 安全属性](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html) |
| W7 / 低 | MySQL `[MY-011810]`：PID 文件目录 `/var/run/mysqld` 可被所有 OS 用户访问，服务器认为目录配置不安全。 | 属于容器内目录权限问题，不是数据库内容损坏；容器内存在其他低权限用户/进程时，需要评估访问边界。 | 核对目录属主、权限及 entrypoint 行为，在可复现的镜像或启动配置中为 mysqld 提供权限受控的专用 PID 目录；修改后验证启动与停止，避免仅临时改正在运行的容器。 |
| W8 / 已消除的初始化警告 | MySQL 初次初始化时 `[MY-010453]`：`root@localhost is created with an empty password`。只发生在 entrypoint 初始化阶段。 | 不能据此断言当前 root 仍为空密码。本次只读查询确认 `root@localhost` 和 `root@%` 的认证字符串均非空，认证插件为 `caching_sha2_password`，使用本地配置中的数据库凭据已验证连接。 | 当前不需要为这条历史日志改密码。以后检查初始化是否完整结束，以及当前账号实际状态即可；不要将历史初始化提示误报为当前空密码漏洞。 |
| W9 / 低 | Windows 输出编码不一致：Maven 项目中文名称及此前导出的部分测试日志出现乱码。`mvn -o validate` 可复现。 | 影响日志阅读和排障；未发现数据库中文内容损坏，SQL 备份使用二进制管道保存，并已通过数据摘要校验。 | 对齐 JVM 标准输出/错误输出编码、PowerShell 的 `$OutputEncoding` 与控制台编码，以及接收程序的解码方式；文件读取显式 UTF-8。备份继续使用二进制流，不通过 PowerShell 文本重编码。已被替换成乱码字符的旧日志不能靠再次按 UTF-8 读取修复。 |

## 二、部署和版本兼容边界

以下是代码或配置明确存在的限制，不全部是当前故障，也不全部会打印 warning。

| 编号 / 场景 | 内容 | 影响 | 建议解决方案 |
|---|---|---|---|
| C1 / Java 运行环境 | POM 指定 Java 25，当前编译产物以 25 为目标；未固定 Maven Wrapper/Java Toolchain。 | 换电脑或 CI 使用较低 JDK，会出现无法编译目标版本或无法加载较新 class 文件的问题；不同 Maven 环境也可能产生工具链差异。 | IDE、JAVA_HOME、CI 均固定 JDK 25，补充 Maven Wrapper/Toolchain 可提高可复现性。若要支持 Java 21，需单独评估并重新编译测试，不能只把运行命令换成 Java 21。 |
| C2 / 旧浏览器 | `static/js/api.js:2` 直接调用 `AbortSignal.timeout(15000)`；还使用原生 dialog、ES Modules、顶层 await、可选链等，未设置降级方案。 | 缺少 timeout API 时所有 API 请求会在发出前失败；其他现代特性缺失可导致页面或弹窗不能使用。当前桌面浏览器验收不等于旧 WebView/Safari 等均可用。 | 明确支持的浏览器范围；若需兼容旧版本，为超时使用 `AbortController + setTimeout` 回退，为 dialog 提供兼容层，并评估是否需要转译。MDN 将 timeout 标为 Baseline 2024。[浏览器说明](https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal/timeout_static) |
| C3 / 远程数据库部署 | JDBC 为 `useSSL=false&allowPublicKeyRetrieval=true`；使用本地配置中的数据库账号；目标有 root@%，Docker 3306 的 HostIp 为空，未限定 127.0.0.1。 | 本机演示可以运行，但端口可能经宿主机非回环接口暴露（实际可达性还取决于 Docker/防火墙，未从外部探测）。远程使用缺少连接加密及可信服务器身份校验，root 权限过大。 | 本机优先将端口绑定到 `127.0.0.1:3306:3306`。若改为远程部署，另行配置 TLS、受限业务账号、迁移账号和密钥管理；账号策略应另行确认；当前实际连接参数保存在不纳入 Git 的 `config/local.yml` 中。[安全连接属性](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html) |
| C4 / HTTP 部署范围 | 服务只绑定 127.0.0.1，无登录/鉴权，同源 API。 | 其他机器无法按默认配置访问；若简单改为公网监听，管理和模拟器接口也随之开放。此项是本机演示边界，不是当前访问故障。 | 保持本机用途；如要部署到共享网络，在开放访问前增加身份认证、权限控制和 HTTPS，并设计代理访问方式。 |
| C5 / 图片目录 | `UPLOAD_DIR` 默认 `./data/uploads`，服务启动时按工作目录解析绝对路径。 | 从不同目录启动 JAR、运行成服务或换到容器中，可能读到另一处目录，导致历史上传图 404 或文件分散；只备份数据库也不能恢复上传图片。 | 固定工作目录，或将 UPLOAD_DIR 设置为绝对持久化目录；数据库与上传文件成套备份恢复。 |
| C6 / 多实例及真实订单 | 模拟器使用 JVM 内 synchronized 锁；V5 起按批次归属和保留设置清理，启动只清理未保留批次，启停和正常关闭按当前批次设置处理。 | 两个后端实例不能通过该锁互斥，一个实例的启动清理可能删除另一个实例尚未保留的批次。保留功能仍属于模拟器，不能代替真实订单的业务管理。 | 当前保持单实例约定，历史已保留批次不自动清理。若将来需要多实例或真实订单，另行设计分布式调度、锁和数据隔离。 |
| C7 / 两个 Docker 容器 | 源容器和目标容器都映射宿主机 3306。 | 不能按原配置同时启动；可能端口冲突或误连回旧库。 | 旧容器保持停止。若需并行只读核对，将备用容器另行绑定不同端口，并通过 server_uuid 确认连接。生产恢复写入后不得无同步直接回切。 |
| C8 / 测试环境 | 集成测试需要真实 MySQL、建库/建表权限，固定使用同一服务上的 YL_260924_test，继承 DB_HOST/DB_PORT 和账号；凭据从 `config/local.yml` 或环境变量读取。 | MySQL 未运行或账号权限不足时，`mvn verify` 失败；并发 CI 使用同名测试库会互相影响。业务库默认隔离，但测试库内数据不是应长期保留的数据。 | CI 使用独立 MySQL 测试实例；需要并发时为每个执行任务分配数据库。保留库名安全断言，避免将共享测试库当作业务库。 |
| C9 / 图片格式验证覆盖 | 服务依赖 TwelveMonkeys 读取 WebP；自动上传测试主要构造 PNG，历史人工验收覆盖 JPEG，没有找到 WebP 上传回归用例。 | 这是验证缺口，尚无证据说明当前 WebP 插件不兼容。不能用 PNG 测试通过来证明所有 WebP 变体都兼容。 | 增加真实 WebP 样本回归，明确动画/多帧和特殊颜色模式是否属于支持范围；检查打包后 ImageIO provider 能发现插件。 |
| C10 / 驱动维护 | 当前 Connector/J 9.7.0 已连接目标并通过测试；官方当前推荐的 26.7 系列替代 9.7 系列。 | 版本较旧本身不能认定为不兼容，也不能据此编造已知漏洞；继续使用会需要自行跟踪上游修复。 | 在独立升级任务中阅读变更说明，并优先考虑与 Spring Boot 依赖管理保持一致；回归金额、布尔、时间、事务和批量写入。此次审计不建议为“数字更新”而直接改依赖。[官方发布说明](https://dev.mysql.com/doc/relnotes/connector-j/en/news-26-7-0.html) |

## 三、源码中预留的警告/错误分支

这些分支已存在，但没有证据表明它们目前都在业务运行中发生。

| 日志内容 / 位置 | 含义与影响 | 处理建议 |
|---|---|---|
| `无法移除图片 {}`，ImageService.java:155，WARN | 数据库删除已经提交或上传回滚后，文件补偿删除失败；可能残留孤立图片。 | 检查目录权限、磁盘和占用；按数据库引用核对后清理孤立文件，必要时增加持久化重试任务。不能把所有图片批量删除。 |
| `模拟订单批次失败`，OrderSimulatorService.java:98，ERROR | 批次事务失败，状态置为 ERROR；调度会继续尝试，长期失败可能重复刷日志。 | 区分测试注入和真实故障；真实故障检查连接、锁、SQL 和权限，长时间故障可增加退避与日志限频。 |
| `退出清理失败，下次启动将再次清理`，OrderSimulatorService.java:115，ERROR | 停机时数据库不可用等原因导致订单暂时保留，下次启动会再清理；下次仍不可用时也不能保证清理成功或应用可启动。 | 保障停机时数据库与连接池仍可用；按当前模拟器约定在恢复后核查订单，不依赖强制终止完成清理。 |
| `请求处理失败`，GlobalExceptionHandler.java:75，ERROR | 未被专门处理的异常导致 HTTP 500。它不是某个已确定的版本冲突。 | 根据完整堆栈定位具体请求和根因，不能统一通过降低日志级别解决。 |

测试中出现的 `DataAccessResourceFailureException: injected database failure` 来自 OrderSimulatorServiceTest 的主动故障注入。对应测试通过，证明异常状态和恢复路径得到验证；不能将其计入真实数据库故障。

## 四、确认正常或暂未发现问题的部分

- Spring Boot 3.5.16 官方明确支持 Java 17–25；本项目 Framework 6.2.19 也满足要求。当前 JDK 25 不应被列为 Spring Boot 不兼容。[官方系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- MyBatis Starter 3.0 系列官方对应 Spring Boot 3.2–3.5、Java 17 及以上；本项目组合在声明范围内。[官方矩阵](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- Flyway Core 与 MySQL 模块版本一致，未发现二者版本错配。
- MySQL 使用 caching_sha2_password，现有 Connector/J 实际认证成功；没有观察到认证插件不兼容。
- MySQL 全局时区是 UTC，应用连接显式设置 +08:00，JSON 输出也包含 +08:00；全球时区不同本身不是当前缺陷。
- 严格编译没有发现 deprecated/unchecked/rawtype 类警告，只发现 W4；Maven validate 成功。未观察到 Unsafe/native-access 警告，不将常见于其他项目的警告套用到本项目。
- 历史浏览器验收记录为控制台无警告/错误；本次未重新做多浏览器交互测试，不能把历史记录表述为本次覆盖所有浏览器。
- 本次未进行全量 CVE/SCA 扫描、生产压测或所有图片编码变体测试，不给出“没有任何漏洞或兼容性问题”的保证。

## 建议处理顺序

1. 保留当前可用基线，优先确认 Flyway 的 MySQL 9.7 支持策略。
2. 配置测试启动 agent，修正 serial 警告和终端编码；CDS 提示按是否要求安静日志决定保留或关闭共享。
3. 固定工具链、上传目录和浏览器支持范围，补齐 WebP 验证。
4. 只有需要共享网络/生产部署时，再实施账号、TLS、监听地址和单实例限制的调整。

## 本地证据

- [迁移验收报告](../data/backups/mysql-migration-20260926-120140/migration-report.md)
- [应用启动日志](../data/backups/mysql-migration-20260926-120140/application.stdout.log)
- [Maven 全量验收日志](../data/backups/mysql-migration-20260926-120140/maven-verify.log)
- [实际依赖树](../target/compatibility-dependencies.txt)
- [严格编译输出](../target/compatibility-audit/javac-lint.log)
- [MySQL 容器日志快照](../target/compatibility-audit/mysql-container.log)
- [Maven / JDK 版本输出](../target/compatibility-audit/maven-version.log)

`target` 下的辅助检查产物会被下一次 `mvn clean` 清理；迁移备份及本文保留在各自目录。
