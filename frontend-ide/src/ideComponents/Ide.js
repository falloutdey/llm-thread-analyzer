import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import MeuEditor from './MeuEditor';
import { Link } from 'react-router-dom';

const Ide = () => {
  const { idArquivo } = useParams(); // Pegue o idArquivo da URL
  const [caminhoArquivo, setCaminhoArquivo] = useState(null);

  const atualizarCaminhoArquivo = (novoCaminho) => {
    setCaminhoArquivo(novoCaminho);
  };

  return (
    <div>
    
    <div style={{ height: '96vh', background: '#253354', borderRadius: '15px'}}>
          <MeuEditor idArquivo={idArquivo} atualizarCaminho={atualizarCaminhoArquivo} />
          
          {/* Coloque aqui o outro componente que deseja na segunda coluna */}
          <div>
            {/* <h2>Outro Componente</h2>
            <p>Conteúdo do outro componente.</p> */}
          </div>
    </div>
    </div>
  );
};

export default Ide;
