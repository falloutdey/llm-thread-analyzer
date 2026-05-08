import React, { useState, useEffect, useRef } from "react";
import Editor from "@monaco-editor/react";
import axios from "axios";
import MeuEditorSalvarButton from "./MeuEditorSalvarButton";
import MeuEditorDownloadButton from "./MeuEditorDownloadButton";

const MeuEditor = ({ idArquivo, atualizarCaminho, onChange, issues = [], initialValue = "", isDark = true }) => {
  const [conteudoArquivo, setConteudoArquivo] = useState(initialValue);
  const [caminhoArquivo, setCaminhoArquivo]   = useState(null);
  const editorRef     = useRef(null); // instância do Monaco editor
  const monacoRef     = useRef(null); // objeto monaco global
  const decorationsRef = useRef([]);  // IDs das decorações atuais

  const obterLinguagem = () => {
    if (!caminhoArquivo) return "java";
    const ext = caminhoArquivo.toLowerCase();
    if (ext.endsWith(".java")) return "java";
    if (ext.endsWith(".c")) return "c";
    if (ext.endsWith(".cpp") || ext.endsWith(".c++") || ext.endsWith(".cc")) return "cpp";
    return "java";
  };

  useEffect(() => {
    if (onChange) return;
    const carregarArquivo = async () => {
      try {
        const res = await axios.get(`http://localhost:5000/api/arquivos/${idArquivo}/caminho`);
        const caminho = res.data.caminho;
        setCaminhoArquivo(caminho);
        if (atualizarCaminho) atualizarCaminho(caminho);
        const res2 = await axios.get(`http://localhost:5000/api/arquivos/conteudo`, { params: { caminho } });
        setConteudoArquivo(res2.data.conteudo);
      } catch (err) {
        console.error("Erro ao carregar arquivo:", err);
      }
    };
    if (idArquivo) carregarArquivo();
  }, [idArquivo]); // eslint-disable-line react-hooks/exhaustive-deps

  // Aplica/remove highlights sempre que issues mudar
  useEffect(() => {
    if (!editorRef.current || !monacoRef.current) return;
    const monaco = monacoRef.current;
    const editor = editorRef.current;

    const newDecorations = issues.map((issue, idx) => {
      const isError = idx % 2 === 0; // alterna vermelho/âmbar igual ao IssueCard
      return {
        range: new monaco.Range(issue.lineNumber, 1, issue.lineNumber, 1),
        options: {
          isWholeLine: true,
          // fundo da linha
          className: isError ? 'monaco-error-line' : 'monaco-warn-line',
          // ícone na gutter (margem esquerda)
          glyphMarginClassName: isError ? 'monaco-gutter-error' : 'monaco-gutter-warn',
          glyphMarginHoverMessage: { value: `⚠️ ${issue.message}` },
          // tooltip ao passar o mouse na linha
          hoverMessage: { value: `**Problema de concorrência** — Linha ${issue.lineNumber}\n\n${issue.message}` },
          overviewRuler: {
            color: isError ? '#ef4444' : '#f59e0b',
            position: monaco.editor.OverviewRulerLane.Right,
          },
        },
      };
    });

    // deltaDecorations substitui as anteriores e retorna novos IDs
    decorationsRef.current = editor.deltaDecorations(
      decorationsRef.current,
      newDecorations
    );
  }, [issues]);

  const handleChange = (newValue) => {
    setConteudoArquivo(newValue ?? "");
    if (onChange) onChange(newValue ?? "");
  };

  const handleSave = async () => {
    if (!caminhoArquivo) return;
    try {
      await axios.put(`http://localhost:8081/api/files/arquivos/conteudo`, {
        caminho: caminhoArquivo,
        conteudo: conteudoArquivo,
      });
    } catch (err) {
      console.error("Erro ao salvar:", err);
    }
  };

  // Troca tema do Monaco quando isDark muda
  useEffect(() => {
    if (!monacoRef.current) return;
    monacoRef.current.editor.setTheme(isDark ? 'vs-dark' : 'vs');
  }, [isDark]);

  const handleMount = (editor, monaco) => {
    editorRef.current  = editor;
    monacoRef.current  = monaco;
    // Aplica tema correto imediatamente ao montar
    monaco.editor.setTheme(isDark ? 'vs-dark' : 'vs');

    // Injeta CSS das decorações uma única vez
    const styleId = 'monaco-concurrency-decorations';
    if (!document.getElementById(styleId)) {
      const style = document.createElement('style');
      style.id = styleId;
      style.textContent = `
        /* Fundo da linha com erro */
        .monaco-error-line {
          background: rgba(239, 68, 68, 0.12) !important;
          border-left: 3px solid #ef4444 !important;
        }
        /* Fundo da linha com aviso */
        .monaco-warn-line {
          background: rgba(245, 158, 11, 0.10) !important;
          border-left: 3px solid #f59e0b !important;
        }
        /* Ícone de erro na gutter */
        .monaco-gutter-error::before {
          content: '●';
          color: #ef4444;
          font-size: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;
        }
        /* Ícone de aviso na gutter */
        .monaco-gutter-warn::before {
          content: '●';
          color: #f59e0b;
          font-size: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;
        }
      `;
      document.head.appendChild(style);
    }
  };

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", background: isDark ? "#0d1117" : "#ffffff" }}>
      {!onChange && (
        <div style={{ display: "flex", background: "#161b22", padding: "6px 10px", gap: 4, borderBottom: "1px solid #2a3441" }}>
          <MeuEditorSalvarButton onClick={handleSave} />
          <MeuEditorDownloadButton conteudo={conteudoArquivo} caminhoArquivo={caminhoArquivo} />
        </div>
      )}

      <div style={{ flex: 1, overflow: "hidden" }}>
        <Editor
          height="100%"
          language={obterLinguagem()}
          theme={isDark ? "vs-dark" : "vs"}
          value={conteudoArquivo}
          onChange={handleChange}
          onMount={handleMount}
          options={{
            lineNumbers: "on",
            glyphMargin: true,      // habilita a coluna de ícones na gutter
            roundedSelection: false,
            scrollBeyondLastLine: false,
            readOnly: false,
            wordWrap: "on",
            minimap: { enabled: true },
            fontSize: 15,
            fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
            scrollbar: { vertical: "visible", horizontal: "visible" },
            selectOnLineNumbers: true,
          }}
        />
      </div>
    </div>
  );
};

export default MeuEditor;