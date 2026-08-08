> Owner: architect | Status: Accepted (현재 유효) | Date: 2026-08-08

# ADR-005: 사내 Gitea(synology remote) 추가 — push 시 자동 운영 배포 트리거

## 맥락 (Context)
ADR-004까지는 사내 Git 서버 저장소 준비 여부가 확인되지 않아 GitHub(`origin`)에만 push하고 있었다. 2026-08-08부로 사내 Gitea가 실제로 준비됐다.

## 결정 (Decision)
`synology`라는 이름으로 두 번째 remote를 추가한다.

| remote 이름 | URL | 용도 |
|---|---|---|
| `origin` | `https://github.com/psung616/LightALM_v2.git` | 소스 백업/개인 저장소. push해도 자동화 없음 |
| `synology` | `https://git.ondalprincess.synology.me/FactorySolution/ALM_Repository` | **운영 배포 트리거.** push하면 webhook으로 Jenkins `ALM_Pipeline`이 자동 실행되어 `https://alm.ondalprincess.synology.me/`에 반영됨 |

**배포하려면 반드시 `synology`에도 push해야 한다.** `origin`에만 push하면 소스 백업만 될 뿐 실제 서비스에는 반영되지 않는다.

인증은 Git Credential Manager(GCM)가 브라우저 OAuth로 처리(사내 Gitea 계정 승인). 두 저장소에 항상 동시 반영하고 싶으면 `origin`에 push URL을 두 개 등록하는 방법도 가능(10-deployment.md 부록 D 참고).

## 결과 (Consequences)
- 장점: push 한 번으로 실제 운영 서버 배포까지 자동화됨(약 1~3분 내 반영, ADR-006 참고)
- **중요한 리스크**: 앞으로 어떤 역할(특히 developer/devops 에이전트)이든 `synology`에 push하는 순간 즉시 운영 서버가 바뀐다. 실수로 미완성 브랜치를 push하면 바로 장애로 이어질 수 있으므로, **`synology` push는 명시적 배포 의도가 있을 때만** 수행해야 한다 — 이는 07-integrations.md의 "재시도 큐 없음" 원칙과 마찬가지로 Light 버전이 아직 갖추지 못한 안전장치다
- 이 저장소(`ALM_Repository`)는 GitHub의 `LightALM_v2`와 이름이 다르므로 혼동 주의

## Supersedes / Extends
ADR-004를 대체하지 않고 확장(origin은 유지, synology 추가)

## 참고
- 10-deployment.md 부록 D (2026-08-08 갱신)
