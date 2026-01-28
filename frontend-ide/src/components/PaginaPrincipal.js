import React from "react";
import "./PaginaEstudante.css";
import { Link } from "react-router-dom";
import Logo from "../img/logo111.png";

function PaginaInicial() {
  // Itens de acesso rápido para frameworks
  const frameworkItems = [
    { id: 1, title: 'OpenMP', icon: '⚡', link: '/login', 
      description: 'Programação paralela para sistemas de memória compartilhada' },
    { id: 2, title: 'OpenMPI', icon: '🌐', link: '/login', 
      description: 'Computação distribuída em clusters' },
    { id: 3, title: 'OpenACC', icon: '🚀', link: '/login', 
      description: 'Aceleração com GPUs e processadores paralelos' },
  ];

  // Recursos da plataforma
  const featureItems = [
    { id: 1, title: 'Editor de Código', icon: '✏️', 
      description: 'Editor integrado com realce de sintaxe e execução de código' },
    { id: 2, title: 'Projetos Salvos', icon: '💾', 
      description: 'Armazenamento e gerenciamento de seus projetos' },
    { id: 3, title: 'Tutoriais', icon: '📖', 
      description: 'Guias e exemplos para aprender programação paralela' },
    { id: 4, title: 'Compartilhamento', icon: '🔗', 
      description: 'Compartilhe seus projetos com outros usuários' },
  ];

  return (
    <div className="pagina-estudante">
      {/* Header */}
      <header className="plataforma-header">
        <div className="header-top">
          <div className="container">
            <div className="logo-container">
              <img src={Logo} alt="Logo Plataforma" className="plataforma-logo" />
              <span className="system-name">SUMAWMA</span>
            </div>
            <div className="user-area">
              <Link to="/login" className="login-btn">Entrar</Link>
              <Link to="/registro" className="register-btn">Registrar</Link>
            </div>
          </div>
        </div>
      </header>

      {/* Conteúdo Principal */}
      <main className="main-content container">
        <section className="hero-section">
          <div className="hero-content">
            <h1>Plataforma de Programação Paralela</h1>
            <p className="subtitle">
              Desenvolva, execute e compartilhe projetos usando OpenMP, OpenMPI e OpenACC
            </p>
            <div className="hero-buttons">
              <Link to="/registro" className="primary-btn">Comece Agora</Link>
            </div>
          </div>
        </section>

        {/* Frameworks */}
        <section className="frameworks-section">
          <h2>Frameworks Suportados</h2>
          <p className="section-description">
            Trabalhe com os principais frameworks de programação paralela
          </p>
          <div className="frameworks-grid">
            {frameworkItems.map(item => (
              <Link to={item.link} key={item.id} className="framework-card">
                <span className="card-icon">{item.icon}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </Link>
            ))}
          </div>
        </section>

        {/* Recursos */}
        <section className="features-section">
          <h2>Recursos da Plataforma</h2>
          <p className="section-description">
            Tudo o que você precisa para aprender e desenvolver em paralelo
          </p>
          <div className="features-grid">
            {featureItems.map(item => (
              <div key={item.id} className="feature-card">
                <span className="card-icon">{item.icon}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Chamada para ação */}
        <section className="cta-section">
          <div className="cta-content">
            <h2>Pronto para começar?</h2>
            <p>
              Registre-se agora e tenha acesso a todos os recursos da plataforma
              para desenvolvimento de programas paralelos.
            </p>
            <Link to="/registro" className="primary-btn large">Criar Minha Conta</Link>
          </div>
        </section>
      </main>

      {/* Rodapé */}
      <footer className="gov-footer">
        <div className="container">
          <div className="footer-bottom">
            <p>© 2023 Plataforma SUMAWMA - Todos os direitos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default PaginaInicial;