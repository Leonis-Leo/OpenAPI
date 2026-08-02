# RabbitMQ 笔记

## 本机环境

- 版本：RabbitMQ 4.3.4 + Erlang/OTP 27（Windows 便携版，免安装）
- 位置：`E:\RabbitMQ\rabbitmq_server-4.3.4`，Erlang 在 `E:\Erlang27`
- 端口：AMQP `5672`，管理界面 `15672`（`guest/guest`）
- 启动方式（后台进程）：

```powershell
$env:PATH = 'E:\Erlang27\bin;' + $env:PATH
$env:RABBITMQ_HOME = 'E:\RabbitMQ\rabbitmq_server-4.3.4'
& "$env:RABBITMQ_HOME\sbin\rabbitmq-server.bat"
```

> ⚠️ 踩坑：RabbitMQ 4.3.4 与 Erlang OTP 29 不兼容（`horus` 特性标志报错），必须用 OTP 26/27。

## 在项目中的用途

**接口调用统计的异步落库链路**：

```mermaid
flowchart LR
    Call[签名调用 /api/**] --> Interceptor[SignatureInterceptor]
    Interceptor -->|发送消息| MQ[(RabbitMQ)]
    MQ -->|消费| Consumer[InvokeLogConsumer]
    Consumer --> DB[(invoke_log 表)]
    DB --> Stats[统计接口 + ECharts 图表]
```

- 交换机：`openapi.invoke.exchange`（direct），队列：`openapi.invoke.log`
- 消费者用 `@RabbitListener` 异步写入 `invoke_log`，调用方无感知
- 统计接口：`/v1/stats/overview`（总量/成功率）、`/v1/stats/daily`（近 N 天趋势）

## 关键知识点

- 为什么异步：调用频率高，同步写库拖慢响应；MQ 削峰 + 解耦，统计失败不影响主链路
- 可靠投递：交换机/队列/绑定声明、消息确认机制
- 面试对比：RabbitMQ（功能适中、运维简单）vs Kafka（高吞吐、偏重）

关联：[[CI自动化测试]]、[[项目规划]]
