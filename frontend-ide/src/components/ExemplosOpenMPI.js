import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css';

const ExemplosOpenMPI = () => {
  const [activeExample, setActiveExample] = useState(0);
  const [copiedIndex, setCopiedIndex] = useState(null);

  const examples = [
    {
      title: "Hello World MPI",
      description: "Programa básico que demonstra a inicialização do ambiente MPI",
      code: `#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
  // Inicializa o ambiente MPI
  MPI_Init(&argc, &argv);

  // Obtém o número total de processos
  int world_size;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);

  // Obtém o rank (ID) do processo atual
  int world_rank;
  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

  // Imprime uma mensagem de cada processo
  printf("Processo %d de %d diz: Olá mundo!\\n", 
         world_rank, world_size);

  // Finaliza o ambiente MPI
  MPI_Finalize();
  return 0;
}`,
      explanation: [
        "1. `MPI_Init` inicializa o ambiente MPI",
        "2. `MPI_Comm_size` obtém o número total de processos",
        "3. `MPI_Comm_rank` obtém o ID do processo atual (rank)",
        "4. `MPI_Finalize` finaliza o ambiente MPI",
        "5. Cada processo executa o mesmo código, mas com rank diferente"
      ],
      compileCommand: "mpicc hello_mpi.c -o hello_mpi",
      runCommand: "mpirun -np 4 ./hello_mpi"
    },
    {
      title: "Comunicação Ponto-a-Ponto",
      description: "Troca de mensagens entre dois processos específicos",
      code: `#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
  MPI_Init(&argc, &argv);

  int world_size, world_rank;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);
  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

  int number;
  if (world_rank == 0) {
    number = 42;
    // Processo 0 envia para o processo 1
    MPI_Send(&number, 1, MPI_INT, 1, 0, MPI_COMM_WORLD);
    printf("Processo 0 enviou número %d para processo 1\\n", number);
  } else if (world_rank == 1) {
    // Processo 1 recebe do processo 0
    MPI_Recv(&number, 1, MPI_INT, 0, 0, MPI_COMM_WORLD, 
             MPI_STATUS_IGNORE);
    printf("Processo 1 recebeu número %d do processo 0\\n", number);
  }

  MPI_Finalize();
  return 0;
}`,
      explanation: [
        "1. `MPI_Send` envia dados para um processo específico",
        "2. `MPI_Recv` recebe dados de um processo específico",
        "3. Os parâmetros incluem: buffer, contagem, tipo MPI, rank destino/origem, tag e comunicador",
        "4. A comunicação é bloqueante (o programa espera até a conclusão)",
        "5. MPI_STATUS_IGNORE pode ser usado quando não se precisa de status detalhado"
      ],
      compileCommand: "mpicc send_recv.c -o send_recv",
      runCommand: "mpirun -np 2 ./send_recv"
    },
    {
      title: "Broadcast MPI",
      description: "Distribuição de dados de um processo para todos os outros",
      code: `#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
  MPI_Init(&argc, &argv);

  int world_size, world_rank;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);
  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

  int data;
  if (world_rank == 0) {
    data = 100;  // Apenas o rank 0 inicializa os dados
  }

  // Broadcast envia data do rank 0 para todos os processos
  MPI_Bcast(&data, 1, MPI_INT, 0, MPI_COMM_WORLD);

  printf("Processo %d recebeu valor %d via broadcast\\n", 
         world_rank, data);

  MPI_Finalize();
  return 0;
}`,
      explanation: [
        "1. `MPI_Bcast` distribui dados de um processo raiz para todos os outros",
        "2. Mais eficiente que enviar mensagens individuais para cada processo",
        "3. Todos os processos chamam a mesma função Bcast",
        "4. O processo raiz (parâmetro 4) fornece os dados iniciais",
        "5. Todos os processos terminam com os mesmos valores nos buffers especificados"
      ],
      compileCommand: "mpicc broadcast.c -o broadcast",
      runCommand: "mpirun -np 4 ./broadcast"
    },
    {
      title: "Scatter e Gather",
      description: "Distribuição e coleta de dados entre processos",
      code: `#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv) {
  MPI_Init(&argc, &argv);

  int world_size, world_rank;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);
  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

  int *send_data = NULL;
  int recv_data;
  
  if (world_rank == 0) {
    send_data = (int *)malloc(world_size * sizeof(int));
    for (int i = 0; i < world_size; i++) {
      send_data[i] = i * 10;
    }
  }

  // Distribui um elemento para cada processo
  MPI_Scatter(send_data, 1, MPI_INT, 
              &recv_data, 1, MPI_INT, 
              0, MPI_COMM_WORLD);

  // Cada processo realiza uma operação em seu dado
  recv_data *= 2;

  // Coleta os resultados modificados
  int *gather_data = NULL;
  if (world_rank == 0) {
    gather_data = (int *)malloc(world_size * sizeof(int));
  }

  MPI_Gather(&recv_data, 1, MPI_INT, 
             gather_data, 1, MPI_INT, 
             0, MPI_COMM_WORLD);

  if (world_rank == 0) {
    printf("Dados coletados:\\n");
    for (int i = 0; i < world_size; i++) {
      printf("%d ", gather_data[i]);
    }
    printf("\\n");
    
    free(send_data);
    free(gather_data);
  }

  MPI_Finalize();
  return 0;
}`,
      explanation: [
        "1. `MPI_Scatter` divide um array em partes iguais para todos os processos",
        "2. `MPI_Gather` coleta dados de todos os processos em um array",
        "3. O processo raiz (geralmente rank 0) gerencia os buffers de envio/recebimento",
        "4. Cada processo opera em sua parte dos dados localmente",
        "5. Útil para operações map-reduce e processamento distribuído de arrays"
      ],
      compileCommand: "mpicc scatter_gather.c -o scatter_gather",
      runCommand: "mpirun -np 4 ./scatter_gather"
    },
    {
      title: "Redução Global",
      description: "Cálculo distribuído com operação de redução",
      code: `#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
  MPI_Init(&argc, &argv);

  int world_size, world_rank;
  MPI_Comm_size(MPI_COMM_WORLD, &world_size);
  MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

  // Cada processo tem seu valor local
  int local_value = world_rank + 1;
  int global_sum, global_max;

  // Soma global de todos os valores
  MPI_Reduce(&local_value, &global_sum, 1, MPI_INT, 
             MPI_SUM, 0, MPI_COMM_WORLD);

  // Máximo global de todos os valores
  MPI_Allreduce(&local_value, &global_max, 1, MPI_INT, 
                MPI_MAX, MPI_COMM_WORLD);

  if (world_rank == 0) {
    printf("Soma global: %d\\n", global_sum);
  }

  printf("Processo %d - Máximo global: %d\\n", 
         world_rank, global_max);

  MPI_Finalize();
  return 0;
}`,
      explanation: [
        "1. `MPI_Reduce` combina valores de todos os processos usando uma operação (soma, máximo, etc.)",
        "2. O resultado final é disponibilizado apenas para o processo raiz",
        "3. `MPI_Allreduce` disponibiliza o resultado para todos os processos",
        "4. Operações comuns: MPI_SUM, MPI_MAX, MPI_MIN, MPI_PROD",
        "5. Fundamental para cálculos distribuídos como normas, produtos internos, etc."
      ],
      compileCommand: "mpicc reduction.c -o reduction",
      runCommand: "mpirun -np 4 ./reduction"
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
          <h1>Exemplos de Código OpenMPI</h1>
          <p className="subtitle">Aprenda MPI com exemplos práticos de programação distribuída</p>
        </section>

        <section className="content-section exemplos-mpi">
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

              <div className="compile-run">
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
                
                <div className="run-command">
                  <h3>Como executar:</h3>
                  <code>
                    {examples[activeExample].runCommand}
                    <button
                      onClick={() => copyToClipboard(examples[activeExample].runCommand)}
                      className="copy-small"
                    >
                      {copiedIndex === activeExample ? '✓' : '📋'}
                    </button>
                  </code>
                </div>
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

export default ExemplosOpenMPI;