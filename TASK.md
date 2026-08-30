# NiBen — 일본어 학습 잠금화면 앱 기획서

## 개요
- **목적**: 잠금화면(해제 전)에 오늘의 일본어 문제를 상시 노출하고, **항상 퀴즈 형식**으로 정답을 맞히며 학습.
- **사용자**: 개인 사용(1인), 배포 없음(APK 직접 설치).
- **개발 방식**: 혼자 개발, 점진적 업데이트. 매 단계는 독립적으로 동작 가능해야 함(항상 빌드되는 상태 유지).
- **핵심 제약**: 리소스(배터리·메모리·네트워크) 최소화. 서버/백엔드 없이 완전 오프라인.
- **핵심 학습 구조**: 단순 노출이 아니라 **"문제 → 응답 → 채점 → 기록"**의 퀴즈 루프가 처음부터 끝까지 관통. 향후 난이도 설정을 얹을 수 있도록 데이터 구조를 처음부터 설계.

## 구현 방식 결정
안드로이드 5.0 이후 OS 차원의 "잠금화면 위젯" API는 존재하지 않음. 대신 **상시 알림(Ongoing Notification)** 방식 채택.
- 잠금화면에 알림이 그대로 노출됨(안드로이드 표준 동작).
- `NotificationCompat`만으로 구현 가능 → 특별 권한 불필요(Android 13+ `POST_NOTIFICATIONS` 런타임 권한만 필요).
- 배터리 최적화 예외 처리 불필요, `WorkManager`로 주기적 갱신만 하면 됨.
- 라이브 배경화면(WallpaperService)이나 커스텀 잠금화면(기기 관리자 권한) 대비 개발 난이도·리소스 소모가 가장 낮음.

### 퀴즈를 잠금화면에서 어떻게 풀게 할 것인가
알림 UI는 좁기 때문에 문제 난이도에 따라 두 트랙으로 나눔.

| 퀴즈 형태 | 위치 | 방식 |
|---|---|---|
| OX / 3지선다 | **알림 자체** | 알림 액션 버튼(최대 3개)으로 즉시 응답. `BroadcastReceiver`가 정답 여부 판정 후 알림 텍스트만 갱신(앱을 열 필요 없음). 잠금 해제 없이 학습 가능. |
| 4지선다 / 뜻 입력 / 예문 완성 | **앱 화면** | 알림을 탭하면 앱이 열리며 해당 문제의 퀴즈 화면으로 바로 진입. |

→ MVP는 OX 퀴즈부터 시작(구현이 가장 단순), 이후 단계에서 3지선다·앱 내 4지선다로 확장.

## 기술 스택 (경량 우선)

| 영역 | 선택 | 이유 |
|---|---|---|
| 언어 | Kotlin | 안드로이드 표준, 별도 러닝커브 없음 |
| UI | Jetpack Compose (Material 3) | 화면 수가 적어(퀴즈/결과/설정 등) 선언형 UI로 유지보수 최소화 |
| 로컬 DB | Room (SQLite) | 문제 데이터·오답 기록 저장, 완전 오프라인, 가벼움 |
| 설정 저장 | Jetpack DataStore (Preferences) | 알림 주기·난이도 등 사용자 설정 저장 |
| 백그라운드 스케줄링 | WorkManager | Doze/배터리 최적화 대응 표준 API, 문제 갱신 주기 관리 |
| 알림 | NotificationCompat + BroadcastReceiver(액션 처리) | 잠금화면 노출 + 알림 내 즉시 채점의 핵심 축 |
| 발음 | Android 내장 TextToSpeech API | 별도 TTS 라이브러리·서버 불필요 |
| 콘텐츠 데이터 | 앱 내 번들 JSON/사전 생성 SQLite(assets) | 서버 호출 없음, 네트워크 권한 자체가 불필요 |
| DI | 사용 안 함(수동 DI) | 앱 규모 대비 프레임워크 도입은 과함 |
| 네트워크 라이브러리 | 사용 안 함 | 완전 오프라인이므로 Retrofit 등 불필요 |
| 최소 SDK | API 26(Android 8.0) 권장 | 구형 기기 지원과 WorkManager/알림 채널 호환의 합리적 하한선 |

> 원칙: "필요해지기 전까지 라이브러리를 추가하지 않는다." Firebase/Analytics/Ads/이미지 로딩 라이브러리 등은 도입하지 않음.

