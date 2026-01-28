import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from "react-router-dom";
import "./PaginaEstudante.css";
import Header from "./Header";
import Footer from "./Footer";
import { FaEdit, FaTrash, FaCode, FaPlus } from 'react-icons/fa';

const ProjetosOpenMPI = () => {
    const [projects, setProjects] = useState([]);
    const [confirmDeleteId, setConfirmDeleteId] = useState(null);
    const [editProjectId, setEditProjectId] = useState(null);
    const [newProjectName, setNewProjectName] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchProjects = async () => {
            const token = localStorage.getItem('token');
            const id_estudante = localStorage.getItem('id_estudante');

            if (!token || !id_estudante) {
                console.error('Token ou ID do estudante não encontrado');
                setIsLoading(false);
                return;
            }

            try {
                const response = await fetch(`http://localhost:5000/api/open-mpi/projetos-mpi/${id_estudante}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (!response.ok) throw new Error('Erro ao buscar projetos');
                
                const data = await response.json();
                setProjects(data);
            } catch (error) {
                console.error('Erro ao buscar projetos:', error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchProjects();
    }, []);

    const handleOpenProject = (idProjeto, idArquivo) => {
        navigate(`/editor-open-mpi/${idProjeto}/${idArquivo}`);
    };

    const handleDeleteProject = async (idProjeto) => {
        setIsLoading(true);
        const token = localStorage.getItem('token');

        try {
            const response = await fetch(`http://localhost:5000/api/open-mpi/projetos-mpi/${idProjeto}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) throw new Error('Erro ao deletar projeto');
            
            setProjects(prev => prev.filter(project => project.id_projeto !== idProjeto));
            setConfirmDeleteId(null);
        } catch (error) {
            console.error('Erro ao deletar projeto:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleUpdateProject = async (idProjeto) => {
        setIsLoading(true);
        const token = localStorage.getItem('token');

        try {
            const response = await fetch(`http://localhost:5000/api/open-mpi/projeto-mpi/${idProjeto}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ nome_projeto: newProjectName })
            });

            if (!response.ok) throw new Error('Erro ao atualizar projeto');
            
            const { projeto } = await response.json();
            setProjects(prev => prev.map(p => p.id_projeto === idProjeto ? projeto : p));
            setEditProjectId(null);
        } catch (error) {
            console.error('Erro ao atualizar projeto:', error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div>
            <Header />
            
            <main className="main-content container">
                <section className="welcome-section">
                    <h1>Meus Projetos OpenMPI</h1>
                    <p className="subtitle">Gerencie seus projetos de programação paralela</p>
                    
                    <Link to="/criar-open-mpi" className="new-project-button">
                        <FaPlus /> Novo Projeto
                    </Link>
                </section>

                <section className="projects-section">
                    {isLoading ? (
                        <div className="loading-message">Carregando projetos...</div>
                    ) : projects.length === 0 ? (
                        <div className="empty-projects">
                            <p>Você ainda não possui projetos OpenMPI</p>
                            <Link to="/criar-open-mpi" className="create-project-link">
                                Criar primeiro projeto
                            </Link>
                        </div>
                    ) : (
                        <div className="projects-grid">
                            {projects.map((project) => (
                                <div key={project.id_projeto} className="project-card">
                                    <div className="project-header">
                                        {editProjectId === project.id_projeto ? (
                                            <input
                                                type="text"
                                                value={newProjectName}
                                                onChange={(e) => setNewProjectName(e.target.value)}
                                                onKeyPress={(e) => e.key === 'Enter' && handleUpdateProject(project.id_projeto)}
                                                autoFocus
                                            />
                                        ) : (
                                            <h3 onClick={() => {
                                                setEditProjectId(project.id_projeto);
                                                setNewProjectName(project.nome_projeto);
                                            }}>
                                                {project.nome_projeto}
                                            </h3>
                                        )}
                                        <span className="project-badge">OpenMPI</span>
                                    </div>
                                    
                                    <div className="project-actions">
                                        {editProjectId === project.id_projeto ? (
                                            <>
                                                <button 
                                                    onClick={() => handleUpdateProject(project.id_projeto)}
                                                    className="action-button save"
                                                >
                                                    Salvar
                                                </button>
                                                <button 
                                                    onClick={() => setEditProjectId(null)}
                                                    className="action-button cancel"
                                                >
                                                    Cancelar
                                                </button>
                                            </>
                                        ) : (
                                            <>
                                                <button 
                                                    onClick={() => handleOpenProject(project.id_projeto, project.main_id_arquivo)}
                                                    className="action-button open"
                                                >
                                                    <FaCode /> Abrir
                                                </button>
                                                {confirmDeleteId === project.id_projeto ? (
                                                    <div className="delete-confirmation">
                                                        <p>Tem certeza?</p>
                                                        <button 
                                                            onClick={() => handleDeleteProject(project.id_projeto)}
                                                            className="confirm-button"
                                                        >
                                                            Sim
                                                        </button>
                                                        <button 
                                                            onClick={() => setConfirmDeleteId(null)}
                                                            className="cancel-button"
                                                        >
                                                            Não
                                                        </button>
                                                    </div>
                                                ) : (
                                                    <button 
                                                        onClick={() => setConfirmDeleteId(project.id_projeto)}
                                                        className="action-button delete"
                                                    >
                                                        <FaTrash /> Excluir
                                                    </button>
                                                )}
                                                <button 
                                                    onClick={() => {
                                                        setEditProjectId(project.id_projeto);
                                                        setNewProjectName(project.nome_projeto);
                                                    }}
                                                    className="action-button edit"
                                                >
                                                    <FaEdit /> Editar
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </main>

            <Footer />
        </div>
    );
};

export default ProjetosOpenMPI;