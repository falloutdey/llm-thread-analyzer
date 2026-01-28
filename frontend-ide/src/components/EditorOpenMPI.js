import React, { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import axios from "axios";
import Editor from "@monaco-editor/react";
import Header from "./Header";
import Footer from "./Footer";
import "./EditorOpenMP.css";
import {
  FaPlay,
  FaSave,
  FaPlus,
  FaTrash,
  FaDownload,
  FaEdit,
  FaCode,
  FaBars,
} from "react-icons/fa";

function MonacoCodeEditor({ conteudo, onChange, isActive }) {
  const editorRef = useRef(null);

  return (
    <div className={`editor-container ${isActive ? "active" : ""}`}>
      <div className="editor-header">
        <FaCode className="editor-icon" />
        <span>Editor de Código</span>
      </div>
      <Editor
        height="100%"
        width="100%"
        language="cpp"
        theme="vs-dark"
        value={conteudo}
        onChange={onChange}
        options={{
          lineNumbers: "on",
          roundedSelection: false,
          scrollBeyondLastLine: false,
          wordWrap: "on",
          automaticLayout: true,
          fontSize: 16,
          minimap: { enabled: true },
          scrollbar: {
            vertical: "auto",
            horizontal: "auto",
          },
          renderWhitespace: "selection",
          tabSize: 2,
          autoIndent: "full",
        }}
        onMount={(editor) => {
          editorRef.current = editor;
          editor.focus();
        }}
      />
    </div>
  );
}

function EditorOpenMPI() {
  const [conteudoArquivo, setConteudoArquivo] = useState("");
  const [numThreads, setNumThreads] = useState("");
  const [isCompiling, setIsCompiling] = useState(false);
  const [compilationResult, setCompilationResult] = useState({
    stdout: "",
    stderr: "",
  });
  const [isLoading, setIsLoading] = useState(true);
  const [nomeProjeto, setNomeProjeto] = useState("");
  const [arquivos, setArquivos] = useState([]);
  const [showRenameDialog, setShowRenameDialog] = useState(false);
  const [arquivoParaRenomear, setArquivoParaRenomear] = useState(null);
  const [novoNomeArquivo, setNovoNomeArquivo] = useState("");
  const [activeFile, setActiveFile] = useState(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  const { idArquivo, idProjeto } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "s" && (event.ctrlKey || event.metaKey)) {
        event.preventDefault();
        handleSave();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [conteudoArquivo]);

  useEffect(() => {
    if (idProjeto) carregarProjeto();
    if (idArquivo) carregarConteudoArquivo(idArquivo);
  }, [idProjeto, idArquivo]);

  const carregarProjeto = async () => {
    try {
      const response = await axios.get(
        `http://localhost:5000/api/open-mpi/projetos-mpi/${idProjeto}/completo`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      const projeto = response.data;
      setNomeProjeto(projeto.nome_projeto);
      setArquivos(
        projeto.arquivos.map((arq) => ({
          id_arquivo: arq.id_arquivo,
          nome_arquivo: arq.nome_arquivo,
        }))
      );
    } catch (error) {
      console.error("Erro ao carregar projeto:", error);
    }
    setIsLoading(false);
  };

  const carregarConteudoArquivo = async (id) => {
    try {
      const response = await axios.get(
        `http://localhost:5000/api-mpi/arquivos/${id}/conteudo`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setConteudoArquivo(response.data.conteudo || "");
      setActiveFile(id);
    } catch (error) {
      console.error("Erro ao carregar conteúdo:", error);
    }
  };

  const handleFileClick = (idArquivo) => {
    carregarConteudoArquivo(idArquivo);
    navigate(`/editor-open-mpi/${idProjeto}/${idArquivo}`);
  };

  const handleSave = async () => {
    if (!conteudoArquivo || conteudoArquivo.includes("system")) {
      console.error("Conteúdo inválido ou contém palavra proibida");
      return;
    }

    try {
      await axios.put(
        `http://localhost:5000/api-mpi/projetos/${idProjeto}/arquivos/${idArquivo}`,
        { conteudo: conteudoArquivo },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      console.log("Arquivo salvo com sucesso");
    } catch (error) {
      console.error("Erro ao salvar:", error);
    }
  };

  const compileFile = async () => {
    setIsCompiling(true);
    try {
      const response = await axios.post(
        `http://localhost:5000/api-mpi/compilar/${idProjeto}/${idArquivo}`,
        { num_threads: numThreads },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setCompilationResult({
        stdout: response.data.result || "Execução bem-sucedida!",
        stderr: response.data.error || "Sem erros.",
        detalhes: response.data.detalhes || "",
      });
    } catch (error) {
      setCompilationResult({
        stdout: "",
        stderr: error.response?.data?.error || "Erro na compilação",
        detalhes: error.response?.data?.detalhes || "",
      });
    } finally {
      setIsCompiling(false);
    }
  };

  const criarArquivo = async () => {
    const nomeArquivo = window.prompt("Digite o nome do novo arquivo (deve terminar com .c):");
    
    if (!nomeArquivo) return; // Usuário cancelou
    
    if (!nomeArquivo.endsWith(".c")) {
      alert("O nome do arquivo deve terminar com .c");
      return;
    }

    try {
      const response = await axios.post(
        `http://localhost:5000/api-mpi/arquivo/arquivos`,
        {
          id_projeto: idProjeto,
          nome_arquivo: nomeArquivo,
          student: { nome: jwtDecode(token).nome }
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setArquivos([...arquivos, {
        id_arquivo: response.data.idArquivo,
        nome_arquivo: nomeArquivo
      }]);
    } catch (error) {
      console.error("Erro ao criar arquivo:", error);
      alert("Erro ao criar arquivo. Verifique o console para detalhes.");
    }
  };

  const deleteArquivo = async (id) => {
    try {
      await axios.delete("http://localhost:5000/api-mpi/arquivos/arquivo", {
        headers: { Authorization: `Bearer ${token}` },
        data: { id_arquivo: id },
      });
      setArquivos(arquivos.filter((arq) => arq.id_arquivo !== id));
      if (activeFile === id) {
        setConteudoArquivo("");
        setActiveFile(null);
      }
    } catch (error) {
      console.error("Erro ao deletar:", error);
    }
  };

  const renomearArquivo = async () => {
    try {
      await axios.post(
        `http://localhost:5000/api-mpi/alterar-nome-arquivo`,
        {
          id_arquivo: arquivoParaRenomear.id_arquivo,
          novo_nome_arquivo: novoNomeArquivo,
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setArquivos(
        arquivos.map((arq) =>
          arq.id_arquivo === arquivoParaRenomear.id_arquivo
            ? { ...arq, nome_arquivo: novoNomeArquivo }
            : arq
        )
      );
      setShowRenameDialog(false);
    } catch (error) {
      console.error("Erro ao renomear:", error);
    }
  };

  const handleDownload = () => {
    const blob = new Blob([conteudoArquivo], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "main.c";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="editor-page">
      <Header />

      <main className="editor-main-container">
        <div className={`sidebar ${sidebarCollapsed ? "collapsed" : ""}`}>
          <button
            className="sidebar-toggle"
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          >
            <FaBars />
          </button>

          {!sidebarCollapsed && (
            <>
              <div className="project-header">
                <h3>{nomeProjeto || "Carregando..."}</h3>
                <button className="new-file-btn" onClick={criarArquivo}>
                  <FaPlus /> Novo Arquivo
                </button>
              </div>

              <div className="file-list">
                {arquivos.map((arq) => (
                  <div
                    key={arq.id_arquivo}
                    className={`file-item ${
                      activeFile === arq.id_arquivo ? "active" : ""
                    }`}
                    onClick={() => handleFileClick(arq.id_arquivo)}
                  >
                    <span>{arq.nome_arquivo}</span>
                    <div className="file-actions">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setArquivoParaRenomear(arq);
                          setNovoNomeArquivo(arq.nome_arquivo);
                          setShowRenameDialog(true);
                        }}
                      >
                        <FaEdit />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (
                            window.confirm("Tem certeza que deseja deletar?")
                          ) {
                            deleteArquivo(arq.id_arquivo);
                          }
                        }}
                      >
                        <FaTrash />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        <div className="editor-content-area">
          <div className="editor-header">
            {/* <FaCode className="editor-icon" />
            <span>
              Editor de Código -{" "}
              {arquivos.find((f) => f.id_arquivo === activeFile)
                ?.nome_arquivo || "Nenhum arquivo selecionado"}
            </span> */}
          </div>

          <div className="monaco-editor-container">
            {isLoading ? (
              <div className="loading-editor">Carregando editor...</div>
            ) : (
              <Editor
                height="100%"
                width="100%"
                language="cpp"
                theme="vs-dark"
                value={conteudoArquivo}
                onChange={setConteudoArquivo}
                options={{
                  lineNumbers: "on",
                  roundedSelection: false,
                  scrollBeyondLastLine: true,
                  wordWrap: "on",
                  automaticLayout: true,
                  fontSize: 14,
                  minimap: { enabled: true },
                  scrollbar: {
                    vertical: "auto",
                    horizontal: "auto",
                  },
                  renderWhitespace: "selection",
                  tabSize: 2,
                  autoIndent: "full",
                  folding: true,
                  glyphMargin: true,
                  lineDecorationsWidth: 10,
                  renderLineHighlight: "all",
                  readOnly: !activeFile,
                }}
                onMount={(editor) => {
                  editor.focus();
                }}
              />
            )}
          </div>

          <div className="actions-panel">
            <div className="action-buttons">
              <button onClick={handleSave} disabled={!activeFile}>
                <FaSave /> Salvar (Ctrl+S)
              </button>
              <div className="threads-input">
                <input
                  type="number"
                  value={numThreads}
                  onChange={(e) => setNumThreads(e.target.value)}
                  placeholder="Nº Processos"
                  min="1"
                />
              </div>
              <button
                onClick={compileFile}
                disabled={isCompiling || !activeFile}
              >
                <FaPlay /> {isCompiling ? "Compilando..." : "Executar"}
              </button>
              <button onClick={handleDownload} disabled={!activeFile}>
                <FaDownload /> Download
              </button>
            </div>

            <div className="compilation-output">
              <h4>Saída do Programa:</h4>
              <pre
                className={`output ${compilationResult.stderr ? "error" : ""}`}
              >
                {compilationResult.stdout ||
                  compilationResult.stderr ||
                  "Execute seu código para ver os resultados aqui..."}
              </pre>
            </div>
          </div>
        </div>
      </main>

      {showRenameDialog && (
        <div className="dialog-overlay">
          <div className="dialog">
            <h3>Renomear Arquivo</h3>
            <input
              type="text"
              value={novoNomeArquivo}
              onChange={(e) => setNovoNomeArquivo(e.target.value)}
              placeholder="Novo nome"
              autoFocus
            />
            <div className="dialog-buttons">
              <button onClick={() => setShowRenameDialog(false)}>
                Cancelar
              </button>
              <button onClick={renomearArquivo}>Salvar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default EditorOpenMPI;