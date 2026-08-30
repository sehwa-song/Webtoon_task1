# Task1 Abstract Factory

`ProcessingFamilyFactory`가 `ImageRepository`, `CutPointDetector`,
`PieceWriter`라는 세 제품을 한 제품군으로 만듭니다.

- `StandardProcessingFactory`: 매번 디스크에서 이미지 읽기
- `CachedProcessingFactory`: 동일 경로 재사용 시 LRU 캐시 사용
- 두 팩토리 모두 같은 RGB 컷 알고리즘과 JPEG 저장기를 사용

PowerShell 컴파일:

```powershell
$sources = Get-ChildItem .\task1\factory\*.java | ForEach-Object FullName
javac -encoding UTF-8 -d .\build-task1-factory $sources
```

실행:

```powershell
java -cp .\build-task1-factory task1.factory.FactorySplitterApp --dry-run
java -cp .\build-task1-factory task1.factory.FactorySplitterApp --cached
```
