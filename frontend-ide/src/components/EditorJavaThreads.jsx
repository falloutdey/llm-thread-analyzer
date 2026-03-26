import React, { useState, useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import axios from 'axios';
import MeuEditor from '../ideComponents/MeuEditor';
import styled, { keyframes, createGlobalStyle } from 'styled-components';

const GlobalStyle = createGlobalStyle`
  @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&display=swap');
`;

const slideIn = keyframes`
  from { opacity: 0; transform: translateX(16px); }
  to   { opacity: 1; transform: translateX(0); }
`;

const fadeIn = keyframes`
  from { opacity: 0; }
  to   { opacity: 1; }
`;

const pulse = keyframes`
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
`;

const C = {
  bg0:         '#0d1117',
  bg1:         '#161b22',
  bg2:         '#1c2230',
  bg3:         '#21293a',
  border:      '#2a3441',
  accent:      '#3b82f6',
  accentHover: '#2563eb',
  green:       '#10b981',
  greenDim:    '#059669',
  red:         '#ef4444',
  amber:       '#f59e0b',
  textPri:     '#e2e8f0',
  textSec:     '#8b95a6',
  textDim:     '#4a5568',
  lineNum:     '#3d4a5c',
};

/* ── Shell ── */
const Shell = styled.div`
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: ${C.bg0};
  color: ${C.textPri};
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  overflow: hidden;
  user-select: none;
`;

/* ── Titlebar ── */
const Titlebar = styled.div`
  height: 40px;
  background: ${C.bg1};
  border-bottom: 1px solid ${C.border};
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  flex-shrink: 0;
  z-index: 10;
`;

const TitleDots = styled.div`
  display: flex;
  gap: 6px;
`;

const Dot = styled.div`
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: ${p => p.color};
`;

const TitleText = styled.span`
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: ${C.textDim};
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.02em;
`;

const RunButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: ${p => p.running ? C.bg3 : C.green};
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 500;
  cursor: ${p => p.running ? 'not-allowed' : 'pointer'};
  transition: background 0.15s;
  letter-spacing: 0.02em;
  &:hover:not(:disabled) { background: ${C.greenDim}; }
  svg { width: 10px; height: 10px; flex-shrink: 0; }
`;

const Spinner = styled.div`
  width: 10px;
  height: 10px;
  border: 1.5px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  @keyframes spin { to { transform: rotate(360deg); } }
`;

const Body = styled.div`
  flex: 1;
  display: flex;
  overflow: hidden;
`;

/* ── Sidebar ── */
const Sidebar = styled.div`
  width: ${p => p.open ? '220px' : '40px'};
  background: ${C.bg1};
  border-right: 1px solid ${C.border};
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s ease;
  overflow: hidden;
`;

const SidebarHeader = styled.div`
  height: 36px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  gap: 6px;
  border-bottom: 1px solid ${C.border};
  flex-shrink: 0;
`;

const SidebarTitle = styled.span`
  font-size: 10px;
  color: ${C.textDim};
  text-transform: uppercase;
  letter-spacing: 0.1em;
  white-space: nowrap;
  font-family: 'JetBrains Mono', monospace;
  flex: 1;
  overflow: hidden;
`;

const CollapseBtn = styled.button`
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: ${C.textDim};
  flex-shrink: 0;
  padding: 0;
  transition: color 0.15s;
  &:hover { color: ${C.textSec}; }
`;

const IconBtn = styled.button`
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: ${C.textDim};
  flex-shrink: 0;
  padding: 0;
  border-radius: 3px;
  transition: color 0.15s, background 0.15s;
  &:hover { color: ${C.textPri}; background: ${C.bg3}; }
  opacity: ${p => p.open ? 1 : 0};
  pointer-events: ${p => p.open ? 'auto' : 'none'};
`;

const SidebarContent = styled.div`
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 6px 0;
  opacity: ${p => p.open ? 1 : 0};
  transition: opacity 0.15s;
  pointer-events: ${p => p.open ? 'auto' : 'none'};

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: ${C.border}; border-radius: 2px; }
`;

const EmptySidebar = styled.div`
  padding: 16px 14px;
  font-size: 11px;
  color: ${C.textDim};
  font-family: 'JetBrains Mono', monospace;
  line-height: 1.7;
  text-align: center;
