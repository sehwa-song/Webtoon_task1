# 웹툰 컷 분할기의 디자인 패턴 비교 분석

## 1. 비교 목적과 기준선

이 프로젝트는 **같은 웹툰 컷 검출 알고리즘**을 서로 다른 디자인 패턴으로 구성했을 때 실행 시간, 메모리 사용량, 교체 가능성, 수정 범위가 어떻게 달라지는지 비교한다.

비교 기준선은 `Task1Splitter.java`이다. 이 파일은 알고리즘 일부가 아니라 다음 기능을 모두 가진 완전한 독립 실행 구현이다.

1. 파일 또는 폴더에서 이미지를 재귀적으로 수집한다.
2. `ImageIO`로 이미지를 읽는다.
3. RGB-margin 알고리즘으로 컷 좌표를 찾는다.
4. `--dry-run`이면 좌표만 출력한다.
5. 일반 실행이면 여백의 중간 지점에서 잘라 JPG로 저장한다.
6. 이전 실행에서 남은 불필요한 조각 파일을 정리한다.

`Task1Baseline.java`는 초기에 밝기 차이만 사용하던 단순 구현을 보존한 파일이다. 디자인 패턴 성능 비교에서 말하는 **baseline은 `Task1Baseline`이 아니라 완성된 `Task1Splitter`**이다.

## 2. 가장 중요한 실험 원칙: 같은 알고리즘, 다른 구조

네 실험군은 서로를 호출하지 않는 독립 프로그램이다.

| 실험군 | 위치 | 패키지 | 역할 |
|---|---|---|---|
| Baseline | `Task1Splitter.java` | default package | 패턴을 분리하지 않은 완전 구현 |
| Strategy | `variants/strategy/` | `variants.strategy` | 컷 검출 방식을 Strategy로 교체 |
| Visitor + Composite | `variants/visitor/` | `variants.visitor` | Batch→Episode→Page 구조에 연산 추가 |
| Abstract Factory + Singleton | `variants/factory/` | `variants.factory` | 로더·검출기·저장기를 제품군으로 선택하고 이미지 캐시 사용 |

각 실험군은 RGB-margin 알고리즘과 helper를 자체적으로 가진다.

```text
Task1Splitter.java         → 자체 RGB-margin 알고리즘
variants/strategy/         → MarginCutPointStrategy
variants/visitor/          → VisitorCutAlgorithm
variants/factory/          → RgbMarginCutPointDetector
```

따라서 Strategy가 `Task1Splitter.findCutPoints()`를 호출하거나 Visitor가 Strategy 구현을 빌려 쓰지 않는다. 각 폴더는 자신의 소스만으로 컴파일하고 실행할 수 있다. named package에서는 default-package 클래스를 import할 수도 없으므로 패키지 분리는 독립성을 구조적으로 확인하는 데 도움이 된다.

### 소스 중복은 의도적인 실험 통제다

일반적인 제품이라면 알고리즘을 공통 라이브러리로 추출하는 편이 유지보수에 좋다. 그러나 이번 과제에서는 한 실험군이 다른 구현을 호출하면 공통 객체의 초기화, 캐시, JIT 상태가 측정에 섞이고 어느 패턴의 비용인지 경계가 불분명해진다.

이번 중복은 실수나 리팩터링 누락이 아니라 **알고리즘을 통제 변수로 고정하기 위한 의도적인 중복**이다. 보고서에는 이 점과 함께, 실제 서비스로 발전시킬 때는 공통 모듈화를 고려한다고 적으면 된다.

## 3. 공통 RGB-margin 컷 검출 알고리즘

알고리즘은 OpenCV 없이 순수 Java와 `BufferedImage`만 사용한다. 이미지의 모든 행 `y`를 위에서 아래로 검사한다.

