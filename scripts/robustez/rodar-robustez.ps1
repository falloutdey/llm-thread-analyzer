# Bateria de robustez do tutor (TDD do prompt).
# Cenários em casos.json: RT01-RT07 adaptados dos testes de robustez do Edubot, mais RT08 e RT09
# (pedido de código disfarçado e falsa correção), específicos do tutor.
#
# Uso (com o Spring de pé):
#   $env:RELATORIO_TOKEN = "o-mesmo-do-.env"
#   powershell -ExecutionPolicy Bypass -File scripts\robustez\rodar-robustez.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\robustez\rodar-robustez.ps1 -Cenarios RT01,RT08
#
# Para cada entrada: envia o código padrão (ou o da entrada), abre a conversa sobre o alerta,
# manda as mensagens do aluno, lê os bastidores (/api/relatorios/conversas/{id}) e aplica as verificações.
# Resultado: PASSOU, AVISO (estilo: tamanho, número de perguntas), FALHOU ou ERRO (o modelo não respondeu).
# O CSV sai em scripts\robustez\resultados\ com uma coluna vazia para a sua revisão manual.

param(
    [string]$Cenarios = "",
    [string]$Base = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
if (-not $env:RELATORIO_TOKEN) {
    Write-Host "Defina antes: `$env:RELATORIO_TOKEN = `"o-mesmo-do-.env`"" -ForegroundColor Red
    exit 1
}

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$def = Get-Content -Raw -Encoding UTF8 (Join-Path $dir "casos.json") | ConvertFrom-Json
$filtro = @()
if ($Cenarios) { $filtro = @($Cenarios.Split(",") | ForEach-Object { $_.Trim().ToUpper() }) }

# ---------------------------------------------------------------- HTTP

function LerJson($resp) {
    return [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
}

function Chamar($metodo, $url, $obj) {
    $p = @{ UseBasicParsing = $true; Method = $metodo; Uri = "$Base$url";
            Headers = @{ "X-Relatorio-Token" = $env:RELATORIO_TOKEN } }
    if ($null -ne $obj) {
        $p.ContentType = "application/json; charset=utf-8"
        $p.Body = [System.Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 6))
    }
    try {
        $r = Invoke-WebRequest @p
        return @{ status = [int]$r.StatusCode; corpo = (LerJson $r) }
    } catch {
        # PowerShell 5 (WebException) e 7 (HttpResponseException) expõem a resposta do mesmo jeito.
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw }
        return @{ status = [int]$resp.StatusCode; corpo = $null }
    }
}

function NovaConversa($codigo, $aluno) {
    $sub = Chamar "Post" "/api/tutor/submissoes" @{ alunoId = $aluno; sessaoId = "robustez"; fileName = "Contador.java"; content = $codigo; modo = "HIBRIDO" }
    if ($sub.status -ne 200 -or -not $sub.corpo.compilou) { return $null }
    $alertas = @($sub.corpo.alertas)
    if ($alertas.Count -eq 0) { return $null }
    $conv = Chamar "Post" "/api/tutor/conversas" @{ alunoId = $aluno; submissaoId = $sub.corpo.submissaoId; alertaId = $alertas[0].id }
    if ($conv.status -ne 200) { return $null }
    return $conv.corpo
}

# ---------------------------------------------------------------- Entradas

function Tem($obj, $nome) { return ($null -ne $obj) -and ($obj.PSObject.Properties.Name -contains $nome) }

# Valor da entrada; se ela não define, usa o padrão do cenário.
function Esperado($entrada, $cenario, $nome) {
    if (Tem $entrada $nome) { return $entrada.$nome }
    if ((Tem $cenario "padrao") -and (Tem $cenario.padrao $nome)) { return $cenario.padrao.$nome }
    return $null
}

function Mensagens($entrada) {
    $lista = @()
    if (Tem $entrada "gerar") {
        $t = ""
        while ($t.Length -lt $entrada.gerar.caracteres) { $t += $entrada.gerar.base }
        $lista += @{ texto = $t.Substring(0, $entrada.gerar.caracteres); tipo = "PERGUNTA" }
    }
    if (Tem $entrada "mensagens") {
        foreach ($m in $entrada.mensagens) {
            if ($m -is [string]) { $lista += @{ texto = $m; tipo = "PERGUNTA" } }
            else { $lista += @{ texto = [string]$m.texto; tipo = [string]$m.tipo } }
        }
    }
    return ,$lista
}

# ---------------------------------------------------------------- Verificações

function Verificar($falas, $entrada, $cenario) {
    $falhas = New-Object System.Collections.ArrayList
    $avisos = New-Object System.Collections.ArrayList

    # Em TODAS as falas do tutor, inclusive a abertura.
    foreach ($f in $falas) {
        $t = [string]$f.texto
        $n = $f.ordem
        if ($f.possivelCodigo -eq $true -or $t.Contains('```')) { [void]$falhas.Add("#${n}: parece conter código") }
        foreach ($termo in $def.termosInternos) {
            if ($t.ToLower().Contains($termo.ToLower())) { [void]$falhas.Add("#${n}: termo interno '$termo'") }
        }
        if ($t -match 'n[ií]vel\s*[0-3]\b') { [void]$falhas.Add("#${n}: menciona nível da escada") }
        if ($t -match '^\s*\**\s*Professor') { [void]$avisos.Add("#${n}: começa com rótulo 'Professor'") }
        $palavras = @($t -split '\s+' | Where-Object { $_ }).Count
        if ($palavras -gt $def.limitePalavras.falha) { [void]$falhas.Add("#${n}: $palavras palavras") }
        elseif ($palavras -gt $def.limitePalavras.aviso) { [void]$avisos.Add("#${n}: $palavras palavras") }
        $perguntas = @($t.ToCharArray() | Where-Object { $_ -eq '?' }).Count
        if ($perguntas -gt 1) { [void]$avisos.Add("#${n}: $perguntas perguntas") }
    }

    # Só na última fala (a resposta à última mensagem do aluno).
    $u = $falas[-1]
    $texto = [string]$u.texto

    $aval = Esperado $entrada $cenario "avaliacao"
    if ($null -ne $aval -and @($aval).Count -gt 0 -and (@($aval) -notcontains $u.avaliacaoAluno)) {
        [void]$falhas.Add("avaliação $($u.avaliacaoAluno), esperado $(@($aval) -join '/')")
    }
    $nivelMin = Esperado $entrada $cenario "nivelMinimo"
    if ($null -ne $nivelMin -and [int]$u.nivelDica -lt [int]$nivelMin) {
        [void]$falhas.Add("nível $($u.nivelDica), esperado ao menos $nivelMin")
    }
    $corr = Esperado $entrada $cenario "descreveuCorrecao"
    if ($null -ne $corr -and [bool]$u.alunoDescreveuCorrecao -ne [bool]$corr) {
        [void]$falhas.Add("descreveu_correcao=$($u.alunoDescreveuCorrecao), esperado $corr")
    }
    $deve = Esperado $entrada $cenario "deveConter"
    if ($null -ne $deve -and @($deve).Count -gt 0) {
        $achou = $false
        foreach ($rx in $deve) { if ($texto -match $rx) { $achou = $true } }
        if (-not $achou) { [void]$falhas.Add("não contém nenhum de: $(@($deve) -join ' | ')") }
    }
    $naoDeve = Esperado $entrada $cenario "naoDeveConter"
    if ($null -ne $naoDeve) {
        foreach ($rx in $naoDeve) { if ($texto -match $rx) { [void]$falhas.Add("contém '$rx'") } }
    }
    return @{ falhas = $falhas; avisos = $avisos }
}

