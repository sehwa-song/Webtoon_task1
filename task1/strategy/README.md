# Task1 Strategy 패턴: RowClassifier 교체

이 버전에서 교체되는 전략은 컷 분할 전체가 아니라
`한 y행을 어떤 방식으로 분류할 것인가`이다.

## 패턴 역할

| 클래스 | 역할 |
|---|---|
| `RowClassifier` | Strategy 인터페이스 |
| `FixedColorRowClassifier` | 흰색·검은색을 검사하는 Concrete Strategy |
| `EdgeColorRowClassifier` | 좌우 예상 배경색을 검사하는 Concrete Strategy |
| `HybridRowClassifier` | Fixed 실패 시 Edge를 사용하는 Concrete Strategy |
| `StrategySplitter` | 선택된 RowClassifier를 사용하는 Context |
| `FixedColorStrategyApp` | Fixed 전략을 선택하는 Client |
| `EdgeColorStrategyApp` | Edge 전략을 선택하는 Client |
| `HybridStrategyApp` | Hybrid 전략을 선택하는 Client |
| `StrategyAppRunner` | 파일 수집·로드·저장·시간 출력 보조 코드 |

```text
Client
  └─ new FixedColorRowClassifier()
     또는 new EdgeColorRowClassifier()
     또는 new HybridRowClassifier()
            ↓ 생성자 주입
       StrategySplitter(Context)
            ↓
       rowClassifier.classify(image, y)
```

`StrategySplitter`는 이미지 순회, 연속 여백 추적, 최소 높이 확인,
안전한 cutY 선택이라는 공통 흐름을 담당한다. 각 `RowClassifier`는
현재 행이 그림인지 무채색 여백인지 컬러 여백인지만 다르게 판정한다.

## 전략별 의미

### FixedColorRowClassifier

- 거의 흰색 또는 거의 검은색인 픽셀 수만 계산한다.
- 가장 단순하고 빠르지만 컬러·그라데이션 여백을 놓칠 수 있다.

### EdgeColorRowClassifier

- 좌우 끝 5%에서 예상 배경색을 구한다.
- 행 전체 픽셀이 예상 배경색과 얼마나 비슷한지 계산한다.
- 회색 그라데이션과 분홍색 같은 컬러 여백을 처리할 수 있다.

### HybridRowClassifier

- Fixed 판정을 먼저 실행한다.
- Fixed로 여백을 찾지 못했을 때 Edge 판정을 실행한다.
- 안전한 cutY 점수에는 두 판정 중 더 작은 전경 픽셀 수를 사용한다.
- 현재 Task1의 기본 전략이다.

## 컴파일

```powershell
$sources = Get-ChildItem .\task1\strategy\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-strategy | Out-Null
javac -encoding UTF-8 -d .\build-task1-strategy $sources
```

## 전략별 전체 이미지 실행

```powershell
java -cp .\build-task1-strategy task1.strategy.FixedColorStrategyApp
java -cp .\build-task1-strategy task1.strategy.EdgeColorStrategyApp
java -cp .\build-task1-strategy task1.strategy.HybridStrategyApp
```

기본 출력 폴더:

- `output-task1-patterns/strategy/fixed`
- `output-task1-patterns/strategy/edge`
- `output-task1-patterns/strategy/hybrid`

저장하지 않고 컷 탐색 시간만 확인하려면 `--dry-run`을 사용한다.

```powershell
java -cp .\build-task1-strategy task1.strategy.HybridStrategyApp --dry-run
```

## 대표 결과

`images/003/002.jpg`에서:

| 전략 | 결과 | 의미 |
|---|---:|---|
| Fixed | 5컷 | 컬러·그라데이션 여백 하나를 놓침 |
| Edge | 6컷 | 좌우 배경색으로 해당 여백 탐지 |
| Hybrid | 6컷 | 최신 베이스라인 결과와 동일 |

따라서 세 실행시간은 Strategy 패턴 자체의 비용이 아니라 서로 다른
행 분류 알고리즘의 계산량 차이도 함께 포함한다.
