> Owner: architect | Status: Superseded by ADR-004 | Date: 2026-08-04 이전

# ADR-003: 사내 Git 서버를 origin으로 전환 시도 (폐기됨)

## 맥락 (Context)
초기에는 `origin`을 사내 Git 서버(`https://git.ondalprincess.synology.me/psung616/LightALM.git`)로 교체하고, 기존 GitHub 저장소에는 매번 URL을 직접 지정해 수동 push하는 방식(`git push https://github.com/psung616/LightALM.git main`)을 시도했다.

## 결정 (Decision — 당시)
- `origin` = 사내 Git 서버
- 기존 GitHub는 `origin`을 거치지 않고 URL 직접 지정 방식으로만 push

## 근거 (당시)
사내 Git 서버(`git.ondalprincess.synology.me`)에 저장소가 실제로 준비되어 있는지 확인되지 않아, 우선 기존 GitHub에만 안전하게 반영하고 사내 서버 push는 보류하기 위함이었다.

## 결과 및 폐기 사유
이 방식은 **실제로 실행되지 않았다** — 다중 push-url 스크립트(10-deployment.md 부록 D의 원안)도 참고용으로만 남고 실행되지 않았다. 이후 새 저장소(`LightALM_v2`)가 준비되면서 ADR-004로 완전히 대체됐다. 이 ADR은 "왜 한때 이런 구조를 고려했는지"를 남기기 위한 이력 기록용이다.

## Superseded-by
ADR-004

## 참고
- 02-architecture.md §2.4 "Git 다중 remote(10-deployment.md 부록 D)는 실제로는 사용하지 않았다" 문단
