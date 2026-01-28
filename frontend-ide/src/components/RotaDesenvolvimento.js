import React from "react";
import "./PaginaEstudante.css";
import { Link } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";

function RotaDesenvolvimento() {
  // Frameworks de programação paralela
  const frameworks = [
    { id: 1, title: 'OpenMP', description: 'API para programação paralela em memória compartilhada', icon: '⚡', link: '/criar-openmp' },
    { id: 2, title: 'OpenMPI', description: 'Implementação do padrão MPI para computação distribuída', icon: '🌐', link: '/criar-open-mpi' },
    { id: 3, title: 'OpenACC', description: 'Diretivas para programação paralela em aceleradores (GPUs)', icon: '🚀', link: '/criar-open-acc' },
  ];

  return (
    <div>
      <Header />
      {/* Conteúdo Principal */}
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Frameworks de Programação Paralela</h1>
          <p className="subtitle">Selecione uma tecnologia para começar a desenvolver</p>
        </section>

        {/* Seção de Frameworks */}
        <section className="frameworks-section">
          <div className="frameworks-grid">
            {frameworks.map(framework => (
              <div key={framework.id} className="framework-card">
                <div className="framework-icon">{framework.icon}</div>
                <h3>{framework.title}</h3>
                <p>{framework.description}</p>
                <Link to={framework.link} className="framework-button">
                  Acessar {framework.title}
                </Link>
              </div>
            ))}
          </div>
        </section>

        {/* Seção de Informações Adicionais */}
        <section className="info-section">
          <h2>Sobre Programação Paralela</h2>
          <p>
            A programação paralela permite dividir tarefas computacionais entre múltiplos processadores
            ou núcleos para acelerar a execução. Escolha o framework mais adequado para seu projeto:
          </p>
          <ul>
            <li><strong>OpenMP</strong>: Ideal para computação em máquinas multicore</li>
            <li><strong>OpenMPI</strong>: Melhor para clusters e sistemas distribuídos</li>
            <li><strong>OpenACC</strong>: Otimizado para processamento em GPUs</li>
          </ul>
        </section>
      </main>

      <Footer />
    </div>
  );
}

export default RotaDesenvolvimento;