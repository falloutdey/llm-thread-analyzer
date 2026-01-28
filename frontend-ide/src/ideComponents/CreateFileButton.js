import React from 'react';
import axios from 'axios';

const CreateFileButton = ({ projectId }) => {
  const criarArquivo = () => {
    const nomeArquivo = prompt('Digite o nome do arquivo:');
    const conteudoArquivo = prompt('Digite o conteúdo do arquivo:');

    if (nomeArquivo && conteudoArquivo) {
      axios.post(`http://localhost:5000/api/projects/projetos/${projectId}/arquivos`, {
        nome: nomeArquivo,
        conteudo: conteudoArquivo
      })
      .then(response => {
        alert(response.data.message);
      })
      .catch(error => {
        alert('Erro ao criar o arquivo.');
      });
    }
  };

  return (
    <button onClick={criarArquivo}>Criar Arquivo</button>
  );
};

export default CreateFileButton;