1. 왼쪽과 오른쪽 약 5% 영역에서 대표 RGB 색을 구한다.
2. 양쪽 대표색이 비슷하고 가장자리 색 분산이 작은지 확인한다.
3. 행 전체에서 대표 배경색과 가까운 픽셀 비율을 계산한다.
4. 엄격한 기준을 만족하면 `strongBlank`로 표시한다.
5. 말풍선 꼬리나 작은 글씨 때문에 일부가 가려진 행은 `weakBlank`로 표시한다.
6. 짧은 방해 구간을 메우고 충분히 긴 strong 여백만 후보로 만든다.
7. weak 여백으로 후보 경계를 확장하고 짧은 방해물로 나뉜 여백을 합친다.
8. 여백 가운데에서 배경 비율이 높고 분산이 작은 안전한 행을 고른다.
9. 너무 가까운 후보는 품질이 더 높은 하나만 남긴다.

이 방식은 단순한 `최대 밝기 - 최소 밝기`보다 노을, 하늘, 색 그라데이션을 여백으로 잘못 판단할 가능성이 작다. 모든 실험군의 상수와 계산 순서가 같아야 컷 좌표도 같고 패턴 비교도 공정하다.

## 4. Baseline: `Task1Splitter`

```text
main
  → 입력 이미지 수집
  → ImageIO.read
  → findCutPoints
  → 필요하면 getSubimage + ImageIO.write
```

파일 수집, 알고리즘, 저장 로직이 한 파일에 모여 있다. 호출 단계와 객체 수가 가장 적으므로 다른 패턴의 구조적 비용을 비교하는 기준이 된다. 작은 프로그램에서는 흐름을 한눈에 보기 쉽지만, 알고리즘이나 로딩 방식을 교체할 때 큰 클래스의 여러 부분을 함께 수정하기 쉽다.

## 5. Strategy 실험군

| 클래스 | 패턴 역할 |
|---|---|
| `CutPointStrategy` | 알고리즘 공통 인터페이스 | (전략 (추상화된 알고리즘))
| `MarginCutPointStrategy` | RGB-margin 알고리즘을 가진 Concrete Strategy | (전략 알고리즘 객체들)
| `StrategySplitter` | Strategy를 주입받는 Context | (전략 등록/실행)
| `StrategySplitterApp` | 입력 수집, 로드, 저장을 담당하는 진입점 |

```text
StrategySplitterApp
  → BufferedImage 로드
  → StrategySplitter(Context)
  → CutPointStrategy 인터페이스
  → MarginCutPointStrategy(Concrete Strategy)
  → 컷 좌표
```

컷 검출 방식은 RGB-margin, 밝기 차이, 고정 간격, 외부 모델 등으로 바뀔 수 있다. Context는 인터페이스만 사용하므로 새 알고리즘을 추가해도 Context를 수정할 필요가 없다.

Strategy 호출은 이미지마다 한 번이고 실제 작업은 수백만 픽셀 검사이므로 인터페이스 호출 비용은 매우 작을 것으로 예상된다. Strategy의 주된 이득은 속도가 아니라 **교체 가능성, 단위 테스트, 역할 분리**이다.

## 6. Visitor + Composite 실험군

| 클래스 | 패턴 역할 |
|---|---|
| `WebtoonElement` | `accept`를 제공하는 요소 인터페이스 |
| `WebtoonBatch` | 여러 Episode를 담는 최상위 Composite |
| `WebtoonEpisode` | 여러 Page를 담는 중간 Composite |
| `WebtoonPage` | 이미지 한 장인 leaf |
| `WebtoonVisitor` | 요소 타입별 방문 메서드 인터페이스 |
| `SplitPageVisitor` | 컷 검출과 선택적 저장을 하는 Concrete Visitor |
| `VisitorCutAlgorithm` | 실험군 내부 RGB-margin 알고리즘 |
| `VisitorSplitterApp` | 계층 생성과 방문을 시작하는 진입점 |

