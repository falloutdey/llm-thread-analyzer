import React, { useState, useEffect } from "react";
import "./PaginaEstudante.css";
import { Link, useNavigate } from "react-router-dom";
import Logo from "../img/logo111.png";
import axios from "axios";

function Header() {
  const [userName, setUserName] = useState("Usuário");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUserName = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const token = localStorage.getItem('token');
        const userId = localStorage.getItem('id_estudante');
        
        if (!token || !userId) {
          throw new Error("Faça login para continuar");
        }

        const response = await axios.get(
          `${process.env.REACT_APP_API_URL || 'http://localhost:5000'}/api/users/user/${userId}`,
          {
            headers: { 
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          }
        );

        if (response.data && response.data.nome) {
          setUserName(response.data.nome);
        } else {
          throw new Error("Dados do usuário não encontrados");
        }
      } catch (error) {
        console.error("Erro detalhado:", error.response || error.message);
        setError(error.message);
        
        // Se for erro de autenticação, faz logout automático
        if (error.response?.status === 401) {
          handleLogout();
        }
      } finally {
        setLoading(false);
      }
    };

    fetchUserName();
  }, []);

  const handleLogout = () => {
    // Mostra confirmação
    if (window.confirm('Deseja realmente sair do sistema?')) {
      // Invalida o token no localStorage
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      
      // Opcional: Chamada API para invalidar o token no servidor
      // axios.post('/api/logout', {}, {
      //   headers: {
      //     'Authorization': `Bearer ${localStorage.getItem('token')}`
      //   }
      // });
      
      // Redireciona para a página inicial
      navigate('/', { replace: true });
      
      // Força recarregamento para limpar qualquer estado da aplicação
      window.location.reload();
    }
  };

  return (
    <div className="pagina-estudante">
      <header className="plataforma-header">
        <div className="header-top">
          <div className="container">
            <div className="logo-container">
              <img src={Logo} alt="Logo Plataforma" className="plataforma-logo" />
              <span className="system-name">SUMAWMA</span>
            </div>
            <div className="user-area">
              {error ? (
                <span className="error-message">{error}</span>
              ) : (
                <span className="welcome">
                  Bem-vindo, <strong>{loading ? 'Carregando...' : userName}</strong>
                </span>
              )}
              <button 
                className="logout-btn" 
                onClick={handleLogout}
                disabled={loading}
              >
                {loading ? '...' : 'Sair'}
              </button>
            </div>
          </div>
        </div>
        
        <nav className="main-nav">
          <div className="container">
            <ul>
              <li><Link to="/inicio">Início</Link></li>
              <li><Link to="/desenvolvimento">Desenvolvimento</Link></li>
              <li><Link to="/jogos">Gamificação</Link></li>
              <li><Link to="/configuracoes">Configurações</Link></li>
            </ul>
          </div>
        </nav>
      </header>
    </div>
  );
}

export default Header;