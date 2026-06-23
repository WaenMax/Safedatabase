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

function Add-Panel($slide, [double]$x, [double]$y, [double]$w, [double]$h, [double]$transparency = 0.12) {
  $shape = $slide.Shapes.AddShape(5, $x, $y, $w, $h)
  $shape.Fill.ForeColor.RGB = (RgbInt 248 251 244)
  $shape.Fill.Transparency = $transparency
  $shape.Line.ForeColor.RGB = (RgbInt 91 125 88)
  $shape.Line.Transparency = 0.55
  $shape.Line.Weight = 1
  return $shape
}

function Add-Cover($slide, [double]$x, [double]$y, [double]$w, [double]$h, [int]$fill) {
  $shape = $slide.Shapes.AddShape(1, $x, $y, $w, $h)
  $shape.Fill.Solid()
  $shape.Fill.ForeColor.RGB = $fill
  $shape.Fill.Transparency = 0
  $shape.Line.Visible = 0
  $shape.ZOrder(0)
  return $shape
}

function Add-Arrow($slide, [double]$x1, [double]$y1, [double]$x2, [double]$y2, [int]$color) {
  $conn = $slide.Shapes.AddConnector(1, $x1, $y1, $x2, $y2)
  $conn.Line.ForeColor.RGB = $color
  $conn.Line.Weight = 1.6
  $conn.Line.EndArrowheadStyle = 3
  return $conn
}

function Add-Bullets($slide, [string[]]$items, [double]$x, [double]$y, [double]$w, [double]$h, [int]$size = 16) {
  $text = ($items | ForEach-Object { "• $_" }) -join "`r"
  $shape = Add-Text $slide $text $x $y $w $h $size (RgbInt 48 74 54) $false 1
  $shape.TextFrame2.TextRange.ParagraphFormat.SpaceWithin = 1.05
  return $shape
}

function Add-Background($slide, [string]$path) {
  $pic = $slide.Shapes.AddPicture($path, $false, $true, 0, 0, 960, 540)
  $pic.ZOrder(1)
}

$root = "D:\database"
$bgDir = Join-Path $root "tmp\ppt_data_security\tmp\template_export"
$outDir = Join-Path $root "outputs"
New-Item -ItemType Directory -Force $outDir | Out-Null
$outPptx = Join-Path $outDir "数据分类分级保护系统-答辩PPT.pptx"
if (Test-Path $outPptx) { Remove-Item $outPptx -Force }

$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$pres = $ppt.Presentations.Add()
$pres.PageSetup.SlideWidth = 960
$pres.PageSetup.SlideHeight = 540

$ppLayoutBlank = 12
$green = RgbInt 50 84 58
$deep = RgbInt 34 63 43
$soft = RgbInt 236 243 226
$accent = RgbInt 182 147 104
$line = RgbInt 86 118 83
$orange = RgbInt 196 131 78
$red = RgbInt 173 80 69

function New-Slide([string]$bg) {
  $s = $pres.Slides.Add($pres.Slides.Count + 1, $ppLayoutBlank)
  Add-Background $s (Join-Path $bgDir $bg)
  Add-Cover $s 610 0 335 72 (RgbInt 162 190 145) | Out-Null
  return $s
}

# 1 Cover
$s = New-Slide "slide01.png"
Add-Cover $s 35 412 320 85 (RgbInt 164 190 147) | Out-Null
Add-Panel $s 56 70 385 320 0.05 | Out-Null
Add-Text $s "数据分类分级保护系统" 78 120 335 58 30 $deep $true 1 | Out-Null
Add-Text $s "课程设计答辩展示" 82 185 310 32 18 $green $false 1 | Out-Null
Add-Text $s "Spring Boot 3 + Vue 3 + MySQL + 数据安全治理 Agent" 82 232 328 46 14 (RgbInt 70 93 67) $false 1 | Out-Null
Add-Text $s "面向企业数据资产管理、敏感识别、脱敏审批与审计闭环" 82 288 330 44 13 (RgbInt 70 93 67) $false 1 | Out-Null
Add-Text $s "项目：数据分类分级保护系统`r答辩日期：2026 年 6 月" 54 433 250 38 11 (RgbInt 245 249 238) $false 1 | Out-Null

