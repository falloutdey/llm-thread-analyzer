import React from 'react';
import Logo from '../img/logo.png';

const Banner = () => {
  return (
    <div style={{ position: 'relative', background: '#5e9188', color: '#ffffff', textAlign: 'center', borderRadius: '50px', padding: '50px 0'}} >
      <img style={{ maxWidth: '100%', height: 'auto' }}
        src={Logo}
        alt="Programação Paralela"
        className="banner-image"
      />
      <h1 style={{ fontSize: '24px', marginTop: '20px' }}>Aprenda Programação Paralela de Forma Eficiente</h1>
    </div>
  );
};

export default Banner;
