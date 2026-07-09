# ⏰ 알람메모 (Alarm Memo)

> 메모, 진동 세기, 요일 반복, 스누즈, 커스텀 알람음까지 세세하게 설정할 수 있는 안드로이드 알람 앱. 잠금화면 위에서도 울리는 풀스크린 알람 화면을 갖춘, 삼성 기본 알람 스타일의 네이티브 Kotlin 앱입니다.

---

## 주요 기능

### 알람 메모
- 알람마다 짧은 텍스트 메모를 붙여 목록에서 바로 확인할 수 있습니다. (예: "아침 운동", "회의 준비")

### 반복 요일 설정
- 알람 추가/수정 화면에서 일~토 요일을 개별적으로 토글해 반복 요일을 지정합니다.
- 요일을 하나도 선택하지 않으면 **1회성 알람**이 되어, 울리고 해제한 뒤 자동으로 꺼집니다(다음 날 다시 울리지 않음).
- 요일을 선택하면 해당 요일마다 반복되며, `AlarmManager`가 다음 예정 요일/시각을 자동으로 계산해 재예약합니다.

### 진동
- 없음 / 약함 / 보통 / 강함 4단계로 세기와 패턴을 다르게 설정할 수 있습니다 (`VibrationEffect.createWaveform` 기반, API 26 미만은 레거시 패턴으로 폴백).

### 소리 (사운드 On/Off + 커스텀 곡 선택)
- "소리 사용" 스위치를 끄면 소리 없이 **진동만** 울리는 무음 알람이 됩니다.
- 시스템 파일 선택기(Storage Access Framework, `ACTION_OPEN_DOCUMENT`)로 기기에 저장된 아무 음악/오디오 파일이나 알람음으로 지정할 수 있습니다.
- 선택한 파일에 대한 읽기 권한을 영구 보존(`takePersistableUriPermission`)해 재부팅 후에도 재생 가능하며, 파일이 사라지는 등 재생에 실패하면 기기 기본 알람음으로 자동 폴백합니다.

### 스누즈
- 스누즈 간격(5/10/15/30분)과 최대 반복 횟수(1/2/3/5/10회)를 알람마다 개별 설정합니다.
- 반복 횟수를 모두 소진하면 스누즈 버튼이 사라지고, 무응답 시 설정된 시간(기본 2분) 후 자동으로 스누즈되거나 종료됩니다.

### 알람 울림 화면 (삼성 기본 알람 스타일)
- 알람 시각이 되면 화면이 꺼져 있어도 자동으로 켜지며, 잠금화면 위 **풀스크린**으로 알람 화면이 표시됩니다 (`Notification.Builder#setFullScreenIntent` + `Activity#setShowWhenLocked/setTurnScreenOn`).
- 다른 앱을 사용 중이어도 최상단에 표시되고, 뒤로가기로 임의 종료되지 않습니다.
- 해제는 **밀어서 알람 해제**(슬라이드) 방식이며, 스누즈 가능 여부에 따라 스누즈 버튼이 함께 표시됩니다.
- 알람 소리(또는 진동)는 해제 전까지 반복 재생됩니다.

### 알람 목록
- 보라/코랄 톤의 카드형 UI로 알람 시각, 메모, 반복 요일 요약, 진동 세기를 한눈에 보여주고, 스위치 하나로 켜고 끌 수 있습니다.
- 기기 재부팅 후에도 활성화된 알람은 `BootReceiver`가 전부 자동으로 재예약합니다.

---

## 화면 구성

| 화면 | 설명 |
|---|---|
| 알람 목록 (`AlarmListActivity`) | 등록된 알람을 카드 리스트로 표시, FAB로 추가, 스와이프/길게 눌러 삭제 |
| 알람 추가·수정 (`AlarmEditActivity`) | 시간, 반복 요일, 메모, 소리(On/Off + 곡 선택), 진동 세기, 스누즈 설정 |
| 알람 울림 (`AlarmRingActivity`) | 잠금화면 위 풀스크린, 그라데이션 배경, 밀어서 해제 / 스누즈 버튼 |

---

## 기술 스택

- **언어**: Kotlin
- **플랫폼**: Android (`minSdk 26`, `targetSdk`/`compileSdk 34`)
- **UI**: Android View + ViewBinding, Material Components (`MaterialCardView`, `SwitchMaterial`, `MaterialButton` 등), `ConstraintLayout`
- **비동기**: Kotlin Coroutines (`kotlinx-coroutines-android`), `lifecycleScope`
- **데이터 저장**: Room (`androidx.room`, KSP 컴파일러) — 알람 엔티티/DAO/Repository 계층 구조, `Flow` 기반 목록 관찰
- **알람 예약**: `AlarmManager.setAlarmClock` (Doze 모드에서도 정확한 시각에 발화, 상태바 알람 아이콘 표시)
- **알람 울림 처리**: 포그라운드 `Service`(`mediaPlayback` 타입) — `MediaPlayer` + `Vibrator`/`VibrationEffect`, `NotificationCompat` 풀스크린 인텐트
- **파일 선택**: Storage Access Framework (`ActivityResultContracts.OpenDocument`), 영구 URI 권한
- **아키텍처**: 단순 MVVM (Activity + `AndroidViewModel` + Repository), Room 싱글턴, 화면 간 통신은 `Intent extra` / `BroadcastReceiver` / `Service` 액션 기반

