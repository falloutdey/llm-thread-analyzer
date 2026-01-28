import React, { useState } from 'react';
import { useNavigate, Link } from "react-router-dom";
import "./CriarCodigoMP.css";
import "./PaginaEstudante.css";
import Header from "./Header";
import Footer from "./Footer";
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';

const Openmpi = ({ onCreateProject }) => {
    const [nome_projeto, setNomeProjeto] = useState('');

    const handleSubmit = async (event) => {
        event.preventDefault();
        onCreateProject(nome_projeto);
    };

    const handleInputChange = (event) => {
        setNomeProjeto(event.target.value);
    };
    
    return (
        <section className="project-creation-section">
            <h2>Criar Novo Projeto OpenMP</h2>
            <p className="subtitle">Preencha os detalhes abaixo para iniciar um novo projeto</p>
            
            <Box
                sx={{
                    width: 500,
                    maxWidth: "100%",
                    margin: "20px auto",
                    color: '#ffffff',
                    backgroundColor: '#1b1b1b',
                    padding: '20px',
                    borderRadius: '15px',
                    boxShadow: '0px 4px 10px rgba(0, 0, 0, 0.2)',
                }}
            >
                <form onSubmit={handleSubmit} style={{ textAlign: 'center' }} aria-labelledby="form-title">
                    <h2 id="form-title" style={{ display: 'none' }}>Criar Projeto OpenMP</h2>
                    <TextField
                        fullWidth
                        placeholder="Nome do Projeto OpenMP"
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
                        inputProps={{
                            'aria-label': 'Nome do Projeto',
                        }}
                    />
                    <Button
                        variant="outlined"
                        style={{
                            borderRadius: '20px',
                            color: '#ffffff',
                            borderColor: '#ffffff',
                            padding: '10px 20px',
                            marginTop: '20px'
                        }}
                        type="submit"
                    >
                        CRIAR PROJETO
                    </Button>
                </form>
            </Box>
            <Link to="/projetos-open-mpi"><p className="subtitle">Visualize seus projetos criados</p></Link>

        </section>
    );
}

function CriarCodigoMPI() {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);

    const handleCreateProject = async (projectName) => {
        setIsLoading(true);
        const token = localStorage.getItem("token");
    
        if (!token) {
            alert("Token de autenticação não encontrado. Por favor, faça login novamente.");
            setIsLoading(false);
            return;
        }
    
        try {
            const response = await fetch("http://localhost:5000/api/open-mpi/projeto-mpi", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ nome_projeto: projectName }),
            });
    
            const data = await response.json();
    
            if (response.ok) {
                const { projeto, arquivo } = data;
                if (projeto && arquivo) {
                    navigate(`/editor-open-mpi/${projeto}/${arquivo}`);
                } else {
                    alert("Erro: ID do projeto ou ID do arquivo não encontrados.", data);
                }
            } else {
                alert(data.error || "Erro ao criar o projeto.");
            }
        } catch (error) {
            console.error("Erro ao enviar o formulário:", error);
            alert("Erro ao criar o projeto.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div>
            <Header />
            
            <main className="main-content container">
                <section className="welcome-section">
                    <h1>Desenvolvimento com OpenMP</h1>
                    <p className="subtitle">Crie e gerencie seus projetos de programação paralela</p>
                </section>

                <div className="project-layout">
                    <Openmpi onCreateProject={handleCreateProject} />
                    
                    <section className="project-info-section">
                        <h2>Sobre OpenMP</h2>
                        <div className="info-card">
                            <h3>O que é OpenMP?</h3>
                            <p>
                                OpenMPIé uma API para programação paralela em sistemas de memória compartilhada 
                                que utiliza diretivas de compilação para paralelizar trechos de código.
                            </p>
                        </div>

                        <div className="info-card">
                            <h3>Recursos Úteis</h3>
                            <ul className="resources-list">
                                <li><Link to="/openmpi-examples">Exemplos de Código</Link></li>
                                <li><a href="https://www.openmpi.org/">Documentação Oficial</a></li>
                                <li><Link to="/tutoriais">Tutoriais</Link></li>
                            </ul>
                        </div>
                    </section>
                </div>
            </main>

            <Footer />
        </div>
    );
}

export default CriarCodigoMPI;

// 
// 
// import React, { useState } from 'react';
// import { useNavigate, Link } from "react-router-dom";
// import "./PaginaEstudante.css";
// import "./CriarCodigoMPI.css";
// import Header from "./Header";
// import Footer from "./Footer";
// import TextField from '@mui/material/TextField';
// import Button from '@mui/material/Button';
// import Box from '@mui/material/Box';
// 
// const OpenMPIForm = ({ onCreateProject }) => {
    // const [nome_projeto, setNomeProjeto] = useState('');
// 
    // const handleSubmit = async (event) => {
        // event.preventDefault();
        // onCreateProject(nome_projeto);
    // };
