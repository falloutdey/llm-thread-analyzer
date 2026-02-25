import React from 'react';

const CreateFileButton = ({ projectId }) => {
  const criarArquivo = () => {
    const nomeArquivo = prompt('Digite o nome do arquivo (ex: Teste.java, main.c):');

    if (nomeArquivo) {
      const nomeLower = nomeArquivo.toLowerCase();
      // Validação das extensões permitidas
      if (nomeLower.endsWith('.c') || nomeLower.endsWith('.cpp') || nomeLower.endsWith('.java')) {
        
        // Como o banco de dados está off, vamos apenas avisar que o sistema
        // está pronto para simular esse arquivo no editor.
        alert(`Arquivo "${nomeArquivo}" preparado! Como o banco de dados está desligado, use o editor para escrever seu código.`);
        
        // Aqui, para o seu TCC, o ideal é que você já tenha o Editor aberto
        // e mude o nome do arquivo no estado do componente pai.
      } else {
        alert('Extensão inválida! Use .c, .cpp ou .java');
      }
    }
  };

  return (
    <button 
      onClick={criarArquivo}
      style={{
        padding: '10px',
        backgroundColor: '#3e5954',
        color: 'white',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer'
      }}
    >
      Criar Arquivo
    </button>
  );
};

export default CreateFileButton;