import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import axios from 'axios';
import MeuEditor from '../ideComponents/MeuEditor';
import styled, { keyframes, ThemeProvider } from 'styled-components';

/* ════════════════════════════════
   TEMAS
════════════════════════════════ */
const darkTheme = {
  mode:        'dark',
  bg0:         '#0d1117',
  bg1:         '#161b22',
  bg2:         '#1c2230',
  bg3:         '#21293a',
  border:      '#2a3441',
  borderHov:   '#3a4a5c',
  accent:      '#3b82f6',
  accentHover: '#2563eb',
  green:       '#10b981',
  greenBg:     'rgba(16,185,129,0.08)',
  red:         '#ef4444',
  amber:       '#f59e0b',
  textPri:     '#e8edf3',
  textSec:     '#b0bac8',
  textDim:     '#5a6880',
  statusBg:    '#3b82f6',
  statusText:  '#fff',
};

const lightTheme = {
  mode:        'light',
  bg0:         '#f0f4f8',
  bg1:         '#ffffff',
  bg2:         '#e8edf3',
  bg3:         '#dde3ec',
  border:      '#c8d0dc',
  borderHov:   '#a0adb8',
  accent:      '#2563eb',
  accentHover: '#1d4ed8',
  green:       '#059669',
  greenBg:     'rgba(5,150,105,0.08)',
  red:         '#dc2626',
  amber:       '#d97706',
  textPri:     '#0f172a',
  textSec:     '#374151',
  textDim:     '#6b7280',
  statusBg:    '#2563eb',
  statusText:  '#fff',
};

const slideIn = keyframes`
  from { opacity: 0; transform: translateX(12px); }
  to   { opacity: 1; transform: translateX(0); }
`;
const fadeIn = keyframes`
  from { opacity: 0; }
  to   { opacity: 1; }
`;
const spin = keyframes`
  to { transform: rotate(360deg); }
`;
const pulse = keyframes`
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.35; }
`;

const Shell = styled.div`
  height: 100vh; display: flex; flex-direction: column;
  background: ${p => p.theme.bg0}; color: ${p => p.theme.textPri};
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  overflow: hidden; user-select: none;
`;

const Titlebar = styled.div`
  height: 46px; background: ${p => p.theme.bg1};
  border-bottom: 1px solid ${p => p.theme.border};
  display: flex; align-items: center; padding: 0 16px;
  gap: 12px; flex-shrink: 0; z-index: 10;
`;

const TitleDots = styled.div`display: flex; gap: 6px;`;
const Dot = styled.div`width: 12px; height: 12px; border-radius: 50%; background: ${p => p.color};`;

const BrandArea = styled.div`display: flex; align-items: center; gap: 7px; flex-shrink: 0;`;
const BrandName = styled.span`
  font-size: 12px; font-weight: 700; color: #22d3ee;
  letter-spacing: 0.12em; text-transform: uppercase;
`;

const TitleText = styled.span`
  flex: 1; text-align: center; font-size: 12px; color: ${p => p.theme.textDim};
  letter-spacing: 0.02em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
`;

const RunButton = styled.button`
  display: flex; align-items: center; gap: 7px; padding: 7px 18px;
  background: ${p => p.$running ? p.theme.bg3 : p.theme.accent};
  color: #fff; border: none; border-radius: 6px; font-size: 12px;
  font-family: 'JetBrains Mono', monospace; font-weight: 500;
  cursor: ${p => p.$running ? 'not-allowed' : 'pointer'};
  transition: background 0.15s; letter-spacing: 0.02em; white-space: nowrap; flex-shrink: 0;
  &:hover:not(:disabled) { background: ${p => p.theme.accentHover}; }
`;

const ThemeBtn = styled.button`
  display: flex; align-items: center; gap: 5px; padding: 5px 10px;
  background: ${p => p.theme.bg3}; color: ${p => p.theme.textSec};
  border: 1px solid ${p => p.theme.border}; border-radius: 5px;
  font-size: 11px; font-family: 'JetBrains Mono', monospace;
  cursor: pointer; white-space: nowrap; flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
  &:hover { background: ${p => p.theme.bg2}; color: ${p => p.theme.textPri}; }
`;

const SpinnerEl = styled.div`
  width: 10px; height: 10px;
  border: 1.5px solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: ${spin} 0.7s linear infinite;
`;

const Body = styled.div`flex: 1; display: flex; overflow: hidden; min-height: 0;`;

const SidebarInner = styled.div`
  height: 100%; background: ${p => p.theme.bg1};
  border-right: 1px solid ${p => p.theme.border};
  display: flex; flex-direction: column; overflow: hidden;
`;

const SidebarHeader = styled.div`
  height: 38px; display: flex; align-items: center;
  padding: 0 8px 0 12px; gap: 4px;
  border-bottom: 1px solid ${p => p.theme.border}; flex-shrink: 0;
`;

const SidebarTitle = styled.span`
  font-size: 11px; color: ${p => p.theme.textDim};
  text-transform: uppercase; letter-spacing: 0.1em;
  white-space: nowrap; flex: 1; overflow: hidden;
`;

const CollapseBtn = styled.button`
  width: 24px; height: 24px; display: flex; align-items: center;
  justify-content: center; background: none; border: none; cursor: pointer;
  color: ${p => p.theme.textDim}; padding: 0; border-radius: 4px; flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
  &:hover { color: ${p => p.theme.textSec}; background: ${p => p.theme.bg3}; }
`;

const IconBtn = styled.button`
  width: 24px; height: 24px; display: flex; align-items: center;
  justify-content: center; background: none; border: none; cursor: pointer;
  color: ${p => p.theme.textDim}; padding: 0; border-radius: 4px;
  opacity: ${p => p.$open ? 1 : 0}; pointer-events: ${p => p.$open ? 'auto' : 'none'};
  transition: color 0.15s, background 0.15s;
  &:hover { color: ${p => p.theme.textPri}; background: ${p => p.theme.bg3}; }
`;

const SidebarContent = styled.div`
  flex: 1; overflow-y: auto; overflow-x: hidden; padding: 6px 0;
  opacity: ${p => p.$open ? 1 : 0}; transition: opacity 0.15s;
  pointer-events: ${p => p.$open ? 'auto' : 'none'};
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: ${p => p.theme.border}; border-radius: 2px; }
`;