### 프로젝트 구조

```
Alarm_Memo/
├── app/
│   ├── build.gradle.kts           # 앱 모듈 빌드 설정, 서명(release) 설정
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/thkim0515/alarmmemo/
│           ├── AlarmMemoApp.kt            # Application, 알림 채널 생성
│           ├── data/                      # Room 엔티티 / DAO / DB / Repository
│           │   ├── Alarm.kt
│           │   ├── AlarmDao.kt
│           │   ├── AlarmDatabase.kt
│           │   ├── AlarmRepository.kt
│           │   └── VibrationLevel.kt
│           ├── alarm/                     # 스케줄링 & 알람 울림 로직
│           │   ├── AlarmScheduler.kt      # AlarmManager 예약/취소, 다음 발화 시각 계산
│           │   ├── AlarmReceiver.kt       # AlarmManager → 포그라운드 서비스 기동
│           │   ├── BootReceiver.kt        # 재부팅 시 알람 재예약
│           │   └── AlarmRingService.kt    # 사운드/진동 재생, 스누즈/해제 처리
│           ├── ui/
│           │   ├── list/                  # 알람 목록 화면 + 어댑터 + ViewModel
│           │   ├── edit/                  # 알람 추가/수정 화면
│           │   └── ring/                  # 풀스크린 알람 울림 화면
│           └── util/                      # 상수, 요일 포맷 유틸
│       └── res/                    # 레이아웃, 드로어블, 색상, 문자열 리소스
├── keystore/release.keystore       # CI에서 매번 동일한 서명으로 릴리즈 APK를 만들기 위한 고정 키스토어
├── .github/workflows/
│   ├── android-ci.yml              # push/PR마다 assembleDebug로 빌드 검증
│   └── release.yml                 # 태그(v*.*.*) push 또는 수동 실행 시 서명된 릴리즈 APK를 빌드해 GitHub Release에 첨부
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── README.md
```

---

## 데이터 모델

```kotlin
@Entity(tableName = "alarms")
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val memo: String = "",
    val vibrationLevel: Int = VibrationLevel.MEDIUM.value,   // 0 없음 · 1 약함 · 2 보통 · 3 강함
    val snoozeEnabled: Boolean = true,
    val snoozeIntervalMinutes: Int = 5,
    val snoozeMaxCount: Int = 3,
    val isEnabled: Boolean = true,
    val repeatDays: Int = 0,          // Calendar.SUNDAY..SATURDAY 비트마스크, 0이면 1회성 알람
    val soundEnabled: Boolean = true, // false면 진동만
    val soundUri: String? = null      // 사용자가 고른 곡의 content:// URI, null이면 기본 알람음
)
```

---

## 빌드 & 릴리즈

이 프로젝트는 Gradle(Kotlin DSL) + Android Gradle Plugin 기반의 표준 Android 프로젝트입니다.

```bash
./gradlew assembleDebug     # 디버그 APK 빌드
./gradlew assembleRelease   # 서명된 릴리즈 APK 빌드 (keystore/release.keystore 사용)
```

APK는 **GitHub Actions**에서 빌드/배포됩니다.

- **`Android CI`** (`.github/workflows/android-ci.yml`) — `main` 브랜치 push/PR마다 `assembleDebug`로 빌드가 깨지지 않는지 검증합니다.
- **`Release APK`** (`.github/workflows/release.yml`) — `v*.*.*` 형태의 태그를 push하거나 Actions 탭에서 수동 실행(`workflow_dispatch`)하면, 서명된 릴리즈 APK를 빌드해 **GitHub Release**에 첨부합니다.

최신 APK는 [Releases 페이지](https://github.com/thkim0515/Alarm_Memo/releases)에서 받을 수 있습니다.

---

## 권한

| 권한 | 용도 |
|---|---|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | 정확한 시각에 알람 발화 (Android 12+) |
| `POST_NOTIFICATIONS` | 알람 울림 알림 표시 (Android 13+) |
| `USE_FULL_SCREEN_INTENT` | 잠금화면 위 풀스크린 알람 화면 표시 |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 알람 재예약 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 알람 소리/진동을 안정적으로 재생하는 포그라운드 서비스 |
| `VIBRATE`, `WAKE_LOCK`, `DISABLE_KEYGUARD` | 진동, 화면 깨우기, 잠금화면 위 표시 |

앱 최초 실행 시 알림 권한을, 그리고 "알람 및 리마인더" 권한이 꺼져 있으면 목록 화면에서 설정으로 이동하는 안내가 표시됩니다.
