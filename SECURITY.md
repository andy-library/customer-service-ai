# 安全策略

作者：**andy yang**

> English version: [SECURITY.en.md](SECURITY.en.md)

## 支持的版本

| 版本 | 支持 |
|------|------|
| 0.2.x-rc | 是（当前） |
| 0.1.x | 有限（按需安全修复） |
| < 0.1 | 否 |

## 报告漏洞

请**不要**通过公开 GitHub Issue 报告安全漏洞。

优先方式：

1. 仓库开启时的 GitHub **Security Advisories**（私密）  
2. 通过 GitHub 维护者账号 **andy yang / andy-library** 私信或私密渠道联系  

请包含：

- 问题描述与影响范围  
- 复现步骤 / PoC（如有）  
- 影响版本 / commit  
- 修复建议（可选）  

一般在 **7 天内**确认收到，并与你协调修复与披露时间。

## 本项目已有安全能力

- 可选 API Key 鉴权（`CSAI_SECURITY_ENABLED=true`）  
- Admin / Client 角色分离  
- 限流  
- 会话归属校验  
- 基础提示注入特征拦截  
- 密钥仅应存在于环境变量 / 密钥管理服务  

## 生产加固清单

1. 开启 `CSAI_SECURITY_ENABLED=true`，使用强随机 Key  
2. 管理台勿对公网裸暴露  
3. 在反向代理终止 TLS  
4. 限制 Dify、模型端点、PostgreSQL 网络访问  
5. 定期轮换密钥；永不提交 `.env`  
6. 按容量调整限流与超时  
