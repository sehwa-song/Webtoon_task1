# Task1 Decorator 패턴

핵심 RGB 컷 탐색기는 그대로 두고 검증과 시간 측정 기능을 객체로 감싸서 추가한 독립 실행 예제입니다.

```text
TimingCutPointDetectorDecorator
  └─ CutPointValidationDecorator
       └─ RgbMarginCutPointDetector (원본 기능)
```

PowerShell에서 컴파일하고 실행합니다.

```powershell
$sources = Get-ChildItem .\task1\decorator\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-decorator | Out-Null
javac -encoding UTF-8 -d .\build-task1-decorator $sources
java -cp .\build-task1-decorator task1.decorator.DecoratorSplitterApp
```

특정 파일을 저장 없이 분석하려면 다음처럼 실행합니다.

```powershell
java -cp .\build-task1-decorator task1.decorator.DecoratorSplitterApp --dry-run .\images\003\002.jpg
```

기본 출력 위치는 `output-task1-patterns/decorator`입니다. `--output=폴더`로 바꿀 수 있습니다.