const EmptySidebar = styled.div`
  padding: 20px 14px; font-size: 12px; color: ${p => p.theme.textDim};
  line-height: 1.7; text-align: center;
`;

const ResizeHandle = styled(PanelResizeHandle)`
  width: 5px; background: ${p => p.theme.border}; cursor: col-resize;
  transition: background 0.15s;
  &:hover { background: ${p => p.theme.accent}; }
`;

const TreeRow = styled.div`
  display: flex; align-items: center; gap: 5px;
  padding: 4px 10px 4px ${p => 10 + (p.$depth || 0) * 16}px;
  font-size: 13px;
  color: ${p => p.$active ? p.theme.accent : p.theme.textSec};
  background: ${p => p.$dragOver ? 'rgba(59,130,246,0.15)' : p.$active ? 'rgba(59,130,246,0.1)' : 'transparent'};
  cursor: ${p => p.$dragging ? 'grabbing' : 'pointer'}; white-space: nowrap;
  border-left: 2px solid ${p => p.$dragOver || p.$active ? p.theme.accent : 'transparent'};
  transition: background 0.1s, color 0.1s; position: relative;
  opacity: ${p => p.$dragging ? 0.4 : 1};
  &:hover { background: ${p => p.$dragOver ? 'rgba(59,130,246,0.15)' : p.theme.bg2}; color: ${p => p.theme.textPri}; }
  &:hover .row-actions { opacity: 1; pointer-events: auto; }
`;

const RowName = styled.span`flex: 1; overflow: hidden; text-overflow: ellipsis; font-size: 13px; color: inherit;`;

const RowActionsDiv = styled.div`
  display: flex; gap: 2px; opacity: 0; pointer-events: none;
  transition: opacity 0.15s; margin-left: auto;
`;

const RowActionBtn = styled.button`
  width: 20px; height: 20px; display: flex; align-items: center;
  justify-content: center; background: none; border: none; cursor: pointer;
  color: ${p => p.theme.textDim}; border-radius: 3px; padding: 0;
  transition: color 0.15s, background 0.15s;
  &:hover { color: ${p => p.$danger ? '#fca5a5' : p.theme.textPri}; background: ${p => p.theme.bg3}; }
`;

const InlineInput = styled.input`
  flex: 1; background: ${p => p.theme.bg3}; border: 1px solid ${p => p.theme.accent};
  border-radius: 3px; color: ${p => p.theme.textPri}; font-family: 'JetBrains Mono', monospace;
  font-size: 13px; padding: 2px 6px; outline: none; min-width: 0;
`;

const CtxMenu = styled.div`
  position: fixed; background: ${p => p.theme.bg2}; border: 1px solid ${p => p.theme.border};
  border-radius: 6px; padding: 4px 0; z-index: 1000; min-width: 160px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.35); animation: ${fadeIn} 0.12s ease;
`;

const CtxItem = styled.div`
  padding: 8px 14px; font-size: 13px;
  color: ${p => p.$danger ? p.theme.red : p.theme.textSec};
  cursor: pointer; display: flex; align-items: center; gap: 8px;
  transition: background 0.1s, color 0.1s;
  &:hover { background: ${p => p.theme.bg3}; color: ${p => p.$danger ? '#fca5a5' : p.theme.textPri}; }
`;

const CtxSep = styled.div`height: 1px; background: ${p => p.theme.border}; margin: 4px 0;`;

const EditorArea = styled.div`
  flex: 1; height: 100%; display: flex; flex-direction: column;
  overflow: hidden; min-width: 0; min-height: 0; background: ${p => p.theme.bg0};
`;

const TabBar = styled.div`
  height: 38px; background: ${p => p.theme.bg1};
  border-bottom: 1px solid ${p => p.theme.border};
  display: flex; align-items: flex-end; flex-shrink: 0; overflow-x: auto;
  &::-webkit-scrollbar { display: none; }
`;

const Tab = styled.div`
  height: 34px; padding: 0 18px; display: flex; align-items: center; font-size: 13px;
  color: ${p => p.$active ? p.theme.textPri : p.theme.textDim};
  background: ${p => p.$active ? p.theme.bg0 : 'transparent'};
  border-right: 1px solid ${p => p.theme.border};
  border-top: ${p => p.$active ? `2px solid ${p.theme.accent}` : '2px solid transparent'};
  cursor: pointer; white-space: nowrap; gap: 8px; transition: color 0.1s;
  &:hover { color: ${p => p.theme.textSec}; }
`;

const TabDot = styled.div`
  width: 7px; height: 7px; border-radius: 50%; background: ${p => p.theme.amber};
  animation: ${pulse} 2s ease-in-out infinite;
`;

const RightPanels = styled.div`
  width: ${p => p.$open ? '360px' : '0px'}; background: ${p => p.theme.bg1};
  border-left: ${p => p.$open ? `1px solid ${p.theme.border}` : 'none'};
  display: flex; flex-direction: column; overflow: hidden;
  transition: width 0.25s ease; flex-shrink: 0; min-height: 0;
`;

const PanelHeaderEl = styled.div`
  height: 36px; display: flex; align-items: center; padding: 0 14px; gap: 8px;
  border-bottom: 1px solid ${p => p.theme.border}; flex-shrink: 0; background: ${p => p.theme.bg1};
`;

const PanelLabel = styled.span`
  font-size: 11px; color: ${p => p.theme.textDim}; text-transform: uppercase; letter-spacing: 0.1em;
`;

const PanelDot = styled.div`width: 8px; height: 8px; border-radius: 50%; background: ${p => p.$color};`;

const FeedbackBody = styled.div`
  flex: 1; padding: 14px; overflow-y: auto;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: ${p => p.theme.border}; border-radius: 2px; }
`;

const AlertCard = styled.div`
  background: ${p => p.theme.bg2};
  border: 1px solid ${p => p.$color === 'red' ? 'rgba(239,68,68,0.3)' : 'rgba(245,158,11,0.3)'};
  border-left: 3px solid ${p => p.$color === 'red' ? p.theme.red : p.theme.amber};
  border-radius: 0 6px 6px 0; padding: 12px 14px; margin-bottom: 12px;
  animation: ${slideIn} 0.2s ease forwards; animation-delay: ${p => p.$delay || '0s'}; opacity: 0;
`;

