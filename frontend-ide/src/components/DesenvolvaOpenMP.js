import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import TextField from '@mui/material/TextField';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Logo from "../img/logo111.png";
import './PaginaEstudante.css';

function CreateOpenMPProject() {
  const [nome_projeto, setNomeProjeto] = useState('');
  const [idUser, setIdUser] = useState(null);
  const navigate = useNavigate();

  // Decodifica o token JWT e extrai o id do usuário
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const decodedToken = jwtDecode(token);
        setIdUser(decodedToken.idUser);
      } catch (error) {
        console.error("Erro ao decodificar o token:", error);
      }
    }
  }, []);

  // Atualiza o estado do nome do projeto
  const handleInputChange = (event) => {
    setNomeProjeto(event.target.value);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const token = localStorage.getItem("token");

    if (!token) {
      alert("Token de autenticação não encontrado. Por favor, faça login novamente.");
      return;
    }

    try {
      const response = await fetch("http://localhost:5000/api/openmp/projetomp", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ nome_projeto }),
      });

      const data = await response.json();

      if (response.ok) {
        const { projeto, arquivo } = data;

        console.log("Projeto criado com sucesso:", data);
        console.log(`Redirecionando para /editor-open-mp/${projeto}/${arquivo}`);

        // Verifique se os dados estão corretos antes de navegar
        console.log("ID do Projeto:", projeto);
        console.log("ID do Arquivo:", arquivo);

        if (projeto && arquivo) {
          navigate(`/editor-open-mp/${projeto}/${arquivo}`);
        } else {
          alert("Erro: ID do projeto ou ID do arquivo não encontrados.", data);
        }
      }
      else {
        console.error("Erro ao criar o projeto:", data.error);
        alert(data.error || "Erro ao criar o projeto.");
      }
    } catch (error) {
      console.error("Erro ao enviar o formulário:", error);
      alert("Erro ao criar o projeto.");
    }
  };


  return (
    <div className="pagina-estudante">
      <header className="ide-header">
        <button className="back-button" onClick={() => navigate('/area-do-aluno')}>
          <div className="circle">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="18" viewBox="0 0 22 18" fill="none">
              <path d="M6.33333 1L1 5.77876L6.33333 10.5575" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M1 5.77881H9.53333C15.4243 5.77881 20.2 10.0579 20.2 15.3363V16.2921" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
        </button>

        <h1 className="ide-title">
          <div className="logo-title">
            <img src={Logo} alt="SUMAWMA" className="logo" />
            <h2 className="title-sumawma">SUMAWMA</h2>
          </div>
        </h1>
      </header>
      <div className="container">
        <nav className="menu" aria-label="Menu principal">
          <ul>
            <li><button aria-label="Desenvolva" onClick={() => navigate("/area-do-aluno")} style={{ borderRadius: "27px", background: "#374850", width: "183px", height: "35.665px", flexShrink: "0" }}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M8 18L2 12L8 6L9.425 7.425L4.825 12.025L9.4 16.6L8 18ZM16 18L14.575 16.575L19.175 11.975L14.6 7.4L16 6L22 12L16 18Z" fill="#ACC2D0" />
            </svg> Desenvolva</button></li>
            <li><button aria-label="Arquivos"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M4 20C3.45 20 2.97917 19.8042 2.5875 19.4125C2.19583 19.0208 2 18.55 2 18V6C2 5.45 2.19583 4.97917 2.5875 4.5875C2.97917 4.19583 3.45 4 4 4H10L12 6H20C20.55 6 21.0208 6.19583 21.4125 6.5875C21.8042 6.97917 22 7.45 22 8H11.175L9.175 6H4V18L6.4 10H23.5L20.925 18.575C20.7917 19.0083 20.5458 19.3542 20.1875 19.6125C19.8292 19.8708 19.4333 20 19 20H4ZM6.1 18H19L20.8 12H7.9L6.1 18Z" fill="#ACC2D0" />
            </svg> Arquivos</button></li>
            <li><button aria-label="Tarefas"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="21" viewBox="0 0 24 21" fill="none">
              <path d="M14 8.89605V7.48602C14.55 7.29249 15.1125 7.14734 15.6875 7.05058C16.2625 6.95381 16.8667 6.90543 17.5 6.90543C17.9333 6.90543 18.3583 6.93307 18.775 6.98837C19.1917 7.04366 19.6 7.11278 20 7.19573V8.5228C19.6 8.39839 19.1958 8.30508 18.7875 8.24287C18.3792 8.18067 17.95 8.14956 17.5 8.14956C16.8667 8.14956 16.2583 8.21523 15.675 8.34655C15.0917 8.47788 14.5333 8.66104 14 8.89605ZM14 13.4579V12.0479C14.55 11.8543 15.1125 11.7092 15.6875 11.6124C16.2625 11.5156 16.8667 11.4673 17.5 11.4673C17.9333 11.4673 18.3583 11.4949 18.775 11.5502C19.1917 11.6055 19.6 11.6746 20 11.7576V13.0846C19.6 12.9602 19.1958 12.8669 18.7875 12.8047C18.3792 12.7425 17.95 12.7114 17.5 12.7114C16.8667 12.7114 16.2583 12.7736 15.675 12.898C15.0917 13.0224 14.5333 13.2091 14 13.4579ZM14 11.177V9.76694C14.55 9.57341 15.1125 9.42826 15.6875 9.33149C16.2625 9.23473 16.8667 9.18634 17.5 9.18634C17.9333 9.18634 18.3583 9.21399 18.775 9.26929C19.1917 9.32458 19.6 9.3937 20 9.47664V10.8037C19.6 10.6793 19.1958 10.586 18.7875 10.5238C18.3792 10.4616 17.95 10.4305 17.5 10.4305C16.8667 10.4305 16.2583 10.4961 15.675 10.6275C15.0917 10.7588 14.5333 10.942 14 11.177ZM6.5 13.9555C7.28333 13.9555 8.04583 14.0281 8.7875 14.1733C9.52917 14.3184 10.2667 14.5361 11 14.8264V6.6566C10.3167 6.32483 9.59167 6.076 8.825 5.91012C8.05833 5.74423 7.28333 5.66129 6.5 5.66129C5.9 5.66129 5.30417 5.70967 4.7125 5.80644C4.12083 5.90321 3.55 6.04835 3 6.24189V14.4532C3.58333 14.2873 4.1625 14.1629 4.7375 14.0799C5.3125 13.997 5.9 13.9555 6.5 13.9555ZM13 14.8264C13.7333 14.5361 14.4708 14.3184 15.2125 14.1733C15.9542 14.0281 16.7167 13.9555 17.5 13.9555C18.1 13.9555 18.6875 13.997 19.2625 14.0799C19.8375 14.1629 20.4167 14.2873 21 14.4532V6.24189C20.45 6.04835 19.8792 5.90321 19.2875 5.80644C18.6958 5.70967 18.1 5.66129 17.5 5.66129C16.7167 5.66129 15.9417 5.74423 15.175 5.91012C14.4083 6.076 13.6833 6.32483 13 6.6566V14.8264ZM12 17.2732C11.2 16.7479 10.3333 16.3401 9.4 16.0498C8.46667 15.7595 7.5 15.6144 6.5 15.6144C5.8 15.6144 5.1125 15.6904 4.4375 15.8425C3.7625 15.9945 3.11667 16.2088 2.5 16.4853C2.15 16.6373 1.8125 16.6304 1.4875 16.4645C1.1625 16.2987 1 16.0567 1 15.7388V5.74423C1 5.59217 1.04583 5.44702 1.1375 5.30878C1.22917 5.17055 1.36667 5.06687 1.55 4.99775C2.31667 4.66598 3.11667 4.41715 3.95 4.25127C4.78333 4.08538 5.63333 4.00244 6.5 4.00244C7.46667 4.00244 8.4125 4.10612 9.3375 4.31348C10.2625 4.52083 11.15 4.83187 12 5.24658C12.85 4.83187 13.7375 4.52083 14.6625 4.31348C15.5875 4.10612 16.5333 4.00244 17.5 4.00244C18.3667 4.00244 19.2167 4.08538 20.05 4.25127C20.8833 4.41715 21.6833 4.66598 22.45 4.99775C22.6333 5.06687 22.7708 5.17055 22.8625 5.30878C22.9542 5.44702 23 5.59217 23 5.74423V15.7388C23 16.0567 22.8375 16.2987 22.5125 16.4645C22.1875 16.6304 21.85 16.6373 21.5 16.4853C20.8833 16.2088 20.2375 15.9945 19.5625 15.8425C18.8875 15.6904 18.2 15.6144 17.5 15.6144C16.5 15.6144 15.5333 15.7595 14.6 16.0498C13.6667 16.3401 12.8 16.7479 12 17.2732Z" fill="#ACC2D0" />
            </svg> Tarefas</button></li>
            <li><button aria-label="Quiz"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="21" viewBox="0 0 24 21" fill="none">
              <path d="M5.55 16.3327L2 13.3882L3.4 12.227L5.525 13.9895L9.775 10.4645L11.175 11.6464L5.55 16.3327ZM5.55 9.69728L2 6.75282L3.4 5.59163L5.525 7.35416L9.775 3.8291L11.175 5.01103L5.55 9.69728ZM13 14.6738V13.015H22V14.6738H13ZM13 8.03843V6.37958H22V8.03843H13Z" fill="#ACC2D0" />
            </svg> Quiz</button></li>
          </ul>
        </nav>
        <section className="conteudo">
          <Box

            sx={{
              width: 500,
              maxWidth: "100%",
              margin: "auto",
              color: '#ffffff',
              backgroundColor: '#1b1b1b',
              padding: '20px',
              borderRadius: '15px',
              boxShadow: '0px 4px 10px rgba(0, 0, 0, 0.2)',
            }}
          >
            <form onSubmit={handleSubmit} style={{ textAlign: 'center', color: '#fff' }}>
              <TextField
                fullWidth
                placeholder="Nome do Projeto"
                value={nome_projeto}
                onChange={handleInputChange}
                margin="normal"
                required
                variant="standard"
                InputProps={{
                  style: {
                    color: "#ffffff",
                    borderBottom: "1px solid #ffffff",
                  },
                  disableUnderline: true,
                }}
              />
              <Button
                variant="outlined"
                style={{
                  borderRadius: '20px',
                  color: '#ffffff',
                  borderColor: '#ffffff',
                  padding: '10px 20px',
                }}
                type="submit"
              >
                CRIAR
              </Button>
            </form>
          </Box>
        </section>
      </div>
      <footer className="footer">
        <button aria-label="Configurações" className="configuracoes-btn">Configurações</button>
      </footer>
    </div>
  );
}

export default CreateOpenMPProject;