# 2 Contents
$s = New-Slide "slide02.png"
Add-Cover $s 62 104 835 335 (RgbInt 241 245 238) | Out-Null
Add-Text $s "目录" 72 62 160 45 28 $deep $true 1 | Out-Null
$toc = @("项目定位与建设目标", "核心功能与角色权限", "系统技术架构", "数据治理使用流程", "数据库设计与对象", "Agent 能力与演示流程")
$x1 = 155; $y1 = 155
for ($i=0; $i -lt $toc.Count; $i++) {
  $col = [math]::Floor($i / 3)
  $row = $i % 3
  Add-Text $s ("0{0}. {1}" -f ($i+1), $toc[$i]) (155 + $col*345) (152 + $row*70) 290 34 17 $green ($i -eq 0) 1 | Out-Null
}

# 3 Project positioning
$s = New-Slide "slide05.png"
Add-Cover $s 710 15 210 35 (RgbInt 162 190 145) | Out-Null
Add-Cover $s 54 92 460 42 (RgbInt 162 190 145) | Out-Null
Add-Cover $s 54 135 420 170 (RgbInt 162 190 145) | Out-Null
Add-Cover $s 35 280 430 215 (RgbInt 162 190 145) | Out-Null
Add-Cover $s 475 360 440 135 (RgbInt 162 190 145) | Out-Null
Add-Text $s "项目定位：数据安全治理闭环平台" 60 72 455 40 23 $deep $true 1 | Out-Null
Add-Bullets $s @(
  "统一登记数据源、数据表、字段资产，形成字段级资产台账",
  "通过 L1-L5 分类分级识别敏感字段，并支持规则自动分类",
  "普通用户访问 L4/L5 原始值必须提交申请并经过审批",
  "查看、分类、审批、Agent 分析等关键动作全量写入审计日志"
) 63 135 420 160 15 | Out-Null
Add-Panel $s 520 118 330 230 0.05 | Out-Null
Add-Text $s "治理闭环" 550 142 270 28 20 $green $true 2 | Out-Null
$nodes = @("资产登记", "分类分级", "脱敏展示", "申请审批", "审计留痕", "风险整改")
for ($i=0; $i -lt $nodes.Count; $i++) {
  $angle = 2 * [math]::PI * $i / $nodes.Count - [math]::PI / 2
  $cx = 685 + [math]::Cos($angle) * 105
  $cy = 245 + [math]::Sin($angle) * 72
  Add-Box $s $nodes[$i] ($cx-42) ($cy-17) 84 34 $soft $line $deep 12 $true | Out-Null
}

# 4 Functions
$s = New-Slide "slide09.png"
Add-Text $s "核心功能模块" 60 58 260 36 24 $deep $true 1 | Out-Null
$cards = @(
  @("资产管理", "数据源 / 数据表 / 字段资产 CRUD`r字段样例值与敏感标识维护"),
  @("分类分级", "L1-L5 等级字典`r人工分类 + keyword/regex 自动识别"),
  @("脱敏与审批", "手机号、邮箱、身份证、银行卡等脱敏`rL4/L5 原始值走申请审批"),
  @("审计与看板", "登录、查看、分类、审批留痕`r统计资产、敏感字段与最近日志"),
  @("治理 Agent", "字段分类、审批建议、风险分析`r安全报告与悬浮问答助手")
)
for ($i=0; $i -lt $cards.Count; $i++) {
  $x = 65 + ($i % 3) * 285
  $y = 130 + [math]::Floor($i / 3) * 132
  $w = if ($i -lt 3) { 235 } else { 330 }
  Add-Box $s $cards[$i][0] $x $y $w 34 $green $green (RgbInt 255 255 255) 16 $true | Out-Null
  $body = Add-Box $s $cards[$i][1] $x ($y+38) $w 72 (RgbInt 224 238 218) $line $deep 12 $false
  $body.TextFrame2.TextRange.ParagraphFormat.Alignment = 1
}