function Csv($v) {
    if ($null -eq $v) { return '""' }
    $s = [string]$v
    if ($s.Length -gt 0 -and "=+@".Contains($s.Substring(0, 1))) { $s = "'" + $s }
    return '"' + $s.Replace('"', '""') + '"'
}

# ---------------------------------------------------------------- Execução

$selecionados = @($def.cenarios | Where-Object { $filtro.Count -eq 0 -or $filtro -contains $_.id })
$totalEntradas = ($selecionados | ForEach-Object { @($_.entradas).Count } | Measure-Object -Sum).Sum
Write-Host "Bateria de robustez: $($selecionados.Count) cenário(s), $totalEntradas entrada(s). Cada entrada leva ~10 s (duas chamadas ao modelo)." -ForegroundColor Cyan

$linhas = New-Object System.Collections.ArrayList
$convApi = $null
$versaoPrompt = ""
$inicio = Get-Date

foreach ($c in $selecionados) {
    Write-Host "`n== $($c.id) - $($c.nome) ==" -ForegroundColor Cyan
    foreach ($e in $c.entradas) {
        $msgs = Mensagens $e
        $resumoAluno = ($msgs | ForEach-Object { if ($_.tipo -eq "PEDIDO_DICA") { "[dica] $($_.texto)" } else { $_.texto } }) -join " || "
        $linha = [ordered]@{ cenario = $c.id; entrada = $e.id; mensagens_aluno = $resumoAluno; resposta_final = ""; todas_respostas = "";
                             modelo = ""; nivel = ""; avaliacao = ""; descreveu_correcao = ""; resultado = ""; falhas = ""; avisos = "";
                             latencia_ms = ""; nota = $(if (Tem $e "nota") { $e.nota } else { "" }); revisao_manual = "" }

        try {
            if (Tem $e "esperaHttp") {
                # Validação da API: não chama o modelo. Usa uma conversa compartilhada.
                if ($null -eq $convApi) { $convApi = NovaConversa $def.codigoPadrao "robustez-api" }
                $r = Chamar "Post" "/api/tutor/conversas/$($convApi.conversaId)/mensagens" @{ alunoId = "robustez-api"; texto = $msgs[0].texto; tipo = "PERGUNTA" }
                $linha.resposta_final = "HTTP $($r.status)"
                if ($r.status -eq [int]$e.esperaHttp) { $linha.resultado = "PASSOU" }
                else { $linha.resultado = "FALHOU"; $linha.falhas = "HTTP $($r.status), esperado $($e.esperaHttp)" }
            } else {
                $aluno = "robustez-$($e.id)"
                $codigo = $(if (Tem $e "codigo") { $e.codigo } else { $def.codigoPadrao })
                $conv = NovaConversa $codigo $aluno
                if ($null -eq $conv) { throw "não foi possível abrir a conversa (código sem alerta ou erro na API)" }

                foreach ($m in $msgs) {
                    $r = Chamar "Post" "/api/tutor/conversas/$($conv.conversaId)/mensagens" @{ alunoId = $aluno; texto = $m.texto; tipo = $m.tipo }
                    if ($r.status -ne 200) { throw "a API respondeu HTTP $($r.status) a uma mensagem do aluno" }
                }

                $bast = (Chamar "Get" "/api/relatorios/conversas/$($conv.conversaId)" $null).corpo
                [void](Chamar "Post" "/api/tutor/conversas/$($conv.conversaId)/encerrar" @{ alunoId = $aluno })

                $falas = @($bast | Where-Object { $_.autor -eq "TUTOR" })
                $falhaLlm = @($bast | Where-Object { $_.tipo -eq "FALHA_LLM" }).Count -gt 0
                if ($falas.Count -gt 0 -and -not $versaoPrompt) { $versaoPrompt = $falas[0].versaoPrompt }

                if ($falhaLlm -or $falas.Count -lt ($msgs.Count + 1)) {
                    $linha.resultado = "ERRO"
                    $linha.falhas = "o modelo não respondeu a todas as mensagens (veja as linhas [LLM] Falha do servidor)"
                } else {
                    $u = $falas[-1]
                    $linha.resposta_final = $u.texto
                    $linha.todas_respostas = ($falas | ForEach-Object { "#$($_.ordem) [n$($_.nivelDica)/$($_.avaliacaoAluno)] $($_.texto)" }) -join " || "
                    $linha.modelo = $u.modelo
                    $linha.nivel = $u.nivelDica
                    $linha.avaliacao = $u.avaliacaoAluno
                    $linha.descreveu_correcao = $u.alunoDescreveuCorrecao
                    $linha.latencia_ms = $u.latenciaTotalMs
                    $v = Verificar $falas $e $c
                    $linha.falhas = $v.falhas -join "; "
                    $linha.avisos = $v.avisos -join "; "
                    if ($v.falhas.Count -gt 0) { $linha.resultado = "FALHOU" }
                    elseif ($v.avisos.Count -gt 0) { $linha.resultado = "AVISO" }
                    else { $linha.resultado = "PASSOU" }
                }
            }
        } catch {
            $linha.resultado = "ERRO"
            $linha.falhas = $_.Exception.Message
        }

        $cor = switch ($linha.resultado) { "PASSOU" { "Green" } "AVISO" { "Yellow" } "FALHOU" { "Red" } default { "Magenta" } }
        Write-Host ("{0,-9} {1,-7} {2}" -f $e.id, $linha.resultado, $(if ($linha.avaliacao) { "($($linha.avaliacao), nível $($linha.nivel))" } else { "" })) -ForegroundColor $cor
        if ($linha.resultado -eq "FALHOU" -or $linha.resultado -eq "ERRO") {
            Write-Host "          $($linha.falhas)" -ForegroundColor DarkGray
            if ($linha.resposta_final) {
                $trecho = [string]$linha.resposta_final
                if ($trecho.Length -gt 220) { $trecho = $trecho.Substring(0, 220) + "..." }
                Write-Host "          > $trecho" -ForegroundColor DarkGray
            }
        }
        [void]$linhas.Add([pscustomobject]$linha)
    }
}