`;

/* ── Tree items ── */
const TreeRow = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px 3px ${p => 8 + p.depth * 14}px;
  font-size: 12px;
  color: ${p => p.active ? C.accent : C.textSec};
  background: ${p => p.active ? 'rgba(59,130,246,0.1)' : 'transparent'};
  cursor: pointer;
  white-space: nowrap;
  border-left: ${p => p.active ? `2px solid ${C.accent}` : '2px solid transparent'};
  transition: background 0.1s, color 0.1s;
  position: relative;

  &:hover { background: ${C.bg2}; color: ${C.textPri}; }
  &:hover .row-actions { opacity: 1; pointer-events: auto; }
`;

const RowName = styled.span`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const RowActions = styled.div`
  className: row-actions;
  display: flex;
  gap: 2px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
  margin-left: auto;
`;

const RowActionBtn = styled.button`
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: ${C.textDim};
  border-radius: 3px;
  padding: 0;
  transition: color 0.15s, background 0.15s;
  &:hover { color: ${C.textPri}; background: ${C.bg3}; }
`;

/* ── Inline input (rename / create) ── */
const InlineInput = styled.input`
  flex: 1;
  background: ${C.bg3};
  border: 1px solid ${C.accent};
  border-radius: 3px;
  color: ${C.textPri};
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  padding: 1px 5px;
  outline: none;
  min-width: 0;
`;

/* ── Context menu ── */
const CtxMenu = styled.div`
  position: fixed;
  background: ${C.bg2};
  border: 1px solid ${C.border};
  border-radius: 6px;
  padding: 4px 0;
  z-index: 1000;
  min-width: 160px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  animation: ${fadeIn} 0.12s ease;
`;

const CtxItem = styled.div`
  padding: 6px 14px;
  font-size: 12px;
  color: ${p => p.danger ? C.red : C.textSec};
  cursor: pointer;
  font-family: 'JetBrains Mono', monospace;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.1s, color 0.1s;
  &:hover {
    background: ${C.bg3};
    color: ${p => p.danger ? '#fca5a5' : C.textPri};
  }
`;

const CtxSep = styled.div`
  height: 1px;
  background: ${C.border};
  margin: 4px 0;
`;

/* ── Editor area ── */
const EditorArea = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
`;

const TabBar = styled.div`
  height: 36px;
  background: ${C.bg1};
  border-bottom: 1px solid ${C.border};
  display: flex;
  align-items: flex-end;
  flex-shrink: 0;
  overflow-x: auto;
  &::-webkit-scrollbar { display: none; }
`;

const Tab = styled.div`
  height: 32px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  font-size: 12px;
  color: ${p => p.active ? C.textPri : C.textDim};
  background: ${p => p.active ? C.bg0 : 'transparent'};
  border-right: 1px solid ${C.border};
  border-top: ${p => p.active ? `1px solid ${C.accent}` : '1px solid transparent'};
  cursor: pointer;
  white-space: nowrap;
  gap: 8px;
  transition: color 0.1s;
  &:hover { color: ${C.textSec}; }
`;

const TabDot = styled.div`
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: ${C.amber};
  animation: ${pulse} 2s ease-in-out infinite;
`;

const RightPanels = styled.div`
  width: ${p => p.open ? '340px' : '0px'};
  background: ${C.bg1};
  border-left: ${p => p.open ? `1px solid ${C.border}` : 'none'};
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.25s ease;
  flex-shrink: 0;
`;

const PanelHeader = styled.div`
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 8px;
  border-bottom: 1px solid ${C.border};
  flex-shrink: 0;
`;

const PanelLabel = styled.span`
  font-size: 10px;
  color: ${C.textDim};
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-family: 'JetBrains Mono', monospace;
`;

const PanelDot = styled.div`
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: ${p => p.color};
`;

const FeedbackBody = styled.div`
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  animation: ${fadeIn} 0.3s ease;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: ${C.border}; border-radius: 2px; }
`;

