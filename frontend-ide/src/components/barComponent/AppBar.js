import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../../img/logo111.png";
import "./AppBar.css";
import "./SideMenu.css";
import "./themes.css"; // Importe os temas
import "./ToggleSwitch.css";

const ToggleSwitch = ({ initialState, onToggle }) => {
  const [isOn, setIsOn] = useState(initialState);

  const handleToggle = () => {
    const newState = !isOn;
    setIsOn(newState);
    onToggle(newState);
  };

  return (
    <div
      className={`toggle-switch ${isOn ? "on" : "off"} ${isOn ? "dark" : ""}`}
      onClick={handleToggle}
      role="switch"
      aria-checked={isOn}
      tabIndex="0"
      onKeyDown={(e) => e.key === "Enter" && handleToggle()}
    >
      <div className="toggle-thumb"></div>
    </div>
  );
};

const SideMenu = ({ isOpen, onClose }) => {
  const navigate = useNavigate();

  const handleNavigation = (path) => {
    navigate(path); // Navega para a rota especificada
    onClose(); // Fecha o menu lateral
  };

  return (
    <div className={`side-menu ${isOpen ? "open" : ""}`}>
      <button onClick={onClose}>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          height="24px"
          viewBox="0 -960 960 960"
          width="24px"
          fill="#111"
        >
          <path d="m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z" />
        </svg>
      </button>
      <nav>
        <ul>
          <li>
            <button onClick={() => handleNavigation("/area-do-aluno")}>
              Área do Aluno
            </button>
          </li>
          <li>
            <button onClick={() => handleNavigation("/open-mp")}>
              Novo Código OpenMP
            </button>
          </li>
          <li>
            <button onClick={() => handleNavigation("/open-mpi")}>
              Novo Código OpenMPI
            </button>
          </li>
          <li>
            <button onClick={() => handleNavigation("/open-acc")}>
              Novo Código OpenACC
            </button>
          </li>
          <li>
            <button onClick={() => handleNavigation("/codigos")}>
              Projetos OpenMP
            </button>
          </li>
          <li>
            <button onClick={() => handleNavigation("/codigos-mpi")}>
              Projetos OpenMPI
            </button>
          </li>
          <li>
            <button onClick={() => handleNavigation("/codigos-acc")}>
              Projetos OpenACC
            </button>
          </li>
        </ul>
      </nav>
    </div>
  );
};

const AppBar = () => {
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [theme, setTheme] = useState("light-theme");

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  const toggleTheme = (newState) => {
    setTheme(newState ? "dark-theme" : "light-theme");
  };

  const handleLogout = () => {
    localStorage.removeItem('token'); // Remove o token do localStorage
    localStorage.removeItem('id_estudante'); // Remover outros itens, se necessário
    navigate('/'); // Redireciona para a rota raiz
  };

  return (
    <div className={theme}>
      <header>
        <div className="header-container">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            height="24px"
            viewBox="0 -960 960 960"
            width="24px"
            fill="#111"
            onClick={toggleMenu}
          >
            <path d="M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z" />
          </svg>
          <div className="logo-title-container">
            <img src={Logo} alt="SUMAWMA Logo" className="logo" />
            <h1>SUMAWMA</h1>
          </div>
          {/* <ToggleSwitch
            initialState={theme === 'dark-theme'}
            onToggle={toggleTheme}
          /> */}
          <button onClick={handleLogout} style={{background: "none", border: "none", cursor: "pointer"}}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              height="24px"
              viewBox="0 -960 960 960"
              width="24px"
              fill="#111"
            >
              <path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h280v80H200v560h280v80H200Zm440-160-55-58 102-102H360v-80h327L585-622l55-58 200 200-200 200Z" />
            </svg>
          </button>
        </div>
        <SideMenu isOpen={isMenuOpen} onClose={toggleMenu} />
      </header>
    </div>
  );
};

export default AppBar;
