import React, { useState, useEffect } from "react";
import "./PaginaEstudante.css";
import { useNavigate, Navigate, Link } from "react-router-dom";
import Logo from "../img/logo111.png";
import AppBar from "./barComponent/AppBar";

function PaginaEstudante() {
  
  const quickAccessItems = [
    { id: 1, title: 'OpenMP', icon: '📅', link: '/calendario' },
    { id: 2, title: 'OPenMPI', icon: '📊', link: '/boletim' },
    { id: 3, title: 'OpenACC', icon: '📚', link: '/materiais' },
    { id: 4, title: 'Projetos Criados', icon: '✅', link: '/frequencia' },
    { id: 5, title: 'Avaliações', icon: '✏️', link: '/avaliacoes' },
    { id: 6, title: 'Comunicados', icon: '📢', link: '/comunicados' },
  ];

  // Notícias recentes
  const newsItems = [
    { id: 1, title: 'Novo curso técnico disponível', date: '15/05/2023', excerpt: 'Inscrições abertas para o curso técnico em Desenvolvimento de Sistemas.' },
    { id: 2, title: 'Período de matrículas', date: '10/05/2023', excerpt: 'Matrículas para o segundo semestre estarão abertas de 01 a 15 de junho.' },
    { id: 3, title: 'Atualização da plataforma', date: '05/05/2023', excerpt: 'Nova versão da plataforma traz melhorias na experiência do usuário.' },
  ];

 
  return (
    <div className="pagina-estudante">
     
     <header className="plataforma-header">
          <div className="header-top">
            <div className="container">
              <div className="logo-container">
                <img src={Logo} alt="Logo Plataforma" className="plataforma-logo" />
                <span className="system-name">SUMAWMA</span>
              </div>
              <div className="user-area">
                <span className="welcome">Bem-vindo, <strong>Usuário</strong></span>
                <button className="logout-btn">Sair</button>
              </div>
            </div>
          </div>
          <nav className="main-nav">
            <div className="container">
              <ul>
                <li><Link to="/area-do-aluno">Início</Link></li>
                <li><Link to="/cursos">Desenvolvimento</Link></li>
                <li><Link to="/sala">Salas</Link></li>
                <li><Link to="/alunos">Alunos</Link></li>
                <li><Link to="/jogos">Jogos</Link></li>
                <li><Link to="/configuracoes">Configurações</Link></li>
              </ul>
            </div>
          </nav>
        </header>
         {/* Conteúdo Principal */}
        <main className="main-content container">
          <section className="welcome-section">
            <h1>Bem-vindo a Plataforma Sumawma</h1>
            <p className="subtitle">Acesse os principais Frameworks de Programação Paralela</p>
          </section>

          {/* Acesso Rápido */}
          <section className="quick-access">
            <h2>Acesso Rápido</h2>
            <div className="quick-access-grid">
              {quickAccessItems.map(item => (
                <Link to={item.link} key={item.id} className="quick-access-card">
                  <span className="card-icon">{item.icon}</span>
                  <span className="card-title">{item.title}</span>
                </Link>
              ))}
            </div>
          </section>

          {/* Notícias e Destaques */}
          <section className="news-section">
            <div className="news-container">
              <h2>Notícias e Avisos</h2>
              <div className="news-list">
                {newsItems.map(news => (
                  <div key={news.id} className="news-item">
                    <h3>{news.title}</h3>
                    <span className="news-date">{news.date}</span>
                    <p>{news.excerpt}</p>
                    <Link to={`/noticias/${news.id}`} className="read-more">Leia mais</Link>
                  </div>
                ))}
              </div>
              <Link to="/noticias" className="all-news-link">Ver todas as notícias</Link>
            </div>

            <div className="highlights-container">
              <h2>Destaques</h2>
              <div className="highlight-card">
                <h3>Enem 2023</h3>
                <p>Inscrições abertas para o Exame Nacional do Ensino Médio.</p>
                <Link to="/enem" className="highlight-link">Saiba mais</Link>
              </div>
              <div className="highlight-card">
                <h3>Olimpíada de Matemática</h3>
                <p>Inscrições para a OBMEP 2023 estão abertas até 31 de maio.</p>
                <Link to="/olimpiada" className="highlight-link">Participar</Link>
              </div>
            </div>
          </section>
        </main>

        {/* Rodapé */}
        <footer className="gov-footer">
          <div className="container">
            <div className="footer-links">
              <div className="footer-section">
                <h3>Sobre</h3>
                <ul>
                  <li><Link to="/sobre">Sobre o sistema</Link></li>
                  <li><Link to="/termos">Termos de uso</Link></li>
                  <li><Link to="/privacidade">Política de privacidade</Link></li>
                </ul>
              </div>
              <div className="footer-section">
                <h3>Ajuda</h3>
                <ul>
                  <li><Link to="/ajuda">Central de ajuda</Link></li>
                  <li><Link to="/contato">Fale conosco</Link></li>
                  <li><Link to="/tutoriais">Tutoriais</Link></li>
                </ul>
              </div>
              <div className="footer-section">
                <h3>Redes Sociais</h3>
                <ul>
                  <li><Link to="/facebook">Facebook</Link></li>
                  <li><Link to="/twitter">Twitter</Link></li>
                  <li><Link to="/youtube">YouTube</Link></li>
                </ul>
              </div>
            </div>
            <div className="footer-bottom">
              <p>© 2023 Sistema de Ensino - Governo do Estado. Todos os direitos reservados.</p>
            </div>
          </div>
        </footer>
    </div>
  );
}

export default PaginaEstudante;
