import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";
import Header from "./Header";
import Footer from "./Footer";
import "./Cadastro.css"; // Specific styles for this component

const Cadastro = () => {
  const [formData, setFormData] = useState({
    nome: "",
    email: "",
    matricula: "",
    senha: "",
    confirmarSenha: "",
  });

  const [message, setMessage] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
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

  const handleCadastro = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const { nome, email, matricula, senha, confirmarSenha } = formData;

      // Field validation
      if (!nome || !email || !matricula || !senha || !confirmarSenha) {
        setMessage({ type: "error", text: "Preencha todos os campos obrigatórios." });
        setIsSubmitting(false);
        return;
      }

      if (senha !== confirmarSenha) {
        setMessage({ type: "error", text: "As senhas não coincidem." });
        setIsSubmitting(false);
        return;
      }

      // API call
      const response = await axios.post("http://localhost:5000/api/users/register", {
        nome,
        email,
        matricula,
        senha,
      });

      if (response.status === 201) {
        setMessage({ type: "success", text: "Cadastro realizado com sucesso! Redirecionando..." });
        setTimeout(() => navigate("/login"), 2000);
      }
    } catch (error) {
      const errorMsg = error.response?.data?.message || "Erro no cadastro. Tente novamente.";
      setMessage({ type: "error", text: errorMsg });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="pagina-container">
      <main className="main-content container">
        <section className="welcome-section">
          <h1>Criar Nova Conta</h1>
          <p className="subtitle">Preencha os dados abaixo para se cadastrar</p>
        </section>

        <section className="content-section cadastro-form">
          {message && (
            <div className={`alert-message ${message.type}`}>
              {message.text}
            </div>
          )}

          <form onSubmit={handleCadastro}>
            <div className="form-group">
              <label htmlFor="nome">Nome Completo*</label>
              <input
                type="text"
                id="nome"
                name="nome"
                value={formData.nome}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">Email*</label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="matricula">Matrícula*</label>
              <input
                type="text"
                id="matricula"
                name="matricula"
                value={formData.matricula}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="senha">Senha*</label>
              <div className="password-input">
                <input
                  type={showPassword ? "text" : "password"}
                  id="senha"
                  name="senha"
                  value={formData.senha}
                  onChange={handleChange}
                  required
                />
                <button
                  type="button"
                  className="toggle-password"
                  onClick={handleTogglePassword}
                  aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20">
                      <path d="M480-320q75 0 127.5-52.5T660-500q0-75-52.5-127.5T480-680q-75 0-127.5 52.5T300-500q0 75 52.5 127.5T480-320Zm0-72q-45 0-76.5-31.5T372-500q0-45 31.5-76.5T480-608q45 0 76.5 31.5T588-500q0 45-31.5 76.5T480-392Zm0 192q-146 0-266-81.5T40-500q54-137 174-218.5T480-800q146 0 266 81.5T920-500q-54 137-174 218.5T480-200Zm0-300Zm0 220q113 0 207.5-59.5T832-500q-50-101-144.5-160.5T480-720q-113 0-207.5 59.5T128-500q50 101 144.5 160.5T480-280Z"/>
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20">
                      <path d="m644-428-58-58q9-47-27-88t-93-32l-58-58q17-8 34.5-12t37.5-4q75 0 127.5 52.5T660-500q0 20-4 37.5T644-428Zm128 126-58-56q38-29 67.5-63.5T832-500q-50-101-143.5-160.5T480-720q-29 0-57 4t-55 12l-62-62q41-17 84-25.5t90-8.5q151 0 269 83.5T920-500q-23 59-60.5 109.5T772-302Zm20 246L624-222q-35 11-70.5 16.5T480-200q-151 0-269-83.5T40-500q21-53 53-98.5t73-81.5L56-792l56-56 736 736-56 56ZM222-624q-29 26-53 57t-41 67q50 101 143.5 160.5T480-280q20 0 39-2.5t39-5.5l-36-38q-11 3-21 4.5t-21 1.5q-75 0-127.5-52.5T300-500q0-11 1.5-21t4.5-21l-84-82Zm319 93Zm-151 75Z"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="confirmarSenha">Confirmar Senha*</label>
              <div className="password-input">
                <input
                  type={showPassword ? "text" : "password"}
                  id="confirmarSenha"
                  name="confirmarSenha"
                  value={formData.confirmarSenha}
                  onChange={handleChange}
                  required
                />
                <button
                  type="button"
                  className="toggle-password"
                  onClick={handleTogglePassword}
                  aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20">
                      <path d="M480-320q75 0 127.5-52.5T660-500q0-75-52.5-127.5T480-680q-75 0-127.5 52.5T300-500q0 75 52.5 127.5T480-320Zm0-72q-45 0-76.5-31.5T372-500q0-45 31.5-76.5T480-608q45 0 76.5 31.5T588-500q0 45-31.5 76.5T480-392Zm0 192q-146 0-266-81.5T40-500q54-137 174-218.5T480-800q146 0 266 81.5T920-500q-54 137-174 218.5T480-200Zm0-300Zm0 220q113 0 207.5-59.5T832-500q-50-101-144.5-160.5T480-720q-113 0-207.5 59.5T128-500q50 101 144.5 160.5T480-280Z"/>
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20">
                      <path d="m644-428-58-58q9-47-27-88t-93-32l-58-58q17-8 34.5-12t37.5-4q75 0 127.5 52.5T660-500q0 20-4 37.5T644-428Zm128 126-58-56q38-29 67.5-63.5T832-500q-50-101-143.5-160.5T480-720q-29 0-57 4t-55 12l-62-62q41-17 84-25.5t90-8.5q151 0 269 83.5T920-500q-23 59-60.5 109.5T772-302Zm20 246L624-222q-35 11-70.5 16.5T480-200q-151 0-269-83.5T40-500q21-53 53-98.5t73-81.5L56-792l56-56 736 736-56 56ZM222-624q-29 26-53 57t-41 67q50 101 143.5 160.5T480-280q20 0 39-2.5t39-5.5l-36-38q-11 3-21 4.5t-21 1.5q-75 0-127.5-52.5T300-500q0-11 1.5-21t4.5-21l-84-82Zm319 93Zm-151 75Z"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="form-actions">
              <button 
                type="submit" 
                className="submit-btn"
                disabled={isSubmitting}
              >
                {isSubmitting ? "Cadastrando..." : "Cadastrar"}
              </button>
            </div>
          </form>

          <div className="login-link">
            Já tem uma conta? <Link to="/login">Faça login</Link>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default Cadastro;