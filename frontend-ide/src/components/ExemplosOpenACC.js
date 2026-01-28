import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css';

const ExemplosOpenACC = () => {
  const [activeExample, setActiveExample] = useState(0);
  const [copiedIndex, setCopiedIndex] = useState(null);

  const examples = [
    {
      title: "Hello World OpenACC",
      description: "Programa básico que demonstra a estrutura de um programa OpenACC",
      code: `#include <stdio.h>
#include <openacc.h>

int main() {
  int num_devices = acc_get_num_devices(acc_device_nvidia);
  printf("Número de dispositivos OpenACC disponíveis: %d\\n", num_devices);

  // Região paralela OpenACC
  #pragma acc parallel
  {
    int thread_id = __pgi_gangidx() * __pgi_workersize() + __pgi_workeridx();
    printf("Thread %d diz: Olá mundo!\\n", thread_id);
  }

  return 0;
}`,
      explanation: [
        "1. `acc_get_num_devices` verifica dispositivos aceleradores disponíveis",
        "2. `#pragma acc parallel` cria uma região de execução paralela no acelerador",
        "3. Variáveis especiais como `__pgi_gangidx()` identificam threads no acelerador",
        "4. Cada thread no acelerador executa o bloco de código",
        "5. OpenACC gerencia automaticamente a transferência de dados quando necessário"
      ],
      compileCommand: "pgcc -acc -Minfo=accel hello_acc.c -o hello_acc",
      runCommand: "./hello_acc"
    },
    {
      title: "Paralelização de Loop",
      description: "Como paralelizar um loop for com OpenACC",
      code: `#include <stdio.h>
#define N 1000

int main() {
  float a[N], b[N], c[N];
  
  // Inicialização
  for (int i = 0; i < N; i++) {
    a[i] = i * 1.0f;
    b[i] = i * 2.0f;
  }

  // Paralelização do loop com OpenACC
  #pragma acc parallel loop copyin(a[0:N], b[0:N]) copyout(c[0:N])
  for (int i = 0; i < N; i++) {
    c[i] = a[i] + b[i];
  }

  // Verificação
  printf("Resultado final: c[999] = %f\\n", c[999]);
  return 0;
}`,
      explanation: [
        "1. `#pragma acc parallel loop` paraleliza o loop no acelerador",
        "2. `copyin` transfere dados para o dispositivo antes da execução",
        "3. `copyout` transfere resultados de volta para a CPU após execução",
        "4. O compilador determina automaticamente o mapeamento para threads/blocos",
        "5. Pode-se adicionar `vector_length(128)` para otimizar o tamanho do vetor"
      ],
      compileCommand: "pgcc -acc -Minfo=accel vector_add.c -o vector_add",
      runCommand: "./vector_add"
    },
    {
      title: "Redução com OpenACC",
      description: "Cálculo da soma de elementos com redução paralela",
      code: `#include <stdio.h>
#define N 1000000

int main() {
  float array[N], sum = 0.0f;
  
  // Inicialização
  for (int i = 0; i < N; i++) {
    array[i] = (i % 10) * 0.1f;
  }

  // Cálculo paralelo da soma com redução
  #pragma acc parallel loop copyin(array[0:N]) reduction(+:sum)
  for (int i = 0; i < N; i++) {
    sum += array[i];
  }

  printf("Soma total = %f\\n", sum);
  return 0;
}`,
      explanation: [
        "1. `reduction(+:sum)` especifica uma operação de redução",
        "2. OpenACC cria somas parciais automaticamente e as combina no final",
        "3. Suporta várias operações: +, *, max, min, etc.",
        "4. Evita condições de corrida sem necessidade de sincronização explícita",
        "5. `copyin` transfere apenas os dados necessários para o dispositivo"
      ],
      compileCommand: "pgcc -acc -Minfo=accel reduction.c -o reduction",
      runCommand: "./reduction"
    },
    {
      title: "Kernels OpenACC",
      description: "Uso da diretiva kernels para paralelização automática",
      code: `#include <stdio.h>
#define N 1000

int main() {
  float a[N][N], b[N][N], c[N][N];
  
  // Inicialização
  for (int i = 0; i < N; i++) {
    for (int j = 0; j < N; j++) {
      a[i][j] = i + j;
      b[i][j] = i - j;
    }
  }

  // Região kernels - compilador identifica oportunidades de paralelismo
  #pragma acc kernels copyin(a,b) copyout(c)
  {
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        c[i][j] = a[i][j] + b[i][j];
      }
    }
  }

  printf("Resultado: c[500][500] = %f\\n", c[500][500]);
  return 0;
}`,
      explanation: [
        "1. `#pragma acc kernels` permite que o compilador identifique paralelismo",
        "2. Mais flexível que `parallel loop` para códigos complexos",
        "3. Compilador analisa dependências de dados automaticamente",
        "4. `copyin` e `copyout` gerenciam transferência de dados",
        "5. Use `-Minfo=accel` para ver como o compilador paralelizou o código"
      ],
      compileCommand: "pgcc -acc -Minfo=accel kernels.c -o kernels",
      runCommand: "./kernels"
    },
    {
      title: "Uso de Memória Local",
      description: "Otimização com memória local no dispositivo",
      code: `#include <stdio.h>
#define N 1024
#define BLOCK_SIZE 32

int main() {
  float a[N][N], b[N][N], c[N][N];
  
  // Inicialização
  for (int i = 0; i < N; i++) {
    for (int j = 0; j < N; j++) {
      a[i][j] = i + j;
      b[i][j] = i - j;
    }
  }

  // Multiplicação de matrizes otimizada
  #pragma acc parallel loop copyin(a,b) copyout(c) \
          tile(BLOCK_SIZE, BLOCK_SIZE)
  for (int i = 0; i < N; i++) {
    for (int j = 0; j < N; j++) {
      float sum = 0.0f;
      #pragma acc loop seq
      for (int k = 0; k < N; k++) {
        sum += a[i][k] * b[k][j];
      }
      c[i][j] = sum;
    }
  }

  printf("Resultado: c[500][500] = %f\\n", c[500][500]);
  return 0;
}`,
      explanation: [
        "1. `tile(BLOCK_SIZE, BLOCK_SIZE)` divide o trabalho em blocos",
        "2. Melhora a localidade dos dados e reutilização de cache",
        "3. `#pragma acc loop seq` mantém um loop sequencial interno",
        "4. OpenACC gerencia automaticamente a memória compartilhada/local",
        "5. Estratégia comum para algoritmos de multiplicação de matrizes"
      ],
      compileCommand: "pgcc -acc -Minfo=accel matmul.c -o matmul",
      runCommand: "./matmul"
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
          <h1>Exemplos de Código OpenACC</h1>
          <p className="subtitle">Aprenda programação paralela em aceleradores com exemplos práticos</p>
        </section>

        <section className="content-section exemplos-acc">
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

              <div className="acc-tips">
                <h3>Dicas OpenACC:</h3>
                <ul>
                  <li>Use <code>-Minfo=accel</code> para ver detalhes de como o compilador paralelizou seu código</li>
                  <li>Comece com <code>#pragma acc parallel loop</code> para loops simples</li>
                  <li>Use <code>kernels</code> para regiões mais complexas com múltiplos loops</li>
                  <li>Experimente diferentes <code>vector_length</code> para otimização</li>
                  <li>Minimize transferências de dados entre CPU e GPU com <code>copy</code>, <code>copyin</code>, <code>copyout</code></li>
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

export default ExemplosOpenACC;