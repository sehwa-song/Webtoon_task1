# Task1 Composite + Visitor

Composite 계층:

```text
WebtoonBatch (Composite)
└─ WebtoonEpisode (Composite)
   └─ WebtoonPage (Leaf)
```

`VisitorSplitterApp`이 계층을 만든 뒤 `batch.accept(splitPageVisitor)`를 호출합니다.
`SplitPageVisitor`는 각 Page에 RGB 컷 탐색과 저장 작업을 적용합니다.

PowerShell 컴파일:

```powershell
$sources = Get-ChildItem .\task1\visitor\*.java | ForEach-Object FullName
javac -encoding UTF-8 -d .\build-task1-visitor $sources
```

실행:

```powershell
java -cp .\build-task1-visitor task1.visitor.VisitorSplitterApp --dry-run
java -cp .\build-task1-visitor task1.visitor.VisitorSplitterApp
```
