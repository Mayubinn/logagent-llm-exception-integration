# logagent-exception-analyzer-sdk

一个 **Java 8** 可用的 LLM 异常分析 SDK，同时支持 Spring Boot 自动装配。

它的目标不是简单把堆栈扔给模型，而是尽量把“异常发生点附近的代码”一起交给 LLM，这样模型输出更接近真正能落地的排障建议。

---

## 1. 能力概览

### 1.1 基础能力

- 异步发送异常到 LLM API
- 线程池有界，队列满时直接丢弃，不阻塞主链路
- Java 8 可用
- Spring Boot 自动装配
- 统一配置前缀：`exception.llm.*`
- 开关为单布尔值，默认 `false`

### 1.2 这次新增的核心能力

发送给 LLM 的内容包含：

- 异常堆栈
- 请求上下文
- 报错位置源码片段
- 根因位置源码片段
- jar 运行场景下的 sources.jar 源码
- 拿不到源码时的反编译 Java 代码
- 实在拿不到时的反射方法骨架

换句话说，这个 SDK 会尽量让 LLM 在“看到代码”的前提下分析异常，而不是只看栈。

---

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.logagent</groupId>
    <artifactId>logagent-exception-analyzer-sdk</artifactId>
    <version>1.0.1-SNAPSHOT</version>
</dependency>
```

SDK 里已经带了 CFR 依赖，用于在运行期把 `.class` 尽量转成 Java 代码文本。

---

## 3. 模块结构

```text
logagent-exception-analyzer-sdk
├── pom.xml
├── README.md
└── src/main
    ├── java/com/logagent/sdk
    │   ├── AnalysisCallback.java
    │   ├── ExceptionAnalyzer.java
    │   └── ExceptionAnalyzerConfig.java
    ├── java/com/logagent/sdk/code
    │   ├── CodeContextCollector.java
    │   ├── CodeSnippet.java
    │   ├── ExceptionCodeContext.java
    │   ├── OptionalCfrDecompiler.java
    │   └── SourceCodeLocator.java
    ├── java/com/logagent/sdk/spring
    │   ├── DefaultExceptionAnalyzerFacade.java
    │   ├── ExceptionAnalysisContext.java
    │   ├── ExceptionAnalyzerAutoConfiguration.java
    │   ├── ExceptionAnalyzerCustomizer.java
    │   ├── ExceptionAnalyzerFacade.java
    │   └── ExceptionAnalyzerProperties.java
    └── resources/META-INF/spring.factories
```

---

## 4. 纯 Java 用法

### 4.1 默认配置

```java
ExceptionAnalyzer analyzer = ExceptionAnalyzer.create();
analyzer.analyze(exception);
```

### 4.2 自定义配置

```java
ExceptionAnalyzerConfig config = ExceptionAnalyzerConfig.builder()
        .llmApiUrl("https://your-llm-endpoint.example.com/api/v2/completions")
        .llmModel("deepseek")
        .authHeaderName("token")
        .authToken(System.getenv("LLM_API_TOKEN"))
        .maxTokens(1500)
        .temperature(0.2)
        .codeContextEnabled(true)
        .codeContextLinesBefore(25)
        .codeContextLinesAfter(25)
        .addApplicationPackage("com.bocsoft.overseabase")
        .build();

ExceptionAnalyzer analyzer = ExceptionAnalyzer.create(config);
analyzer.analyze(exception, "requestUri=/demo/test\nhttpMethod=GET");
```

### 4.3 回调方式

```java
analyzer.setCallback(new AnalysisCallback() {
    @Override
    public void onSuccess(Throwable exception, String report) {
        System.out.println(report);
    }

    @Override
    public void onFailure(Throwable exception, Throwable error) {
        error.printStackTrace();
    }
});
```

---

## 5. Spring Boot 用法

### 5.1 直接注入门面

```java
@Autowired
private ExceptionAnalyzerFacade exceptionAnalyzerFacade;

