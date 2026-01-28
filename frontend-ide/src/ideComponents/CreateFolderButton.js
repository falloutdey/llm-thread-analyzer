import React from 'react';
import axios from 'axios';

const CreateFolderButton = ({ projectId }) => {
  const criarPasta = () => {
    const nomePasta = prompt('Digite o nome da pasta:');

    if (nomePasta) {
      axios.post(`http://localhost:9600/projetos/${projectId}/pastas`, {
        nome: nomePasta
      })
      .then(response => {
        alert(response.data.message);
      })
      .catch(error => {
        alert('Erro ao criar a pasta.');
      });
    }
  };

  return (
    <button onClick={criarPasta}>Criar Pasta</button>
  );
};

export default CreateFolderButton;
