import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import PaginaPrincipal from './components/PaginaPrincipal';
import Cadastro from './components/Cadastro';
import Login from './components/Login';
import ResetPassword from './components/ResetPassword';
import RotaInicio from './components/RotaInicio';
import RotaDesenvolvimento from './components/RotaDesenvolvimento';
import RotaSala from './components/RotaSala';
import RotaJogo from './components/RotaJogo';
import RotaConfigurar from './components/RotaConfigurar';
import CriarCodigoMP from './components/CriarCodigoMP';

import EditorOpenMP from './components/EditorOpenMP';
import EditorOpenMPI from './components/EditorOpenMPI';
import CriarCodigoMPI from './components/CriarCodigoMPI';
import CriarCodigoACC from './components/CriarCodigoACC';
import EditorOpenACC from './components/EditorOpenACC';
import ArquivosACC from './components/ArquivosACC';
import ProjetosOpenMP from './components/ProjetosOpenMP';
import ProjetosOpenMPI from './components/ProjetosOpenMPI';
import PoliticaPrivacidade from './components/PoliticaPrivacidade';
import SobreSistema from './components/SobreSistema';
import TermosDeUso from './components/TermosDeUso';
import FaleConosco from './components/FaleConosco';
import CentralAjuda from './components/CentralAjuda';
import Tutoriais from './components/Tutoriais';
import ExemplosOpenMP from './components/ExemplosOpenMP';
import ExemplosOpenMPI from './components/ExemplosOpenMPI';
import ExemplosOpenACC from './components/ExemplosOpenACC';

const App = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const PrivateRoute = ({ children }) => {
    return isAuthenticated ? children : <Navigate to="/" />;
  };
  return (
    <BrowserRouter>
      <Routes>
        {/* Rotas funcionando */}
        <Route path="/" element={<PaginaPrincipal />} />
        <Route path="/registro" element={<Cadastro />} />
        <Route path="/login" element={<Login onLogin={setIsAuthenticated} />} />
        <Route path="/recuperar-senha" element={<ResetPassword />} />

        <Route path="/inicio" element={<RotaInicio />} /> 
        <Route path="/desenvolvimento" element={<RotaDesenvolvimento />} /> 
        <Route path="/sala" element={<RotaSala />} /> 
        <Route path="/jogos" element={<RotaJogo />} /> 
        <Route path="/configuracoes" element={<RotaConfigurar />} /> 

        <Route path="/privacidade" element={<PoliticaPrivacidade />} /> 
        <Route path="/sobre" element={<SobreSistema />} /> 
        <Route path="/termos" element={<TermosDeUso />} /> 
        <Route path="/contato" element={<FaleConosco />} /> 
        <Route path="/ajuda" element={<CentralAjuda />} /> 
        <Route path="/tutoriais" element={<Tutoriais />} /> 


        {/* Rotas do OpenMP */}
        <Route path="/criar-openmp" element={<CriarCodigoMP />} />
        <Route path="/openmp-examples" element={<ExemplosOpenMP />} />
        <Route path="/projetos-openmp" element={<ProjetosOpenMP />} /> 
        <Route path="/editor-open-mp/:idProjeto/:idArquivo" element={<EditorOpenMP />} />

        {/* Rotas do OpenMPi */}
        <Route path="/criar-open-mpi" element={<CriarCodigoMPI />} />
        <Route path="/openmpi-examples" element={<ExemplosOpenMPI />} />
        <Route path="/projetos-open-mpi" element={<ProjetosOpenMPI />} /> 
        <Route path="/editor-open-mpi/:idProjeto/:idArquivo" element={<EditorOpenMPI />} />

        {/* Rotas do OpenACC */}
        <Route path="/criar-open-acc" element={<CriarCodigoACC />} />
        <Route path="/openacc-examples" element={<ExemplosOpenACC />} />
        <Route path="/projetos-open-acc" element={<PrivateRoute><ArquivosACC /></PrivateRoute>} /> 
        <Route path="/editor-open-acc/:idProjeto/:idArquivo" element={<EditorOpenACC />} />
       
      </Routes>
    </BrowserRouter>
  );
};
export default App;