import React from 'react';
import Logo1 from '../img/logo111.png';

const HomeButton = ({ label, onClick }) => {
  return (
    <div>
        {/* Botão 1 */}
    <button className="custom-button" style={{
        background: '#007bff',
        color: '#ffffff',
        border: 'none',
        padding: '10px 20px',
        margin: '5px',
        fontSize: '16px',
        alignItems: 'center',
        textAlign: 'center',
        cursor: 'pointer' }} onClick={onClick}>
            <div style={{  }}>
                <img src={Logo1} alt="Logo" style={{ width: '100px', height: '100px' }} />
            </div>
                <span style={{ fontSize: '18px', marginTop: '5px' }}>OpenMP</span>
        </button>
        {/* Botão 2 */}

    </div>
  );
};

export default HomeButton;
