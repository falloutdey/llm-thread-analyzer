import React, { useState, useEffect } from "react";
import MonacoEditor from "react-monaco-editor";
import axios from "axios";
import { Box } from "@mui/system";
import Button from '@mui/material/Button';
import { Typography } from '@mui/material';
import MeuEditorSalvarButton from "./MeuEditorSalvarButton";
import MeuEditorDownloadButton from "./MeuEditorDownloadButton";
import Start from "./../img/start.png"
import Home from "./../img/home.png"
import styled from 'styled-components';
import CpuUsageComponent from "./CpuUsageComponent";
import { Link } from 'react-router-dom';
import MemoryUsageComponent from "./MemoryUsageComponent";

const StyledButton = styled(Button)`
  &:hover {
    background-color: #232226; // Altere para a cor desejada
  }
`;

const CompileButton = ({ onCompile }) => {
  return (
    <StyledButton onClick={onCompile} >
            <img style={{ maxWidth: '30%', height: 'auto' }}
              src={Start}
              alt="Executar"
          />
    </StyledButton>
  );
};

const HomeButton = ({ onCompile }) => {
  return (
    <StyledButton onClick={onCompile} style={{ marginRight: '150px'}}>
      <Link
            to="/homepage"
          >
            <img style={{ maxWidth: '30%', height: 'auto' }}
              src={Home}
              alt="Executar"
          />
          </Link>
    </StyledButton>
  );
};

const CompileResult = ({ compilationResult }) => {
  return (
    <Box mt={2}>
      {compilationResult && (
        <Box
          p={2}
          style={{
            maxHeight: '280px',
            overflowY: 'auto',
            border: '1px solid #ccc',
            borderRadius: '4px',
            marginTop: '-20px',
            borderColor: '#253342',
            backgroundColor: '#253342',
            color: '#ffffff'
          }}
        >
          {compilationResult.error ? (
            <Typography variant="body1" style={{ color: '#ef4444' }}>
              {compilationResult.error}
            </Typography>
          ) : (
            <>
              <Typography variant="h6" style={{ color: '#ffffff', marginBottom: '10px' }}>
                Feedback da Análise de Threads:
              </Typography>
              
              {/* Verifica se o backend devolveu erros de concorrência */}
              {compilationResult.issues && compilationResult.issues.length > 0 ? (
                compilationResult.issues.map((issue, index) => (
                  <Box key={index} p={2} mt={1} style={{ backgroundColor: '#1e293b', borderRadius: '8px' }}>
                     {/* Mensagem fria do SpotBugs */}
                    <Typography variant="body2" style={{ color: '#fca5a5', fontWeight: 'bold' }}>
                      Alerta Técnico (Linha {issue.lineNumber}): {issue.message}
                    </Typography>
                    
                    {/* Explicação didática do LLM */}
                    <Typography variant="body2" style={{ color: '#6ee7b7', marginTop: '8px', whiteSpace: 'pre-wrap' }}>
                      🤖 Professor LLM: {issue.interpretation}
                    </Typography>
                  </Box>
                ))
              ) : (
                <Typography variant="body2" style={{ color: '#a7f3d0' }}>
                  Análise concluída: Nenhum problema de concorrência (threads) detetado no código!
                </Typography>
              )}
            </>
          )}
        </Box>
      )}
    </Box>
  );
};