```text
VisitorSplitterApp
  → WebtoonBatch.accept(visitor)
      → visitor.enterBatch(batch)
      → WebtoonEpisode.accept(visitor)
          → visitor.enterEpisode(episode)
          → WebtoonPage.accept(visitor)
              → visitor.visitPage(page)
                  → 이미지 로드
                  → VisitorCutAlgorithm 실행
                  → 필요하면 JPG 저장
          → visitor.leaveEpisode(episode)
      → visitor.leaveBatch(batch)
```

벤치마크 공개 API인 `findCutPoints`, `analyzeLoadedImages`, `analyzePaths`, `benchmarkLoadedImages`도 알고리즘을 바로 부르지 않는다. 모두 Batch→Episode→Page를 만들고 실제 `accept/visit` 경로로 결과를 반환한다.

웹툰은 자연스럽게 `전체 묶음 → 에피소드 → 페이지` 계층으로 표현된다. 요소 구조는 비교적 안정적이지만 컷 분석, 크기 통계, 좌표 검증, 보고서 생성 같은 연산은 늘어날 수 있다. Visitor는 요소를 고치지 않고 새 연산 클래스를 추가할 때 적합하다. Batch, Episode, Page별 메서드가 따로 있으므로 `visit(WebtoonElement)` 하나만 형식적으로 둔 구현과도 다르다.

픽셀마다 Visitor를 호출하지 않은 이유도 중요하다. 720×10,000 이미지는 720만 픽셀이다. 각 픽셀을 객체로 만들고 `accept()`를 호출하면 객체 생성과 동적 디스패치가 지나치게 늘어난다. 이 구현은 Page 수준에서 한 번 방문하고 내부 픽셀 루프는 일반 반복문으로 유지한다.

반대로 `Chapter` 같은 요소 타입이 자주 추가된다면 모든 Visitor를 수정해야 하므로 불편하다. Visitor는 **요소 구조는 안정적이고 연산 종류가 자주 늘어날 때** 가장 알맞다.

## 7. Abstract Factory + Singleton 실험군

| 클래스 | 패턴 역할 |
|---|---|
| `ProcessingFamilyFactory` | 호환 제품군을 만드는 Abstract Factory |
| `StandardProcessingFactory` | 매번 디스크에서 읽는 표준 제품군 |
| `CachedProcessingFactory` | Singleton LRU 저장소를 쓰는 캐시 제품군 |
| `ImageRepository` | 이미지 로더 제품 인터페이스 |
| `DiskImageRepository` | 매번 `ImageIO.read`하는 제품 |
| `SingletonLruImageRepository` | 이미지를 제한적으로 재사용하는 Singleton 제품 |
| `CutPointDetector` | 컷 검출 제품 인터페이스 |
| `RgbMarginCutPointDetector` | 자체 알고리즘을 가진 Singleton 제품 |
| `PieceWriter` / `JpegPieceWriter` | 출력 제품 인터페이스와 JPG 구현 |
| `WebtoonProcessingService` | Factory가 준 인터페이스 제품만 쓰는 서비스 |
| `FactorySplitterApp` | 제품군 선택과 실행을 담당하는 CLI |
| `FactoryBenchmark` | 실제 Factory→Service→제품 경로를 재는 API |

```text
StandardProcessingFactory
  ├─ DiskImageRepository
  ├─ RgbMarginCutPointDetector
  └─ JpegPieceWriter

CachedProcessingFactory.getInstance()
  ├─ SingletonLruImageRepository.getInstance()
  ├─ RgbMarginCutPointDetector.getInstance()
  └─ JpegPieceWriter.getInstance()

선택된 제품군 → WebtoonProcessingService.process
```

캐시 사용 여부를 서비스가 구체 클래스별 조건문으로 선택하면 생성 코드가 퍼진다. Abstract Factory는 서로 함께 사용할 로더·검출기·저장기를 한 제품군으로 제공하고, 서비스는 제품 인터페이스만 알게 한다.

