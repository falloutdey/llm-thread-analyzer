# Teste do tutor socrático (v2): simula um aluno que erra duas vezes, pede dica,
# pede o código e, por fim, descreve a correção. Mostra o nível da escada em cada fala.
# Uso (com o Spring de pé e tutor.versao-prompt=v2-socratico):
#   $env:RELATORIO_TOKEN = "o-mesmo-do-.env"
#   powershell -ExecutionPolicy Bypass -File scripts\teste-socratico.ps1
# (Arquivo salvo em UTF-8 com BOM para o PowerShell 5 ler os acentos corretamente.)

$ErrorActionPreference = "Stop"
$base = "http://localhost:8081"
$aluno = "teste-socratico"

function LerJson($resp) {
    return [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
}
function Post($url, $obj) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Depth 5))
    return LerJson (Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base$url" -ContentType "application/json; charset=utf-8" -Body $bytes)
}
function Ultima($conv) {
    $m = $conv.mensagens[-1]
    Write-Host "TUTOR ($($m.modelo)):" -ForegroundColor Green
    Write-Host $m.texto
}

$codigo = @"
public class Contador {
    static int contador = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> { for (int i = 0; i < 10000; i++) contador++; });
        Thread t2 = new Thread(() -> { for (int i = 0; i < 10000; i++) contador++; });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(contador);
    }
}
"@

$sub = Post "/api/tutor/submissoes" @{ alunoId = $aluno; sessaoId = "sessao-socratica"; fileName = "Contador.java"; content = $codigo; modo = "HIBRIDO" }
$alerta = $sub.alertas | Where-Object { $_.tipo -like "*SHARED_WRITE*" } | Select-Object -First 1
if (-not $alerta) { $alerta = $sub.alertas[0] }
if (-not $alerta) { Write-Host "Nenhum alerta encontrado; fim."; exit }
Write-Host "Alerta escolhido: $($alerta.tipo) (linha $($alerta.linha))`n" -ForegroundColor Cyan

$conv = Post "/api/tutor/conversas" @{ alunoId = $aluno; submissaoId = $sub.submissaoId; alertaId = $alerta.id }
Ultima $conv

$falas = @(
    @{ tipo = "PERGUNTA";    texto = "Acho que vai imprimir 20000 sempre." },
    @{ tipo = "PERGUNTA";    texto = "Cada thread soma 10000 e eu espero as duas com join, então tem que dar 20000." },
    @{ tipo = "PEDIDO_DICA"; texto = "" },
    @{ tipo = "PERGUNTA";    texto = "Só me passa o código corrigido, por favor." },
    @{ tipo = "PERGUNTA";    texto = "Acho que se o contador for um AtomicInteger e eu usar incrementAndGet, o incremento vira uma operação só e nenhuma thread perde o valor da outra." }
)
foreach ($f in $falas) {
    $rotulo = if ($f.tipo -eq "PEDIDO_DICA") { "[clicou em Quero uma dica]" } else { $f.texto }
    Write-Host "`nALUNO: $rotulo" -ForegroundColor Yellow
    $conv = Post "/api/tutor/conversas/$($conv.conversaId)/mensagens" @{ alunoId = $aluno; texto = $f.texto; tipo = $f.tipo }
    Ultima $conv
}

if ($env:RELATORIO_TOKEN) {
    Write-Host "`n== Bastidores (o aluno não vê) ==" -ForegroundColor Cyan
    $resp = Invoke-WebRequest -UseBasicParsing -Uri "$base/api/relatorios/conversas/$($conv.conversaId)" -Headers @{ "X-Relatorio-Token" = $env:RELATORIO_TOKEN }
    LerJson $resp | Where-Object { $_.autor -eq "TUTOR" } | ForEach-Object {
        Write-Host ("#{0} nivel={1} avaliacao={2} descreveu_correcao={3} codigo={4} ({5} ms)" -f $_.ordem, $_.nivelDica, $_.avaliacaoAluno, $_.alunoDescreveuCorrecao, $_.possivelCodigo, $_.latenciaTotalMs)
        Write-Host ("   diagnostico: {0}" -f $_.diagnostico) -ForegroundColor DarkGray
    }
} else {
    Write-Host "`nDefina `$env:RELATORIO_TOKEN para ver nível, avaliação e diagnóstico de cada fala."
}