// 
    // const handleInputChange = (event) => {
        // setNomeProjeto(event.target.value);
    // };
    // 
    // return (
        // <section className="project-creation-section">
            {/* <h2>Criar Novo Projeto OpenMPI</h2> */}
            {/* <p className="subtitle">Preencha os detalhes abaixo para iniciar um novo projeto de computação distribuída</p> */}
            {/*  */}
            {/* <Box */}
                // sx={{
                    // width: 500,
                    // maxWidth: "100%",
                    // margin: "20px auto",
                    // color: '#ffffff',
                    // backgroundColor: '#1b1b1b',
                    // padding: '20px',
                    // borderRadius: '15px',
                    // boxShadow: '0px 4px 10px rgba(0, 0, 0, 0.2)',
                // }}
            // >
                {/* <form onSubmit={handleSubmit} style={{ textAlign: 'center' }} aria-labelledby="form-title"> */}
                    {/* <h2 id="form-title" style={{ display: 'none' }}>Criar Projeto OpenMPI</h2> */}
                    {/* <TextField */}
                        // fullWidth
                        // placeholder="Nome do Projeto OpenMPI"
                        // value={nome_projeto}
                        // onChange={handleInputChange}
                        // margin="normal"
                        // required
                        // variant="standard"
                        // InputProps={{
                            // style: {
                                // color: "#ffffff",
                                // borderBottom: "1px solid #ffffff",
                            // },
                            // disableUnderline: true,
                        // }}
                        // inputProps={{
                            // 'aria-label': 'Nome do Projeto',
                        // }}
                    // />
                    {/* <Button */}
                        // variant="outlined"
                        // style={{
                            // borderRadius: '20px',
                            // color: '#ffffff',
                            // borderColor: '#ffffff',
                            // padding: '10px 20px',
                            // marginTop: '20px'
                        // }}
                        // type="submit"
                    // >
                        {/* CRIAR PROJETO */}
                    {/* </Button> */}
                {/* </form> */}
            {/* </Box> */}
            {/* <Link to="/projetos-open-mpi"><p className="subtitle">Visualize seus projetos criados</p></Link> */}
            {/*  */}
        {/* </section> */}
    // );
// }
// 
// function CriarCodigoMPI() {
    // const navigate = useNavigate();
    // const [isLoading, setIsLoading] = useState(false);
// 
    // const handleCreateProject = async (projectName) => {
        // setIsLoading(true);
        // const token = localStorage.getItem("token");
    // 
        // if (!token) {
            // alert("Token de autenticação não encontrado. Por favor, faça login novamente.");
            // setIsLoading(false);
            // return;
        // }
    // 
        // try {
            // const response = await fetch("http://localhost:5000/api/open-mpi/projeto-mpi", {
                // method: "POST",
                // headers: {
                    // "Content-Type": "application/json",
                    // Authorization: `Bearer ${token}`,
                // },
                // body: JSON.stringify({ nome_projeto: projectName }),
            // });
    // 
            // const data = await response.json();
    // 
            // if (response.ok) {
                // const { projeto, arquivo } = data;
                // if (projeto && arquivo) {
                    // navigate(`/editor-open-mpi/${projeto}/${arquivo}`);
                // } else {
                    // alert("Erro: ID do projeto ou ID do arquivo não encontrados.", data);
                // }
            // } else {
                // alert(data.error || "Erro ao criar o projeto.");
            // }
        // } catch (error) {
            // console.error("Erro ao enviar o formulário:", error);
            // alert("Erro ao criar o projeto.");
        // } finally {
            // setIsLoading(false);
        // }
    // };
// 
    // return (
        // <div>
            {/* <Header /> */}
            {/*  */}
            {/* <main className="main-content container"> */}
                {/* <section className="welcome-section"> */}
                    {/* <h1>Desenvolvimento com OpenMPI</h1> */}
                    {/* <p className="subtitle">Crie e gerencie seus projetos de computação distribuída</p> */}
                {/* </section> */}
{/*  */}
                {/* <div className="project-layout"> */}
                    {/* <OpenMPIForm onCreateProject={handleCreateProject} /> */}
                    {/*  */}
                    {/* <section className="project-info-section"> */}
                        {/* <h2>Sobre OpenMPI</h2> */}
                        {/* <div className="info-card"> */}
                            {/* <h3>O que é OpenMPI?</h3> */}
                            {/* <p> */}
                                {/* OpenMPI é uma implementação de código aberto do padrão MPI (Message Passing Interface)  */}
                                {/* para programação paralela em sistemas distribuídos e clusters. */}
                            {/* </p> */}
                        {/* </div> */}
{/*  */}
                        {/* <div className="info-card"> */}
                            {/* <h3>Recursos Úteis</h3> */}
                            {/* <ul className="resources-list"> */}
                                {/* <li><Link to="/openmpi/examples">Exemplos de Código</Link></li> */}
                                {/* <li><Link to="/openmpi/docs">Documentação Oficial</Link></li> */}
                                {/* <li><Link to="/openmpi/tutorials">Tutoriais</Link></li> */}
                            {/* </ul> */}
                        {/* </div> */}
{/*  */}
                        {/* <div className="info-card"> */}
                            {/* <h3>Quando usar OpenMPI?</h3> */}
                            {/* <p> */}
                                {/* OpenMPI é ideal para problemas que requerem comunicação entre processos, */}
                                {/* especialmente em clusters onde a memória não é compartilhada entre os nós. */}
                            {/* </p> */}
                        {/* </div> */}
                    {/* </section> */}
                {/* </div> */}
            {/* </main> */}
{/*  */}
            {/* <Footer /> */}
        {/* </div> */}
    // );
// }
// 
// export default CriarCodigoMPI;