# Task2: 실제 콘텐츠만 추출하기

Task1은 긴 웹툰을 **여백의 가운데 좌표에서 나누는 작업**이다. 반면 Task2는
버려도 안전한 여백의 범위를 찾은 뒤 그 범위를 출력에서 제외하고,
실제 그림·말풍선·글자가 있는 콘텐츠 구간만 각각의 JPEG로 저장한다.

현재 구현은 원본 가로 폭은 유지하고 **세로 방향의 웹툰 여백만 제거**한다.
좌우의 흰 공간까지 잘라 내는 사각형 바운딩 박스 추출은 별도의 기능이다.

## 폴더 구성

| 구현 | 실행 클래스 | 적용 패턴 | 알고리즘이 들어 있는 파일 |
|---|---|---|---|
| Baseline | `task2.baseline.Task2Baseline` | 디자인 패턴 없이 한 흐름으로 구현 | `baseline/Task2Baseline.java` |
| Strategy | `task2.variants.strategy.StrategyExtractorApp` | Strategy | `strategy/MarginContentRegionStrategy.java` |
| Visitor | `task2.variants.visitor.VisitorExtractorApp` | Visitor + Composite | `visitor/VisitorContentAlgorithm.java` |
| Factory | `task2.variants.factory.FactoryExtractorApp` | Abstract Factory + Singleton LRU Cache | `factory/RgbMarginContentRegionDetector.java` |

각 폴더는 다른 구현의 클래스를 import하거나 호출하지 않는다. 같은 계산 코드가
반복되는 것은 실수라기보다, 각 디자인 패턴을 **그 폴더만으로 컴파일·실행**하고
나중에 같은 알고리즘을 통제 변수로 성능 비교하기 위한 구성이다.

## Task2 공통 알고리즘

한 행 `y`를 위에서 아래로 분석하며 다음 순서로 처리한다.

1. 행의 좌우 끝에서 가장 자주 나타나는 RGB 색을 찾아 예상 배경색을 만든다.
2. 행 전체 픽셀 중 예상 배경색과 비슷한 픽셀 비율을 계산한다.
3. 배경 비율과 색 분산을 이용해 `strongBlank`, `weakBlank`를 판정한다.
4. 말풍선·작은 글자·선처럼 보이는 행은 `protectedContent`로 보호한다.
5. 720px 폭 기준 48행 이상인 여백을 찾고, 위아래 80행 안에 실제 콘텐츠가
   있는 후보만 분할 대상으로 삼는다. 이미지 폭이 다르면 이 값도 비례 조정된다.
6. 후보 안에서 보호 콘텐츠와 겹치지 않는 가장 넓은 안전 여백을 고른다.
   가로 방향으로 거의 균일한 단색·그라데이션 캔버스는 제거할 수 있지만,
   인물·구름·글자처럼 행 안에 구조가 있는 그림은 콘텐츠로 남긴다.
7. 삭제 경계 전후 24행을 픽셀로 다시 검사해 말풍선 외곽선과 효과선이 잘리지
   않도록 경계를 물린다. 짧은 여백을 관통하는 연결선이 있으면 분할 자체를 취소한다.
8. 제거 구간의 여집합을 `ContentRegion(top, bottomExclusive)` 목록으로 만든다.
9. 각 콘텐츠 구간을 `원본이름_content_번호.jpg`로 저장한다.

예를 들어 높이가 1,000인 이미지에서 버릴 여백이 `[300, 500)`이면 결과는
`[0, 300)`, `[500, 1000)` 두 콘텐츠 구간이다. Task1처럼 `400`이라는 한 개의
컷 좌표만 만드는 것이 아니라, Task2는 **남길 범위 자체**를 계산한다.

실제 콘텐츠를 삭제하는 실수가 남는 여백보다 위험하므로 판정은 보수적이다.
말풍선이 여백에 조금 걸친 경우 해당 행을 통째로 버리지 않고, 그 주변의 정말
안전한 연속 여백만 제거한다.

## 1. Baseline 실행

프로젝트 루트의 PowerShell에서 실행한다.

```powershell
New-Item -ItemType Directory -Force .build\task2-baseline | Out-Null
$sources = (Get-ChildItem task2\baseline\*.java).FullName
javac -encoding UTF-8 -d .build\task2-baseline $sources
```

전체 이미지를 좌표만 확인하려면:

```powershell
java -cp .build\task2-baseline task2.baseline.Task2Baseline --dry-run images
```

전체 콘텐츠 조각을 저장하려면:

```powershell
java -cp .build\task2-baseline task2.baseline.Task2Baseline --output=output-task2-baseline images
```

## 2. Strategy 실행

```powershell
New-Item -ItemType Directory -Force .build\task2-strategy | Out-Null
$sources = (Get-ChildItem task2\variants\strategy\*.java).FullName
javac -encoding UTF-8 -d .build\task2-strategy $sources
java -cp .build\task2-strategy task2.variants.strategy.StrategyExtractorApp --dry-run images
```