# 5 Architecture diagram
$s = New-Slide "slide10.png"
Add-Cover $s 410 430 310 86 (RgbInt 238 240 237) | Out-Null
Add-Text $s "项目技术架构图" 58 45 300 38 24 $deep $true 1 | Out-Null
Add-Panel $s 47 96 866 365 0 | Out-Null
$api = Add-Box $s "Spring Boot REST API`r认证 / 资产 / 分类 / 审批 / 审计 / Agent" 355 205 230 70 $green $green (RgbInt 255 255 255) 14 $true
$ui = Add-Box $s "Vue 3 前端`rElement Plus + Axios + Router`r后台管理 + 悬浮 AI 助手" 70 195 210 88 (RgbInt 224 238 218) $line $deep 14 $true
$db = Add-Box $s "MySQL 数据库`r20 张业务表 + 视图`r存储过程 / 触发器 / 索引" 680 195 210 88 (RgbInt 224 238 218) $line $deep 14 $true
Add-Arrow $s 280 239 355 239 $line | Out-Null
Add-Arrow $s 585 239 680 239 $line | Out-Null
$services = @(
  @("AuthService", 355, 120), @("AssetService", 505, 120),
  @("Classification", 355, 315), @("Workflow", 505, 315),
  @("Audit", 220, 330), @("Agent", 650, 330)
)
foreach ($svc in $services) {
  Add-Box $s $svc[0] $svc[1] $svc[2] 120 38 (RgbInt 246 239 220) $accent $deep 12 $true | Out-Null
}
Add-Arrow $s 415 158 430 205 $accent | Out-Null
Add-Arrow $s 565 158 500 205 $accent | Out-Null
Add-Arrow $s 415 315 430 275 $accent | Out-Null
Add-Arrow $s 565 315 500 275 $accent | Out-Null
Add-Arrow $s 340 349 430 275 $accent | Out-Null
Add-Arrow $s 650 349 540 275 $accent | Out-Null
Add-Text $s "架构特点：前后端分离、REST 统一接口、数据库对象完整、Agent 作为治理辅助层不绕过权限边界。" 85 418 780 34 13 (RgbInt 63 85 61) $false 2 | Out-Null

# 6 Workflow
$s = New-Slide "slide06.png"
Add-Cover $s 360 68 260 42 (RgbInt 162 190 145) | Out-Null
Add-Text $s "使用流程图：从资产登记到风险整改" 55 48 430 36 22 $deep $true 1 | Out-Null
Add-Panel $s 45 92 870 365 0 | Out-Null
$flow = @(
  @("1 资产登记", "登记数据源、表、字段、样例值", 70, 142),
  @("2 分类分级", "规则自动识别或人工分类，Agent 给出建议", 275, 142),
  @("3 脱敏访问", "L1-L3 展示授权或脱敏值", 500, 142),
  @("4 访问申请", "L4/L5 原始值提交申请", 700, 142),
  @("5 审批决策", "审批人员参考 Agent 建议", 700, 290),
  @("6 审计留痕", "查看、审批、分类动作写入日志", 500, 290),
  @("7 风险分析", "Agent 分析审计日志生成告警", 275, 290),
  @("8 安全报告", "形成整改建议并持续优化规则", 70, 290)
)
foreach ($f in $flow) {
  Add-Box $s ($f[0] + "`r" + $f[1]) $f[2] $f[3] 155 64 (RgbInt 231 241 224) $line $deep 12 $true | Out-Null
}
Add-Arrow $s 225 174 275 174 $line | Out-Null
Add-Arrow $s 430 174 500 174 $line | Out-Null
Add-Arrow $s 655 174 700 174 $line | Out-Null
Add-Arrow $s 777 206 777 290 $orange | Out-Null
Add-Arrow $s 700 322 655 322 $line | Out-Null
Add-Arrow $s 500 322 430 322 $line | Out-Null
Add-Arrow $s 275 322 225 322 $line | Out-Null
Add-Arrow $s 147 290 147 206 $orange | Out-Null
Add-Text $s "闭环关键：高敏数据默认不可直接暴露，审批与审计共同保证可控、可追溯、可整改。" 110 410 740 28 13 (RgbInt 63 85 61) $false 2 | Out-Null