public void handle(Throwable throwable) {
    exceptionAnalyzerFacade.analyze(throwable);
}
```

### 5.2 带上下文分析

```java
ExceptionAnalysisContext context = ExceptionAnalysisContext.builder()
        .requestUri(request.getRequestURI())
        .httpMethod(request.getMethod())
        .queryString(request.getQueryString())
        .clientIp(request.getRemoteAddr())
        .traceId(MDC.get("traceId"))
        .handlerSignature("com.demo.UserController#getUser")
        .build();

exceptionAnalyzerFacade.analyze(throwable, context);
```

---

## 6. 发送给 LLM 的内容结构

SDK 发送的 user prompt 结构大致如下：

````text
### 异常堆栈
...

### 请求上下文
...

### 报错代码位置
#### 异常抛出点
- class: ...
- method: ...
- file: ...
- line: ...
- sourceType: SOURCE_FILE / SOURCES_JAR / DECOMPILED / REFLECTION_STUB
```java
...
```

#### 根因位置
...
````

这意味着模型能同时看到：

- 报错是什么
- 请求是怎么进来的
- 哪个类/方法/行附近出了问题
- 看到的是原始源码还是反编译代码

---

## 7. 报错位置代码是怎么找出来的

### 7.1 先选业务堆栈帧

`CodeContextCollector` 会优先找：

- 异常抛出点
- 根因位置

如果你配置了：

```yaml
exception:
  llm:
    application-packages:
      - com.bocsoft.overseabase
```

那 SDK 只会在这些包前缀里挑代码帧。

如果你没配置，它会自动排除常见框架包，再挑第一层业务帧。

### 7.2 找代码的优先级

`SourceCodeLocator` 会按下面顺序查找：

1. classpath 里的 `.java`
2. 本地源码目录（`source-roots`）
3. `*-sources.jar`
4. `.class` 反编译（CFR）
5. 反射骨架兜底

### 7.3 反编译说明

当源码不可用时，SDK 会读取异常类对应的 `.class` 并尝试反编译成 Java 代码。

这一层的目标不是完全还原源码，而是尽量让 LLM 至少看到：

- 出错方法的大致逻辑
- 分支、判空、调用链
- 类和方法签名

因此，LLM 输出里应该明确区分：

- 源码分析
- 反编译代码辅助分析

---

## 8. 外部化配置项

前缀统一为：

```yaml
exception.llm
```

### 8.1 开关

```yaml
exception:
  llm:
    enabled: false
```

规则只有：

- `true` 开启
- `false` 关闭
- 默认 `false`

### 8.2 请求上下文配置

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `include-request-context` | `true` | 是否附带请求上下文 |
| `include-query-string` | `false` | 是否附带 queryString |
| `include-client-ip` | `false` | 是否附带客户端 IP |
| `include-trace-id` | `true` | 是否附带 traceId |
| `include-handler-signature` | `true` | 是否附带处理器签名 |
| `trace-id-mdc-key` | `traceId` | MDC 中 traceId 的 key |
| `max-context-length` | `1024` | 上下文最大长度 |

### 8.3 代码上下文配置

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `code-context-enabled` | `true` | 是否提取报错代码上下文 |
| `code-context-lines-before` | `20` | 异常行前取多少行 |
| `code-context-lines-after` | `20` | 异常行后取多少行 |
| `code-context-max-chars` | `12000` | 最终代码上下文最大长度 |
| `search-sources-jar` | `true` | 是否继续搜索 sources.jar |
| `decompile-when-source-missing` | `true` | 拿不到源码时是否反编译 class |
| `max-code-frames` | `2` | 最多附带几个关键堆栈帧 |
| `application-packages` | 空 | 业务包前缀，用于精确定位代码帧 |
| `source-roots` | `src/main/java`,`src/test/java` | 本地源码根目录 |

### 8.4 LLM 调用配置

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `llm-api-url` | `https://your-llm-endpoint.example.com/api/v2/completions` | LLM 接口地址 |
| `llm-model` | `deepseek` | 模型名称 |
| `auth-header-name` | `token` | 认证头名称 |
| `auth-token` | 空 | 认证 token |
| `headers.*` | 空 | 额外请求头 |
| `custom-system-prompt` | 空 | 自定义 system prompt |
| `output-mode` | `LOG` | `LOG` / `STDOUT` / `CALLBACK` |
| `max-tokens` | `1500` | 最大生成 token |
| `temperature` | `0.2` | 采样温度 |
| `max-exception-chars` | `16000` | 异常堆栈最大长度 |

