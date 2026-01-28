import React from "react";
import "./PaginaEstudante.css";
import { Link } from "react-router-dom";
import Logo from "../img/logo111.png";
import Header from "./Header";
import Footer from "./Footer";

function RotaInicio() {
  
  const frameworksItems = [
    { 
      id: 1, 
      title: 'OpenMP', 
      icon: '⚡', 
      link: '/criar-openmp',
      description: 'API para programação paralela em memória compartilhada'
    },
    { 
      id: 2, 
      title: 'OpenMPI', 
      icon: '🔗', 
      link: '/criar-open-mpi',
      description: 'Implementação MPI para computação distribuída'
    },
    { 
      id: 3, 
      title: 'OpenACC', 
      icon: '🚀', 
      link: '/criar-open-acc',
      description: 'Diretivas para paralelismo em aceleradores (GPUs)'
    },
  ];

  const learningResources = [
    { 
      id: 1, 
      title: 'Tutoriais Interativos', 
      icon: '📚', 
      link: '/tutoriais' 
    },
    { 
      id: 2, 
      title: 'Laboratórios Virtuais', 
      icon: '💻', 
      link: '/laboratorios' 
    },
    { 
      id: 3, 
      title: 'Desafios de Código', 
      icon: '🧩', 
      link: '/desafios' 
    },
  ];

  const newsItems = [
    { 
      id: 1, 
      title: 'Novo módulo: Otimização com OpenMP', 
      date: '15/06/2023', 
      excerpt: 'Aprenda técnicas avançadas de paralelização com OpenMP 5.0' 
    },
    { 
      id: 2, 
      title: 'Workshop: Programação Híbrida', 
      date: '10/06/2023', 
      excerpt: 'Combine OpenMP, MPI e OpenACC para máxima performance' 
    },
    { 
      id: 3, 
      title: 'Atualização: Suporte a GPU NVIDIA', 
      date: '05/06/2023', 
      excerpt: 'Melhorias no suporte a OpenACC para placas NVIDIA' 
    },
  ];

  return (
    <div>
      <Header/>
      
      <main className="main-content container">
        {/* Seção de Boas-vindas */}
        <section className="welcome-section">
          <h1>Plataforma de Programação Paralela</h1>
          <p className="subtitle">Domine OpenMP, OpenMPI e OpenACC para computação de alto desempenho</p>
        </section>

        {/* Frameworks Principais */}
        <section className="frameworks-section">
          <h2>Frameworks Principais</h2>
          <div className="frameworks-grid">
            {frameworksItems.map(item => (
              <Link to={item.link} key={item.id} className="framework-card">
                <span className="card-icon">{item.icon}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
                <span className="card-link">Explorar →</span>
              </Link>
            ))}
          </div>
        </section>
      </main>
      
      <Footer/>
    </div>
  );
}

export default RotaInicio;