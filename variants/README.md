# 독립 디자인 패턴 실험군 빠른 안내

`variants` 아래의 세 폴더는 서로 또는 루트의 default-package 클래스에 의존하지 않는다. 각 폴더가 같은 RGB-margin 컷 검출 알고리즘을 자체 보유하므로 따로 컴파일하고 실행할 수 있다.

| 폴더 | 진입점 | 적용 패턴 | 알고리즘 위치 |
|---|---|---|---|
| `strategy` | `variants.strategy.StrategySplitterApp` | Strategy | `MarginCutPointStrategy` |
| `visitor` | `variants.visitor.VisitorSplitterApp` | Visitor + Composite | `VisitorCutAlgorithm` |
| `factory` | `variants.factory.FactorySplitterApp` | Abstract Factory + Singleton LRU | `RgbMarginCutPointDetector` |

기준선은 루트의 완전 구현인 `Task1Splitter.java`이다. `Task1Baseline.java`는 초기 단순 밝기 방식이므로 디자인 패턴 비교 기준선이 아니다.

## 가장 빠른 확인 방법

프로젝트 루트의 PowerShell에서 실행한다.

```powershell
# Strategy
New-Item -ItemType Directory -Force .build\strategy | Out-Null
javac -encoding UTF-8 -d .build\strategy variants/strategy/*.java
java -cp .build\strategy variants.strategy.StrategySplitterApp --dry-run images/002/005.jpg

# Visitor + Composite
New-Item -ItemType Directory -Force .build\visitor | Out-Null
javac -encoding UTF-8 -d .build\visitor variants/visitor/*.java
java -cp .build\visitor variants.visitor.VisitorSplitterApp --dry-run images/002/005.jpg

# Abstract Factory 표준 제품군
New-Item -ItemType Directory -Force .build\factory | Out-Null
javac -encoding UTF-8 -d .build\factory variants/factory/*.java
java -cp .build\factory variants.factory.FactorySplitterApp --dry-run images/002/005.jpg

# Abstract Factory + Singleton LRU 캐시 제품군
java -cp .build\factory variants.factory.FactorySplitterApp --cached --dry-run images/002/005.jpg
```

`--dry-run`을 빼면 JPG 조각을 저장한다. 결과가 섞이지 않게 출력 폴더를 나누는 것이 좋다.

```powershell
java -cp .build\strategy variants.strategy.StrategySplitterApp --output=output-strategy images/002/005.jpg
java -cp .build\visitor variants.visitor.VisitorSplitterApp --output=output-visitor images/002/005.jpg
java -cp .build\factory variants.factory.FactorySplitterApp --cached --output=output-factory images/002/005.jpg
```

## 결과 확인 원칙

먼저 동일 이미지에서 모든 구현의 컷 Y 좌표가 완전히 같은지 확인한다. 좌표가 다르면 패턴 비용이 아니라 알고리즘 차이를 비교하게 되므로 시간·메모리 결과를 사용하면 안 된다.

소스가 반복되어 보이는 것은 각 실험군을 독립 실행하고 알고리즘을 통제 변수로 고정하기 위한 의도적인 구성이다. 실제 제품 코드에서는 검증이 끝난 뒤 공통 알고리즘 모듈로 합칠 수 있다.

설계 이유, 통합 벤치마크, 시간·메모리 해석 방법은 루트의 `DESIGN_PATTERN_ANALYSIS.md`를 참고한다.