# 7 Database design
$s = New-Slide "slide11.png"
Add-Text $s "数据库设计：六个业务域" 55 50 330 35 23 $deep $true 1 | Out-Null
Add-Panel $s 52 95 845 360 0.05 | Out-Null
$domains = @(
  @("用户权限域", "sys_user / sys_role / sys_permission`rsys_user_role / sys_role_permission"),
  @("数据资产域", "data_source / data_table_asset`rdata_field_asset"),
  @("分类分级域", "classification_category / level / rule`rfield_classification"),
  @("访问控制域", "masking_policy / access_request`rapproval_record"),
  @("审计域", "audit_log"),
  @("Agent 域", "agent_task / agent_recommendation`rrisk_alert / agent_chat_history")
)
for ($i=0; $i -lt $domains.Count; $i++) {
  $x = 80 + ($i % 3) * 270
  $y = 130 + [math]::Floor($i / 3) * 132
  Add-Box $s $domains[$i][0] $x $y 220 34 $green $green (RgbInt 255 255 255) 14 $true | Out-Null
  $b = Add-Box $s $domains[$i][1] $x ($y+38) 220 62 (RgbInt 238 244 232) $line $deep 11 $false
  $b.TextFrame2.TextRange.ParagraphFormat.Alignment = 1
}
Add-Text $s "数据库对象覆盖：20 张表、视图、存储过程、触发器与关键索引，满足课程设计完整性与演示可运行性。" 92 405 760 34 13 (RgbInt 63 85 61) $false 2 | Out-Null

# 8 API and permissions
$s = New-Slide "slide13.png"
Add-Text $s "接口与权限边界" 330 70 320 35 23 $deep $true 2 | Out-Null
Add-Panel $s 290 120 560 300 0.10 | Out-Null
Add-Text $s "统一前缀：/api，登录后携带 Authorization: Bearer <token>" 315 140 510 26 14 $green $true 1 | Out-Null
$apis = @(
  "认证：POST /auth/login，GET /auth/me",
  "资产：/data-sources、/tables、/fields",
  "分类：/field-classifications、/auto-classify",
  "审批：/access-requests、approve / reject",
  "Agent：/agent/classify-field、review-access-request、analyze-audit-logs、security-report、chat"
)
Add-Bullets $s $apis 320 180 500 145 12 | Out-Null
Add-Box $s "角色模型：admin / security_admin / approver / user`r普通用户查看 L4/L5 原始值必须存在有效 APPROVED 记录" 330 345 470 54 (RgbInt 246 239 220) $accent $deep 12 $true | Out-Null

