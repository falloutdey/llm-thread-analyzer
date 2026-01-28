import React, { useState, useEffect } from 'react';
import Header from './Header';
import Footer from './Footer';
import './PaginaEstudante.css';

const FaleConosco = () => {
  const [formData, setFormData] = useState({
    nome: '',
    email: '',
    assunto: '',
    mensagem: ''
  });

  const [errors, setErrors] = useState({
    nome: '',
    email: '',
    assunto: '',
    mensagem: ''
  });

  const [submissionStatus, setSubmissionStatus] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  // Verifica conexão com a internet
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const validateEmail = (email) => {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
  };

  const validateForm = () => {
    let valid = true;
    const newErrors = {
      nome: '',
      email: '',
      assunto: '',
      mensagem: ''
    };

    if (!formData.nome.trim()) {
      newErrors.nome = 'Nome é obrigatório';
      valid = false;
    } else if (formData.nome.trim().length < 3) {
      newErrors.nome = 'Nome deve ter pelo menos 3 caracteres';
      valid = false;
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email é obrigatório';
      valid = false;
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Email inválido';
      valid = false;
    }

    if (!formData.assunto) {
      newErrors.assunto = 'Assunto é obrigatório';
      valid = false;
    }

    if (!formData.mensagem.trim()) {
      newErrors.mensagem = 'Mensagem é obrigatória';
      valid = false;
    } else if (formData.mensagem.trim().length < 10) {
      newErrors.mensagem = 'Mensagem deve ter pelo menos 10 caracteres';
      valid = false;
    }

    setErrors(newErrors);
    return valid;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // Limpa o erro quando o usuário começa a digitar
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!isOnline) {
      setSubmissionStatus({
        success: false,
        message: 'Você está offline. Conecte-se à internet para enviar sua mensagem.'
      });
      return;
    }

    if (!validateForm()) return;

    setIsSubmitting(true);
    
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL || 'http://localhost:5000/api/users'}/fale-conosco`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Erro ao enviar mensagem');
      }

      setSubmissionStatus({
        success: true,
        message: data.message || 'Mensagem enviada com sucesso! Responderemos em até 48 horas.'
      });
      
      // Limpa o formulário após o envio bem-sucedido
      setFormData({
        nome: '',
        email: '',
        assunto: '',
        mensagem: ''
      });
    } catch (error) {
      setSubmissionStatus({
        success: false,
        message: error.message || 'Erro ao enviar mensagem. Por favor, tente novamente mais tarde.'
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="pagina-container">
      <Header />
      
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Fale Conosco</h1>
          <p className="subtitle">Tire suas dúvidas ou envie sugestões sobre nossa plataforma</p>
        </section>

        <section className="content-section fale-conosco">
          <div className="contact-grid">
            <div className="contact-info">
              <h2>Informações de Contato</h2>
              
              <div className="info-item">
                <h3>Email</h3>
                <p>marccosaavedraa3@gmail.com</p>
                <p>josivaldo@ufpa.br</p>
              </div>
              
              <div className="info-item">
                <h3>Endereço</h3>
                <p>Universidade Federal do Pará</p>
                <p>Instituto de Ciências Exatas e Naturais</p>
                <p>Laboratório de Bioinformática e Computação de Alto Desempenho (LABIOCAD)</p>
                <p>Rua Augusto Corrêa, nº 01 - Bairro: Guamá. CEP: 66075. Belém - Pará - Brasil</p>
              </div>
              
              <div className="social-links">
                <h3>Redes Sociais</h3>
                <div className="social-icons">
                  <a href="#" aria-label="Facebook">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="#3b5998">
                      <path d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.878v-6.987h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.988C18.343 21.128 22 16.991 22 12z"/>
                    </svg>
                  </a>
                  <a href="#" aria-label="Twitter">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="#1da1f2">
                      <path d="M8.29 20.251c7.547 0 11.675-6.253 11.675-11.675 0-.178 0-.355-.012-.53A8.348 8.348 0 0022 5.92a8.19 8.19 0 01-2.357.646 4.118 4.118 0 001.804-2.27 8.224 8.224 0 01-2.605.996 4.107 4.107 0 00-6.993 3.743 11.65 11.65 0 01-8.457-4.287 4.106 4.106 0 001.27 5.477A4.072 4.072 0 012.8 9.713v.052a4.105 4.105 0 003.292 4.022 4.095 4.095 0 01-1.853.07 4.108 4.108 0 003.834 2.85A8.233 8.233 0 012 18.407a11.616 11.616 0 006.29 1.84"/>
                    </svg>
                  </a>
                  <a href="#" aria-label="Instagram">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="#e1306c">
                      <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/>
                    </svg>
                  </a>
                </div>
              </div>
            </div>

            <div className="contact-form">
              <h2>Formulário de Contato</h2>
              
              {!isOnline && (
                <div className="offline-warning">
                  Você está offline. Conecte-se à internet para enviar sua mensagem.
                </div>
              )}
              
              {submissionStatus && (
                <div className={`submission-message ${submissionStatus.success ? 'success' : 'error'}`}>
                  {submissionStatus.message}
                </div>
              )}
              
              <form onSubmit={handleSubmit}>
                <div className="form-group">
                  <label htmlFor="nome">Nome Completo*</label>
                  <input
                    type="text"
                    id="nome"
                    name="nome"
                    value={formData.nome}
                    onChange={handleChange}
                    className={errors.nome ? 'error-input' : ''}
                    disabled={isSubmitting}
                  />
                  {errors.nome && <span className="error-message">{errors.nome}</span>}
                </div>
                
                <div className="form-group">
                  <label htmlFor="email">Email*</label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    className={errors.email ? 'error-input' : ''}
                    disabled={isSubmitting}
                  />
                  {errors.email && <span className="error-message">{errors.email}</span>}
                </div>
                
                <div className="form-group">
                  <label htmlFor="assunto">Assunto*</label>
                  <select
                    id="assunto"
                    name="assunto"
                    value={formData.assunto}
                    onChange={handleChange}
                    className={errors.assunto ? 'error-input' : ''}
                    disabled={isSubmitting}
                  >
                    <option value="">Selecione um assunto</option>
                    <option value="duvida">Dúvida sobre a plataforma</option>
                    <option value="sugestao">Sugestão de melhoria</option>
                    <option value="problema">Reportar problema</option>
                    <option value="outro">Outro assunto</option>
                  </select>
                  {errors.assunto && <span className="error-message">{errors.assunto}</span>}
                </div>
                
                <div className="form-group">
                  <label htmlFor="mensagem">Mensagem*</label>
                  <textarea
                    id="mensagem"
                    name="mensagem"
                    rows="6"
                    value={formData.mensagem}
                    onChange={handleChange}
                    className={errors.mensagem ? 'error-input' : ''}
                    disabled={isSubmitting}
                  ></textarea>
                  {errors.mensagem && <span className="error-message">{errors.mensagem}</span>}
                </div>
                
                <div className="form-actions">
                  <button 
                    type="submit" 
                    disabled={isSubmitting || !isOnline}
                    className={isSubmitting ? 'submitting' : ''}
                  >
                    {isSubmitting ? (
                      <>
                        <span className="spinner"></span>
                        Enviando...
                      </>
                    ) : 'Enviar Mensagem'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default FaleConosco;