Singleton인 것은 이미지 한 장이 아니라 `CachedProcessingFactory`, 여러 경로를 관리하는 `SingletonLruImageRepository`, 무상태 검출기와 writer 같은 **관리·서비스 객체**이다. 특히 Singleton을 단독 진입점으로 쓰지 않고 Abstract Factory가 캐시 저장소를 포함한 호환 제품군을 공급한다.

캐시 키에는 정규화 절대 경로, 파일 크기, 마지막 수정 시각이 들어간다. 같은 경로의 파일이 바뀌었는데 예전 이미지를 반환할 가능성을 줄이기 위해서다. 캐시 연산은 동기화되어 있고, 긴 웹툰을 무제한 보유하지 않도록 최근 사용 순서의 오래된 항목을 제거하는 LRU를 사용한다. 기본 용량은 4장이다.

작은 알고리즘 객체를 Singleton으로 만든 효과는 미미할 수 있다. 실제 시간 차이는 반복 `ImageIO.read`와 디코딩을 피하는 이미지 캐시에서 나타날 가능성이 크며, 그 대가로 retained heap과 전역 상태, 동기화, 무효화 관리 비용이 생긴다.

## 8. 독립 컴파일과 실행

프로젝트 루트의 PowerShell에서 실행한다. JDK가 설치되어 있고 `java`, `javac`가 PATH에 있어야 한다. 서로 다른 클래스 폴더에 컴파일하면 독립성이 명확하고 소스 폴더에 `.class`도 남지 않는다.

### Baseline

```powershell
New-Item -ItemType Directory -Force .build\baseline | Out-Null
javac -encoding UTF-8 -d .build\baseline Task1Splitter.java
java -cp .build\baseline Task1Splitter --dry-run images/002/005.jpg
java -cp .build\baseline Task1Splitter --output=output-baseline images/002/005.jpg
```

### Strategy

```powershell
New-Item -ItemType Directory -Force .build\strategy | Out-Null
javac -encoding UTF-8 -d .build\strategy variants/strategy/*.java
java -cp .build\strategy variants.strategy.StrategySplitterApp --dry-run images/002/005.jpg
java -cp .build\strategy variants.strategy.StrategySplitterApp --output=output-strategy images/002/005.jpg
```

### Visitor + Composite

```powershell
New-Item -ItemType Directory -Force .build\visitor | Out-Null
javac -encoding UTF-8 -d .build\visitor variants/visitor/*.java
java -cp .build\visitor variants.visitor.VisitorSplitterApp --dry-run images/002/005.jpg
java -cp .build\visitor variants.visitor.VisitorSplitterApp --output=output-visitor images/002/005.jpg
```

### Abstract Factory 표준/캐시 제품군

```powershell
New-Item -ItemType Directory -Force .build\factory | Out-Null
javac -encoding UTF-8 -d .build\factory variants/factory/*.java

# 매번 디스크에서 읽는 표준 제품군
java -cp .build\factory variants.factory.FactorySplitterApp --dry-run images/002/005.jpg

# Singleton LRU 캐시 제품군
java -cp .build\factory variants.factory.FactorySplitterApp --cached --dry-run images/002/005.jpg

# 실제 결과 저장
java -cp .build\factory variants.factory.FactorySplitterApp --cached --output=output-factory images/002/005.jpg
```

LRU 용량은 JVM 시스템 속성으로 바꿀 수 있다. `-D`는 클래스 이름보다 앞에 둔다.

```powershell
java "-Dvariants.factory.cache.maxEntries=8" -cp .build\factory variants.factory.FactorySplitterApp --cached --dry-run images/002
```

입력을 생략하면 각 CLI는 `images` 폴더를 재귀 처리한다. 결과가 섞이지 않도록 `--output`을 실험군마다 다르게 지정하는 편이 좋다.

