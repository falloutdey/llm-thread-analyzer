import React from 'react';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css'; // Reusing your existing styles

const PoliticaPrivacidade = () => {
  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Política de Privacidade</h1>
          <p className="subtitle">Última atualização: {new Date().toLocaleDateString()}</p>
        </section>

        <section className="content-section politica-privacidade">
          <div className="politica-item">
            <h2>1. Informações que Coletamos</h2>
            <p>
              Coletamos informações que você nos fornece diretamente quando cria uma conta, 
              como nome, e-mail e informações acadêmicas. Também coletamos dados de uso 
              automáticos quando você interage com nossa plataforma.
            </p>
          </div>

          <div className="politica-item">
            <h2>2. Como Usamos Suas Informações</h2>
            <p>
              Utilizamos suas informações para:
            </p>
            <ul>
              <li>Fornecer e melhorar nossos serviços</li>
              <li>Personalizar sua experiência de aprendizado</li>
              <li>Comunicar atualizações e novidades</li>
              <li>Garantir a segurança da plataforma</li>
            </ul>
          </div>

          <div className="politica-item">
            <h2>3. Compartilhamento de Dados</h2>
            <p>
              Não compartilhamos suas informações pessoais com terceiros, exceto:
            </p>
            <ul>
              <li>Quando necessário para prestação de serviços</li>
              <li>Por exigência legal</li>
              <li>Com seu consentimento explícito</li>
            </ul>
          </div>

          <div className="politica-item">
            <h2>4. Segurança dos Dados</h2>
            <p>
              Implementamos medidas de segurança técnicas e organizacionais para proteger 
              suas informações contra acesso não autorizado, alteração ou destruição.
            </p>
          </div>

          <div className="politica-item">
            <h2>5. Seus Direitos</h2>
            <p>
              Você tem o direito de:
            </p>
            <ul>
              <li>Acessar suas informações pessoais</li>
              <li>Solicitar correção de dados incorretos</li>
              <li>Solicitar a exclusão de seus dados</li>
              <li>Revogar consentimentos dados</li>
            </ul>
          </div>

          <div className="politica-item">
            <h2>6. Alterações nesta Política</h2>
            <p>
              Podemos atualizar esta política periodicamente. Notificaremos você sobre 
              mudanças significativas através do e-mail cadastrado ou por meio de avisos 
              em nossa plataforma.
            </p>
          </div>

          <div className="politica-item">
            <h2>7. Contato</h2>
            <p>
              Para dúvidas sobre esta política ou sobre seus dados pessoais, entre em 
              contato conosco.
            </p>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default PoliticaPrivacidade;