const AlertCard = styled.div`
  background: ${C.bg2};
  border-left: 3px solid ${p => p.color || C.red};
  border-radius: 0 6px 6px 0;
  padding: 10px 12px;
  margin-bottom: 10px;
  animation: ${slideIn} 0.25s ease forwards;
  animation-delay: ${p => p.delay || '0s'};
  opacity: 0;
`;

const AlertTitle = styled.div`
  font-size: 11px;
  color: ${p => p.color || '#fca5a5'};
  font-weight: 500;
  margin-bottom: 6px;
  line-height: 1.4;
`;

const AlertBody = styled.div`
  font-size: 11px;
  color: ${C.green};
  line-height: 1.6;
  white-space: pre-wrap;
  font-family: sans-serif;
`;

const SuccessCard = styled.div`
  background: rgba(16,185,129,0.08);
  border-left: 3px solid ${C.green};
  border-radius: 0 6px 6px 0;
  padding: 12px;
  animation: ${fadeIn} 0.3s ease;
`;

const EmptyState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 10px;
  color: ${C.textDim};
  font-size: 12px;
  text-align: center;
  padding: 20px;
  font-family: 'JetBrains Mono', monospace;
`;

const TerminalWrapper = styled.div`
  height: 110px;
  background: ${C.bg0};
  border-top: 1px solid ${C.border};
  flex-shrink: 0;
  padding: 10px 14px;
  overflow-y: auto;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  animation: ${fadeIn} 0.3s ease;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: ${C.border}; border-radius: 2px; }
`;

const TermLine = styled.div`
  line-height: 1.7;
  color: ${p =>
    p.type === 'prompt' ? '#4ade80' :
    p.type === 'error'  ? C.red :
    p.type === 'warn'   ? C.amber :
    p.type === 'ok'     ? C.green :
    C.textDim};
`;

const Statusbar = styled.div`
  height: 22px;
  background: ${C.accent};
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 16px;
  flex-shrink: 0;
`;

const SbItem = styled.span`
  font-size: 11px;
  color: rgba(255,255,255,${p => p.dim ? '0.65' : '1'});
  font-family: 'JetBrains Mono', monospace;
`;

const SbAlert = styled.span`
  font-size: 11px;
  color: ${p => p.color || '#fff'};
  font-family: 'JetBrains Mono', monospace;
  display: flex;
  align-items: center;
  gap: 4px;
