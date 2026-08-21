# Task1Splitter 디자인 패턴 비교 분석

## 1. 설계 원칙

비교 대상은 모두 `Task1Splitter.findCutPoints(BufferedImage)`를 호출한다. 패턴마다 다른 컷 검출 알고리즘을 쓰면 측정값에 **알고리즘 차이**와 **패턴 구조 차이**가 섞이기 때문이다. 따라서 컷 Y 좌표가 완전히 같은지 먼저 검사하고, 그다음 호출 구조만 바꿔 시간과 메모리를 측정한다.

```text
                         Task1Splitter.findCutPoints
                                      ▲
              ┌───────────────────────┴──────────────────────┐
              │                                              │
       DirectSplitter                         MarginCutPointStrategy
                                                        (Singleton)
                                                    ▲          ▲
                                                    │          │
                              CutAnalysisVisitor ───┘          │
                                                               │
 WebtoonProcessingService ── ProcessingComponentFactory ──────┘
                                  ├─ StandardProcessingFactory ── DiskImageRepository
                                  └─ CachedProcessingFactory ──── SingletonImageRepository
```

## 2. 적용한 패턴

| 패턴 | 적용 클래스 | 이 문제에서의 역할 |
|---|---|---|
| 기준선(Direct) | `DirectSplitter` | 인터페이스나 객체군 없이 기존 코어를 직접 호출한다. |
| Strategy | `CutPointStrategy`, `MarginCutPointStrategy` | 컷 검출 방식을 교체 가능하게 한다. 현재 구현은 무상태이므로 인스턴스 하나만 사용한다. |
| Singleton | `MarginCutPointStrategy`, `SingletonImageRepository` | 알고리즘 객체를 하나만 만들고, 이미 읽은 이미지도 하나의 전역 캐시 관리자가 재사용한다. |
| Abstract Factory | `ProcessingComponentFactory`와 두 Concrete Factory | 캐시 사용/미사용에 맞는 `ImageRepository`와 `CutPointStrategy` 객체군을 함께 만든다. |
| Visitor | `WebtoonVisitor`, `CutAnalysisVisitor`, `PageStatisticsVisitor` | `Batch → Episode → Page` 구조를 바꾸지 않고 컷 분석이나 통계 같은 새 연산을 추가한다. |
| Composite | `WebtoonBatch`, `WebtoonEpisode`, `WebtoonPage` | 실제 웹툰 폴더 구조를 Visitor가 일관되게 순회하도록 표현한다. |

### Strategy + 알고리즘 Singleton

`MarginCutPointStrategy`는 처리 중 상태를 필드에 저장하지 않는다. 따라서 이미지마다 객체를 만들 필요가 없고 모든 요청이 같은 인스턴스를 공유해도 안전하다. 다만 절약되는 것은 작은 Strategy 객체 몇 개뿐이라 시간·메모리 효과는 거의 없다. 핵심 장점은 알고리즘 교체와 단위 테스트가 쉬워진다는 점이다.

### Abstract Factory + 이미지 캐시 Singleton

과제 조건에 맞춰 Singleton을 단독 진입점으로 쓰지 않고 `CachedProcessingFactory`가 Singleton 저장소를 공급한다. 이때 Singleton인 것은 “이미지 한 장”이 아니라 **여러 경로의 이미지를 관리하는 캐시 관리자 하나**이다.

캐시 키는 정규화한 절대 경로뿐 아니라 파일 크기와 마지막 수정 시각도 포함한다. 같은 경로의 파일이 교체됐는데 오래된 이미지가 반환되는 위험을 줄이기 위해서다. 또한 디코딩된 세로 웹툰 한 장이 수십 MiB이므로 무제한 `Map` 대신 기본 4장짜리 LRU를 사용했다. 용량은 JVM 옵션 `webtoon.cache.maxEntries`로 조절할 수 있다.

### Visitor + Composite

Visitor를 RGB 픽셀마다 호출하면 수백만 번의 가상 메서드 호출이 생기므로 적절하지 않다. 대신 안정적인 도메인 구조인 `WebtoonBatch → WebtoonEpisode → WebtoonPage`를 방문하고, 실제 픽셀 루프는 기존 코어에 한 번 위임한다.

`CutAnalysisVisitor` 외에 `PageStatisticsVisitor`를 추가해, 노드 클래스를 수정하지 않고도 페이지 수·총 픽셀 수 같은 새 연산을 붙일 수 있음을 보여 준다. 반대로 요소가 사실상 `Page` 하나뿐이고 추가 연산도 하나뿐이라면 Visitor는 과한 설계라는 점도 보고서에 명시하는 것이 좋다.

## 3. 파일 구성

```text
기존 코어
└─ Task1Splitter.java

기준선/Strategy
├─ DirectSplitter.java
├─ CutPointStrategy.java
└─ MarginCutPointStrategy.java

Abstract Factory/Singleton cache
├─ ProcessingComponentFactory.java
├─ StandardProcessingFactory.java
├─ CachedProcessingFactory.java
├─ WebtoonProcessingService.java
├─ ImageRepository.java
├─ DiskImageRepository.java
└─ SingletonImageRepository.java

Visitor/Composite
├─ WebtoonElement.java
├─ WebtoonVisitor.java
├─ WebtoonBatch.java
├─ WebtoonEpisode.java
├─ WebtoonPage.java
├─ CutAnalysisVisitor.java
└─ PageStatisticsVisitor.java

비교 실행
└─ PatternBenchmark.java
```

