import React from 'react';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css'; // Reusing existing styles

const TermosDeUso = () => {
  const currentDate = new Date().toLocaleDateString('pt-BR');

  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Termos de Uso</h1>
          <p className="subtitle">Última atualização: {currentDate}</p>
        </section>

        <section className="content-section termos-uso">
          <div className="termos-item">
            <h2>1. Aceitação dos Termos</h2>
            <p>
              Ao acessar e utilizar a plataforma de programação paralela Sumawma, você concorda com estes Termos de Uso e com nossa Política de Privacidade. Se você não concordar com estes termos, não deverá utilizar nossos serviços.
            </p>
          </div>

          <div className="termos-item">
            <h2>2. Uso da Plataforma</h2>
            <h3>2.1 Requisitos</h3>
            <p>
              Para utilizar a plataforma, você deve:
            </p>
            <ul>
              <li>Fornecer informações precisas no cadastro</li>
              <li>Manter a confidencialidade de sua conta</li>
            </ul>

            <h3>2.2 Restrições</h3>
            <p>
              Você concorda em não:
            </p>
            <ul>
              <li>Utilizar a plataforma para atividades ilegais</li>
              <li>Tentar acessar contas de outros usuários</li>
              <li>Executar códigos maliciosos ou que possam danificar a infraestrutura</li>
              <li>Violar direitos de propriedade intelectual</li>
            </ul>
          </div>

          <div className="termos-item">
            <h2>3. Propriedade Intelectual</h2>
            <p>
              Todo o conteúdo da plataforma (exceto códigos enviados por usuários) é propriedade da Sumawma ou de seus licenciadores. Você recebe uma licença limitada para uso pessoal e não-comercial.
            </p>
          </div>

          <div className="termos-item">
            <h2>4. Códigos dos Usuários</h2>
            <h3>4.1 Direitos</h3>
            <p>
              Você mantém todos os direitos sobre os códigos que criar e enviar através da plataforma.
            </p>

            <h3>4.2 Responsabilidade</h3>
            <p>
              Você é o único responsável pelo conteúdo dos códigos que enviar e pelas consequências de sua execução.
            </p>
          </div>

          <div className="termos-item">
            <h2>5. Limitação de Responsabilidade</h2>
            <p>
              A Sumawma não será responsável por:
            </p>
            <ul>
              <li>Danos resultantes do uso ou incapacidade de usar a plataforma</li>
              <li>Conteúdo gerado por usuários</li>
              <li>Interrupções temporárias do serviço</li>
              <li>Perda de dados não causada por negligência direta</li>
            </ul>
          </div>

          <div className="termos-item">
            <h2>6. Modificações no Serviço</h2>
            <p>
              Reservamo-nos o direito de modificar ou descontinuar a plataforma, temporária ou permanentemente, sem aviso prévio. Grandes alterações serão comunicadas aos usuários.
            </p>
          </div>

          <div className="termos-item">
            <h2>7. Encerramento de Conta</h2>
            <p>
              Podemos encerrar ou suspender sua conta imediatamente, sem aviso prévio, caso você viole estes Termos de Uso. Você pode encerrar sua conta a qualquer momento através das configurações do perfil.
            </p>
          </div>

          <div className="termos-item">
            <h2>8. Lei Aplicável</h2>
            <p>
              Estes Termos serão regidos e interpretados de acordo com as leis do Brasil. 
            </p>
          </div>

          <div className="termos-item">
            <h2>9. Alterações nos Termos</h2>
            <p>
              Podemos atualizar estes Termos periodicamente. A versão atualizada será publicada na plataforma com a data de revisão atualizada. O uso continuado da plataforma após tais alterações constitui sua aceitação dos novos termos.
            </p>
          </div>

          <div className="termos-item">
            <h2>10. Contato</h2>
            <p>
              Para quaisquer dúvidas sobre estes Termos de Uso, entre em contato.
            </p>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default TermosDeUso;