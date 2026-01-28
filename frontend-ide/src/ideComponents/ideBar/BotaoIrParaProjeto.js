import React from 'react';
import { useNavigate } from 'react-router-dom';

const BotaoIrParaProjeto = () => {
    const navigate = useNavigate();
  
    const handleClick = () => {
      navigate('/criar-projeto');
    };
  
    return (
      <button onClick={handleClick}>
        Meus Projetos
      </button>
    );
  };

export default BotaoIrParaProjeto;
