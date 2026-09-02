# Task1 디자인패턴별 독립 구현

> 패턴별 핵심 클래스 관계 그림: [웹툰 처리 디자인 패턴 관계도](../docs/design-pattern-diagrams.md)

`Task1LearningBaseline.java`의 웹툰 컷 분할 규칙을 바탕으로 다음 7개
디자인패턴을 각각 독립 패키지로 구현했다.

- Strategy
- Template Method
- Chain of Responsibility
- Decorator
- Proxy
- Abstract Factory
- Composite + Visitor

각 폴더는 다른 패턴 폴더나 `Task1LearningBaseline`을 import하거나
호출하지 않는다. 따라서 각 폴더의 `.java` 파일만 따로 컴파일하고
실행할 수 있다.

## 전체 구조

| 패턴 | 폴더 | 실행 클래스 | 기본 출력 폴더 |
|---|---|---|---|
| Strategy | `task1/strategy` | `task1.strategy.HybridStrategyApp` | `output-task1-patterns/strategy/hybrid` |
| Template Method | `task1/template` | `task1.template.TemplateSplitterApp` | `output-task1-patterns/template` |
| Chain of Responsibility | `task1/chain` | `task1.chain.ChainSplitterApp` | `output-task1-patterns/chain` |
| Decorator | `task1/decorator` | `task1.decorator.DecoratorSplitterApp` | `output-task1-patterns/decorator` |
| Proxy | `task1/proxy` | `task1.proxy.ProxySplitterApp` | `output-task1-patterns/proxy` |
| Abstract Factory | `task1/factory` | `task1.factory.FactorySplitterApp` | `output-task1-patterns/factory` |
| Composite + Visitor | `task1/visitor` | `task1.visitor.VisitorSplitterApp` | `output-task1-patterns/visitor` |

입력 경로를 생략하면 모든 프로그램이 `images` 폴더의 72개 이미지를
파일 이름순으로 처리한다.

## 각 패턴에서 실제로 달라지는 구조

### 1. Strategy

```text
Client가 행 분류 전략 중 하나 선택
    ├─ FixedColorRowClassifier
    ├─ EdgeColorRowClassifier
    └─ HybridRowClassifier
            ↓ 생성자 주입
       StrategySplitter(Context)
            ↓
       rowClassifier.classify(image, y)
```

- `RowClassifier`: 교체 가능한 행 분류 알고리즘 인터페이스
- `FixedColorRowClassifier`: 흰색·검은색 기반 전략
- `EdgeColorRowClassifier`: 좌우 예상 배경색 기반 전략
- `HybridRowClassifier`: Fixed 실패 시 Edge를 사용하는 기본 전략
- `StrategySplitter`: 공통 컷 탐색 중 행 판정만 Strategy에 위임하는 Context

### 2. Template Method

```text
AbstractWebtoonSplitter.split() final
    1. loadImage()
    2. detectCutPoints()  ← 구체 클래스가 구현하는 훅
    3. writePieces()
```

- `AbstractWebtoonSplitter`: 전체 처리 순서를 고정
- `RgbMarginTemplateSplitter`: RGB 컷 탐색 훅 구현

### 3. Chain of Responsibility

```text
여백 후보
    → MinimumHeightHandler
    → ColoredStabilityHandler
    → SafeCutSelectionHandler
    → 최종 cutY
```

- 각 Handler는 자기 조건만 검사한다.
- 실패하면 연쇄를 즉시 중단하고, 성공하면 다음 Handler로 전달한다.

### 4. Decorator

```text
TimingCutPointDetectorDecorator
    └─ CutPointValidationDecorator
        └─ RgbMarginCutPointDetector
```

- 원본 RGB 탐색기를 수정하지 않고 검증과 시간 측정 기능을 감싼다.
- 모든 객체가 같은 `CutPointDetector` 인터페이스로 사용된다.

### 5. Proxy

```text
ProxySplittingService
    → ImageRepository
        → CachingImageRepositoryProxy(대리자)
            → DiskImageRepository(원본 객체)
```

- 처음 읽는 이미지는 원본 객체가 디스크에서 읽는다.
- 같은 경로를 다시 읽으면 Proxy가 메모리 캐시에서 반환한다.
- 서비스는 실제 객체인지 Proxy인지 구분하지 않는다.

### 6. Abstract Factory

```text
ProcessingFamilyFactory
    ├─ StandardProcessingFactory
    └─ CachedProcessingFactory

각 Factory가 함께 사용할 제품군 생성
    ImageRepository + CutPointDetector + PieceWriter
```

- 표준 팩토리는 디스크 Repository 제품을 만든다.
- 캐시 팩토리는 Singleton LRU Repository 제품을 만든다.
- 서비스 코드를 바꾸지 않고 제품군 전체를 교체할 수 있다.

### 7. Composite + Visitor

```text
WebtoonBatch(Composite)
    └─ WebtoonEpisode(Composite)
        └─ WebtoonPage(Leaf)

batch.accept(SplitPageVisitor)
    → episode 방문
        → 각 page 방문 및 컷 분할
```

