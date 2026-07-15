## 变更摘要 / Summary

<!-- 用 1–3 句话说明：改了什么、为什么改 -->

## 变更类型 / Type

- [ ] 缺陷修复 / Bug fix
- [ ] 新功能 / Feature
- [ ] 文档 / Docs
- [ ] 重构 / Refactor
- [ ] CI / 工程化
- [ ] 破坏性变更 / Breaking（请更新 CHANGELOG）

## 关联 Issue / Related

<!-- 例如：Closes #123 -->

## 如何验证 / How to test

1. `./scripts/install-framework.sh`（如本地尚未安装 framework）
2. `mvn test`
3. （可选）本地启动步骤：

## 检查清单 / Checklist

- [ ] `mvn test` 本地已通过
- [ ] CI 全部 job 绿色（`test` + `secrets-hygiene`）
- [ ] **未提交** `.env`、真实 API Key、Token、证书私钥
- [ ] 行为变更有单测或说明为何无需单测
- [ ] 用户可见变更已更新 `CHANGELOG.md`（如适用）
- [ ] 中文文档已同步（如适用）

## 维护者备注 / Maintainer note

- 日常小改动：维护者可直推 `main`  
- **任何 PR 合并：CI 必须全绿**（`test` + `secrets-hygiene`），请勿 “Merge without waiting”  
- 外部贡献：只能走 PR  