const AlertHeader = styled.div`display: flex; align-items: center; gap: 8px; margin-bottom: 10px;`;

const AlertIcon = styled.div`
  width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
`;

const AlertTitle = styled.span`
  font-size: 12px; font-weight: 600;
  color: ${p => p.$color === 'red' ? '#fca5a5' : '#fcd34d'}; letter-spacing: 0.03em;
`;

const AlertSubtitle = styled.div`
  font-size: 12px;
  color: ${p => p.$color === 'red' ? 'rgba(252,165,165,0.7)' : 'rgba(252,211,77,0.7)'}; margin-top: 2px;
`;

const SectionBlock = styled.div`margin-bottom: 10px;`;

const SectionLabel = styled.div`
  display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600;
  color: ${p => p.theme.textSec}; margin-bottom: 5px;
`;

const SectionText = styled.div`
  font-size: 12px; color: ${p => p.$green ? p.theme.green : p.theme.textSec};
  line-height: 1.65; font-family: ${p => p.$sans ? 'system-ui, sans-serif' : 'inherit'}; white-space: pre-wrap;
`;

const FixItem = styled.div`
  display: flex; align-items: flex-start; gap: 6px; font-size: 12px;
  color: ${p => p.theme.textSec}; line-height: 1.5; margin-bottom: 4px; font-family: system-ui, sans-serif;
`;

const FixDot = styled.div`
  width: 15px; height: 15px; border-radius: 50%;
  background: ${p => p.theme.greenBg}; border: 1px solid ${p => p.theme.green};
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 1px;
`;

const Divider = styled.div`height: 1px; background: ${p => p.theme.border}; margin: 8px 0;`;

const SuccessCard = styled.div`
  background: ${p => p.theme.greenBg}; border: 1px solid rgba(16,185,129,0.25);
  border-left: 3px solid ${p => p.theme.green}; border-radius: 0 6px 6px 0;
  padding: 14px; animation: ${fadeIn} 0.3s ease;
`;

const EmptyState = styled.div`
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; gap: 10px; color: ${p => p.theme.textDim}; font-size: 12px;
  text-align: center; padding: 20px; line-height: 1.7;
`;

const WelcomeScreen = styled.div`
  flex: 1; width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; background: ${p => p.theme.bg0};
  gap: 28px; user-select: none; position: relative; overflow: hidden;
`;

const WelcomeBgLogo = styled.img`
  position: absolute; width: 340px;
  opacity: ${p => p.theme.mode === 'dark' ? 0.055 : 0.07};
  pointer-events: none; user-select: none;
  filter: ${p => p.theme.mode === 'light' ? 'saturate(0.5)' : 'saturate(0.4)'};
`;

const WelcomeBrand = styled.div`
  display: flex; flex-direction: column; align-items: center; gap: 8px; position: relative;
`;

const WelcomeBrandName = styled.div`
  font-size: 24px; font-weight: 700; letter-spacing: 0.18em; color: #22d3ee; text-transform: uppercase;
`;

const WelcomeBrandSub = styled.div`font-size: 12px; color: ${p => p.theme.textDim}; letter-spacing: 0.06em;`;

const WelcomeGrid = styled.div`
  display: flex; flex-direction: column; gap: 8px; min-width: 300px; position: relative;
`;

const WelcomeItem = styled.div`
  display: flex; align-items: center; justify-content: space-between;
  padding: 11px 16px; background: ${p => p.theme.bg1}; border: 1px solid ${p => p.theme.border};
  border-radius: 7px; cursor: pointer; transition: border-color 0.15s, background 0.15s;
  &:hover { background: ${p => p.theme.bg2}; border-color: ${p => p.theme.borderHov}; }
`;

const WelcomeItemLabel = styled.span`
  font-size: 13px; color: ${p => p.theme.textSec}; display: flex; align-items: center; gap: 9px;
`;

const WelcomeKbd = styled.kbd`
  font-size: 11px; color: ${p => p.theme.textDim}; background: ${p => p.theme.bg3};
  border: 1px solid ${p => p.theme.border}; border-radius: 4px; padding: 2px 8px;
  font-family: 'JetBrains Mono', monospace;
`;

const SaveIndicator = styled.div`
  position: absolute; top: 12px; right: 18px; font-size: 12px; font-weight: 500;
  color: ${p => p.$saved ? p.theme.green : p.theme.amber};
  background: ${p => p.$saved
    ? (p.theme.mode === 'dark' ? 'rgba(16,185,129,0.12)' : 'rgba(5,150,105,0.1)')
    : (p.theme.mode === 'dark' ? 'rgba(245,158,11,0.12)' : 'rgba(217,119,6,0.1)')};
  border: 1px solid ${p => p.$saved ? p.theme.green : p.theme.amber};
  border-radius: 5px; padding: 4px 10px; display: flex; align-items: center; gap: 5px;
  opacity: ${p => p.$visible ? 1 : 0}; transition: opacity 0.4s; pointer-events: none; z-index: 5;
`;

const TerminalWrapper = styled.div`
  flex: 1; background: ${p => p.theme.bg0}; padding: 10px 16px;
  overflow-y: auto; font-size: 12px; line-height: 1.75;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: ${p => p.theme.border}; border-radius: 2px; }
`;

const TermLine = styled.div`
  color: ${p =>
    p.$type === 'prompt' ? '#4ade80' :
    p.$type === 'error'  ? p.theme.red :
    p.$type === 'warn'   ? p.theme.amber :
    p.$type === 'ok'     ? p.theme.green :
    p.theme.textDim};
`;

const Statusbar = styled.div`
  height: 24px; background: ${p => p.theme.statusBg}; display: flex; align-items: center;
  padding: 0 14px; gap: 16px; flex-shrink: 0;
`;

const SbItem = styled.span`font-size: 11px; color: rgba(255,255,255,${p => p.$dim ? '0.65' : '1'});`;

const SbAlert = styled.span`font-size: 11px; color: #fff; display: flex; align-items: center; gap: 4px;`;

