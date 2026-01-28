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
            maxHeight: '280px', // Defina a altura máxima da caixa
            overflowY: 'auto', // Ativa a barra de rolagem vertical se necessário
            border: '1px solid #ccc', // Opcional: adiciona uma borda para visualização
            borderRadius: '4px', // Opcional: arredonda os cantos da borda
            marginTop: '-20px',
            borderColor: '#253342',
            backgroundColor: '#253342', // Opcional: cor de fundo para a caixa
            color: '#ffffff'
          }}
        >
          <Typography variant="body1" color={compilationResult.error ? 'error' : 'textPrimary'} style={{ color: compilationResult.error ? undefined : '#ffffff' }}>
            {compilationResult.error ? compilationResult.error : 'Compilação e execução bem-sucedidas!'}
          </Typography>
          {compilationResult.stdout && (
            <Box mt={1}>
              <Typography variant="body2" color="#ffffff">
                Saída da Execução:
              </Typography>
              <Typography variant="body2" component="pre">
                {compilationResult.stdout}
              </Typography>
            </Box>
          )}
          {compilationResult.stderr && (
            <Box mt={1}>
              <Typography variant="body2" color="textSecondary">
                Erros da Execução:
              </Typography>
              <Typography variant="body2" component="pre">
                {compilationResult.stderr}
              </Typography>
            </Box>
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
        await axios.put(`http://localhost:5000/api/files/arquivos/conteudo`, {
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
      const response = await axios.post(`http://localhost:5000/api/compile`, {
        caminho: caminhoArquivo,
      });
      setCompilationResult(response.data); // Armazena a resposta da compilação e execução
      console.log("Resposta da compilação:", response.data);
    } catch (error) {
      setCompilationResult({ error: "Erro ao compilar e executar o arquivo" });
      console.error("Erro ao compilar e executar o arquivo:", error);
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
