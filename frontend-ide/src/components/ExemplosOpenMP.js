import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css';

const ExemplosOpenMP = () => {
  const [activeExample, setActiveExample] = useState(0);
  const [copiedIndex, setCopiedIndex] = useState(null);

  const examples = [
    {
      title: "Hello World Paralelo",
      description: "Programa básico que demonstra como criar threads com OpenMP",
      code: `#include <omp.h>
#include <stdio.h>

int main() {
  // Diretiva paralela - cria um time de threads
  #pragma omp parallel
  {
    // Cada thread executa este bloco
    int thread_id = omp_get_thread_num();
    int total_threads = omp_get_num_threads();
    
    printf("Thread %d de %d diz: Olá mundo!\\n", 
           thread_id, total_threads);
  }
  
  return 0;
}`,
      explanation: [
        "1. `#pragma omp parallel` cria um grupo de threads",
        "2. Cada thread executa o bloco de código entre chaves",
        "3. `omp_get_thread_num()` retorna o ID da thread atual",
        "4. `omp_get_num_threads()` retorna o número total de threads"
      ],
      compileCommand: "gcc -fopenmp hello.c -o hello"
    },
    {
      title: "Paralelização de Loop For",
      description: "Como paralelizar um loop for com OpenMP",
      code: `#include <omp.h>
#include <stdio.h>
#define N 1000

int main() {
  int i;
  double a[N], b[N], c[N];
  
  // Inicialização
  for (i = 0; i < N; i++) {
    a[i] = i * 1.0;
    b[i] = i * 2.0;
  }

  // Paralelização do loop for
  #pragma omp parallel for
  for (i = 0; i < N; i++) {
    c[i] = a[i] + b[i];
    printf("Iteração %d executada pela thread %d\\n",
           i, omp_get_thread_num());
  }

  return 0;
}`,
      explanation: [
        "1. `#pragma omp parallel for` divide as iterações do loop entre threads",
        "2. Cada thread executa um subconjunto das iterações",
        "3. OpenMP cuida da distribuição e sincronização automaticamente",
        "4. Variáveis compartilhadas (a, b, c) são acessíveis por todas threads"
      ],
      compileCommand: "gcc -fopenmp parallel_for.c -o parallel_for"
    },
    {
      title: "Redução com OpenMP",
      description: "Cálculo da soma de elementos com redução paralela",
      code: `#include <omp.h>
#include <stdio.h>
#define N 1000000

int main() {
  int i;
  double sum = 0.0;
  double array[N];
  
  // Inicialização do array
  for (i = 0; i < N; i++) {
    array[i] = (i + 1) * 0.1;
  }

  // Cálculo paralelo da soma com redução
  #pragma omp parallel for reduction(+:sum)
  for (i = 0; i < N; i++) {
    sum += array[i];
  }

  printf("Soma total = %f\\n", sum);
  return 0;
}`,
      explanation: [
        "1. `reduction(+:sum)` cria uma cópia local de 'sum' para cada thread",
        "2. Cada thread calcula sua soma parcial",
        "3. No final, as somas parciais são combinadas com a operação '+'",
        "4. Evita condições de corrida sem necessidade de locks explícitos"
      ],
      compileCommand: "gcc -fopenmp reduction.c -o reduction"
    },
    {
      title: "Seções Paralelas",
      description: "Execução de diferentes tarefas em paralelo",
      code: `#include <omp.h>
#include <stdio.h>

void task1() {
  printf("Task 1 executada pela thread %d\\n",
         omp_get_thread_num());
}

void task2() {
  printf("Task 2 executada pela thread %d\\n",
         omp_get_thread_num());
}

int main() {
  // Duas seções independentes executadas em paralelo
  #pragma omp parallel sections
  {
    #pragma omp section
    {
      task1();
    }
    
    #pragma omp section
    {
      task2();
    }
  }
  
  return 0;
}`,
      explanation: [
        "1. `#pragma omp parallel sections` cria um bloco paralelo",
        "2. Cada `#pragma omp section` é executado por uma thread diferente",
        "3. Útil para paralelizar tarefas independentes",
        "4. O número de sections pode exceder o número de threads"
      ],
      compileCommand: "gcc -fopenmp sections.c -o sections"
    },
    {
      title: "Regiões Críticas",
      description: "Como proteger acesso a recursos compartilhados",
      code: `#include <omp.h>
#include <stdio.h>

int main() {
  int shared_counter = 0;
  
  #pragma omp parallel
  {
    // Região crítica - apenas uma thread por vez
    #pragma omp critical
    {
      printf("Thread %d acessando o contador\\n",
             omp_get_thread_num());
      shared_counter++;
    }
  }
  
  printf("Valor final do contador: %d\\n", shared_counter);
  return 0;
}`,
      explanation: [
        "1. `#pragma omp critical` define uma região de exclusão mútua",
        "2. Apenas uma thread pode executar o bloco por vez",
        "3. Garante acesso seguro a variáveis compartilhadas",
        "4. Alternativa mais leve que mutex para muitos casos"
      ],
      compileCommand: "gcc -fopenmp critical.c -o critical"
    }
  ];

  const copyToClipboard = (text, index) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Exemplos de Código OpenMP</h1>
          <p className="subtitle">Aprenda com exemplos práticos de programação paralela</p>
        </section>

        <section className="content-section exemplos-openmp">
          <div className="examples-container">
            <div className="examples-sidebar">
              <h2>Exemplos Disponíveis</h2>
              <ul>
                {examples.map((example, index) => (
                  <li 
                    key={index}
                    className={activeExample === index ? 'active' : ''}
                    onClick={() => setActiveExample(index)}
                  >
                    <h3>{example.title}</h3>
                    <p>{example.description}</p>
                  </li>
                ))}
              </ul>
            </div>

            <div className="example-content">
              <div className="example-header">
                <h2>{examples[activeExample].title}</h2>
                <div className="example-actions">
                  <button
                    onClick={() => copyToClipboard(examples[activeExample].code, activeExample)}
                    className="copy-button"
                  >
                    {copiedIndex === activeExample ? 'Copiado!' : 'Copiar Código'}
                  </button>
                </div>
              </div>

              <div className="code-block">
                <pre>{examples[activeExample].code}</pre>
              </div>

              <div className="compile-command">
                <h3>Como compilar:</h3>
                <code>
                  {examples[activeExample].compileCommand}
                  <button
                    onClick={() => copyToClipboard(examples[activeExample].compileCommand)}
                    className="copy-small"
                  >
                    {copiedIndex === activeExample ? '✓' : '📋'}
                  </button>
                </code>
              </div>

              <div className="explanation">
                <h3>Explicação:</h3>
                <ul>
                  {examples[activeExample].explanation.map((item, idx) => (
                    <li key={idx}>{item}</li>
                  ))}
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

export default ExemplosOpenMP;