## 일본어 콘텐츠(문자·단어·문장)를 앱에 넣는 방법

문자 종류별로 만드는 방식이 다르기 때문에 아래처럼 분리해서 처리. 모든 가공은 **빌드/개발 시점에 1회**만 수행하고, 결과물(JSON 또는 미리 채워진 SQLite 파일)만 앱에 번들 → 런타임에는 어떤 네트워크 호출도 없음(리소스 최소화 원칙 유지).

### 1. 히라가나 / 가타가나 (고정 46자 + 탁음·요음)
- 종류가 고정돼 있고 절대 변하지 않으므로 **외부 데이터 없이 Kotlin 코드에 직접 하드코딩**(`object HiraganaTable { ... }` 형태의 리스트).
- 필드: 문자, 로마자 표기, 발음(TTS용), 획순 이미지(선택, 나중에 추가 가능).

### 2. 한자
- 공개 데이터셋 **KANJIDIC2**(퍼블릭 도메인, 한자별 음독/훈독/뜻/획수/JLPT급수 포함) 사용.
- JLPT N5~N1 범위로 필터링해 약 2,000자 내외로 축소 → 용량 최소화.
- 변환: 다운로드한 XML을 개발 중 한 번 스크립트(Python 등)로 파싱해 앱이 바로 읽을 수 있는 JSON/CSV로 가공 후 `assets`에 포함.

### 3. 단어(어휘)
- 공개 사전 데이터셋 **JMdict**(일본어 다국어 사전, 퍼블릭 도메인) 사용.
- 표제어/요미가나/품사/뜻 필드만 추출, JLPT 급수 태그가 있는 항목 위주로 필터링해 크기 축소.
- MVP 단계에서는 전체를 쓰지 않고 **직접 수기로 30~50개 기초 단어 JSON**을 먼저 만들어 시작(품질 검증 겸 콘텐츠 포맷 확정) → 이후 단계에서 JMdict 기반 대량 추가로 확장.

### 4. 문장(예문)
- 공개 예문 코퍼스 **Tatoeba Project**(크리에이티브 커먼즈 라이선스, 일본어-한국어/영어 대역 문장 포함) 사용.
- 난이도가 높은 문장은 제외하고 기초 단어 위주 예문만 필터링.

### 콘텐츠 파이프라인 요약
```
[공개 데이터셋 원본] → (개발 중 1회 변환 스크립트) → [JSON/SQLite] → assets 번들 → 앱 최초 실행 시 Room에 삽입(prepopulate)
```
- 이렇게 하면 콘텐츠를 나중에 늘릴 때도 "스크립트 재실행 → 새 JSON 교체 → 앱 업데이트"만 하면 되어 점진적 업데이트 방식과 잘 맞음.
- 라이선스(퍼블릭 도메인/CC) 확인은 Phase 0에서 미리 처리.

## 난이도 설정 — 지금 설계하고, 나중에 기능으로 노출

나중에 "난이도 설정" 기능을 추가할 수 있도록, **Phase 1 시점부터 DB 스키마에 난이도 관련 필드를 미리 포함**시켜 마이그레이션 없이 확장 가능하게 함.

### 콘텐츠 테이블 (`content_item`)
| 필드 | 설명 |
|---|---|
| id | PK |
| category | HIRAGANA / KATAKANA / KANJI / VOCAB / SENTENCE |
| japanese_text | 일본어 원문 |
| reading | 요미가나(후리가나) |
| meaning_ko | 한글 뜻 |
| level | 난이도 값(1~5 또는 JLPT N5~N1 매핑) — **Phase 1부터 저장, UI 노출은 Phase 4** |
| example_sentence | 예문(선택) |
| source | 출처 기록(라이선스 추적용) |

### 퀴즈 기록 테이블 (`quiz_log`)
| 필드 | 설명 |
|---|---|
| id | PK |
| item_id | FK → content_item |
| quiz_type | OX / 3지선다 / 4지선다 / 뜻 입력 등 |
| is_correct | 정답 여부 |
| answered_at | 응답 시각 |

- 이 로그가 쌓이면 Phase 4에서 "자주 틀리는 항목 위주 출제", "정답률 기반 자동 난이도 조정" 같은 적응형 학습으로 확장 가능.
- 난이도 필터 UI(설정 화면의 "N5만 보기" 같은 드롭다운)는 Phase 4에서 추가하지만, 데이터는 처음부터 있으므로 마이그레이션 비용 없음.

