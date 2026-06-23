$ErrorActionPreference = "Stop"

function RgbInt([int]$r, [int]$g, [int]$b) {
  return $r + ($g * 256) + ($b * 65536)
}

function Add-Text($slide, [string]$text, [double]$x, [double]$y, [double]$w, [double]$h, [int]$size,
  [int]$color, [bool]$bold = $false, [int]$align = 1) {
  $shape = $slide.Shapes.AddTextbox(1, $x, $y, $w, $h)
  $shape.TextFrame2.MarginLeft = 4
  $shape.TextFrame2.MarginRight = 4
  $shape.TextFrame2.MarginTop = 2
  $shape.TextFrame2.MarginBottom = 2
  $shape.TextFrame2.TextRange.Text = $text
  $shape.TextFrame2.TextRange.Font.Name = "Microsoft YaHei"
  $shape.TextFrame2.TextRange.Font.NameFarEast = "Microsoft YaHei"
  $shape.TextFrame2.TextRange.Font.Size = $size
  $shape.TextFrame2.TextRange.Font.Fill.ForeColor.RGB = $color
  $shape.TextFrame2.TextRange.Font.Bold = [int]$bold
  $shape.TextFrame2.TextRange.ParagraphFormat.Alignment = $align
  return $shape
}

function Add-Box($slide, [string]$text, [double]$x, [double]$y, [double]$w, [double]$h,
  [int]$fill, [int]$line, [int]$fontColor, [int]$size = 16, [bool]$bold = $false) {
  $shape = $slide.Shapes.AddShape(5, $x, $y, $w, $h)
  $shape.Fill.ForeColor.RGB = $fill
  $shape.Fill.Transparency = 0.08
  $shape.Line.ForeColor.RGB = $line
  $shape.Line.Weight = 1.4
  $shape.TextFrame2.MarginLeft = 10
  $shape.TextFrame2.MarginRight = 10
  $shape.TextFrame2.MarginTop = 7
  $shape.TextFrame2.MarginBottom = 7
  $shape.TextFrame2.VerticalAnchor = 3
  $shape.TextFrame2.TextRange.Text = $text
  $shape.TextFrame2.TextRange.Font.Name = "Microsoft YaHei"
  $shape.TextFrame2.TextRange.Font.NameFarEast = "Microsoft YaHei"
  $shape.TextFrame2.TextRange.Font.Size = $size
  $shape.TextFrame2.TextRange.Font.Fill.ForeColor.RGB = $fontColor
  $shape.TextFrame2.TextRange.Font.Bold = [int]$bold
  $shape.TextFrame2.TextRange.ParagraphFormat.Alignment = 2
  return $shape
}

function Add-ScreenshotSlide($pres, [int]$index, [string]$title, [string]$subtitle, [string]$imagePath) {
  $slide = $pres.Slides.Add($index, 12)
  $slide.Background.Fill.ForeColor.RGB = (RgbInt 246 249 243)
  Add-Text $slide $title 55 26 520 30 22 (RgbInt 34 63 43) $true 1 | Out-Null
  Add-Text $slide $subtitle 58 58 760 22 12 (RgbInt 70 93 67) $false 1 | Out-Null
  $pic = $slide.Shapes.AddPicture($imagePath, $false, $true, 80, 86, 800, 450)
  $pic.Line.Visible = -1
  $pic.Line.ForeColor.RGB = (RgbInt 91 125 88)
  $pic.Line.Weight = 1
  Add-Box $slide "运行界面截图" 748 32 118 28 (RgbInt 162 190 145) (RgbInt 162 190 145) (RgbInt 255 255 255) 11 $true | Out-Null
  return $slide
}

function Add-DualScreenshotSlide($pres, [int]$index, [string]$title, [string]$subtitle, [string]$leftImage, [string]$rightImage) {
  $slide = $pres.Slides.Add($index, 12)
  $slide.Background.Fill.ForeColor.RGB = (RgbInt 246 249 243)
  Add-Text $slide $title 55 28 600 30 22 (RgbInt 34 63 43) $true 1 | Out-Null
  Add-Text $slide $subtitle 58 60 790 22 12 (RgbInt 70 93 67) $false 1 | Out-Null
  $left = $slide.Shapes.AddPicture($leftImage, $false, $true, 45, 116, 420, 236)
  $right = $slide.Shapes.AddPicture($rightImage, $false, $true, 495, 116, 420, 236)
  foreach ($pic in @($left, $right)) {
    $pic.Line.Visible = -1
    $pic.Line.ForeColor.RGB = (RgbInt 91 125 88)
    $pic.Line.Weight = 1
  }
  Add-Box $slide "安全报告" 140 368 230 42 (RgbInt 236 243 226) (RgbInt 91 125 88) (RgbInt 34 63 43) 15 $true | Out-Null
  Add-Box $slide "悬浮 AI 助手" 590 368 230 42 (RgbInt 236 243 226) (RgbInt 91 125 88) (RgbInt 34 63 43) 15 $true | Out-Null
  Add-Text $slide "报告页把资产概览、分类覆盖率、风险告警和整改建议生成到同一视图；悬浮助手提供任意页面可用的本地规则问答入口。" 88 430 790 40 13 (RgbInt 48 74 54) $false 2 | Out-Null
  return $slide
}

$root = (Resolve-Path ".").Path
$source = Join-Path $root "outputs\数据分类分级保护系统-答辩PPT.pptx"
$out = Join-Path $root "outputs\数据分类分级保护系统-答辩PPT-优化版.pptx"
$shots = Join-Path $root "outputs\project_screenshots"
$exportDir = Join-Path $root "outputs\ppt_optimized_export"

if (Test-Path $out) { Remove-Item -LiteralPath $out -Force }
if (Test-Path $exportDir) { Remove-Item -LiteralPath $exportDir -Recurse -Force }
New-Item -ItemType Directory -Force $exportDir | Out-Null
Copy-Item -LiteralPath $source -Destination $out -Force

$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$pres = $ppt.Presentations.Open($out, $false, $false, $true)

foreach ($slide in $pres.Slides) {
  foreach ($shape in $slide.Shapes) {
    if ($shape.HasTextFrame -eq -1 -and $shape.TextFrame2.HasText -eq -1) {
      $text = $shape.TextFrame2.TextRange.Text
      $newText = $text
      $newText = $newText.Replace("20 张业务表 + 视图", "16 张核心表 + Agent 4 表")
      $newText = $newText.Replace("20 张表、视图", "16 张核心表 + 4 张 Agent 表、视图")
      if ($newText -ne $text) {
        $shape.TextFrame2.TextRange.Text = $newText
      }
    }
  }
}

Add-ScreenshotSlide $pres 5 "运行截图：首页看板支撑治理总览" "对齐报告中的治理态势说明，展示资产、敏感字段、待审批申请、风险告警和分类覆盖率。" (Join-Path $shots "01_dashboard.png") | Out-Null
Add-ScreenshotSlide $pres 6 "运行截图：Agent 工作台体现智能治理入口" "对齐项目新增 Agent 模块，展示字段分类、审批建议、风险告警和安全报告的一站式入口。" (Join-Path $shots "03_agent_workbench.png") | Out-Null
Add-DualScreenshotSlide $pres 7 "运行截图：安全报告与悬浮助手" "对齐答辩演示流程的最后一步，展示报告生成和任意页面问答能力。" (Join-Path $shots "07_security_report_generated.png") (Join-Path $shots "06_floating_ai.png") | Out-Null

$pres.Save()
$pres.Export($exportDir, "PNG", 1280, 720)
$pres.Close()
$ppt.Quit()

Write-Host $out
Write-Host $exportDir
