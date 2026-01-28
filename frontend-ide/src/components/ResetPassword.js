import React, { useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";
import { Alert } from "@mui/material";
import Logo from "../img/logo111.png";
import "./ResetPassword.css";

const ResetPassword = () => {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    email: "",
    resetToken: "",
    newPassword: "",
    confirmarSenha: "",
  });
  const [message, setMessage] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleTogglePassword = () => {
    setShowPassword(!showPassword);
  };

  const handleRequestToken = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage(null);

    try {
      const { email } = formData;
      await axios.post("http://localhost:5000/api/users/request-password-reset", { email });
      setMessage({ type: 'success', text: 'Código de verificação enviado para o e-mail' });
      setStep(2);
    } catch (error) {
      setMessage({ 
        type: 'error', 
        text: error.response?.data?.message || 'Erro ao solicitar código de verificação' 
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage(null);

    try {
      const { email, resetToken, newPassword, confirmarSenha } = formData;

      if (newPassword !== confirmarSenha) {
        setMessage({ type: 'error', text: 'As senhas não coincidem.' });
        return;
      }

      await axios.post("http://localhost:5000/api/users/reset-password", { 
        email, 
        resetToken, 
        newPassword 
      });
      
      setMessage({ type: 'success', text: 'Senha alterada com sucesso!' });
      setTimeout(() => navigate("/login"), 2000);
    } catch (error) {
      setMessage({ 
        type: 'error', 
        text: error.response?.data?.message || 'Erro ao redefinir senha' 
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="reset-password-page">
      <header className="plataforma-header">
        <div className="header-top">
          <div className="container">
            <div className="logo-container">
              <img src={Logo} alt="Logo Plataforma" className="plataforma-logo" />
              <span className="system-name">SUMAWMA</span>
            </div>
          </div>
        </div>
      </header>

      <main className="main-content container">
        <section className="reset-password-section">
          <div className="reset-password-card">
            {step === 1 ? (
              <>
                <h1 className="reset-title">Redefinir Senha</h1>
                <p className="reset-subtitle">
                  Digite seu e-mail para receber o código de verificação
                </p>

                <form onSubmit={handleRequestToken} className="reset-form">
                  {message && (
                    <Alert severity={message.type} className="alert-message">
                      {message.text}
                    </Alert>
                  )}

                  <div className="form-group">
                    <label htmlFor="email">E-mail</label>
                    <input
                      type="email"
                      id="email"
                      name="email"
                      value={formData.email}
                      onChange={handleChange}
                      required
                      placeholder="Digite seu e-mail cadastrado"
                    />
                  </div>

                  <button 
                    type="submit" 
                    className="submit-button"
                    disabled={isLoading}
                  >
                    {isLoading ? 'Enviando...' : 'Enviar Código'}
                  </button>

                  <div className="back-to-login">
                    <Link to="/login">Voltar para o login</Link>
                  </div>
                </form>
              </>
            ) : (
              <>
                <h1 className="reset-title">Criar Nova Senha</h1>
                <p className="reset-subtitle">
                  Insira o código recebido por e-mail e sua nova senha
                </p>

                <form onSubmit={handleResetPassword} className="reset-form">
                  {message && (
                    <Alert severity={message.type} className="alert-message">
                      {message.text}
                    </Alert>
                  )}

                  <div className="form-group">
                    <label htmlFor="resetToken">Código de Verificação</label>
                    <input
                      type="text"
                      id="resetToken"
                      name="resetToken"
                      value={formData.resetToken}
                      onChange={handleChange}
                      required
                      placeholder="Digite o código recebido"
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="newPassword">Nova Senha</label>
                    <div className="password-input">
                      <input
                        type={showPassword ? "text" : "password"}
                        id="newPassword"
                        name="newPassword"
                        value={formData.newPassword}
                        onChange={handleChange}
                        required
                        placeholder="Digite sua nova senha"
                      />
                      <button
                        type="button"
                        className="toggle-password"
                        onClick={handleTogglePassword}
                        aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                      >
                        {showPassword ? (
                          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20">
                            <path d="M12 19c.946 0 1.81-.103 2.598-.281l-1.757-1.757c-.273.021-.55.038-.841.038-5.351 0-7.424-3.846-7.926-5a8.642 8.642 0 0 1 1.508-2.297L4.184 8.305c-1.538 1.667-2.121 3.346-2.132 3.379a.994.994 0 0 0 0 .633C2.073 12.383 4.367 19 12 19zm0-14c-1.837 0-3.346.396-4.604.981L3.707 2.293 2.293 3.707l18 18 1.414-1.414-3.319-3.319c2.614-1.951 3.547-4.615 3.561-4.657a.994.994 0 0 0 0-.633C21.927 11.617 19.633 5 12 5zm4.972 10.558-2.28-2.28c.19-.39.308-.819.308-1.278 0-1.641-1.359-3-3-3-.459 0-.888.118-1.277.309L8.915 7.501A9.26 9.26 0 0 1 12 7c5.351 0 7.424 3.846 7.926 5-.302.692-1.166 2.342-2.954 3.558z"/>
                          </svg>
                        ) : (
                          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20">
                            <path d="M15 12c0 1.654-1.346 3-3 3s-3-1.346-3-3 1.346-3 3-3 3 1.346 3 3zm9-.449s-4.252 8.449-11.985 8.449c-7.18 0-12.015-8.449-12.015-8.449s4.446-7.551 12.015-7.551c7.694 0 11.985 7.551 11.985 7.551zm-7 .449c0-2.757-2.243-5-5-5s-5 2.243-5 5 2.243 5 5 5 5-2.243 5-5z"/>
                          </svg>
                        )}
                      </button>
                    </div>
                  </div>

                  <div className="form-group">
                    <label htmlFor="confirmarSenha">Confirmar Nova Senha</label>
                    <div className="password-input">
                      <input
                        type={showPassword ? "text" : "password"}
                        id="confirmarSenha"
                        name="confirmarSenha"
                        value={formData.confirmarSenha}
                        onChange={handleChange}
                        required
                        placeholder="Confirme sua nova senha"
                      />
                    </div>
                  </div>

                  <button 
                    type="submit" 
                    className="submit-button"
                    disabled={isLoading}
                  >
                    {isLoading ? 'Redefinindo...' : 'Redefinir Senha'}
                  </button>

                  <div className="back-to-login">
                    <Link to="/login">Voltar para o login</Link>
                  </div>
                </form>
              </>
            )}
          </div>
        </section>
      </main>

      <footer className="gov-footer">
        <div className="container">
          <div className="footer-links">
            <div className="footer-section">
              <h3>Sobre</h3>
              <ul>
                <li><Link to="/sobre">Sobre o sistema</Link></li>
                <li><Link to="/termos">Termos de uso</Link></li>
                <li><Link to="/privacidade">Política de privacidade</Link></li>
              </ul>
            </div>
            <div className="footer-section">
              <h3>Ajuda</h3>
              <ul>
                <li><Link to="/ajuda">Central de ajuda</Link></li>
                <li><Link to="/contato">Fale conosco</Link></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <p>© 2023 Plataforma SUMAWMA - Todos os direitos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default ResetPassword;