import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import axios from 'axios';
import MeuEditor from '../ideComponents/MeuEditor';
import { Typography, Box } from '@mui/material';

const EditorJavaThreads = () => {
  const { idProjeto, idArquivo } = useParams();
  const [caminhoArquivo, setCaminhoArquivo] = useState(null);
  
  // Estados para gerir o código e a resposta da API
  const [codigoFonte, setCodigoFonte] = useState(""); 
  const [resultadoAnalise, setResultadoAnalise] = useState(null); 
  const [isAnalisando, setIsAnalisando] = useState(false);

  // Função que envia o código para o seu backend Spring Boot
  const analisarCodigo = async () => {
    if (!codigoFonte || !codigoFonte.trim()) {
      alert("Por favor, escreva algum código Java antes de analisar.");
      return;
    }

    setIsAnalisando(true);
    setResultadoAnalise(null); // Limpa análises anteriores

    try {
      // Faz o POST para a API do seu TCC
      const response = await axios.post(`http://localhost:8081/api/files/analisar`, {
        fileName: "CodigoAluno.java", // Nome fictício para a análise em memória
        content: codigoFonte,     
      });
      
      console.log("Resposta do LLM/SpotBugs:", response.data);
      setResultadoAnalise(response.data);

    } catch (error) {
      console.error("Erro ao comunicar com a API:", error);
      setResultadoAnalise({ error: "Erro de ligação. Verifique se o backend (Spring Boot) está a correr na porta 8081." });
    } finally {
      setIsAnalisando(false);
    }
  };

  return (
    <div style={{ height: '100vh', background: '#253342', padding: '10px', boxSizing: 'border-box' }}>
      
      {/* Cabeçalho da Ferramenta */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px', background: '#3e5954', padding: '10px', borderRadius: '8px' }}>
        <h2 style={{ color: '#fff', margin: 0, fontSize: '1.5rem' }}>Analisador de Threads Java</h2>
        <button 
          onClick={analisarCodigo}
          disabled={isAnalisando}
          style={{ 
            padding: '10px 20px', 
            background: isAnalisando ? '#94a3b8' : '#10b981', 
            color: '#fff', 
            border: 'none', 
            borderRadius: '4px', 
            cursor: isAnalisando ? 'not-allowed' : 'pointer',
            fontWeight: 'bold',
            fontSize: '1rem'
          }}
        >
          {isAnalisando ? 'A Processar Análise...' : 'Executar e Analisar Threads'}
        </button>
      </div>

      <div style={{ height: 'calc(100% - 70px)', borderRadius: '8px', overflow: 'hidden' }}>
        <PanelGroup direction="horizontal">
          
          {/* PAINEL ESQUERDO: Editor de Código */}
          <Panel defaultSize={55} minSize={30}>
            <div style={{ height: '100%', background: '#1e1e1e', overflow: 'hidden' }}>
              <MeuEditor 
                idArquivo={idArquivo} 
                atualizarCaminho={setCaminhoArquivo} 
                // A magia acontece aqui: recebemos o código sempre que o aluno digita
                onChange={(novoCodigo) => setCodigoFonte(novoCodigo)} 
              />
            </div>
          </Panel>
          
          {/* Divisor Redimensionável */}
          <PanelResizeHandle style={{ width: '8px', background: '#253342', cursor: 'col-resize' }} />
          
          {/* PAINEL DIREITO: Feedback e Terminal */}
          <Panel defaultSize={45} minSize={20}>
            <PanelGroup direction="vertical">
              
              {/* TOPO DIREITO: Feedback Educacional do LLM + SpotBugs */}
              <Panel defaultSize={65} minSize={30}>
                <div style={{ height: '100%', background: '#1e293b', padding: '20px', color: '#e2e8f0', overflowY: 'auto', borderRadius: '0 8px 0 0' }}>
                  <Typography variant="h5" style={{ color: '#60a5fa', marginBottom: '15px', fontWeight: 'bold' }}>
                    💡 Feedback Educacional
                  </Typography>
                  
                  {isAnalisando ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#94a3b8' }}>
                      <p>A submeter o código ao analisador estático e a gerar explicação didática...</p>
                    </div>
                  ) : resultadoAnalise ? (
                    resultadoAnalise.error ? (
                      <p style={{ color: '#ef4444' }}>{resultadoAnalise.error}</p>
                    ) : resultadoAnalise.issues && resultadoAnalise.issues.length > 0 ? (
                      resultadoAnalise.issues.map((issue, index) => (
                        <Box key={index} p={2} mb={2} style={{ backgroundColor: '#0f172a', borderLeft: '4px solid #f87171', borderRadius: '4px' }}>
                          <Typography variant="subtitle2" style={{ color: '#fca5a5', fontWeight: 'bold' }}>
                            Alerta Técnico (Linha {issue.lineNumber}): {issue.message}
                          </Typography>
                          <Typography variant="body1" style={{ color: '#a7f3d0', marginTop: '10px', whiteSpace: 'pre-wrap', lineHeight: '1.6' }}>
                            🤖 Professor LLM: {issue.interpretation}
                          </Typography>
                        </Box>
                      ))
                    ) : (
                      <Box p={2} style={{ backgroundColor: 'rgba(16, 185, 129, 0.1)', borderLeft: '4px solid #10b981', borderRadius: '4px' }}>
                        <Typography variant="body1" style={{ color: '#34d399', fontWeight: 'bold' }}>
                          Análise concluída com sucesso! Nenhum problema de concorrência ou deadlock detetado no código.
                        </Typography>
                      </Box>
                    )
                  ) : (
                    <p style={{ color: '#94a3b8', fontSize: '1.1rem', lineHeight: '1.5' }}>
                      Bem-vindo! Escreva o seu código Java que utiliza múltiplas threads à esquerda e clique em <b>"Executar e Analisar Threads"</b>. 
                      <br/><br/>
                      O sistema irá procurar por condições de corrida (race conditions), deadlocks e sugerir as melhores práticas para a programação concorrente.
                    </p>
                  )}
                </div>
              </Panel>
              
              <PanelResizeHandle style={{ height: '8px', background: '#253342', cursor: 'row-resize' }} />
              
              {/* BASE DIREITA: Terminal / Consola */}
              <Panel defaultSize={35} minSize={20}>
                <div style={{ height: '100%', background: '#000000', padding: '15px', color: '#10b981', fontFamily: 'monospace', borderRadius: '0 0 8px 0', overflowY: 'auto' }}>
                  <Typography variant="subtitle2" style={{ color: '#64748b', marginBottom: '10px', textTransform: 'uppercase' }}>
                    Terminal de Saída (Output)
                  </Typography>
                  <p style={{ margin: 0 }}>$ A aguardar execução do código Java...</p>
                </div>
              </Panel>

            </PanelGroup>
          </Panel>

        </PanelGroup>
      </div>
    </div>
  );
};

export default EditorJavaThreads;