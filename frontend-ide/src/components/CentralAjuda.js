import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css';

const CentralAjuda = () => {
  const [activeCategory, setActiveCategory] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const categories = [
    {
      id: 1,
      title: 'Primeiros Passos',
      questions: [
        {
          id: 101,
          question: 'Como criar minha conta na plataforma?',
          answer: 'Para criar sua conta, clique em "Cadastre-se" no canto superior direito e preencha o formulário com seus dados pessoais e acadêmicos.'
        },
        {
          id: 102,
          question: 'Quais são os requisitos do sistema?',
          answer: 'Nossa plataforma é compatível com os principais navegadores (Chrome, Firefox, Edge) nas versões mais recentes. Não é necessário hardware especial para a programação básica.'
        }
      ]
    },
    {
      id: 2,
      title: 'Editores de Código',
      questions: [
        {
          id: 201,
          question: 'Como salvar meu código?',
          answer: 'Seu código é salvo qundo clicar no botão salvar. Você também pode usar o atalho Ctrl+S (Cmd+S no Mac) para salvar seu código.'
        },
        {
          id: 202,
          question: 'Posso usar atalhos de teclado no editor?',
          answer: 'Sim, o editor suporta vários atalhos. Pressione Ctrl+Alt+H (Cmd+Option+H no Mac) para ver a lista completa de atalhos disponíveis.'
        }
      ]
    },
    {
      id: 3,
      title: 'Compilação e Execução',
      questions: [
        {
          id: 301,
          question: 'Por que meu código não está compilando?',
          answer: 'Verifique a mensagem de erro no painel de saída. Os erros mais comuns são sintaxe incorreta e falta de bibliotecas necessárias.'
        },
        {
          id: 302,
          question: 'Como visualizar a saída do meu programa?',
          answer: 'Após a compilação bem-sucedida, os resultados aparecerão no painel de saída abaixo do editor. Para programas com entrada interativa, use o terminal integrado.'
        }
      ]
    },
    {
      id: 4,
      title: 'Problemas Comuns',
      questions: [
        {
          id: 401,
          question: 'Esqueci minha senha, como recuperar?',
          answer: 'Na página de login, clique em "Esqueci minha senha" e siga as instruções para redefinição. Você receberá um email com um link seguro para criar uma nova senha.'
        },
        {
          id: 402,
          question: 'Como reportar um bug ou problema técnico?',
          answer: 'Use nosso formulário de contato ou envie um email diretamente para suporte@parallelcode.edu.br com detalhes do problema encontrado.'
        }
      ]
    }
  ];

  const toggleCategory = (categoryId) => {
    setActiveCategory(activeCategory === categoryId ? null : categoryId);
  };

  const filteredCategories = categories.map(category => ({
    ...category,
    questions: category.questions.filter(q => 
      q.question.toLowerCase().includes(searchTerm.toLowerCase()) ||
      q.answer.toLowerCase().includes(searchTerm.toLowerCase())
    )
  })).filter(category => category.questions.length > 0);

  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Central de Ajuda</h1>
          <p className="subtitle">Encontre respostas para suas dúvidas ou entre em contato com nosso suporte</p>
        </section>

        <section className="content-section central-ajuda">
          <div className="search-box">
            <input
              type="text"
              placeholder="Buscar na ajuda..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <svg width="20" height="20" viewBox="0 0 24 24" fill="#536570">
              <path d="M15.5 14h-.79l-.28-.27a6.5 6.5 0 0 0 1.48-5.34c-.47-2.78-2.79-5-5.59-5.34a6.505 6.505 0 0 0-7.27 7.27c.34 2.8 2.56 5.12 5.34 5.59a6.5 6.5 0 0 0 5.34-1.48l.27.28v.79l4.25 4.25c.41.41 1.08.41 1.49 0 .41-.41.41-1.08 0-1.49L15.5 14zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
            </svg>
          </div>

          <div className="help-categories">
            {filteredCategories.length > 0 ? (
              filteredCategories.map(category => (
                <div key={category.id} className="category-card">
                  <div 
                    className="category-header"
                    onClick={() => toggleCategory(category.id)}
                  >
                    <h2>{category.title}</h2>
                    <svg 
                      width="24" 
                      height="24" 
                      viewBox="0 0 24 24" 
                      fill="#2c3e50"
                      className={activeCategory === category.id ? 'active' : ''}
                    >
                      <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/>
                    </svg>
                  </div>
                  
                  {activeCategory === category.id && (
                    <div className="questions-list">
                      {category.questions.map(q => (
                        <div key={q.id} className="question-item">
                          <h3>{q.question}</h3>
                          <p>{q.answer}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))
            ) : (
              <div className="no-results">
                <p>Nenhum resultado encontrado para "{searchTerm}"</p>
                <Link to="/fale-conosco" className="contact-link">
                  Entre em contato com nosso suporte
                </Link>
              </div>
            )}
          </div>

          <div className="additional-help">
            <h2>Não encontrou o que precisava?</h2>
            <Link to="/contato" className="help-button">
              Fale com nosso suporte
            </Link>
            <p>Ou envie um email para: sumawma@gmail.com</p>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default CentralAjuda;