# 9 Agent
$s = New-Slide "slide14.png"
Add-Cover $s 0 0 960 540 (RgbInt 58 94 68) | Out-Null
Add-Cover $s 0 0 960 118 (RgbInt 91 128 82) | Out-Null
Add-Text $s "数据安全治理 Agent" 55 50 330 35 23 (RgbInt 245 249 238) $true 1 | Out-Null
Add-Panel $s 65 105 815 330 0.20 | Out-Null
$agentCaps = @(
  @("字段智能分类", "读取字段名、类型、说明、样例值，输出分类、分级、理由、置信度"),
  @("审批建议", "结合申请人、字段等级、申请理由和历史记录，给出 approve/reject/manual_review"),
  @("审计风险分析", "识别高频敏感访问、L4/L5 访问集中、登录失败、异常 IP 等风险"),
  @("安全报告与问答", "生成 Markdown 治理报告；悬浮 AI 助手支持自然语言查询")
)
for ($i=0; $i -lt $agentCaps.Count; $i++) {
  $x = 95 + ($i % 2) * 390
  $y = 135 + [math]::Floor($i / 2) * 118
  Add-Box $s $agentCaps[$i][0] $x $y 320 34 (RgbInt 65 102 73) $green (RgbInt 255 255 255) 14 $true | Out-Null
  $b = Add-Box $s $agentCaps[$i][1] $x ($y+38) 320 55 (RgbInt 236 243 226) $line $deep 11 $false
  $b.TextFrame2.TextRange.ParagraphFormat.Alignment = 1
}
Add-Text $s "安全约束：Agent 只做分析、建议、解释和报告，不绕过系统权限，不自动批准高风险访问。" 120 392 700 28 13 (RgbInt 237 246 230) $false 2 | Out-Null

# 10 Demo flow
$s = New-Slide "slide15.png"
Add-Text $s "推荐答辩演示流程" 330 57 310 35 23 $deep $true 2 | Out-Null
Add-Panel $s 260 115 590 330 0.08 | Out-Null
$demo = @(
  "admin / 123456 登录，展示首页看板与资产统计",
  "新增 id_card_no 字段，样例值填写身份证格式",
  "进入 Agent 字段智能分类，生成并应用 L4 建议",
  "切换 user 申请访问 L5 字段原始值",
  "approver 查看 Agent 审批建议并处理申请",
  "回到审计日志、风险告警与安全报告页面验证闭环"
)
for ($i=0; $i -lt $demo.Count; $i++) {
  Add-Box $s ("{0}" -f ($i+1)) 295 (145 + $i*43) 30 30 $green $green (RgbInt 255 255 255) 13 $true | Out-Null
  Add-Text $s $demo[$i] 340 (145 + $i*43) 470 30 13 $deep $false 1 | Out-Null
}

# 11 Highlights
$s = New-Slide "slide18.png"
Add-Text $s "项目亮点与验收价值" 55 55 340 35 23 $deep $true 1 | Out-Null
Add-Panel $s 52 105 560 320 0.08 | Out-Null
Add-Bullets $s @(
  "数据库对象完整：表、主外键、索引、视图、存储过程、触发器齐全",
  "规则驱动自动分类：keyword/regex 命中后写入分类分级结果",
  "访问控制闭环：脱敏展示、申请审批、授权访问、审计留痕",
  "Agent 本地规则 fallback：外部模型不可用时仍可离线演示",
  "前后端可运行：Spring Boot 后端与 Vue 3 管理端覆盖核心页面"
) 80 138 500 175 13 | Out-Null
Add-Box $s "答辩定位`r既展示数据库课程设计完整性，也突出数据安全治理场景的业务闭环和工程实现。" 635 160 240 118 (RgbInt 236 243 226) $line $deep 13 $true | Out-Null

# 12 Ending
$s = New-Slide "slide20.png"
Add-Cover $s 575 392 320 92 (RgbInt 90 125 84) | Out-Null
Add-Text $s "感谢观看！" 560 185 260 42 30 (RgbInt 245 249 238) $true 2 | Out-Null
Add-Text $s "数据分类分级保护系统`r课程设计答辩" 560 245 260 55 15 (RgbInt 245 249 238) $false 2 | Out-Null
Add-Text $s "Spring Boot 3 + Vue 3 + MySQL`r数据安全治理 Agent" 580 408 270 42 12 (RgbInt 245 249 238) $false 2 | Out-Null

$pres.SaveAs($outPptx)
$pres.Close()
$ppt.Quit()
Write-Host $outPptx