## 디자인 방향
- **알림 UI**: 문제(일본어 문자/단어/문장)를 큰 글씨로 표시 + OX 또는 선택지 액션 버튼. 조용한 알림(소리·진동 없음), 낮은 우선순위 채널. 응답 후 알림 텍스트가 "정답입니다 / 정답은 OO입니다"로 갱신.
- **앱 UI**: 화면 최소화 유지 (퀴즈 화면 / 결과·오답노트 화면 / 설정 화면).
  - Material 3 단일 포인트 컬러(예: 인디고) + 시스템 다크모드 자동 대응.
  - 일본어 가독성을 위해 Noto Sans JP(또는 시스템 기본 CJK 폰트) 사용.
  - 정답/오답 시 색상 피드백(초록/빨강) 외 애니메이션·이미지는 배제(리소스 절약).

## 실기기 개발 환경 (에뮬레이터 없이 USB로 즉시 확인)

이 앱은 잠금화면 알림·배터리 최적화·OEM별 알림 정책이 핵심이라 **에뮬레이터보다 실기기 테스트가 사실상 필수**(에뮬레이터는 잠금화면 동작이나 제조사 배터리 제한을 재현하지 못함). 지금 기술스택(Android Studio + Kotlin/Compose)에서 실기기를 USB로 연결하면 코드 수정 후 몇 초 안에 폰에서 바로 확인 가능.

**1) 폰에서 개발자 옵션 활성화**
- 설정 → 휴대전화 정보 → "빌드 번호"를 7번 연속 탭 → "개발자 옵션" 메뉴 생성됨.
- 설정 → 개발자 옵션 → **USB 디버깅** 켜기.

**2) USB로 연결**
- 폰-PC를 USB 케이블(데이터 전송 가능한 케이블)로 연결.
- 폰에 "USB 디버깅을 허용하시겠습니까?" 팝업이 뜨면 **이 컴퓨터에서 항상 허용** 체크 후 허용.
- 처음 연결 시 Windows가 드라이버를 못 잡으면, 삼성은 "Samsung USB Driver", 그 외는 Android Studio에 포함된 Google USB Driver(SDK Manager → SDK Tools에서 설치)로 해결됨.

**3) Android Studio에서 실행**
- 상단 실행 대상 드롭다운에 연결된 기기명이 뜨면 선택 → ▶(Run) 클릭 시 디버그 APK가 실기기에 바로 설치·실행됨(에뮬레이터 부팅 대기 없음).
- 코드/리소스만 바뀐 경우 **Apply Changes**(⚡ 아이콘)로 앱 재시작 없이 즉시 반영 가능, Compose UI는 **Live Edit**으로 더 빠르게 반영 가능.
- 하단 **Logcat** 탭에서 실시간 로그 확인 → 알림/퀴즈 채점 로직 디버깅에 사용.

**4) (선택) 이후 무선 디버깅으로 전환**
- Android 11+ 폰은 최초 1회만 USB로 페어링하면, 이후 설정 → 개발자 옵션 → 무선 디버깅으로 케이블 없이도 같은 방식으로 실행 가능(잠금화면 테스트 시 케이블에 안 걸리적거려서 편함).

## Phase 0 — 준비
- [x] 프로젝트 목표/범위 문서화 (본 TASK.md)
- [ ] 실기기 USB 디버깅 연결 환경 구성 (아래 "실기기 개발 환경" 참고) — **사용자 작업 필요**(물리 기기에서 개발자 옵션 활성화 + USB 연결)
- [x] Android Studio + Kotlin 프로젝트 생성 (minSdk 26, targetSdk 34) — `app/build.gradle.kts`, Compose+Room+WorkManager+DataStore 의존성까지 추가됨
- [x] Git 저장소 초기화 및 `.gitignore` 설정
- [x] 앱 아이콘/이름 가(假) 확정 — 앱 이름 "NiBen", 기본 launcher 아이콘 적용
- [x] KANJIDIC2 / JMdict / Tatoeba 라이선스 및 사용 조건 확인 — 아래 "콘텐츠 라이선스 확인 결과" 참고, 모두 사용 가능(출처 표기 조건부)
- [x] `content_item` / `quiz_log` DB 스키마 설계 (난이도 필드 포함) — `app/src/main/java/com/niben/app/data/`에 Room Entity(`ContentItem`, `QuizLog`)·DAO(`ContentDao`, `QuizLogDao`)·`NibenDatabase` 구현, `assembleDebug` 빌드 성공 확인, 스키마 export를 `app/schemas/`에 설정
- [ ] 알림 노출 + 액션 버튼 응답 방식 실기기 검증 (잠금화면에서 OX 버튼 클릭이 실제로 동작하는지) — 검증용 최소 프로토타입 코드는 구현 완료(`com.niben.app.notification` 패키지: `QuizNotifier`, `QuizActionReceiver`, MainActivity의 "잠금화면 OX 퀴즈 테스트 알림 보내기" 버튼). **실기기에서 직접 눌러서 확인하는 것은 사용자 작업 필요**(에뮬레이터는 잠금화면 동작을 재현 못 함)