const MeuEditor = ({ idArquivo, atualizarCaminho }) => {
  const [conteudoArquivo, setConteudoArquivo] = useState("");
  const [windowHeight, setWindowHeight] = useState(window.innerHeight);
  const [caminhoArquivo, setCaminhoArquivo] = useState(null);
  const [compilationResult, setCompilationResult] = useState(null); // Estado para armazenar o resultado da compilação e execução

  useEffect(() => {
    const obterCaminhoArquivo = async (idArquivo) => {
      try {
        const response = await axios.get(
          `http://localhost:5000/api/arquivos/${idArquivo}/caminho`
        );
        return response.data.caminho;
      } catch (error) {
        console.error("Erro ao obter o caminho do arquivo:", error);
        return null;
      }
    };

    const obterConteudoArquivo = async (caminho) => {
      try {
        const response = await axios.get(
          `http://localhost:5000/api/arquivos/conteudo`,
          {
            params: { caminho },
          }
        );
        setConteudoArquivo(response.data.conteudo);
        console.log("Conteúdo do arquivo obtido com sucesso!");
      } catch (error) {
        console.error("Erro ao obter o conteúdo do arquivo:", error);
      }
    };

    const carregarArquivo = async (idArquivo) => {
      const caminho = await obterCaminhoArquivo(idArquivo);
      if (caminho) {
        setCaminhoArquivo(caminho); // Salve o caminho do arquivo no estado
        atualizarCaminho(caminho); // Chame a função de callback para atualizar o caminho no componente pai
        await obterConteudoArquivo(caminho);
      }
    };

    if (idArquivo) {
      carregarArquivo(idArquivo);
    }

    const handleResize = () => {
      setTimeout(() => {
        setWindowHeight(window.innerHeight);
      }, 100); // Definir um atraso de 100ms
    };

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, [idArquivo, atualizarCaminho]);

  const handleChange = (newValue) => {
    setConteudoArquivo(newValue);
  };

  const handleSave = async () => {
    try {
      if (caminhoArquivo) {
        await axios.put(`http://localhost:8080/api/files/arquivos/conteudo`, {
          caminho: caminhoArquivo,
          conteudo: conteudoArquivo,
        });
        console.log("Arquivo salvo com sucesso!");
      } else {
        console.error("Caminho do arquivo não definido.");
      }
    } catch (error) {
      console.error("Erro ao salvar o conteúdo do arquivo:", error);
    }
  };

  const handleCompile = async () => {
    try {
      const response = await axios.post(`http://localhost:8080/api/files/analisar`, {
        fileName: "CodigoAluno.java",
        content: conteudoArquivo,     
      });
      
      setCompilationResult(response.data); 
      console.log("Análise recebida:", response.data);
      
    } catch (error) {
      setCompilationResult({ error: "Erro ao comunicar com o analisador de threads do backend." });
      console.error("Erro ao analisar o código:", error);
    }
  };

  return (
    <div
      style={{
        height: "100vh",
        display: "flex",
        flexDirection: "column",
        background: "#253342",
      }}
    >
      <div style={{ display: "flex", background: "#3e5954", padding: "10px", justifyContent: 'center', alignItems: 'center'}}>
        {/* <HomeButton/> */}
        <MeuEditorSalvarButton onClick={handleSave} />
        <MeuEditorDownloadButton
          conteudo={conteudoArquivo}
          caminhoArquivo={caminhoArquivo}
        />
        <CompileButton onCompile={handleCompile} />
        
        {/* componentes com os dados do processador  */}
        <div style={{ display: 'flex', justifyContent: 'right', marginRight: 'right'}}>
          <CpuUsageComponent />
          <MemoryUsageComponent/>
        </div>
      </div>
      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <MonacoEditor
          width="100%"
          height="calc(100vh - 400px)" // Ajuste a altura para que o editor não ocupe todo o espaço disponível
          language="cpp"
          theme="vs-dark"
          value={conteudoArquivo}
          onChange={handleChange}
          options={{
            lineNumbers: "on",
            roundedSelection: false,
            scrollBeyondLastLine: false,
            readOnly: false,
            theme: "vs-dark",
            wordWrap: "on",
            handleMouseWheel: true,
            scrollbar: {
              vertical: "visible",
              horizontal: "visible",
            },
            selectOnLineNumbers: true,
          }}
        />
        <div style={{ padding: "10px" }}>
          <CompileResult compilationResult={compilationResult} />
        </div>
      </div>
    </div>
  );
};

export default MeuEditor;
