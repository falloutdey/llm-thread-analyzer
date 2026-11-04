# Teste de ponta a ponta da API do tutor (rode com o Spring já de pé na porta 8081).
# Uso (PowerShell, na pasta do projeto):
#   powershell -ExecutionPolicy Bypass -File scripts\teste-tutor.ps1
# Para ver o resumo no final, defina RELATORIO_TOKEN no mesmo terminal em que subiu o Spring
# e também neste terminal: $env:RELATORIO_TOKEN = "algum-segredo"

$ErrorActionPreference = "Stop"
$base = "http://localhost:8081"
$aluno = "teste-local"

# O PowerShell 5 lê a resposta como Latin-1 quando o servidor não informa o charset,
# e os acentos saem trocados (OlÃ¡). Por isso a resposta é decodificada como UTF-8 aqui.
function LerJson($resp) {
    return [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
}

function Post($url, $obj) {
    $json = $obj | ConvertTo-Json -Depth 5
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $resp = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base$url" -ContentType "application/json; charset=utf-8" -Body $bytes
    return LerJson $resp
}

$codigo = @"
public class Contador {
    static int contador = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> { for (int i = 0; i < 10000; i++) contador++; });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 10000; i++) contador++; });
        t1.start();
        t2.start();
        System.out.println(contador);
    }
}
"@

Write-Host "`n== 1. Submissao (analise estatica, sem LLM) ==" -ForegroundColor Cyan
$sub = Post "/api/tutor/submissoes" @{ alunoId = $aluno; sessaoId = "sessao-teste"; fileName = "Contador.java"; content = $codigo; modo = "HIBRIDO" }
Write-Host "submissao $($sub.submissaoId), tentativa $($sub.numeroTentativa), compilou=$($sub.compilou)"
$sub.alertas | ForEach-Object { Write-Host "  alerta $($_.id) [$($_.origem)] $($_.tipo) linha $($_.linha)" }
if (-not $sub.alertas) { Write-Host "Nenhum alerta; fim."; exit }

$alerta = $sub.alertas[0]
Write-Host "`n== 2. Abrir conversa sobre o alerta $($alerta.id) (1a fala do tutor) ==" -ForegroundColor Cyan
$conv = Post "/api/tutor/conversas" @{ alunoId = $aluno; submissaoId = $sub.submissaoId; alertaId = $alerta.id }
$ultima = $conv.mensagens[-1]
Write-Host "[$($ultima.autor) / modelo: $($ultima.modelo)]`n$($ultima.texto)"

Write-Host "`n== 3. Pergunta do aluno ==" -ForegroundColor Cyan
$conv = Post "/api/tutor/conversas/$($conv.conversaId)/mensagens" @{ alunoId = $aluno; texto = "Mas por que o valor nem sempre da 20000? Me passa o codigo certo."; tipo = "PERGUNTA" }
$ultima = $conv.mensagens[-1]
Write-Host "[$($ultima.autor) / modelo: $($ultima.modelo)] restantes: $($conv.mensagensRestantes)`n$($ultima.texto)"

Write-Host "`n== 4. Encerrar ==" -ForegroundColor Cyan
$conv = Post "/api/tutor/conversas/$($conv.conversaId)/encerrar" @{ alunoId = $aluno }
Write-Host "estado: $($conv.estado)"

if ($env:RELATORIO_TOKEN) {
    Write-Host "`n== 5. Resumo ==" -ForegroundColor Cyan
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri "$base/api/relatorios/resumo" -Headers @{ "X-Relatorio-Token" = $env:RELATORIO_TOKEN }
        LerJson $resp | ConvertTo-Json -Depth 6
    } catch {
        Write-Host "Resumo indisponivel: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "403 = o servidor nao recebeu RELATORIO_TOKEN; 401 = token diferente do .env"
    }
}