### 8.5 线程池与网络

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `core-pool-size` | `1` | 核心线程数 |
| `max-pool-size` | `2` | 最大线程数 |
| `queue-capacity` | `64` | 队列大小 |
| `keep-alive-sec` | `60` | 空闲线程存活秒数 |
| `connect-timeout-sec` | `10` | HTTP 连接超时 |
| `read-timeout-sec` | `60` | HTTP 读超时 |
| `write-timeout-sec` | `10` | HTTP 写超时 |

---

## 9. 配置示例

### application.yml

```yaml
exception:
  llm:
    enabled: false
    llm-api-url: https://your-llm-endpoint.example.com/api/v2/completions
    llm-model: deepseek
    auth-header-name: token
    auth-token: ${LLM_API_TOKEN:}
    include-request-context: true
    include-handler-signature: true
    code-context-enabled: true
    code-context-lines-before: 20
    code-context-lines-after: 20
    application-packages:
      - com.bocsoft.overseabase
```

### application-dev.yml

```yaml
exception:
  llm:
    enabled: true
```

### application-prod.yml

```yaml
exception:
  llm:
    enabled: false
```

---

## 10. 动态客户化方式

如果你需要在 `yml` 之外再改参数，可以声明 `ExceptionAnalyzerCustomizer`。

```java
@Bean
public ExceptionAnalyzerCustomizer exceptionAnalyzerCustomizer() {
    return (builder, properties) -> builder
            .maxTokens(1200)
            .temperature(0.1)
            .codeContextLinesBefore(30)
            .codeContextLinesAfter(30)
            .header("x-app-name", "oversea-base")
            .addApplicationPackage("com.bocsoft.overseabase")
            .customSystemPrompt("请优先结合 Spring MVC 调用链和报错代码位置分析异常");
}
```

`DefaultExceptionAnalyzerFacade` 会在每次真正触发分析前重新绑定 `exception.llm.*`：

- 配置没变就复用实例
- 配置变化就自动重建实例

---

## 11. 线程模型与稳定性

- 线程池是有界队列
- 拒绝策略是 `DiscardPolicy`
- 队列满时直接丢弃，不阻塞主线程
- 全部分析动作都在守护线程中执行
- 即使 LLM 调用失败，也不会影响业务异常处理链路

---

## 12. 注意事项

1. 建议显式配置 `application-packages`，不然业务帧识别只能靠排除法
2. 如果你们生产环境需要严格合规，建议继续保持 `enabled: false`
3. 发送源码、sources.jar 或反编译代码到外部 LLM 前，请确认符合你们的安全规范
4. `trace-id-mdc-key` 需要和你们自己的链路追踪实现一致
5. 反编译代码是辅助信息，不应该替代真实源码审查

---

## 13. 本次实现落点

这版 SDK 已经满足：

- `sdk` 抽成独立 Maven 模块
- Java 8 兼容
- Spring Boot 自动装配
- 开关简化为单布尔配置，默认关闭
- 参数全量外部化
- Spring Boot 应用动态客户化
- 把报错代码位置一起交给 LLM 分析
- jar 运行场景提供 sources.jar / 反编译 Java 代码兜底
