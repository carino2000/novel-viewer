# Novel Viewer 프로젝트

## 프로젝트 개요
웹소설을 읽으면서 AI 사이드패널을 함께 보여주는 서비스.
밀리의서재처럼 소설을 읽게 해주는 웹앱이며, 오른쪽 패널에서 AI 기능을 제공한다.

## 기술 스택
- **Backend**: Spring Boot (Java 21, Maven)
- **Frontend**: Next.js
- **Database**: MySQL (DB명: novel_viewer, 인코딩: utf8mb4)
- **AI**: Claude API (platform.claude.com 별도 결제)
- **개발환경**: 로컬 PC (배포 없음)
- **로그인 기능**: 미구현 (추후 추가 예정)

## UI 구조
- 화면 왼쪽: 소설 본문 뷰어
- 화면 오른쪽: 탭 패널
  - 인물 정보 탭 (AI 업데이트)
  - 직전 에피소드 요약 탭 (AI 업데이트)
  - 자유 메모장 탭
  - 인물 관계도 탭 (relations_json 기반)

## AI 호출 설계
- 트리거: 사용자가 수동으로 누르는 "AI 업데이트 버튼"
- 전송 범위: last_updated_episode ~ current_episode 구간 원문
- 1회 호출로 인물정보 + 요약 + 관계도를 JSON으로 한번에 받아옴
- 직전 요약은 직전 몇 화 원문을 러프하게 전송
- 챗봇 기능은 복잡도로 인해 제외 (추후 RAG 구조로 구현 고려)

## DB 테이블 구조

### novels (소설 메타정보)
- novel_id, title, author, description, total_episodes, created_at, updated_at

### episodes (화별 원문)
- episode_id, novel_id, episode_number, title, content(LONGTEXT), char_count, created_at
- UNIQUE: (novel_id, episode_number)

### reading_progress (읽기 진행상황)
- progress_id, novel_id, current_episode, paragraph_index, last_updated_episode, relations_json, last_read_at
- UNIQUE: (novel_id)
- paragraph_index: 화 내 문단 위치 (스크롤 복귀용)
- last_updated_episode: 마지막 AI 업데이트 시점 화번호
- relations_json: 인물 관계도 JSON [{"from": "김수현", "to": "세라프", "relation": "적대적 협력"}]

### characters (인물 정보)
- character_id, novel_id, name, description, first_appeared_at, last_updated_at, stats_json, created_at, updated_at
- stats_json 구조: {"근력": 86, "내구": 92, "민첩": 96, "체력": 78, "마력": 48, "행운": 36}
- UNIQUE: (novel_id, name)

### episode_summaries (직전 에피소드 요약 캐시)
- summary_id, novel_id, episode_number, summary_text, created_at
- UNIQUE: (novel_id, episode_number)

### memos (자유 메모장)
- memo_id, novel_id, title(NULL 허용), content, episode_number(NULL 허용), created_at, updated_at

## 소설 파싱 정보
- 현재 소설: 메모라이즈 (총 1069화)
- 파일 인코딩: CP949
- 화 구분 패턴: `^\d{5}\s+(.*?)\s*=+` (정규식)
- 화당 평균 글자수: 약 7,700자

## JPA 엔티티 패키지
- `novel_viewer.domain.entity`
- 엔티티명 단수형 사용 (Novel, Episode, ReadingProgress, Character, EpisodeSummary, Memo)
- @Table(name = "테이블명") 명시
- camelCase → snake_case 자동변환 (Spring Boot 기본 설정)
- @PrePersist, @PreUpdate로 created_at, updated_at 자동 관리

## 다음 작업 순서
1. JPA 엔티티 전체 작성 (Novel 완성, 나머지 작성 필요)
2. 소설 파싱 & DB 적재 API 구현 (MultipartFile → CP949 디코딩 → 정규식 파싱 → INSERT)
3. Next.js 기본 UI (좌우 레이아웃)
4. 기능 연결 (읽기 진행상황 저장, AI 업데이트 버튼)
