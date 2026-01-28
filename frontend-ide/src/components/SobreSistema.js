import React from 'react';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css'; // Reusing existing styles

const SobreSistema = () => {
  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Sobre o Sistema</h1>
          <p className="subtitle">Conheça nossa plataforma de aprendizado em programação paralela</p>
        </section>

        <section className="content-section sobre-sistema">
          <div className="sobre-item">
            <h2>Nossa Missão</h2>
            <p>
              Facilitar o aprendizado de técnicas de programação paralela (OpenMP, OpenMPI e OpenACC)
              através de uma plataforma interativa com editores integrados e ambiente de execução.
            </p>
          </div>

          <div className="sobre-item">
            <h2>Funcionalidades Principais</h2>
            <ul>
              <li>
                <strong>Editores Especializados</strong> - Ambientes de codificação com realce de sintaxe 
                para OpenMP, OpenMPI e OpenACC
              </li>
              <li>
                <strong>Compilação e Execução</strong> - Execute seus códigos diretamente na plataforma
              </li>
              <li>
                <strong>Gerenciamento de Projetos</strong> - Organize seus trabalhos em projetos com múltiplos arquivos
              </li>
              <li>
                <strong>Material Didático</strong> - Tutoriais integrados e exemplos práticos
              </li>
              <li>
                <strong>Colaboração</strong> - Compartilhamento de projetos para trabalho em equipe
              </li>
            </ul>
          </div>

          <div className="sobre-item">
            <h2>Tecnologias Utilizadas</h2>
            <div className="tecnologias-grid">
              <div className="tecnologia-card">
                <h3>Frontend</h3>
                <ul>
                  <li>React.js</li>
                  <li>Monaco Editor</li>
                  <li>React Router</li>
                  <li>Axios</li>
                </ul>
              </div>
              <div className="tecnologia-card">
                <h3>Backend</h3>
                <ul>
                  <li>Node.js</li>
                  <li>Express</li>
                  <li>Docker</li>
                  <li>GCC/G++</li>
                </ul>
              </div>
              <div className="tecnologia-card">
                <h3>Infraestrutura</h3>
                <ul>
                  <li>Linux</li>
                  <li>Apache Server</li>
                  <li>Postgre</li>
                </ul>
              </div>
            </div>
          </div>

          <div className="sobre-item">
            <h2>Equipe de Desenvolvimento</h2>
            <p>
              Plataforma desenvolvida pelo Laboratório de Computação de Alto Desempenho da Universidade Federal do Pará.
            </p>
          </div>

          <div className="sobre-item">
            <h2>Versão Atual</h2>
            <div className="versao-info">
              <p><strong>v2.1.0</strong> - Lançada em {new Date().toLocaleDateString()}</p>
              <ul>
                <li>Suporte a OpenACC</li>
                <li>Novos exemplos didáticos</li>
                <li>Melhorias na interface</li>
              </ul>
            </div>
          </div>

        </section>
      </main>

      <Footer />
    </div>
  );
};

export default SobreSistema;