## 9. 통합 벤치마크

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac -encoding UTF-8 -d build Task1Splitter.java PatternBenchmark.java variants/strategy/*.java variants/visitor/*.java variants/factory/*.java
java -cp build PatternBenchmark --repetitions=3 images/002/005.jpg
```

옵션은 최종 `PatternBenchmark` 구현을 기준으로 사용한다. 한 번의 수치로 결론 내리지 말고 여러 번 실행해 중앙값을 비교한다.

## 10. 공정한 시간 비교

패턴 호출 비용과 이미지 로딩 비용은 성격이 다르므로 두 실험으로 나눈다.

### 실험 A: 알고리즘과 패턴 호출 구조

대상은 Baseline, Strategy, Visitor, 그리고 Abstract Factory를 통해 생성한 Detector 제품이다.

1. 같은 파일을 한 번만 읽어 동일한 `BufferedImage`를 준비한다.
2. JPG 저장을 끈다.
3. 각 실험군에 같은 이미지와 순서를 사용한다.
4. 모든 컷 Y 목록이 같은지 측정 전에 검사한다.
5. 결과를 checksum에 반영해 JIT가 계산을 제거하지 못하게 한다.
6. 워밍업 뒤 여러 회 측정한다.

Visitor는 알고리즘을 직접 호출하지 말고 실제 Composite와 `accept/visit`를 측정해야 한다. Strategy도 Concrete Strategy를 우회하지 않고 Context와 인터페이스를 거쳐야 한다. Factory는 Factory가 생성한 `CutPointDetector` 제품을 사용하되, 이미지 캐시의 효과는 실험 B에서 따로 측정한다.

### 실험 B: 이미지 로딩과 Singleton 캐시

대상은 Factory 표준 제품군과 캐시 제품군이다.

1. 출력 저장을 끈다.
2. 표준 제품군은 매 반복마다 파일을 읽는다.
3. cold 측정 전에는 `clearCache()`를 호출한다.
4. 첫 캐시 실행은 cold, 같은 파일의 후속 반복은 warm으로 나눈다.
5. hit, miss, eviction, 보유 픽셀 바이트를 함께 기록한다.

warm cache를 사전 로드 이미지를 쓰는 실험 A와 그대로 비교하면 공정하지 않다. 하나는 파일 디코딩을 포함하고 다른 하나는 제외하기 때문이다.

### 시간 측정 체크리스트

- 같은 JDK, JVM 옵션, 입력 파일, 파일 순서를 사용한다.
- JPG 저장과 콘솔 출력은 timed 구간에서 제외한다.
- 클래스 로딩과 JIT 영향을 줄이도록 워밍업한다.
- 평균뿐 아니라 중앙값을 기록한다.
- 가능하면 실행 순서를 번갈아 CPU와 OS 부하 편향을 줄인다.
- `System.nanoTime()`을 사용한다.
- 노트북 전원 모드와 백그라운드 작업을 동일하게 유지한다.

## 11. 공정한 메모리 비교

이미지 너비를 `W`, 높이를 `H`, 페이지 수를 `P`, 캐시 용량을 `K`라 하면 다음과 같다.

| 실험군 | 시간 복잡도 | 핵심 추가 메모리 |
|---|---:|---:|
| Baseline | 페이지마다 `O(W×H)` | 한 이미지 + `O(W+H)` 분석 배열 |
| Strategy | 페이지마다 `O(W×H)` | Baseline + Strategy/Context `O(1)` |
| Visitor | 페이지마다 `O(W×H)` | 한 이미지 + Composite 노드 `O(P)` + 컷 결과 |
| Factory 표준 | `O(W×H)` + 매번 decode | 한 번에 처리하는 이미지 중심 |
| Factory cached | `O(W×H)`, warm에서 decode 생략 | 최대 `K`장 픽셀 배열 보유 |

720×10,000 이미지는 720만 픽셀이다. 픽셀당 3~4바이트만 계산해도 약 20.6~27.5MiB이고 객체 부가 비용도 있다. JPG가 디스크에서 1MiB여도 메모리에서 1MiB인 것은 아니다. JPG는 압축 파일이고 `BufferedImage`는 디코딩된 픽셀을 보유한다.

권장 지표는 처리당 할당 바이트, peak heap, 종료 후 retained heap, 캐시 보유 픽셀 바이트와 entry/hit/miss/eviction이다. `Runtime.totalMemory() - freeMemory()` 전후 차이만으로 결론 내리면 GC 시점 때문에 부정확하다. 가능하면 여러 JVM fork, JDK Flight Recorder 또는 JMH GC profiler를 사용한다. 간단한 과제에서는 `ThreadMXBean` 할당량과 캐시 snapshot을 함께 제시하고 heap 차이는 근삿값이라고 명시한다.

`System.gc()`는 즉시 GC를 보장하지 않는다. timed 구간 밖에서 실험군 상태를 비슷하게 만드는 보조 수단으로만 사용한다.

## 12. 결과 해석

모든 구현의 주된 시간은 `O(W×H)` 픽셀 분석이다. Baseline, Strategy, Visitor 차이는 작거나 실행마다 순서가 바뀔 수 있다. 한 번의 작은 차이로 가장 빠른 패턴을 결정하면 안 된다.

- Strategy의 핵심 이득: 알고리즘 교체와 테스트
- Visitor의 핵심 이득: 안정적인 객체 계층에 새 연산 추가
- Abstract Factory의 핵심 이득: 서로 맞는 제품군을 한곳에서 선택
- Singleton cache의 핵심 이득: 반복 디코딩 제거
- Singleton cache의 비용: retained memory, 전역 상태, 동기화, 무효화 관리

무상태 검출기 Singleton만으로는 큰 속도 향상이 없을 수 있다. 작은 객체 하나보다 수백만 픽셀 검사가 훨씬 비싸기 때문이다. 이미지 캐시는 warm 실행을 빠르게 만들 수 있지만 긴 이미지 여러 장을 메모리에 남기는 비용을 반드시 함께 보고해야 한다.

## 13. 보고서 결과표 예시

먼저 JDK, OS, CPU/메모리, 입력 수와 총 픽셀, 워밍업/반복 수, JVM 옵션, 캐시 용량을 기록한다.

### 알고리즘·패턴 비교

| 구현 | 중앙값 ms/image | 할당 MiB/image | 컷 결과 동일 | 해석 |
|---|---:|---:|---|---|
| Baseline |  |  |  |  |
| Strategy |  |  |  |  |
| Visitor + Composite |  |  |  |  |
| Abstract Factory detector |  |  |  |  |

### 로딩·캐시 비교

| 구현 | 상태 | 중앙값 ms/image | retained/보유 MiB | hit/miss | 해석 |
|---|---|---:|---:|---|---|
| Factory standard | no cache |  |  |  |  |
| Factory cached | cold |  |  |  |  |
| Factory cached | warm |  |  |  |  |

## 14. 최종 선택 기준

- 구조 변화가 거의 없는 작은 프로그램이면 Baseline이 단순하다.
- 알고리즘을 추가·교체할 예정이면 Strategy가 비용 대비 실용적이다.
- Batch·Episode·Page에 분석, 검증, 통계, 보고서 연산이 계속 추가되면 Visitor + Composite가 적합하다.
- 로더·검출기·저장기 조합을 일관되게 바꿔야 하면 Abstract Factory가 적합하다.
- 같은 대형 이미지를 반복 처리하고 메모리 여유가 있을 때만 제한된 Singleton LRU 캐시가 유리하다.

디자인 패턴은 많이 적용할수록 좋은 것이 아니다. 해결하려는 변화 지점이 분명할 때 적용하고, 시간·메모리와 유지보수성을 함께 비교해 최종 구조를 선택해야 한다.
