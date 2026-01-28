import React from 'react';
import Button from '@mui/material/Button';
import Save from "./../img/save.png"
import styled from 'styled-components';

const StyledButton = styled(Button)`
  &:hover {
    background-color: #232226; // Altere para a cor desejada
  }
`;

const MeuEditorSalvarButton = ({ onClick }) => {
  return (
    <StyledButton onClick={onClick}>
     <img style={{ maxWidth: '30%', height: 'auto' }}
        src={Save}
        alt="Download"
      />
    </StyledButton>
  );
};

export default MeuEditorSalvarButton;