`;

/* ── Icons ── */
const IconFolder = ({ open }) => (
  <svg width="13" height="13" viewBox="0 0 16 16" fill={open ? '#fcd34d' : '#f59e0b'} style={{ flexShrink: 0 }}>
    {open
      ? <path d="M1 4.5A1.5 1.5 0 012.5 3h3.764c.516 0 1.01.245 1.328.664L8.5 5H14a1 1 0 011 1v6.5A1.5 1.5 0 0113.5 14h-11A1.5 1.5 0 011 12.5v-8z"/>
      : <path d="M1 3.5A1.5 1.5 0 012.5 2h3.764c.516 0 1.01.245 1.328.664L8.5 4H13.5A1.5 1.5 0 0115 5.5v7A1.5 1.5 0 0113.5 14h-11A1.5 1.5 0 011 12.5v-9z"/>
    }
  </svg>
);

const IconFile = ({ color = '#8b95a6' }) => (
  <svg width="12" height="12" viewBox="0 0 16 16" fill={color} style={{ flexShrink: 0 }}>
    <rect x="2" y="1" width="12" height="14" rx="2"/>
    <path d="M5 5h6M5 8h6M5 11h4" stroke="#1a1f2e" strokeWidth="1.2" fill="none"/>
  </svg>
);

const IconChevron = ({ open }) => (
  <svg width="10" height="10" viewBox="0 0 10 10" fill="none"
    style={{ flexShrink: 0, transform: open ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.15s' }}>
    <path d="M3 2l4 3-4 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

/* ══════════════════════
   TREE HELPERS
══════════════════════ */

// Estrutura de nó: { id, name, type: 'folder'|'file', children: [], expanded: bool }
let _nextId = 1;
const newId = () => String(_nextId++);

const newFile = (name = 'Arquivo.java') => ({
  id: newId(), name, type: 'file', children: [],
});

const newFolder = (name = 'pasta') => ({
  id: newId(), name, type: 'folder', children: [], expanded: true,
});

// Percorre a árvore e aplica uma função de atualização no nó com o id dado
const updateNode = (nodes, id, updater) =>
  nodes.map(n => {
    if (n.id === id) return updater(n);
    if (n.children?.length) return { ...n, children: updateNode(n.children, id, updater) };
    return n;
  });

// Remove um nó pelo id
const removeNode = (nodes, id) =>
  nodes
    .filter(n => n.id !== id)
    .map(n => n.children?.length ? { ...n, children: removeNode(n.children, id) } : n);

// Insere filho em um folder
const insertChild = (nodes, parentId, child) =>
  updateNode(nodes, parentId, n => ({ ...n, children: [...n.children, child] }));

// Achata a árvore para pegar todos os arquivos
const flatFiles = (nodes, acc = []) => {
  for (const n of nodes) {
    if (n.type === 'file') acc.push(n);
    if (n.children?.length) flatFiles(n.children, acc);
  }
  return acc;
};

/* ══════════════════════
   COMPONENTE SIDEBAR TREE
══════════════════════ */
const SidebarTree = ({ tree, setTree, activeFileId, setActiveFileId }) => {
  const [editingId, setEditingId]   = useState(null); // id do nó sendo renomeado/criado
  const [editValue, setEditValue]   = useState('');
  const [creatingIn, setCreatingIn] = useState(null); // { parentId, type }
  const [ctxMenu, setCtxMenu]       = useState(null); // { x, y, node }
  const inputRef = useRef(null);

  useEffect(() => {
    if (inputRef.current) inputRef.current.focus();
  }, [editingId, creatingIn]);

  // fecha ctx menu ao clicar fora
  useEffect(() => {
    const close = () => setCtxMenu(null);
    window.addEventListener('click', close);
    return () => window.removeEventListener('click', close);
  }, []);

  const startCreate = (parentId, type) => {
    setCreatingIn({ parentId, type });
    setEditValue(type === 'file' ? 'Arquivo.java' : 'pacote');
    // expande a pasta pai
    if (parentId !== 'root') {
      setTree(t => updateNode(t, parentId, n => ({ ...n, expanded: true })));
    }
  };

  const commitCreate = () => {
    if (!creatingIn || !editValue.trim()) { setCreatingIn(null); return; }
    const name = editValue.trim();
    const child = creatingIn.type === 'file' ? newFile(name) : newFolder(name);
    if (creatingIn.parentId === 'root') {
      setTree(t => [...t, child]);
    } else {
      setTree(t => insertChild(t, creatingIn.parentId, child));
    }
    if (creatingIn.type === 'file') setActiveFileId(child.id);
    setCreatingIn(null);
  };

  const startRename = (node) => {
    setEditingId(node.id);
    setEditValue(node.name);
    setCtxMenu(null);
  };

  const commitRename = (id) => {
    if (editValue.trim()) {
      setTree(t => updateNode(t, id, n => ({ ...n, name: editValue.trim() })));
    }
    setEditingId(null);
  };

  const deleteNode = (id) => {
    setTree(t => removeNode(t, id));
    if (activeFileId === id) setActiveFileId(null);
    setCtxMenu(null);
  };

  const toggleExpand = (id) => {
    setTree(t => updateNode(t, id, n => ({ ...n, expanded: !n.expanded })));
  };

  const onCtxMenu = (e, node) => {
    e.preventDefault();
    e.stopPropagation();
    setCtxMenu({ x: e.clientX, y: e.clientY, node });
  };

  const renderNodes = (nodes, depth = 0) => {
    const items = [];

    nodes.forEach(node => {
      const isEditing = editingId === node.id;

      items.push(
        <TreeRow
          key={node.id}
          depth={depth}
          active={node.type === 'file' && activeFileId === node.id}
          onClick={() => {
            if (node.type === 'folder') toggleExpand(node.id);
            else setActiveFileId(node.id);
          }}
          onContextMenu={e => onCtxMenu(e, node)}
        >
          {node.type === 'folder' && (
            <IconChevron open={node.expanded} />
          )}
          {node.type === 'folder'
            ? <IconFolder open={node.expanded} />
            : <IconFile color={activeFileId === node.id ? '#60a5fa' : '#8b95a6'} />
          }

          {isEditing ? (
            <InlineInput
              ref={inputRef}
              value={editValue}
              onChange={e => setEditValue(e.target.value)}
              onBlur={() => commitRename(node.id)}
              onKeyDown={e => {
                if (e.key === 'Enter') commitRename(node.id);
                if (e.key === 'Escape') setEditingId(null);
              }}
              onClick={e => e.stopPropagation()}
            />
          ) : (
            <RowName>{node.name}</RowName>
          )}

          {!isEditing && (
            <RowActions className="row-actions" style={{ opacity: undefined }}>
              {node.type === 'folder' && (
                <>
                  <RowActionBtn
                    title="Novo arquivo"
                    onClick={e => { e.stopPropagation(); startCreate(node.id, 'file'); }}
                  >
                    <svg width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <rect x="1" y="1" width="7" height="9" rx="1"/>
                      <path d="M9 7v4M7 9h4" strokeLinecap="round"/>
                    </svg>
                  </RowActionBtn>
                  <RowActionBtn
                    title="Nova pasta"
                    onClick={e => { e.stopPropagation(); startCreate(node.id, 'folder'); }}
                  >
                    <svg width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <path d="M1 3a1 1 0 011-1h2.5l1 1.5H10a1 1 0 011 1v4a1 1 0 01-1 1H2a1 1 0 01-1-1V3z"/>
                      <path d="M6 5v3M4.5 6.5h3" strokeLinecap="round"/>
                    </svg>
                  </RowActionBtn>
                </>
              )}
              <RowActionBtn
                title="Renomear"
                onClick={e => { e.stopPropagation(); startRename(node); }}
              >
                <svg width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M2 9.5h2l4.5-4.5-2-2L2 7.5v2z" strokeLinejoin="round"/>
                  <path d="M6.5 3l2 2" strokeLinecap="round"/>
                </svg>
              </RowActionBtn>
              <RowActionBtn
                title="Excluir"
                onClick={e => { e.stopPropagation(); deleteNode(node.id); }}
                style={{ color: C.textDim }}
              >
                <svg width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M2 3h8M5 3V2h2v1M4 3l.5 7h3L8 3" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </RowActionBtn>
            </RowActions>
          )}
        </TreeRow>
      );

      // se folder expandido, renderiza filhos
      if (node.type === 'folder' && node.expanded) {
        items.push(...renderNodes(node.children, depth + 1));

        // input de criação dentro dessa pasta
        if (creatingIn?.parentId === node.id) {
          items.push(
            <TreeRow key="creating" depth={depth + 1} active={false}>
              {creatingIn.type === 'folder'
                ? <IconFolder open={false} />
                : <IconFile color="#60a5fa" />
              }
              <InlineInput
                ref={inputRef}
                value={editValue}
                onChange={e => setEditValue(e.target.value)}
                onBlur={commitCreate}
                onKeyDown={e => {
                  if (e.key === 'Enter') commitCreate();
                  if (e.key === 'Escape') setCreatingIn(null);
                }}
              />
            </TreeRow>
          );
        }
      }
    });

    return items;
  };

  const hasContent = tree.length > 0 || creatingIn?.parentId === 'root';

  return (
    <>
      {!hasContent && !creatingIn ? (
        <EmptySidebar>
          Nenhum arquivo ainda.<br />
          Use os botões acima para criar.
        </EmptySidebar>
      ) : (
        <>
          {renderNodes(tree)}
          {creatingIn?.parentId === 'root' && (
            <TreeRow depth={0} active={false}>
              {creatingIn.type === 'folder'
                ? <IconFolder open={false} />
                : <IconFile color="#60a5fa" />
              }
              <InlineInput
                ref={inputRef}
                value={editValue}
                onChange={e => setEditValue(e.target.value)}
                onBlur={commitCreate}
                onKeyDown={e => {
                  if (e.key === 'Enter') commitCreate();
                  if (e.key === 'Escape') setCreatingIn(null);
                }}
              />
            </TreeRow>
          )}
        </>
      )}

      {/* Context Menu */}
      {ctxMenu && (
        <CtxMenu style={{ top: ctxMenu.y, left: ctxMenu.x }} onClick={e => e.stopPropagation()}>
          {ctxMenu.node.type === 'folder' && (
            <>
              <CtxItem onClick={() => { startCreate(ctxMenu.node.id, 'file'); setCtxMenu(null); }}>
                <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <rect x="1" y="1" width="7" height="9" rx="1"/>
                  <path d="M9 7v4M7 9h4" strokeLinecap="round"/>
                </svg>
                Novo arquivo
              </CtxItem>
              <CtxItem onClick={() => { startCreate(ctxMenu.node.id, 'folder'); setCtxMenu(null); }}>
                <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M1 3a1 1 0 011-1h2.5l1 1.5H10a1 1 0 011 1v4a1 1 0 01-1 1H2a1 1 0 01-1-1V3z"/>
                  <path d="M6 5v3M4.5 6.5h3" strokeLinecap="round"/>
                </svg>
                Nova pasta
              </CtxItem>
              <CtxSep />
            </>
          )}
          <CtxItem onClick={() => startRename(ctxMenu.node)}>
            <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 9.5h2l4.5-4.5-2-2L2 7.5v2z" strokeLinejoin="round"/>
            </svg>
            Renomear
          </CtxItem>
          <CtxItem danger onClick={() => deleteNode(ctxMenu.node.id)}>
            <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 3h8M5 3V2h2v1M4 3l.5 7h3L8 3" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Excluir
          </CtxItem>
        </CtxMenu>
      )}
    </>
  );
};

/* ══════════════════════════════════════════
   COMPONENTE PRINCIPAL
══════════════════════════════════════════ */
const EditorJavaThreads = () => {
  const { idArquivo } = useParams();
  const [caminhoArquivo, setCaminhoArquivo]     = useState(null);
  const [codigoFonte, setCodigoFonte]           = useState('');
  const [resultadoAnalise, setResultadoAnalise] = useState(null);
  const [isAnalisando, setIsAnalisando]         = useState(false);
  const [sidebarOpen, setSidebarOpen]           = useState(true);
  const [painelOpen, setPainelOpen]             = useState(false);
  const [terminalLines, setTerminalLines]       = useState([]);
  const [statusMsg, setStatusMsg]               = useState('Pronto');
  const [alertCount, setAlertCount]             = useState(0);
  const feedbackRef = useRef(null);

  // Árvore de arquivos dinâmica
  const [tree, setTree]               = useState([]);
  const [activeFileId, setActiveFileId] = useState(null);

  // Nome do arquivo ativo derivado
  const activeFile = flatFiles(tree).find(f => f.id === activeFileId) || null;
  const nomeArquivoAtivo = activeFile?.name || 'CodigoAluno.java';

  const addTermLine = (text, type = 'default') =>
    setTerminalLines(prev => [...prev, { text, type }]);

  const analisarCodigo = async () => {
    if (!codigoFonte.trim()) {
      alert('Escreva algum código Java antes de analisar.');
      return;
    }
    setIsAnalisando(true);
    setResultadoAnalise(null);
    setPainelOpen(true);
    setTerminalLines([]);
    setStatusMsg('Analisando...');
    setAlertCount(0);
    addTermLine(`$ javac ${nomeArquivoAtivo}`, 'prompt');

    try {
      const response = await axios.post('http://localhost:8081/api/files/analisar', {
        fileName: nomeArquivoAtivo,
        content: codigoFonte,
      });
      const data = response.data;
      setResultadoAnalise(data);
      const count = data.issues?.length || 0;
      setAlertCount(count);
      if (count > 0) {
        addTermLine('Compilado com sucesso', 'ok');
        data.issues.forEach(issue => {
          addTermLine(`WARN: ${issue.message} (linha ${issue.lineNumber})`, 'warn');
        });
        setStatusMsg(`${count} alerta${count > 1 ? 's' : ''} encontrado${count > 1 ? 's' : ''}`);
      } else {
        addTermLine('Compilado com sucesso', 'ok');
        addTermLine('Nenhum problema de concorrência detectado.', 'ok');
        setStatusMsg('Sem problemas detectados');
      }
    } catch (error) {
      const msg = error.response?.data || 'Erro de conexão com o backend.';
      setResultadoAnalise({ error: typeof msg === 'string' ? msg : 'Erro interno no servidor.' });
      addTermLine('ERRO: ' + (typeof msg === 'string' ? msg.split('\n')[0] : 'Erro interno'), 'error');
      setStatusMsg('Erro na análise');
    } finally {
      setIsAnalisando(false);
    }
  };

  useEffect(() => {
    if (feedbackRef.current) feedbackRef.current.scrollTop = 0;
  }, [resultadoAnalise]);

  const renderFeedback = () => {
    if (!resultadoAnalise) {
      return (
        <EmptyState>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke={C.textDim} strokeWidth="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 8v4M12 16h.01"/>
          </svg>
          Execute o código para ver o feedback
        </EmptyState>
      );
    }
    if (resultadoAnalise.error) {
      return (
        <AlertCard color={C.red} delay="0s">
          <AlertTitle>Erro na análise</AlertTitle>
          <AlertBody style={{ color: '#fca5a5' }}>{resultadoAnalise.error}</AlertBody>
        </AlertCard>
      );
    }
    if (!resultadoAnalise.issues || resultadoAnalise.issues.length === 0) {
      return (
        <SuccessCard>
          <AlertTitle style={{ color: '#6ee7b7' }}>Nenhum problema detectado</AlertTitle>
          <AlertBody>O código não apresenta problemas de concorrência ou deadlocks.</AlertBody>
        </SuccessCard>
      );
    }
    return resultadoAnalise.issues.map((issue, i) => (
      <AlertCard key={i} color={i % 2 === 0 ? C.red : C.amber} delay={`${i * 0.08}s`}>
        <AlertTitle color={i % 2 === 0 ? '#fca5a5' : '#fcd34d'}>
          Linha {issue.lineNumber}: {issue.message}
        </AlertTitle>
        <AlertBody>{issue.interpretation}</AlertBody>
      </AlertCard>
    ));
  };

  return (
    <>
      <GlobalStyle />
      <Shell>

        {/* ── Titlebar ── */}
        <Titlebar>
          <TitleDots>
            <Dot color="#ff5f57" />
            <Dot color="#febc2e" />
            <Dot color="#28c840" />
          </TitleDots>
          <TitleText>
            {nomeArquivoAtivo} — Analisador de Threads Java
          </TitleText>
          <RunButton running={isAnalisando} onClick={analisarCodigo} disabled={isAnalisando}>
            {isAnalisando ? (
              <><Spinner /> A processar...</>
            ) : (
              <>
                <svg viewBox="0 0 10 10" fill="white"><polygon points="1,1 9,5 1,9"/></svg>
                Executar e analisar
              </>
            )}
          </RunButton>
        </Titlebar>

        {/* ── Body ── */}
        <Body>

          {/* ── Sidebar ── */}
          <Sidebar open={sidebarOpen}>
            <SidebarHeader>
              {sidebarOpen && (
                <>
                  <svg width="12" height="12" viewBox="0 0 16 16" fill={C.textDim} style={{ flexShrink: 0 }}>
                    <path d="M2 2h4l2 2h6v10H2V2z"/>
                  </svg>
                  <SidebarTitle>Explorador</SidebarTitle>
                  {/* Botão novo arquivo na raiz */}
                  <IconBtn
                    open={sidebarOpen}
                    title="Novo arquivo"
                    onClick={() => {
                      /* expõe o SidebarTree via ref seria complexo;
                         usamos um estado de criação direto */
                      setTree(t => {
                        const f = newFile('Arquivo.java');
                        // setar ativo após render
                        setTimeout(() => setActiveFileId(f.id), 0);
                        return [...t, f];
                      });
                    }}
                  >
                    <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <rect x="1" y="1" width="7" height="9" rx="1"/>
                      <path d="M9 7v4M7 9h4" strokeLinecap="round"/>
                    </svg>
                  </IconBtn>
                  {/* Botão nova pasta na raiz */}
                  <IconBtn
                    open={sidebarOpen}
                    title="Nova pasta"
                    onClick={() => {
                      setTree(t => [...t, newFolder('pacote')]);
                    }}
                  >
                    <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <path d="M1 3a1 1 0 011-1h2.5l1 1.5H10a1 1 0 011 1v4a1 1 0 01-1 1H2a1 1 0 01-1-1V3z"/>
                      <path d="M6 5v3M4.5 6.5h3" strokeLinecap="round"/>
                    </svg>
                  </IconBtn>
                </>
              )}
              <CollapseBtn open={sidebarOpen} onClick={() => setSidebarOpen(v => !v)}>
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none"
                  style={{ transform: sidebarOpen ? 'none' : 'scaleX(-1)', transition: 'transform 0.2s' }}>
                  <path d="M9 3L5 7L9 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </CollapseBtn>
            </SidebarHeader>

            <SidebarContent open={sidebarOpen}>
              <SidebarTree
                tree={tree}
                setTree={setTree}
                activeFileId={activeFileId}
                setActiveFileId={setActiveFileId}
              />
            </SidebarContent>
          </Sidebar>

          {/* ── Editor ── */}
          <EditorArea>
            <TabBar>
              <Tab active>
                {activeFile && <TabDot />}
                {nomeArquivoAtivo}
              </Tab>
            </TabBar>
            <div style={{ flex: 1, overflow: 'hidden' }}>
              <MeuEditor
                idArquivo={idArquivo}
                atualizarCaminho={setCaminhoArquivo}
                onChange={novoCodigo => setCodigoFonte(novoCodigo)}
              />
            </div>
          </EditorArea>

          {/* ── Painéis direitos ── */}
          <RightPanels open={painelOpen}>
            <PanelGroup direction="vertical">
              <Panel defaultSize={65} minSize={30}>
                <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <PanelHeader>
                    <PanelDot color={C.accent} />
                    <PanelLabel>Feedback educacional</PanelLabel>
                    {alertCount > 0 && (
                      <span style={{
                        marginLeft: 'auto',
                        background: 'rgba(239,68,68,0.15)',
                        color: '#fca5a5',
                        fontSize: 10,
                        padding: '1px 7px',
                        borderRadius: 10,
                        fontFamily: 'JetBrains Mono, monospace',
                      }}>
                        {alertCount} alerta{alertCount > 1 ? 's' : ''}
                      </span>
                    )}
                  </PanelHeader>
                  <FeedbackBody ref={feedbackRef}>
                    {renderFeedback()}
                  </FeedbackBody>
                </div>
              </Panel>

              <PanelResizeHandle style={{ height: 6, background: C.border, cursor: 'row-resize' }} />

              <Panel defaultSize={35} minSize={20}>
                <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <PanelHeader>
                    <PanelDot color={C.green} />
                    <PanelLabel>Terminal</PanelLabel>
                  </PanelHeader>
                  <TerminalWrapper>
                    {terminalLines.length === 0 ? (
                      <TermLine type="default">$ aguardando execução...</TermLine>
                    ) : (
                      terminalLines.map((l, i) => (
                        <TermLine key={i} type={l.type}>{l.text}</TermLine>
                      ))
                    )}
                  </TerminalWrapper>
                </div>
              </Panel>
            </PanelGroup>
          </RightPanels>

        </Body>

        {/* ── Statusbar ── */}
        <Statusbar>
          <SbItem>Java</SbItem>
          <SbItem dim>UTF-8</SbItem>
          <SbItem dim>{nomeArquivoAtivo}</SbItem>
          {alertCount > 0 && (
            <SbAlert color="#fca5a5">
              <svg width="10" height="10" viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 1L1 14h14L8 1zm0 3l5 9H3l5-9zm0 3v3m0 2v1" stroke="currentColor" strokeWidth="1" fill="none"/>
              </svg>
              {alertCount} alerta{alertCount > 1 ? 's' : ''}
            </SbAlert>
          )}
          <SbItem style={{ marginLeft: 'auto' }}>{statusMsg}</SbItem>
        </Statusbar>

      </Shell>
    </>
  );
};

export default EditorJavaThreads;