- Composite가 `Batch → Episode → Page` 계층을 표현한다.
- Visitor가 계층을 순회하며 각 Page에 컷 분할 연산을 적용한다.

## 패턴별 독립 컴파일

프로젝트 최상위 폴더의 PowerShell에서 실행한다.

### Strategy

```powershell
$sources = Get-ChildItem .\task1\strategy\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-strategy | Out-Null
javac -encoding UTF-8 -d .\build-task1-strategy $sources
```

### Template Method

```powershell
$sources = Get-ChildItem .\task1\template\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-template | Out-Null
javac -encoding UTF-8 -d .\build-task1-template $sources
```

### Chain of Responsibility

```powershell
$sources = Get-ChildItem .\task1\chain\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-chain | Out-Null
javac -encoding UTF-8 -d .\build-task1-chain $sources
```

### Decorator

```powershell
$sources = Get-ChildItem .\task1\decorator\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-decorator | Out-Null
javac -encoding UTF-8 -d .\build-task1-decorator $sources
```

### Proxy

```powershell
$sources = Get-ChildItem .\task1\proxy\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-proxy | Out-Null
javac -encoding UTF-8 -d .\build-task1-proxy $sources
```

### Abstract Factory

```powershell
$sources = Get-ChildItem .\task1\factory\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-factory | Out-Null
javac -encoding UTF-8 -d .\build-task1-factory $sources
```

### Composite + Visitor

```powershell
$sources = Get-ChildItem .\task1\visitor\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-visitor | Out-Null
javac -encoding UTF-8 -d .\build-task1-visitor $sources
```

## 전체 이미지 실행

각 명령은 서로 독립적으로 실행된다.

```powershell
java -cp .\build-task1-strategy task1.strategy.HybridStrategyApp
java -cp .\build-task1-template task1.template.TemplateSplitterApp
java -cp .\build-task1-chain task1.chain.ChainSplitterApp
java -cp .\build-task1-decorator task1.decorator.DecoratorSplitterApp
java -cp .\build-task1-proxy task1.proxy.ProxySplitterApp
java -cp .\build-task1-factory task1.factory.FactorySplitterApp
java -cp .\build-task1-visitor task1.visitor.VisitorSplitterApp
```

PowerShell에서 한글이 깨질 때는 Java 출력 인코딩 옵션을 따옴표로
감싸서 실행한다.

```powershell
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp .\build-task1-visitor task1.visitor.VisitorSplitterApp --dry-run
```

## 저장 없이 시간 측정

각 명령에 `--dry-run`을 붙이면 JPEG 저장을 생략하고 로딩과 분석만
실행한다.

```powershell
java -cp .\build-task1-strategy task1.strategy.HybridStrategyApp --dry-run
java -cp .\build-task1-template task1.template.TemplateSplitterApp --dry-run
java -cp .\build-task1-chain task1.chain.ChainSplitterApp --dry-run
java -cp .\build-task1-decorator task1.decorator.DecoratorSplitterApp --dry-run
java -cp .\build-task1-proxy task1.proxy.ProxySplitterApp --dry-run
java -cp .\build-task1-factory task1.factory.FactorySplitterApp --dry-run
java -cp .\build-task1-visitor task1.visitor.VisitorSplitterApp --dry-run
```

특정 이미지만 실행하려면 마지막에 경로를 붙인다.

```powershell
java -cp .\build-task1-chain task1.chain.ChainSplitterApp --dry-run images\013\001.jpg
```

## 패턴 전용 옵션

Proxy 캐시 hit 확인:

```powershell
java -cp .\build-task1-proxy task1.proxy.ProxySplitterApp --dry-run --passes=2 images\003\002.jpg
```

Abstract Factory의 캐시 제품군 선택:

```powershell
java -cp .\build-task1-factory task1.factory.FactorySplitterApp --cached --dry-run images\003\002.jpg images\003\002.jpg
```

## 실행시간 출력

모든 App은 `System.nanoTime()`으로 다음 값을 출력한다.

- 이미지별 `load`: 이미지 로딩 시간
- 이미지별 `detect`: 컷 위치 탐색 시간
- 이미지별 `write`: 잘린 JPEG 저장 시간
- 이미지별 `total`: 해당 이미지 전체 처리 시간
- 마지막 `프로그램 전체`: 파일 탐색과 모든 이미지 처리 시간

정확한 알고리즘 비교에는 `--dry-run`의 누적 `detect`를 우선 사용한다.
실제 사용자가 기다리는 시간을 비교하려면 저장을 포함한 `프로그램 전체`를
사용한다. 첫 실행은 JIT 컴파일과 디스크 캐시의 영향을 받으므로 여러 번
실행해야 하며, 최종 보고서용 정밀 측정은 JMH로 수행한다.

## 회귀 검증 결과

| 이미지 | 기대 결과 | 7개 패턴 결과 |
|---|---:|---:|
| `images/003/002.jpg` | 6컷 | 모두 6컷 |
| `images/013/001.jpg` | 15컷 | 모두 15컷 |

`images/003/002.jpg`에 대해서는 모든 패턴의 실제 JPEG 저장 결과도
각각 6개씩 생성되는 것을 확인했다.
