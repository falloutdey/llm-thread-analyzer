import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css';

const Tutoriais = () => {
  const [activeTech, setActiveTech] = useState('openmp');
  const [activeTutorial, setActiveTutorial] = useState(null);

  const technologies = {
    openmp: {
      name: 'OpenMP',
      description: 'API para programação paralela em memória compartilhada',
      tutorials: [
        {
          id: 'omp1',
          title: 'Introdução ao OpenMP',
          duration: '15 min',
          level: 'Iniciante',
          content: (
            <div>
              <h3>O que é OpenMP?</h3>
              <p>OpenMP é uma API para programação paralela em sistemas de memória compartilhada que utiliza diretivas de compilação.</p>
              
              <h3>Primeiro Programa Paralelo</h3>
              <pre>{`#include <omp.h>
#include <stdio.h>

int main() {
  #pragma omp parallel
  {
    int tid = omp_get_thread_num();
    printf("Thread %d executando\\n", tid);
  }
  return 0;
}`}</pre>
              
              <h3>Como Compilar</h3>
              <code>gcc -fopenmp programa.c -o programa</code>
            </div>
          )
        },
        {
          id: 'omp2',
          title: 'Diretivas Parallel e For',
          duration: '20 min',
          level: 'Intermediário',
          content: (
            <div>
              <h3>Paralelizando Loops</h3>
              <pre>{`#pragma omp parallel for
for (int i = 0; i < 100; i++) {
  printf("Iteração %d executada pela thread %d\\n", 
         i, omp_get_thread_num());
}`}</pre>
            </div>
          )
        }
      ]
    },
    openmpi: {
      name: 'OpenMPI',
      description: 'Biblioteca para programação paralela em memória distribuída',
      tutorials: [
        {
          id: 'mpi1',
          title: 'Conceitos Básicos do MPI',
          duration: '20 min',
          level: 'Iniciante',
          content: (
            <div>
              <h3>Inicialização do Ambiente MPI</h3>
              <pre>{`#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
  MPI_Init(&argc, &argv);
  
  int world_size;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);

  int world_rank;
  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

  printf("Processo %d de %d\\n", world_rank, world_size);
  
  MPI_Finalize();
}`}</pre>
            </div>
          )
        }
      ]
    },
    openacc: {
      name: 'OpenACC',
      description: 'Diretivas para programação paralela em aceleradores',
      tutorials: [
        {
          id: 'acc1',
          title: 'Introdução ao OpenACC',
          duration: '25 min',
          level: 'Iniciante',
          content: (
            <div>
              <h3>Primeiro Programa com OpenACC</h3>
              <pre>{`#include <stdio.h>
#define N 1000

int main() {
  float a[N], b[N], c[N];
  
  // Inicialização
  for (int i = 0; i < N; i++) {
    a[i] = i;
    b[i] = i * 2;
  }

  // Paralelização com OpenACC
  #pragma acc parallel loop copyin(a[0:N],b[0:N]) copyout(c[0:N])
  for (int i = 0; i < N; i++) {
    c[i] = a[i] + b[i];
  }

  printf("Resultado: %f\\n", c[N-1]);
  return 0;
}`}</pre>
            </div>
          )
        }
      ]
    }
  };

  const toggleTutorial = (tutorialId) => {
    setActiveTutorial(activeTutorial === tutorialId ? null : tutorialId);
  };

  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Tutoriais de Programação Paralela</h1>
          <p className="subtitle">Aprenda OpenMP, OpenMPI e OpenACC com exemplos práticos</p>
        </section>

        <section className="content-section tutoriais">
          <div className="tech-selector">
            {Object.keys(technologies).map((techKey) => (
              <button
                key={techKey}
                className={`tech-button ${activeTech === techKey ? 'active' : ''}`}
                onClick={() => {
                  setActiveTech(techKey);
                  setActiveTutorial(null);
                }}
              >
                {technologies[techKey].name}
                <span>{technologies[techKey].description}</span>
              </button>
            ))}
          </div>

          <div className="tutorials-container">
            <div className="tutorials-list">
              <h2>Tutoriais de {technologies[activeTech].name}</h2>
              
              {technologies[activeTech].tutorials.map((tutorial) => (
                <div 
                  key={tutorial.id}
                  className={`tutorial-card ${activeTutorial === tutorial.id ? 'active' : ''}`}
                  onClick={() => toggleTutorial(tutorial.id)}
                >
                  <div className="tutorial-header">
                    <h3>{tutorial.title}</h3>
                    <div className="tutorial-meta">
                      <span className="duration">{tutorial.duration}</span>
                      <span className="level">{tutorial.level}</span>
                    </div>
                  </div>
                  
                  {activeTutorial === tutorial.id && (
                    <div className="tutorial-content">
                      {tutorial.content}
                      <div className="tutorial-actions">
                        <Link 
                          to={`/editor-${activeTech}/novo`} 
                          className="try-button"
                        >
                          Experimente no Editor
                        </Link>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>

            <div className="resources-sidebar">
              <h3>Recursos Adicionais</h3>
              
              <div className="resource-card">
                <h4>Documentação Oficial</h4>
                <ul>
                  <li>
                    <a href="https://www.openmp.org/" target="_blank" rel="noopener noreferrer">
                      OpenMP Specification
                    </a>
                  </li>
                  <li>
                    <a href="https://www.open-mpi.org/" target="_blank" rel="noopener noreferrer">
                      OpenMPI Documentation
                    </a>
                  </li>
                  <li>
                    <a href="https://www.openacc.org/" target="_blank" rel="noopener noreferrer">
                      OpenACC Resources
                    </a>
                  </li>
                </ul>
              </div>
              
              <div className="resource-card">
                <h4>Livros Recomendados</h4>
                <ul>
                  <li>"Parallel Programming in C with MPI and OpenMP" - Michael Quinn</li>
                  <li>"Using OpenMP" - Barbara Chapman et al.</li>
                  <li>"OpenACC for Programmers" - Sunita Chandrasekaran</li>
                </ul>
              </div>
              
              <div className="resource-card">
                <h4>Próximos Tutoriais</h4>
                <ul>
                  <li>Otimização de Desempenho</li>
                  <li>Depuração de Código Paralelo</li>
                  <li>Padrões Comuns de Paralelismo</li>
                </ul>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default Tutoriais;