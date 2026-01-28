import React from 'react';
import { AppBar, Toolbar, Typography, IconButton, Button, Grid } from '@mui/material';
import { Link } from 'react-router-dom';
import HomeIcon from '@mui/icons-material/Home';
import EditIcon from '@mui/icons-material/Edit';
import FileIcon from '@mui/icons-material/FileOpen';
import Stack from '@mui/material/Stack';
import Logo from '../img/logo111.png';
import HomeButton from './HomeButtons';
import Banner from './Banner';
import FeatureSection from './FeatureSection';

const Rodape = () => {
  return (
    <div>
      <footer style={{ backgroundColor: '#253342', padding: '20px', textAlign: 'center', color: '#ffffff'}}>
      <p>&copy; {new Date().getFullYear()} All Right Reserved.</p>
    </footer>
    </div>
  )
}

const HomePage = () => {
  return (
    <div style={{ backgroundColor: '#253342' }}>
            
     <div style={{ marginLeft: '30px', marginRight: '30px',}}>
      <Banner />
     </div>
     <div>
        <FeatureSection/>
     </div>
     <div style={{ marginTop: '30px' }}>
        <Rodape/>
      </div>     
     
    </div>
  );
};

export default HomePage;