/* ════════════════════════════════
   ICONS
════════════════════════════════ */
const IcoFolder = ({ open }) => (
  <svg width="15" height="15" viewBox="0 0 16 16" fill={open ? '#fcd34d' : '#f59e0b'} style={{ flexShrink: 0 }}>
    {open
      ? <path d="M1 4.5A1.5 1.5 0 012.5 3h3.764c.516 0 1.01.245 1.328.664L8.5 5H14a1 1 0 011 1v6.5A1.5 1.5 0 0113.5 14h-11A1.5 1.5 0 011 12.5v-8z"/>
      : <path d="M1 3.5A1.5 1.5 0 012.5 2h3.764c.516 0 1.01.245 1.328.664L8.5 4H13.5A1.5 1.5 0 0115 5.5v7A1.5 1.5 0 0113.5 14h-11A1.5 1.5 0 011 12.5v-9z"/>
    }
  </svg>
);

const IcoFile = ({ color = '#8b95a6' }) => (
  <svg width="14" height="14" viewBox="0 0 16 16" fill={color} style={{ flexShrink: 0 }}>
    <rect x="2" y="1" width="12" height="14" rx="2"/>
    <path d="M5 5h6M5 8h6M5 11h4" stroke="#1a1f2e" strokeWidth="1.2" fill="none"/>
  </svg>
);