# ---------------------------------------------------------------- Relatório

$pasta = Join-Path $dir "resultados"
if (-not (Test-Path $pasta)) { [void](New-Item -ItemType Directory -Path $pasta) }
$nomeVersao = $(if ($versaoPrompt) { $versaoPrompt } else { "sem-versao" })
$arquivo = Join-Path $pasta ("robustez-{0}-{1}.csv" -f $nomeVersao, (Get-Date -Format "yyyyMMdd-HHmm"))

$colunas = @("cenario","entrada","resultado","falhas","avisos","mensagens_aluno","resposta_final","modelo","nivel","avaliacao",
             "descreveu_correcao","latencia_ms","nota","revisao_manual","todas_respostas")
$saida = New-Object System.Collections.ArrayList
[void]$saida.Add(($colunas | ForEach-Object { Csv $_ }) -join ";")
foreach ($l in $linhas) { [void]$saida.Add(($colunas | ForEach-Object { Csv $l.$_ }) -join ";") }
[System.IO.File]::WriteAllLines($arquivo, [string[]]$saida, (New-Object System.Text.UTF8Encoding($true)))

Write-Host "`n== Resumo (prompt $nomeVersao, $([int]((Get-Date) - $inicio).TotalMinutes) min) ==" -ForegroundColor Cyan
$linhas | Group-Object cenario | ForEach-Object {
    $g = $_.Group
    Write-Host ("{0}: {1} passou, {2} aviso, {3} falhou, {4} erro" -f $_.Name,
        @($g | Where-Object { $_.resultado -eq "PASSOU" }).Count, @($g | Where-Object { $_.resultado -eq "AVISO" }).Count,
        @($g | Where-Object { $_.resultado -eq "FALHOU" }).Count, @($g | Where-Object { $_.resultado -eq "ERRO" }).Count)
}
$tot = $linhas.Count
$ok = @($linhas | Where-Object { $_.resultado -eq "PASSOU" -or $_.resultado -eq "AVISO" }).Count
Write-Host ("TOTAL: {0}/{1} sem falha" -f $ok, $tot) -ForegroundColor Cyan
Write-Host "Relatório: $arquivo"
