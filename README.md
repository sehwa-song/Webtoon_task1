# Webtoon Task1 / Task2 디자인 패턴 학습 코드

이 저장소는 긴 베이스라인 전체를 복제하지 않고 다음 핵심만 짧게 남겼다.

```text
이미지 읽기 → 여백 행 탐지 → 좌표 생성 → 이미지 자르기 → 저장
```

- Task1: 50px 이상 이어지는 여백의 가운데를 절단점으로 사용한다.
- Task2: 48px 이상 이어지는 여백을 제거하고 그림이 있는 구간만 저장한다.
- 캐시, 싱글톤, 실행 시간 측정, 운영체제 관련 최적화는 없다.

공통 픽셀 알고리즘은 `webtoon/core`, 파일 처리는 `webtoon/io`, 패턴 예제는
`task1/<패턴>`과 `task2/<패턴>`에 있다. 상세 설명은 각 Task의 `README.md`,
그림은 `design-pattern-diagrams`에서 확인한다.

> 일곱 패턴을 모두 쓰는 것이 실제 제품의 최선이라는 뜻은 아니다. 같은 작은 알고리즘을
> 패턴별로 표현해 구조 차이를 학습하기 위한 코드다. 특히 단순 Proxy, Factory, Visitor는
> 처리 속도나 탐지 정확도를 높이지 않는다.

## 실행

```powershell
javac -encoding UTF-8 -d .build (Get-ChildItem webtoon,task1,task2 -Recurse -Filter *.java | ForEach-Object FullName)
java -cp .build task1.strategy.StrategyExample images\001\003.jpg output\task1
java -cp .build task2.strategy.StrategyExample images\001\003.jpg output\task2
```

다른 패턴은 `strategy.StrategyExample` 대신 `template.TemplateMethodExample`,
`chain.ChainExample`, `decorator.DecoratorExample`, `proxy.ProxyExample`,
`factory.FactoryExample`, `visitor.VisitorExample`을 사용한다.
