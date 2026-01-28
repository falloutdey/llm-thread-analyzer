import React from "react";
import mpi from "../img/mpi.png"
import CUDA from "../img/CUDA.png"
import mp from "../img/openmp.png"
import "./Main.css";

const Main = () => {

  return (
    <main className="main-conteudo">
      <div className="cartao">
        <div className="cartao-destaque">
          <h1 className="cartao-title">Desbloqueie seu potencial na Programação Paralela</h1>
        </div>
        <div>
          <div className="card-quem-somos">
            <h1>Quem Somos</h1>
            <p>Somos uma equipe desenvolvendo ferramentas de  ensino de programação paralela. Nosso objetivo é simplificar o aprendizado de conceitos complexos de programação, tornando-os acessíveis para todos.</p>
          </div>
          <div className="card-paralela">
            <h1>A Programação Paralela</h1>
            <p>Programação paralela permite que você execute múltiplas tarefas ao mesmo tempo, acelerando processos complexos e otimizando a performance.</p>
          </div>
        </div>
      </div>

      <div className="ferramentas">
        <h1 className="ferramentas-titulo">FERRAMENTAS</h1>
      </div>
      <div className="ferramenta-paralela">
      <div className="img-mp"><img src={mp} alt="Open-MP" /></div>
      <div className="img-cuda"><img src={CUDA} alt="Cuda" /></div>
      <div className="img-mpi"><img src={mpi} alt="Open-MPI" /></div>
      </div>
    </main>
  )
}

export default Main;