### 콘텐츠 라이선스 확인 결과
| 데이터셋 | 라이선스 | 사용 조건 |
|---|---|---|
| [KANJIDIC2](https://www.edrdg.org/edrdg/licence.html) | CC BY-SA 4.0 (저작권자: EDRDG) | 소프트웨어/서버 등에 출처 명시 필요. 수정본 배포 시에도 동일 라이선스(SA) 유지 |
| [JMdict](https://www.edrdg.org/edrdg/licence.html) | CC BY-SA 4.0 (저작권자: EDRDG) | 상당량 발췌 사용 시 문서/공지/웹사이트 등에 출처 명시. 앱 패키징상 라이선스 파일 포함이 어려우면(iOS 앱 등) 링크 제공으로 대체 가능 → NiBen도 앱 내 "정보/라이선스" 화면에 출처 링크만 넣으면 충분 |
| [Tatoeba](https://tatoeba.org/en/terms_of_use) | 문장별 상이 — 기본은 CC BY 2.0 FR, 일부는 CC0 | 문장 단위로 라이선스가 붙어 있으므로 예문 반입 시 각 문장의 라이선스/저자 정보도 함께 저장해 출처 표기 의무를 지킬 것(`content_item.source` 필드에 기록) — 오디오는 라이선스가 별도이므로 사용 안 함 |

→ 셋 다 개인용 오프라인 앱에서 사용 가능. 다만 CC BY-SA/CC BY 계열이라 **앱 내 "정보" 화면에 출처·라이선스 문구를 넣는 작업이 필요**(대량 반입 시 함께 처리). ※ Phase 3에서 실제로는 이 데이터셋들을 사용하지 않기로 방향 전환함(사유는 Phase 3 섹션 참고) — 현재는 출처 표기 의무 없음.

## Phase 1 — MVP (OX 퀴즈로 잠금화면 학습)
- [x] 히라가나/가타가나 전체 테이블 하드코딩 — `data/KanaTable.kt`, 청음46+탁음20+반탁음5+요음33=104쌍을 히라가나/가타카나 양쪽 `ContentItem`으로 생성(총 208개)
- [x] 기초 단어 30~50개 수기 JSON 작성 (품사/뜻/레벨 포함) — `assets/vocab_seed.json`, N5 기초 단어 42개(레벨 1)
- [x] Room DB 구성 및 초기 데이터 시딩 (`content_item`, `quiz_log`) — `data/ContentSeeder.kt`가 앱 최초 실행 시(`NibenApplication.onCreate`) 가나 표 + 단어 JSON을 `content_item`에 삽입(이미 데이터가 있으면 스킵)
- [x] 알림 채널 생성 (낮은 우선순위, 무음) — `notification/QuizNotifier.createChannel` (Phase 0에서 구현, 유지)
- [x] OX 퀴즈 알림 표시 구현 (문제 노출 + O/X 액션 버튼) — `quiz/QuizGenerator.generateOx`가 DB에서 무작위 문항을 뽑아 정답/오답 쌍을 만들고, `QuizNotifier.showNextQuiz`가 실제 콘텐츠로 알림 표시(기존 하드코딩 테스트 문제 대체)
- [x] `BroadcastReceiver`로 액션 클릭 처리 → 정답 판정 → `quiz_log` 기록 → 알림 텍스트 갱신 — `QuizActionReceiver`가 `goAsync()`로 코루틴에서 `QuizLogDao.insert()` 호출해 실제 DB에 기록, 알림 텍스트도 정답/오답으로 갱신
- [x] `POST_NOTIFICATIONS` 런타임 권한 요청 플로우 (Android 13+) — `MainActivity`가 앱 실행 시 `LaunchedEffect`로 자동 권한 요청(거부해도 앱은 계속 사용 가능)
- [ ] 잠금화면에서 실제로 퀴즈 응답이 되는지 실기기 테스트 — **사용자 작업 필요**(물리 기기에서 화면 잠근 뒤 알림의 O/X 버튼을 눌러 알림 텍스트가 갱신되는지, 정답 기록이 쌓이는지 확인)

## Phase 2 — 자동 갱신 + 앱 내 퀴즈 화면
- [x] WorkManager로 주기적 문제 교체 작업 구현 (예: N시간마다) — `work/QuizRefreshWorker`(`CoroutineWorker`)가 3시간마다 실행되어 65% 확률로 OX 알림, 35% 확률로 3/4지선다 안내 알림을 새로 띄움. `work/WorkScheduler.scheduleQuizRefresh`를 `NibenApplication.onCreate`에서 `ExistingPeriodicWorkPolicy.KEEP`으로 등록
- [x] 문제 출제 로직 (순차/랜덤, 최근 출제 항목 제외) — `data/RecentItemsStore`(DataStore)가 최근 출제된 항목 id 최대 15개를 보관, `ContentDao.getRandomItemExcludingIds`로 무작위 선택 시 제외. OX/선택형 생성 모두 이 목록을 사용하도록 `QuizGenerator` 수정(별도 "순차" 모드는 무작위+최근제외로 충분히 대체된다고 판단해 만들지 않음)
- [x] 기기 재부팅 후에도 스케줄 유지 (`BOOT_COMPLETED` 대응) — WorkManager 라이브러리 자체에 병합되는 `RescheduleReceiver`(`BOOT_COMPLETED` 수신)가 예약된 주기 작업을 자동 재등록하므로 앱에서 별도 리시버를 만들 필요 없음. 앱 프로세스가 뜰 때마다(`NibenApplication.onCreate`) `KEEP` 정책으로 재호출해도 중복 생성되지 않아 보강됨
- [x] 앱 내 퀴즈 화면 구현: 3지선다/4지선다 (알림 탭 시 진입) — `ui/QuizScreen.kt` 신설. `QuizGenerator.generateMultipleChoice`가 같은 카테고리에서 오답 보기를 뽑아 3/4지선다 문제 생성. `QuizNotifier.showMultipleChoicePrompt`가 액션 버튼 없이 `contentIntent`만 있는 알림을 띄우고, 탭하면 `MainActivity`가 인텐트 extra(`EXTRA_OPEN_QUIZ`)를 읽어 `QuizScreen`으로 진입(`launchMode="singleTop"` + `onNewIntent`로 앱이 이미 떠 있어도 처리)
- [x] 결과 화면 (정답/오답 즉시 피드백) — 별도 화면 대신 `QuizScreen` 내에서 선택 즉시 정답 보기는 초록, 잘못 고른 보기는 빨강으로 표시하고 "정답입니다/오답입니다" 텍스트 노출(디자인 방향의 "애니메이션·이미지 배제, 색상 피드백만" 원칙을 그대로 따름), `quiz_log`에도 기록
- [ ] 배터리 소모 확인 (개발자 옵션의 배터리 사용량 점검) — **사용자 작업 필요**(실기기에서 며칠 사용 후 설정 → 배터리 사용량에서 NiBen 확인)

## Phase 3 — 콘텐츠 확장

> **방향 전환(2026-08-24)**: 당초 계획이던 KANJIDIC2/JMdict/Tatoeba 대량 파싱은 보류. 이유는 아래 "Phase 3 방향 전환 사유" 참고. 대신 Phase 1의 `vocab_seed.json`과 동일한 "수기 작성 → assets JSON 번들" 방식을 한자·문장까지 확장.

- [x] N5 수준 한자 126자 수기 큐레이션(음독/훈독/뜻 포함) → `assets/kanji_seed.json`
- [x] 여행/생활 회화 기초 단어 109개 수기 추가(인사·숫자·시간·교통·숙소·식당·쇼핑·길찾기·긴급상황 등) → 기존 `assets/vocab_seed.json`에 병합(총 151개)
- [x] 여행 회화 문장 151개 수기 작성(인사/공항/교통/숙소/식당/쇼핑/길찾기/긴급상황/스몰토크) → `assets/sentence_seed.json`
- [x] `ContentSeeder`가 vocab/kanji/sentence 3개 자산을 모두 Room에 시딩하도록 확장(`loadCategoryFromAssets` 공통화)
- [x] 카테고리별(히라가나/가타가나/한자/단어/문장) 문제 출제 비율 조정 기능 — `data/CategoryRatioStore.kt`(DataStore, 카테고리별 가중치 0~100, 기본값 20)와 `ContentDao.getRandomItemInCategory(ExcludingIds)`로 가중치 기반 카테고리 선택 후 항목을 뽑도록 `QuizGenerator.pickItem` 수정, `ui/CategoryRatioScreen.kt`(±5 스텝퍼)로 MainActivity에서 조절 가능

### Phase 3 방향 전환 사유
- **JMdict에는 한국어 뜻이 없음**: 영어(및 일부 독일어/프랑스어 등) 대역만 제공되고 한국어 gloss가 없어 `content_item.meaning_ko`를 바로 채울 수 없음. 기계번역을 끼우면 수천 건 품질 검증이 필요해 1인 개발 리소스에 비해 부담이 큼.
- **KANJIDIC2는 공식 JLPT 급수 필드를 더 이상 제공하지 않음**(2010년 JLPT 개편 이후 폐지). 외부 급수 매핑 데이터를 추가로 붙여야 함.
- **개인용 오프라인 앱 특성상 과한 파이프라인**: 위 두 문제를 해결하려는 대규모 XML 파싱·필터링·번역 스크립트보다, Phase 1에서 이미 검증된 "수기 작성" 방식이 품질(자연스러운 번역)과 개발 공수, 앱 용량(리소스 최소화 원칙) 모두에서 더 유리하다고 판단.
- Tatoeba도 같은 이유로 대량 반입 대신 여행 회화 목적에 맞는 문장을 직접 작성하는 방식으로 대체.
- KANJIDIC2/JMdict/Tatoeba 라이선스 확인 결과(Phase 0)는 향후 대량 반입을 다시 고려할 경우를 위해 문서에 남겨둠(현재는 사용하지 않음).

## Phase 4 — 난이도 설정 & 적응형 학습
- [x] 설정 화면에 난이도 필터 UI 추가 (예: N5만 / N5~N3 등) — `ui/LevelFilterScreen.kt` 추가 및 `MainActivity` 연동 완료
- [x] 난이도 선택값을 문제 출제 로직에 반영 — `LevelFilterStore` 및 `ContentDao.getItemsWithLogStatus`로 쿼리 필터 적용 완료
- [x] `quiz_log` 기반 오답노트 화면 (자주 틀리는 항목 모아보기) — `ui/IncorrectNoteScreen.kt` 추가 및 `QuizLogDao.getIncorrectItems` 쿼리, 외웠음 삭제 기능 구현 완료
- [x] (선택) 정답률 기반 자동 난이도 조정 — SRS 가중치 알고리즘을 도입해 누적 오답 횟수 및 정답률 기반 가중치 부여 방식으로 세부 조정 완료
- [x] 간격 반복(SRS) 알고리즘 도입 → 출제 우선순위에 반영 — `QuizGenerator.pickItem`에 최근 풀이 여부, 오답 경과 시각(2분~24시간), 정답 경과 시각(24시간 이내 감쇠)을 연동한 룰렛 휠 가중 출제 로직 적용 완료

## Phase 5 — 학습 강화 / 편의 기능
- [ ] 뜻 입력형 주관식 퀴즈 (앱 내)
- [ ] 예문 완성형 퀴즈
- [ ] TTS 발음 재생
- [ ] 학습 통계 (누적 문제 수, 연속 학습일, 카테고리별 정답률)
- [ ] 사용자 단어 직접 추가/수정/삭제
- [ ] 즐겨찾기

## Phase 6 — 선택 기능
- [ ] 홈 화면 진짜 위젯(App Widget) 추가 검토 — 필요 시에만
- [ ] 백업/복원 (로컬 파일 export/import, 서버 없이)

## Phase 7 — 마무리
- [ ] 다양한 제조사(삼성/샤오미 등) 배터리 최적화 예외로 인한 알림 미갱신 이슈 점검
- [ ] APK 서명 및 개인 설치용 빌드
- [ ] 릴리즈 노트 간단 기록 (버전별 변경사항)
- [ ] 회고 및 다음 개선 아이디어 정리

## 진행 상황 기록
- 2026-08-08: 기획서 초안 작성. 구현 방식(상시 알림) 및 기술스택 확정.
- 2026-08-08: 퀴즈 기반 학습 구조로 개편, 난이도 설정 확장성 반영, 콘텐츠(히라가나/가타가나/한자/단어/문장) 확보 방법 명시.
- 2026-08-11: Android Studio 프로젝트 뼈대 생성(`feat: build gradlew` 커밋). `com.niben.app` 패키지, minSdk 26/targetSdk 34, Compose(Material3) + Room + WorkManager + DataStore 의존성 세팅 완료. `MainActivity`는 아직 "こんにちは" placeholder 화면만 있음(퀴즈 로직·DB 엔티티·알림·권한 플로우 전부 미구현). Phase 1 착수 전 단계.
- 2026-08-19: Phase 0 나머지 항목 진행. KANJIDIC2/JMdict/Tatoeba 라이선스 확인(모두 사용 가능, 출처 표기 조건). `content_item`/`quiz_log` Room 스키마(Entity+DAO+Database) 구현 및 빌드 검증. 잠금화면 알림 OX 액션 응답 검증용 최소 프로토타입(`QuizNotifier`/`QuizActionReceiver` + MainActivity 테스트 버튼) 구현, `assembleDebug` 빌드 성공 확인. 로컬 `local.properties`의 `sdk.dir`을 현재 PC 경로로 수정. 남은 두 항목(실기기 USB 연결, 잠금화면 실기기 검증)은 물리 기기가 있어야 하는 사용자 작업.
- 2026-08-21: Phase 1 진행. 히라가나/가타카나 전체 104쌍(청음+탁음+반탁음+요음) 하드코딩(`data/KanaTable.kt`), N5 기초 단어 42개 JSON 작성(`assets/vocab_seed.json`). `ContentSeeder`로 앱 최초 실행 시 Room에 초기 데이터 시딩(`NibenApplication` 신설). `QuizGenerator`로 DB 기반 무작위 OX 문제 생성(정답/오답 쌍을 다른 항목과 섞어 오답 보기도 만듦) 구현, `QuizNotifier`/`QuizActionReceiver`를 하드코딩 테스트 문제 대신 실제 콘텐츠와 연결하고 응답 시 `quiz_log`에 실제로 기록되도록 변경. `POST_NOTIFICATIONS` 권한을 앱 실행 시 자동 요청하도록 `MainActivity` 수정. `assembleDebug` 빌드 성공 확인. 남은 항목(잠금화면 실기기 검증)은 물리 기기 사용자 작업.
- 2026-08-24: Phase 3 진행. 여행 회화 단어/문장 및 N5 한자 수기 번들링 방식 전환 완료. 카테고리별 가중치 조절 DataStore와 UI Screen(CategoryRatioScreen) 연동 및 QuizGenerator 반영 완료.
- 2026-09-01: Phase 4 진행. `LevelFilterStore`(Preferences DataStore)로 1~5레벨 난이도 온/오프 상태 관리를 신설하고, `ContentDao`에 `LEFT JOIN` 쿼리를 추가해 퀴즈 로그 통계와 결합된 DTO(`ItemWithLogStatus`)를 조회하도록 확장. `QuizGenerator.pickItem`에 룰렛 휠 선택 알고리즘을 도입하고, 미학습 우선(+15), 최근 오답 즉시 복습(+30), 오답 후 2분~24시간 사이 복습 가중(+20), 24시간 이내 정답 가중치 감쇠(2.0), 누적 오답 횟수 가중(+5.0 * 횟수)을 조합한 간격 반복(SRS) 및 오답 집중 학습 로직 구현. UI에서는 JLPT 난이도 다중 선택 화면(`LevelFilterScreen.kt`)과 자주 틀린 단어를 모아보고 "외웠음" 버튼으로 오답 이력을 초기화할 수 있는 오답노트 화면(`IncorrectNoteScreen.kt`)을 추가하고, `MainActivity`에 해당 화면들로의 네비게이션 버튼을 연동함.