연결 관계는 다음과 같다.

```text
StrategyExtractorApp (Client)
  -> StrategyExtractor (Context)
       -> ContentRegionStrategy (Strategy 인터페이스)
            -> MarginContentRegionStrategy (Concrete Strategy)
```

`StrategyExtractor`는 픽셀 계산 방법을 알지 못하고 인터페이스의
`findContentRegions(image)`만 호출한다. 따라서 나중에 다른 추출 알고리즘을
추가할 때 새로운 Strategy 클래스를 만들어 주입할 수 있다.

## 3. Visitor + Composite 실행

```powershell
New-Item -ItemType Directory -Force .build\task2-visitor | Out-Null
$sources = (Get-ChildItem task2\variants\visitor\*.java).FullName
javac -encoding UTF-8 -d .build\task2-visitor $sources
java -cp .build\task2-visitor task2.variants.visitor.VisitorExtractorApp --dry-run images
```

연결 관계는 다음과 같다.

```text
WebtoonBatch (Composite)
  -> WebtoonEpisode (Composite)
       -> WebtoonPage (Leaf)
            -> accept(WebtoonVisitor)
                 -> ExtractPageVisitor.visitPage(page)
                      -> VisitorContentAlgorithm
```

Composite는 `Batch -> Episode -> Page`라는 웹툰 계층을 만들고, Visitor는
그 계층을 순회하며 각 Page에 콘텐츠 추출 작업을 추가한다. 페이지 클래스에
통계, 검증, 미리보기 같은 작업을 계속 넣지 않고 새로운 Visitor로 확장할 수 있다.

## 4. Abstract Factory 실행

표준 제품군은 이미지를 실행할 때마다 디스크에서 읽는다.

```powershell
New-Item -ItemType Directory -Force .build\task2-factory | Out-Null
$sources = (Get-ChildItem task2\variants\factory\*.java).FullName
javac -encoding UTF-8 -d .build\task2-factory $sources
java -cp .build\task2-factory task2.variants.factory.FactoryExtractorApp --dry-run images
```

캐시 제품군은 같은 경로를 다시 처리할 때 Singleton LRU 저장소에 보관된 이미지를
재사용한다.

```powershell
java -cp .build\task2-factory task2.variants.factory.FactoryExtractorApp --cached --dry-run images
```

연결 관계는 다음과 같다.

```text
ProcessingFamilyFactory (Abstract Factory)
  |-- StandardProcessingFactory
  |     |-- DiskImageRepository
  |     |-- RgbMarginContentRegionDetector
  |     `-- JpegRegionWriter
  |
  `-- CachedProcessingFactory
        |-- SingletonLruImageRepository  <- 실제 Singleton + LRU cache
        |-- RgbMarginContentRegionDetector
        `-- JpegRegionWriter

FactoryExtractorApp -> WebtoonExtractionService -> 위 세 제품 사용
```

여기서 Singleton으로 공유할 필요가 있는 핵심은 **읽은 이미지와 캐시 상태를 가진
`SingletonLruImageRepository`**이다. 검출기와 저장기는 상태가 없는 일반 객체로
만들어, 표준/캐시 팩토리의 차이가 이미지 로딩 정책이라는 점을 분명히 했다.

## 공정한 시간·메모리 비교 전 확인할 것

JMH로 성능을 측정하기 전에 동일한 모든 이미지에서 네 구현의
`[top,bottom)` 결과가 완전히 같은지 먼저 확인해야 한다. 좌표가 다르면 디자인
패턴의 비용이 아니라 서로 다른 알고리즘의 비용을 비교하게 된다.

성능 비교 시에는 다음 두 실험을 분리하는 것이 좋다.

- 검출 알고리즘 비교: 이미지를 미리 메모리에 읽고 `findContentRegions`만 측정
- 전체 작업 비교: 파일 읽기 + 검출 + JPEG 쓰기를 함께 측정

Factory의 캐시는 동일 경로를 반복 처리할 때 의미가 있다. 서로 다른 이미지를 한 번씩만
처리하면 캐시 이득은 거의 없고, 오히려 캐시가 차지하는 메모리만 늘어날 수 있다.

네 구현의 결과를 한 번에 검증하려면 다음 명령을 실행한다. `verification`만 모든
구현을 참조하는 테스트 코드이며, 실제 패턴 구현들의 독립성에는 영향을 주지 않는다.

```powershell
New-Item -ItemType Directory -Force .build\task2-all | Out-Null
$sources = (Get-ChildItem task2 -Recurse -Filter *.java).FullName
javac -encoding UTF-8 -d .build\task2-all $sources
java -cp .build\task2-all task2.verification.Task2EquivalenceCheck images
```

사용자가 확인한 여섯 개 오분할 사례, 이어진 말풍선의 과분할, 경계 그림선
잘림이 재발하지 않는지 검사하려면:

```powershell
java -cp .build\task2-all task2.verification.Task2RegressionCheck
```