const IcoChevron = ({ open }) => (
  <svg width="11" height="11" viewBox="0 0 10 10" fill="none"
    style={{ flexShrink: 0, transform: open ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.15s' }}>
    <path d="M3 2l4 3-4 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const IcoNewFile = () => (
  <svg width="13" height="13" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
    <rect x="1" y="1" width="7" height="9" rx="1"/>
    <path d="M9 7v4M7 9h4" strokeLinecap="round"/>
  </svg>
);

const IcoNewFolder = () => (
  <svg width="13" height="13" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
    <path d="M1 3a1 1 0 011-1h2.5l1 1.5H10a1 1 0 011 1v4a1 1 0 01-1 1H2a1 1 0 01-1-1V3z"/>
    <path d="M6 5v3M4.5 6.5h3" strokeLinecap="round"/>
  </svg>
);

/* ════════════════════════════════
   TREE HELPERS
════════════════════════════════ */
let _nextId = 1;
const newId = () => String(_nextId++);
const newFile = (name = 'Arquivo.java') => ({ id: newId(), name, type: 'file', children: [] });
const newFolder = (name = 'pacote') => ({ id: newId(), name, type: 'folder', children: [], expanded: true });

const updateNode = (nodes, id, fn) =>
  nodes.map(n => {
    if (n.id === id) return fn(n);
    if (n.children?.length) return { ...n, children: updateNode(n.children, id, fn) };
    return n;
  });

const removeNode = (nodes, id) =>
  nodes.filter(n => n.id !== id).map(n =>
    n.children?.length ? { ...n, children: removeNode(n.children, id) } : n
  );

const insertChild = (nodes, parentId, child) =>
  updateNode(nodes, parentId, n => ({ ...n, children: [...n.children, child] }));

const flatFiles = (nodes, acc = []) => {
  for (const n of nodes) {
    if (n.type === 'file') acc.push(n);
    if (n.children?.length) flatFiles(n.children, acc);
  }
  return acc;
};

const moveNode = (nodes, dragId, targetId) => {
  let dragNode = null;
  const find = (ns) => { for (const n of ns) { if (n.id === dragId) { dragNode = n; return; } if (n.children?.length) find(n.children); } };
  find(nodes);
  if (!dragNode) return nodes;
  return insertChild(removeNode(nodes, dragId), targetId, dragNode);
};

/* ════════════════════════════════
   SIDEBAR TREE
════════════════════════════════ */
const SidebarTree = ({ tree, setTree, activeFileId, setActiveFileId, onStartCreate, theme }) => {
  const [editingId, setEditingId]   = useState(null);
  const [editValue, setEditValue]   = useState('');
  const [creatingIn, setCreatingIn] = useState(null);
  const [ctxMenu, setCtxMenu]       = useState(null);
  const [dragId, setDragId]         = useState(null);
  const [dragOverId, setDragOverId] = useState(null);
  const inputRef = useRef(null);

  useEffect(() => { if (inputRef.current) inputRef.current.focus(); }, [editingId, creatingIn]);

  useEffect(() => {
    const close = () => setCtxMenu(null);
    window.addEventListener('click', close);
    return () => window.removeEventListener('click', close);
  }, []);

  useEffect(() => { if (onStartCreate) onStartCreate.current = startCreate; }); // eslint-disable-line

  const startCreate = (parentId, type) => {
    setCreatingIn({ parentId, type });
    setEditValue('');
    if (parentId !== 'root') setTree(t => updateNode(t, parentId, n => ({ ...n, expanded: true })));
  };

  const commitCreate = () => {
    if (!creatingIn) return;
    const name = editValue.trim() || (creatingIn.type === 'file' ? 'Arquivo.java' : 'pacote');
    const child = creatingIn.type === 'file' ? newFile(name) : newFolder(name);
    if (creatingIn.parentId === 'root') setTree(t => [...t, child]);
    else setTree(t => insertChild(t, creatingIn.parentId, child));
    if (creatingIn.type === 'file') setActiveFileId(child.id);
    setCreatingIn(null);
  };

  const startRename = (node) => { setEditingId(node.id); setEditValue(node.name); setCtxMenu(null); };
  const commitRename = (id) => {
    if (editValue.trim()) setTree(t => updateNode(t, id, n => ({ ...n, name: editValue.trim() })));
    setEditingId(null);
  };

  const deleteNode = (id) => {
    setTree(t => removeNode(t, id));
    if (activeFileId === id) setActiveFileId(null);
    setCtxMenu(null);
  };

  const toggleExpand = (id) => setTree(t => updateNode(t, id, n => ({ ...n, expanded: !n.expanded })));
  const onCtxMenu = (e, node) => { e.preventDefault(); e.stopPropagation(); setCtxMenu({ x: e.clientX, y: e.clientY, node }); };

  const onDragStart = (e, node) => { setDragId(node.id); e.dataTransfer.effectAllowed = 'move'; };
  const onDragOver  = (e, node) => { if (node.type !== 'folder' || node.id === dragId) return; e.preventDefault(); setDragOverId(node.id); };
  const onDragLeave = () => setDragOverId(null);
  const onDrop = (e, targetNode) => {
    e.preventDefault(); setDragOverId(null);
    if (!dragId || targetNode.type !== 'folder' || targetNode.id === dragId) return;
    setTree(t => moveNode(t, dragId, targetNode.id));
    setDragId(null);
  };
  const onDragEnd = () => { setDragId(null); setDragOverId(null); };

  const CreatingRow = ({ depth, type }) => (
    <TreeRow theme={theme} $depth={depth} $active={false}>
      {type === 'folder' ? <IcoFolder open={false} /> : <IcoFile color="#60a5fa" />}
      <InlineInput theme={theme} ref={inputRef} value={editValue}
        placeholder={type === 'file' ? 'Arquivo.java' : 'pacote'}
        onChange={e => setEditValue(e.target.value)}
        onBlur={commitCreate}
        onKeyDown={e => { if (e.key === 'Enter') commitCreate(); if (e.key === 'Escape') setCreatingIn(null); }}
      />
    </TreeRow>
  );

  const renderNodes = (nodes, depth = 0) => {
    const items = [];
    nodes.forEach(node => {
      const isEditing = editingId === node.id;
      items.push(
        <TreeRow key={node.id} theme={theme} $depth={depth}
          $active={node.type === 'file' && activeFileId === node.id}
          $dragging={dragId === node.id} $dragOver={dragOverId === node.id}
          draggable
          onDragStart={e => onDragStart(e, node)} onDragOver={e => onDragOver(e, node)}
          onDragLeave={onDragLeave} onDrop={e => onDrop(e, node)} onDragEnd={onDragEnd}
          onClick={() => { node.type === 'folder' ? toggleExpand(node.id) : setActiveFileId(node.id); }}
          onContextMenu={e => onCtxMenu(e, node)}
        >
          {node.type === 'folder' && <IcoChevron open={node.expanded} />}
          {node.type === 'folder'
            ? <IcoFolder open={node.expanded} />
            : <IcoFile color={activeFileId === node.id ? '#60a5fa' : (theme.mode === 'light' ? '#4a7abf' : '#8b95a6')} />
          }
          {isEditing ? (
            <InlineInput theme={theme} ref={inputRef} value={editValue}
              onChange={e => setEditValue(e.target.value)}
              onBlur={() => commitRename(node.id)}
              onKeyDown={e => { if (e.key === 'Enter') commitRename(node.id); if (e.key === 'Escape') setEditingId(null); }}
              onClick={e => e.stopPropagation()}
            />
          ) : (
            <RowName theme={theme}>{node.name}</RowName>
          )}
          {!isEditing && (
            <RowActionsDiv className="row-actions">
              {node.type === 'folder' && (
                <>
                  <RowActionBtn theme={theme} title="Novo arquivo" onClick={e => { e.stopPropagation(); startCreate(node.id, 'file'); }}><IcoNewFile /></RowActionBtn>
                  <RowActionBtn theme={theme} title="Nova pasta" onClick={e => { e.stopPropagation(); startCreate(node.id, 'folder'); }}><IcoNewFolder /></RowActionBtn>
                </>
              )}
              <RowActionBtn theme={theme} title="Renomear" onClick={e => { e.stopPropagation(); startRename(node); }}>
                <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M2 9.5h2l4.5-4.5-2-2L2 7.5v2z" strokeLinejoin="round"/>
                  <path d="M6.5 3l2 2" strokeLinecap="round"/>
                </svg>
              </RowActionBtn>
              <RowActionBtn theme={theme} $danger title="Excluir" onClick={e => { e.stopPropagation(); deleteNode(node.id); }}>
                <svg width="11" height="11" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M2 3h8M5 3V2h2v1M4 3l.5 7h3L8 3" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </RowActionBtn>
            </RowActionsDiv>
          )}
        </TreeRow>
      );
      if (node.type === 'folder' && node.expanded) {
        items.push(...renderNodes(node.children, depth + 1));
        if (creatingIn?.parentId === node.id) items.push(<CreatingRow key="creating" depth={depth + 1} type={creatingIn.type} />);
      }
    });
    return items;
  };

  return (
    <>
      {tree.length === 0 && !creatingIn
        ? <EmptySidebar theme={theme}>Nenhum arquivo ainda.<br/>Use os botões acima para criar.</EmptySidebar>
        : <>
            {renderNodes(tree)}
            {creatingIn?.parentId === 'root' && <CreatingRow depth={0} type={creatingIn.type} />}
          </>
      }
      {ctxMenu && (
        <CtxMenu theme={theme} style={{ top: ctxMenu.y, left: ctxMenu.x }} onClick={e => e.stopPropagation()}>
          {ctxMenu.node.type === 'folder' && (
            <>
              <CtxItem theme={theme} onClick={() => { startCreate(ctxMenu.node.id, 'file'); setCtxMenu(null); }}><IcoNewFile /> Novo arquivo</CtxItem>
              <CtxItem theme={theme} onClick={() => { startCreate(ctxMenu.node.id, 'folder'); setCtxMenu(null); }}><IcoNewFolder /> Nova pasta</CtxItem>
              <CtxSep theme={theme} />
            </>
          )}
          <CtxItem theme={theme} onClick={() => startRename(ctxMenu.node)}>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 9.5h2l4.5-4.5-2-2L2 7.5v2z" strokeLinejoin="round"/>
            </svg>
            Renomear
          </CtxItem>
          <CtxItem theme={theme} $danger onClick={() => deleteNode(ctxMenu.node.id)}>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 3h8M5 3V2h2v1M4 3l.5 7h3L8 3" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Excluir
          </CtxItem>
        </CtxMenu>
      )}
    </>
  );
};

/* ════════════════════════════════
   FEEDBACK RENDERER
════════════════════════════════ */
const parseInterpretation = (text) => {
  const sections = { explanation: '', fixes: [], professor: '' };
  if (!text) return sections;
  const lines = text.split('\n').map(l => l.trim()).filter(Boolean);
  let mode = 'professor';
  for (const line of lines) {
    const lower = line.toLowerCase();
    if (lower.startsWith('explicaç') || lower.startsWith('explica')) { mode = 'explanation'; continue; }
    if (lower.startsWith('como corrigir') || lower.startsWith('correç') || lower.startsWith('sugest')) { mode = 'fixes'; continue; }
    if (mode === 'professor' && sections.professor.length < 200) sections.professor += (sections.professor ? ' ' : '') + line;
    else if (mode === 'explanation') sections.explanation += (sections.explanation ? ' ' : '') + line;
    else if (mode === 'fixes') sections.fixes.push(line.replace(/^[-•*]\s*/, ''));
    else sections.professor += (sections.professor ? ' ' : '') + line;
  }
  if (sections.fixes.length === 0) {
    for (const l of text.split(/[.。\n]/).map(l => l.trim()).filter(Boolean)) {
      const ll = l.toLowerCase();
      if (ll.includes('use ') || ll.includes('utilize ') || ll.includes('opte ') || ll.includes('synchronized') || ll.includes('copyonwrite')) sections.fixes.push(l);
    }
  }
  return sections;
};

const IssueCard = ({ issue, index }) => {
  const color = index % 2 === 0 ? 'red' : 'amber';
  const { professor, explanation, fixes } = parseInterpretation(issue.interpretation);
  return (
    <AlertCard $color={color} $delay={`${index * 0.08}s`}>
      <AlertHeader>
        <AlertIcon>
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 2L1 14h14L8 2z" stroke={color === 'red' ? '#fca5a5' : '#fcd34d'} strokeWidth="1.5" strokeLinejoin="round"/>
            <path d="M8 7v3M8 12v.5" stroke={color === 'red' ? '#fca5a5' : '#fcd34d'} strokeWidth="1.5" strokeLinecap="round"/>
          </svg>
        </AlertIcon>
        <div style={{ flex: 1 }}>
          <AlertTitle $color={color}>Alerta Técnico</AlertTitle>
          <AlertSubtitle $color={color}>Linha {issue.lineNumber}: {issue.message}</AlertSubtitle>
        </div>
      </AlertHeader>
      {professor && <SectionBlock><SectionLabel><span style={{ fontSize: 14 }}>🤖</span><span>Professor LLM:</span></SectionLabel><SectionText $sans>{professor}</SectionText></SectionBlock>}
      {explanation && <><Divider /><SectionBlock><SectionLabel><span style={{ fontSize: 14 }}>📦</span><span>Explicação</span></SectionLabel><SectionText $sans>{explanation}</SectionText></SectionBlock></>}
      {fixes.length > 0 && <><Divider /><SectionBlock><SectionLabel><span style={{ fontSize: 14 }}>💡</span><span>Como Corrigir</span></SectionLabel>
        {fixes.map((fix, i) => (
          <FixItem key={i}>
            <FixDot><svg width="8" height="8" viewBox="0 0 8 8" fill="none"><path d="M1.5 4l2 2 3-3" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/></svg></FixDot>
            <span>{fix}</span>
          </FixItem>
        ))}
      </SectionBlock></>}
    </AlertCard>
  );
};

/* ════════════════════════════════
   MAIN COMPONENT
════════════════════════════════ */
const EditorJavaThreads = () => {
  const { idArquivo } = useParams();

  const [isDark, setIsDark] = useState(() => {
    const saved = localStorage.getItem('sumawma-theme');
    return saved ? saved === 'dark' : true;
  });
  const theme = isDark ? darkTheme : lightTheme;

  const toggleTheme = () => {
    const next = !isDark;
    setIsDark(next);
    localStorage.setItem('sumawma-theme', next ? 'dark' : 'light');
  };

  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'sumawma-theme') setIsDark(e.newValue === 'dark');
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const [codigoFonte, setCodigoFonte]           = useState('');
  const [resultadoAnalise, setResultadoAnalise] = useState(null);
  const [isAnalisando, setIsAnalisando]         = useState(false);
  const [sidebarOpen, setSidebarOpen]           = useState(true);
  const [painelOpen, setPainelOpen]             = useState(false);
  const [terminalLines, setTerminalLines]       = useState([]);
  const [statusMsg, setStatusMsg]               = useState('Pronto');
  const [alertCount, setAlertCount]             = useState(0);
  const [tree, setTree]                         = useState([]);
  const [activeFileId, setActiveFileId]         = useState(null);
  const [editorKey, setEditorKey]               = useState('initial');
  const [saveStatus, setSaveStatus]             = useState('saved');
  const [saveVisible, setSaveVisible]           = useState(false);
  const feedbackRef      = useRef(null);
  const termRef          = useRef(null);
  const autosaveTimer    = useRef(null);
  const saveVisibleTimer = useRef(null);
  const startCreateRef   = useRef(null);
  // ref para acessar analisarCodigo dentro do useEffect do Ctrl+Enter
  const analisarRef      = useRef(null);

  const activeFile = flatFiles(tree).find(f => f.id === activeFileId) || null;
  const nomeArquivoAtivo = activeFile?.name || null;

  useEffect(() => { setEditorKey(activeFileId || 'empty'); }, [activeFileId]);

  const handleCodigoChange = useCallback((novoCodigo) => {
    setCodigoFonte(novoCodigo);
    setSaveStatus('unsaved'); setSaveVisible(true);
    clearTimeout(autosaveTimer.current);
    autosaveTimer.current = setTimeout(() => {
      setSaveStatus('saved');
      clearTimeout(saveVisibleTimer.current);
      saveVisibleTimer.current = setTimeout(() => setSaveVisible(false), 1500);
    }, 1000);
  }, []);

  // Ctrl+S
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        clearTimeout(autosaveTimer.current);
        setSaveStatus('saved'); setSaveVisible(true);
        clearTimeout(saveVisibleTimer.current);
        saveVisibleTimer.current = setTimeout(() => setSaveVisible(false), 1500);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  // Ctrl+N
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
        e.preventDefault();
        startCreateRef.current?.('root', 'file');
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  // ── NOVO: Ctrl+Enter dispara análise ──
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        analisarRef.current?.();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  const addTermLine = useCallback((text, type = 'default') => {
    setTerminalLines(prev => [...prev, { text, type }]);
  }, []);

  useEffect(() => { if (termRef.current) termRef.current.scrollTop = termRef.current.scrollHeight; }, [terminalLines]);

  const analisarCodigo = async () => {
    if (!codigoFonte.trim()) { alert('Escreva algum código Java antes de analisar.'); return; }
    setIsAnalisando(true); setResultadoAnalise(null); setPainelOpen(true);
    setTerminalLines([]); setStatusMsg('Analisando...'); setAlertCount(0);
    addTermLine(`$ javac ${nomeArquivoAtivo || 'CodigoAluno.java'}`, 'prompt');
    try {
      const response = await axios.post('http://localhost:8081/api/files/analisar', {
        fileName: nomeArquivoAtivo || 'CodigoAluno.java',
        content: codigoFonte,
      });
      const data = response.data;
      setResultadoAnalise(data);
      const count = data.issues?.length || 0;
      setAlertCount(count);
      if (count > 0) {
        addTermLine('Compilado com sucesso.', 'ok');
        data.issues.forEach(i => addTermLine(`WARN  linha ${i.lineNumber}: ${i.message}`, 'warn'));
        setStatusMsg(`${count} alerta${count > 1 ? 's' : ''} encontrado${count > 1 ? 's' : ''}`);
      } else {
        addTermLine('Compilado com sucesso.', 'ok');
        addTermLine('Nenhum problema de concorrência detectado.', 'ok');
        setStatusMsg('Sem problemas detectados');
      }
    } catch (error) {
      const msg = error.response?.data || 'Erro de conexão com o backend.';
      const msgStr = typeof msg === 'string' ? msg : 'Erro interno no servidor.';
      setResultadoAnalise({ error: msgStr });
      addTermLine('ERRO: ' + msgStr.split('\n')[0], 'error');
      setStatusMsg('Erro na análise');
    } finally { setIsAnalisando(false); }
  };

  // Mantém a ref sempre atualizada com a versão mais recente de analisarCodigo
  useEffect(() => { analisarRef.current = analisarCodigo; });

  useEffect(() => { if (feedbackRef.current) feedbackRef.current.scrollTop = 0; }, [resultadoAnalise]);

  const renderFeedback = () => {
    if (!resultadoAnalise) return (
      <EmptyState theme={theme}>
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke={theme.textDim} strokeWidth="1.2">
          <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01" strokeLinecap="round"/>
        </svg>
        Execute o código para<br/>ver o feedback educacional
      </EmptyState>
    );
    if (resultadoAnalise.error) return (
      <AlertCard $color="red" $delay="0s">
        <AlertHeader><AlertTitle $color="red">Erro na análise</AlertTitle></AlertHeader>
        <SectionText style={{ color: '#fca5a5' }}>{resultadoAnalise.error}</SectionText>
      </AlertCard>
    );
    if (!resultadoAnalise.issues || resultadoAnalise.issues.length === 0) return (
      <SuccessCard theme={theme}>
        <SectionLabel theme={theme} style={{ color: '#6ee7b7', marginBottom: 6 }}>
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="#10b981" strokeWidth="1.5">
            <circle cx="8" cy="8" r="7"/><path d="M5 8l2.5 2.5L11 5" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          Nenhum problema detectado
        </SectionLabel>
        <SectionText theme={theme} $sans $green>O código não apresenta problemas de concorrência ou deadlocks.</SectionText>
      </SuccessCard>
    );
    return resultadoAnalise.issues.map((issue, i) => <IssueCard key={i} issue={issue} index={i} />);
  };

  return (
    <ThemeProvider theme={theme}>
      <Shell>

        <Titlebar>
          <TitleDots>
            <Dot color="#ff5f57" /><Dot color="#febc2e" /><Dot color="#28c840" />
          </TitleDots>
          <BrandArea>
            <img src="/logo.png" alt="Sumawma" style={{ width: 24, height: 24, objectFit: 'contain', flexShrink: 0 }} />
            <BrandName>Sumawma</BrandName>
          </BrandArea>
          <TitleText>
            {nomeArquivoAtivo ? `${nomeArquivoAtivo} — Analisador de Threads Java` : 'Analisador de Threads Java'}
          </TitleText>
          <ThemeBtn onClick={toggleTheme}>
            {isDark ? '☀️ Modo claro' : '🌙 Modo escuro'}
          </ThemeBtn>
          <RunButton $running={isAnalisando} onClick={analisarCodigo} disabled={isAnalisando}>
            {isAnalisando
              ? <><SpinnerEl /> Processando...</>
              : <><svg width="9" height="9" viewBox="0 0 10 10" fill="white"><polygon points="1,1 9,5 1,9"/></svg>Executar e Analizar Threads</>
            }
          </RunButton>
        </Titlebar>

        <Body>
          <PanelGroup direction="horizontal" style={{ flex: 1, overflow: 'hidden' }}>

            <Panel defaultSize={18} minSize={10} maxSize={35} collapsible
              onCollapse={() => setSidebarOpen(false)} onExpand={() => setSidebarOpen(true)}
              style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}
            >
              <SidebarInner>
                <SidebarHeader>
                  {sidebarOpen && (
                    <>
                      <svg width="13" height="13" viewBox="0 0 16 16" fill={theme.textDim} style={{ flexShrink: 0 }}>
                        <path d="M2 2h4l2 2h6v10H2V2z"/>
                      </svg>
                      <SidebarTitle>Explorador</SidebarTitle>
                      <IconBtn $open title="Novo arquivo (Ctrl+N)" onClick={() => startCreateRef.current?.('root', 'file')}><IcoNewFile /></IconBtn>
                      <IconBtn $open title="Nova pasta" onClick={() => startCreateRef.current?.('root', 'folder')}><IcoNewFolder /></IconBtn>
                    </>
                  )}
                  <CollapseBtn onClick={() => setSidebarOpen(v => !v)} title={sidebarOpen ? 'Recolher' : 'Expandir'}>
                    <svg width="14" height="14" viewBox="0 0 14 14" fill="none"
                      style={{ transform: sidebarOpen ? 'none' : 'scaleX(-1)', transition: 'transform 0.2s' }}>
                      <path d="M9 3L5 7L9 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  </CollapseBtn>
                </SidebarHeader>
                <SidebarContent $open={sidebarOpen}>
                  <SidebarTree
                    tree={tree} setTree={setTree}
                    activeFileId={activeFileId} setActiveFileId={setActiveFileId}
                    onStartCreate={startCreateRef} theme={theme}
                  />
                </SidebarContent>
              </SidebarInner>
            </Panel>

            <ResizeHandle theme={theme} />

            <Panel style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden', minWidth: 0, flex: 1 }}>
              <EditorArea>
                {!activeFile ? (
                  <WelcomeScreen>
                    <WelcomeBgLogo src="/logo.png" alt="" />
                    <WelcomeBrand>
                      <img src="/logo.png" alt="Sumawma" style={{ width: 56, height: 56, objectFit: 'contain' }} />
                      <WelcomeBrandName>Sumawma</WelcomeBrandName>
                      <WelcomeBrandSub>Analisador de Threads Java</WelcomeBrandSub>
                    </WelcomeBrand>
                    <WelcomeGrid>
                      <WelcomeItem onClick={() => startCreateRef.current?.('root', 'file')}>
                        <WelcomeItemLabel><IcoNewFile /> Novo arquivo Java</WelcomeItemLabel>
                        <WelcomeKbd>Ctrl+N</WelcomeKbd>
                      </WelcomeItem>
                      <WelcomeItem onClick={() => startCreateRef.current?.('root', 'folder')}>
                        <WelcomeItemLabel><IcoNewFolder /> Nova pasta</WelcomeItemLabel>
                        <WelcomeKbd>—</WelcomeKbd>
                      </WelcomeItem>
                      <WelcomeItem style={{ cursor: 'default', background: 'transparent', borderColor: 'transparent' }}>
                        <WelcomeItemLabel style={{ fontSize: 12 }}>
                          <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                            <circle cx="6" cy="6" r="5"/><path d="M6 4v3M6 8.5v.5" strokeLinecap="round"/>
                          </svg>
                          {/* ── NOVO: hint atualizado ── */}
                          Autosave ativo · Ctrl+S salvar · Ctrl+Enter analisar
                        </WelcomeItemLabel>
                      </WelcomeItem>
                    </WelcomeGrid>
                  </WelcomeScreen>
                ) : (
                  <>
                    <TabBar>
                      <Tab $active><TabDot />{nomeArquivoAtivo}</Tab>
                    </TabBar>
                    <div style={{ flex: 1, overflow: 'hidden', minHeight: 0, position: 'relative' }}>
                      <SaveIndicator theme={theme} $saved={saveStatus === 'saved'} $visible={saveVisible}>
                        {saveStatus === 'saved'
                          ? <><svg width="11" height="11" viewBox="0 0 10 10" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M1.5 5l2.5 2.5 4.5-4" strokeLinecap="round" strokeLinejoin="round"/></svg>Salvo</>
                          : <><svg width="11" height="11" viewBox="0 0 10 10" fill="none" stroke="currentColor" strokeWidth="1.5"><circle cx="5" cy="5" r="4"/><path d="M5 3v2.5" strokeLinecap="round"/></svg>Salvando...</>
                        }
                      </SaveIndicator>
                      <div key={editorKey} style={{ height: '100%' }}>
                        {/* ── NOVO: prop issues passada para o MeuEditor ── */}
                        <MeuEditor
                          idArquivo={idArquivo}
                          atualizarCaminho={() => {}}
                          onChange={handleCodigoChange}
                          issues={resultadoAnalise?.issues || []}
                        />
                      </div>
                    </div>
                  </>
                )}
              </EditorArea>

              <RightPanels $open={painelOpen}>
                <PanelGroup direction="vertical" style={{ height: '100%' }}>
                  <Panel defaultSize={62} minSize={30} style={{ display: 'flex', flexDirection: 'column', minHeight: 0 }}>
                    <PanelHeaderEl>
                      <PanelDot $color={theme.accent} />
                      <PanelLabel>Feedback Educacional</PanelLabel>
                      {alertCount > 0 && (
                        <span style={{ marginLeft: 'auto', background: 'rgba(239,68,68,0.15)', color: '#fca5a5', fontSize: 11, padding: '2px 9px', borderRadius: 10 }}>
                          {alertCount} alerta{alertCount > 1 ? 's' : ''}
                        </span>
                      )}
                    </PanelHeaderEl>
                    <FeedbackBody ref={feedbackRef}>{renderFeedback()}</FeedbackBody>
                  </Panel>
                  <PanelResizeHandle style={{ height: 5, background: theme.border, cursor: 'row-resize', flexShrink: 0 }} />
                  <Panel defaultSize={38} minSize={20} style={{ display: 'flex', flexDirection: 'column', minHeight: 0 }}>
                    <PanelHeaderEl>
                      <PanelDot $color={theme.green} />
                      <PanelLabel>Terminal</PanelLabel>
                    </PanelHeaderEl>
                    <TerminalWrapper ref={termRef}>
                      {terminalLines.length === 0
                        ? <TermLine $type="default">$ A aguardar execução do código Java...</TermLine>
                        : terminalLines.map((l, i) => <TermLine key={i} $type={l.type}>{l.text}</TermLine>)
                      }
                    </TerminalWrapper>
                  </Panel>
                </PanelGroup>
              </RightPanels>
            </Panel>

          </PanelGroup>
        </Body>

        <Statusbar>
          <SbItem>Java</SbItem>
          <SbItem $dim>UTF-8</SbItem>
          {nomeArquivoAtivo && <SbItem $dim>{nomeArquivoAtivo}</SbItem>}
          {alertCount > 0 && (
            <SbAlert>
              <svg width="10" height="10" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M8 2L1 14h14L8 2z M8 7v3M8 12v.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {alertCount} alerta{alertCount > 1 ? 's' : ''}
            </SbAlert>
          )}
          <SbItem style={{ marginLeft: 'auto' }}>{statusMsg}</SbItem>
        </Statusbar>

      </Shell>
    </ThemeProvider>
  );
};

export default EditorJavaThreads;