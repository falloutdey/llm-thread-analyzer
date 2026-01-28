import React from "react";
import Button from "@mui/material/Button";
import DowloadImg from "./../img/download.png"
import styled from 'styled-components';

const StyledButton = styled(Button)`
  &:hover {
    background-color: #232226; // Altere para a cor desejada
  }
`;

const MeuEditorDownloadButton = ({ conteudo, caminhoArquivo }) => {
  const handleDownload = () => {
    if (conteudo && caminhoArquivo) {
      // Cria um objeto Blob com o conteúdo do arquivo
      const blob = new Blob([conteudo], { type: "text/plain" });

      // Cria uma URL temporária para o Blob
      const url = window.URL.createObjectURL(blob);

      // Cria um link HTML temporário para iniciar o download
      const a = document.createElement("a");
      a.href = url;

      // Define o nome do arquivo baseado no caminho do arquivo
      const fileName = caminhoArquivo.split("/").pop();
      a.download = fileName;

      // Adiciona o link ao DOM, clica nele e remove-o do DOM
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);

      // Libera a URL temporária
      window.URL.revokeObjectURL(url);
    } else {
      console.error("Conteúdo ou caminho do arquivo não está definido.");
    }
  };

  return (
    <StyledButton onClick={handleDownload} >
      <img style={{ maxWidth: '30%', height: 'auto' }}
        src={DowloadImg}
        alt="Download"
      />
    </StyledButton>
  );
};

export default MeuEditorDownloadButton;
