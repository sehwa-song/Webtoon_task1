# Task1 Proxy 패턴

서비스와 실제 디스크 저장소 사이에 캐시 대리자를 두는 독립 실행 예제입니다.

```text
ProxySplittingService
  └─ ImageRepository 인터페이스
       └─ CachingImageRepositoryProxy (대리자)
            └─ DiskImageRepository (원본 대상)
```

PowerShell에서 컴파일하고 실행합니다.

```powershell
$sources = Get-ChildItem .\task1\proxy\*.java | ForEach-Object FullName
New-Item -ItemType Directory -Force .\build-task1-proxy | Out-Null
javac -encoding UTF-8 -d .\build-task1-proxy $sources
java -cp .\build-task1-proxy task1.proxy.ProxySplitterApp
```

캐시 hit를 확인하려면 같은 입력을 두 번 읽습니다.

```powershell
java -cp .\build-task1-proxy task1.proxy.ProxySplitterApp --dry-run --passes=2 .\images\003\002.jpg
```

기본 출력 위치는 `output-task1-patterns/proxy`입니다. `--output=폴더`로 바꿀 수 있습니다.