## 4. 이론적 시간·메모리 비교

이미지 너비를 `W`, 높이를 `H`, 캐시 장수를 `K`, 페이지 수를 `P`라고 한다.

| 구현 | 시간 복잡도 | 추가 구조 메모리 | 해석 |
|---|---:|---:|---|
| Direct | `O(W×H)` | `O(W+H)` + 이미지 | 가장 단순한 기준선이다. |
| Strategy Singleton | `O(W×H)` | `O(1)` 객체 추가 | 인터페이스 호출은 픽셀 처리량에 비해 매우 작다. |
| Visitor | `O(W×H)`/페이지 | `O(P)` 노드·결과 | 배치 구조와 연산 종류가 늘 때 유리하다. |
| Factory, no cache | `O(W×H)` + 매회 decode | `O(1)` Factory 객체 | 구조 비용보다 파일 읽기·JPEG decode가 크다. |
| Factory + Singleton cache, warm | `O(W×H)` | 최대 `O(K×W×H)` 보유 | 재로딩은 없어지지만 컷 검출 자체는 다시 수행한다. |

`BufferedImage` 메모리는 압축된 JPG 파일 크기가 아니라 디코딩 픽셀 크기로 계산해야 한다. 예를 들어 720×10,000 이미지는 저장 형식에 따라 픽셀 배열만 대략 20.6~27.5MiB이다. 따라서 캐시 시간 이득에는 큰 retained-memory 비용이 따른다.

## 5. 포함된 벤치마크

컴파일과 실행은 프로젝트 루트의 PowerShell에서 다음과 같이 한다.

```powershell
javac -encoding UTF-8 -Xlint:all *.java
java PatternBenchmark --input=images/002 --limit=3 --warmup=2 --iterations=3
```

캐시를 8장으로 바꾸려면 다음처럼 JVM 옵션을 클래스명보다 앞에 둔다.

```powershell
java "-Dwebtoon.cache.maxEntries=8" PatternBenchmark --input=images/002 --limit=8
```

벤치마크는 다음을 지킨다.

1. Direct, Strategy, Visitor의 모든 cut Y 목록이 같은지 먼저 검증한다.
2. 알고리즘 비교에서는 동일한 사전 로드 `BufferedImage`를 사용한다.
3. 캐시 cold 측정 전에 Singleton 저장소를 비운다.
4. 출력 JPG 압축·디스크 쓰기는 제외한다.
5. 결과가 제거되지 않도록 checksum을 소비한다.
6. 현재 스레드 할당량은 `ThreadMXBean`, 캐시 픽셀 크기는 raster, retained heap은 GC 전후 근삿값으로 표시한다.

### 현재 환경의 예시 결과

JDK 26.0.1, `images/002`의 앞 3장, warmup 2회, 측정 3회, 캐시 용량 4장에서 나온 한 번의 결과다.

| 구현 | ms/image | 할당 MiB/image |
|---|---:|---:|
| Direct | 111.390 | 0.508 |
| Strategy Singleton | 111.353 | 0.508 |
| Visitor | 110.577 | 0.508 |
| Abstract Factory, no cache | 165.439 | 43.464 |
| Abstract Factory + cache, cold | 171.204 | 43.465 |
| Abstract Factory + cache, warm | 111.371 | 0.509 |

Direct·Strategy·Visitor 차이는 1% 미만으로 측정 오차 범위에 가깝다. 이는 패턴 비용보다 `O(W×H)` 픽셀 분석이 훨씬 크기 때문이다. warm cache는 no-cache 파이프라인보다 이 실행에서 약 32.7% 빨랐고 이미지 decode 할당도 제거했다. 대신 3장을 보유하는 데 픽셀 배열 약 64.17MiB, 측정 retained heap 약 70.78MiB가 남았다. cold cache는 아직 읽기와 decode를 해야 하므로 빨라지지 않는다.

이 값은 OS 파일 캐시, JIT, GC, CPU 부하에 따라 달라진다. 과제 보고서에는 같은 명령을 여러 번 실행해 중앙값을 사용하고, 엄밀한 성능 연구라면 JMH의 여러 fork와 `-prof gc` 결과를 함께 제시하는 편이 좋다. `Runtime.totalMemory() - freeMemory()`만 한 번 재는 방식은 메모리 근거로 충분하지 않다.

## 6. 결론

- 가장 단순하고 객체가 적은 구현은 Direct이다.
- 컷 알고리즘을 추가·교체할 가능성이 있으면 Strategy가 비용 대비 가장 실용적이다.
- 폴더/에피소드/페이지를 순회하며 분석·검증·통계·출력 같은 연산이 계속 추가되면 Visitor + Composite가 적합하다.
- 캐시 사용 여부처럼 서로 맞물리는 구현 객체군을 선택하려면 Abstract Factory가 적합하다.
- 실제 속도 향상은 “알고리즘 객체 Singleton”이 아니라 “이미지 캐시 Singleton”에서 생긴다. 그 대가가 큰 메모리 점유, 전역 상태, 동시성 관리이므로 반드시 LRU 제한과 `clear()`가 필요하다.
