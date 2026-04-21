$files = @(
    "c:\Users\huy\Desktop\New code\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\TeacherAssignmentGradingServiceImpl.java",
    "c:\Users\huy\Desktop\New code\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\QuizResultServiceImpl.java"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = [System.IO.File]::ReadAllText($file)
        # Write back as UTF8 WITHOUT BOM
        $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($file, $content, $utf8WithoutBom)
        Write-Host "Removed BOM from $file"
    }
}
