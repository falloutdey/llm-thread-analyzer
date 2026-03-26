import React, { useState } from "react";
import { Link } from "react-router-dom";
import styled, { createGlobalStyle, keyframes, ThemeProvider } from "styled-components";
import Logo from "../img/logo111.png";

/* ══════════════════════════════════════════
   TEMAS
══════════════════════════════════════════ */
const darkTheme = {
  mode: 'dark',
  bg:        '#0f1623',
  bgCard:    '#172033',
  bgCardHov: '#1d2840',
  border:    '#253047',
  accent:    '#3b82f6',
  accentSoft:'rgba(59,130,246,0.12)',
  accentMid: 'rgba(59,130,246,0.25)',
  teal:      '#06b6d4',
  tealSoft:  'rgba(6,182,212,0.1)',
  textPri:   '#f1f5f9',
  textSec:   '#94a3b8',
  textDim:   '#475569',
};

const lightTheme = {
  mode: 'light',
  bg:        '#f8faff',
  bgCard:    '#ffffff',
  bgCardHov: '#f0f5ff',
  border:    '#dde5f0',
  accent:    '#2563eb',
  accentSoft:'rgba(37,99,235,0.08)',
  accentMid: 'rgba(37,99,235,0.18)',
  teal:      '#0891b2',
  tealSoft:  'rgba(8,145,178,0.08)',
  textPri:   '#0f172a',
  textSec:   '#475569',
  textDim:   '#94a3b8',
};

/* ══════════════════════════════════════════
   ANIMAÇÕES
══════════════════════════════════════════ */
const fadeUp = keyframes`
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
`;

const bobble = keyframes`
  0%, 100% { transform: translateY(0); }
  50%       { transform: translateY(-6px); }
`;

/* ══════════════════════════════════════════
   GLOBAL
══════════════════════════════════════════ */
const GlobalStyle = createGlobalStyle`
  @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap');

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    background: ${p => p.theme.bg};
    color: ${p => p.theme.textPri};
    font-family: 'Plus Jakarta Sans', sans-serif;
    transition: background 0.4s, color 0.4s;
    overflow-x: hidden;
  }

  a { text-decoration: none; color: inherit; }
`;

const Page = styled.div`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
`;

/* ══════════════════════════════════════════
   HEADER
══════════════════════════════════════════ */
const Header = styled.header`
  position: sticky;
  top: 0;
  z-index: 100;
  background: ${p => p.theme.mode === 'dark'
    ? 'rgba(15,22,35,0.9)'
    : 'rgba(248,250,255,0.92)'};
  backdrop-filter: blur(18px);
  border-bottom: 1px solid ${p => p.theme.border};
  transition: background 0.4s, border-color 0.4s;
`;

const HeaderInner = styled.div`
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 28px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 16px;
`;

const LogoArea = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
`;

const LogoImg = styled.img`
  height: 34px;
  width: auto;
`;

const BrandName = styled.span`
  font-size: 17px;
  font-weight: 800;
  color: ${p => p.theme.textPri};
  letter-spacing: -0.01em;
`;

const BrandSub = styled.span`
  font-size: 11px;
  font-weight: 500;
  color: ${p => p.theme.textDim};
`;

const HeaderRight = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

const ToggleBtn = styled.button`
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 14px;
  border-radius: 20px;
  border: 1px solid ${p => p.theme.border};
  background: ${p => p.theme.bgCard};
  cursor: pointer;
  color: ${p => p.theme.textSec};
  font-size: 13px;
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 500;
  transition: all 0.25s;

  &:hover {
    border-color: ${p => p.theme.accent};
    color: ${p => p.theme.accent};
  }
`;

const NavLink = styled(Link)`
  font-size: 14px;
  font-weight: 500;
  color: ${p => p.theme.textSec};
  padding: 7px 14px;
  border-radius: 8px;
  transition: color 0.2s, background 0.2s;

  &:hover {
    color: ${p => p.theme.accent};
    background: ${p => p.theme.accentSoft};
  }
`;

const NavBtn = styled(Link)`
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: ${p => p.theme.accent};
  padding: 8px 20px;
  border-radius: 8px;
  transition: background 0.2s, transform 0.2s, box-shadow 0.2s;

  &:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px ${p => p.theme.accentMid};
  }
`;

/* ══════════════════════════════════════════
   HERO
══════════════════════════════════════════ */
const HeroWrap = styled.section`
  padding: 88px 28px 72px;
  text-align: center;
  position: relative;
  overflow: hidden;
`;

const HeroBlob = styled.div`
  position: absolute;
  top: -80px;
  left: 50%;
  transform: translateX(-50%);
  width: 760px;
  height: 420px;
  background: radial-gradient(
    ellipse at center,
    ${p => p.theme.accentSoft} 0%,
    ${p => p.theme.tealSoft} 40%,
    transparent 70%
  );
  pointer-events: none;
`;

const HeroInner = styled.div`
  max-width: 720px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
`;

const HeroPill = styled.div`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: ${p => p.theme.bgCard};
  border: 1px solid ${p => p.theme.border};
  border-radius: 24px;
  padding: 6px 16px 6px 8px;
  margin-bottom: 32px;
  font-size: 13px;
  font-weight: 500;
  color: ${p => p.theme.textSec};
  animation: ${fadeUp} 0.5s ease both;
`;

const PillBadge = styled.span`
  background: ${p => p.theme.accent};
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 12px;
`;

const HeroTitle = styled.h1`
  font-size: clamp(2.1rem, 5vw, 3.4rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: ${p => p.theme.textPri};
  margin-bottom: 20px;
  animation: ${fadeUp} 0.5s 0.08s ease both;

  mark {
    background: none;
    background-image: linear-gradient(90deg, ${p => p.theme.accent}, ${p => p.theme.teal});
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
`;

const HeroDesc = styled.p`
  font-size: 1.05rem;
  color: ${p => p.theme.textSec};
  line-height: 1.75;
  max-width: 540px;
  margin: 0 auto 40px;
  animation: ${fadeUp} 0.5s 0.16s ease both;
`;

const HeroActions = styled.div`
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 60px;
  animation: ${fadeUp} 0.5s 0.24s ease both;
`;

const BtnPrimary = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 30px;
  background: ${p => p.theme.accent};
  color: #fff;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
  transition: filter 0.2s, transform 0.2s, box-shadow 0.2s;

  &:hover {
    filter: brightness(1.1);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px ${p => p.theme.accentMid};
  }
`;

const BtnGhost = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 13px 28px;
  border: 1.5px solid ${p => p.theme.border};
  color: ${p => p.theme.textSec};
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  transition: border-color 0.2s, color 0.2s, background 0.2s, transform 0.2s;

  &:hover {
    border-color: ${p => p.theme.accent};
    color: ${p => p.theme.accent};
    background: ${p => p.theme.accentSoft};
    transform: translateY(-2px);
  }
`;

const HeroStats = styled.div`
  display: inline-flex;
  background: ${p => p.theme.bgCard};
  border: 1px solid ${p => p.theme.border};
  border-radius: 14px;
  overflow: hidden;
  animation: ${fadeUp} 0.5s 0.32s ease both;
  transition: background 0.4s, border-color 0.4s;
`;

const StatItem = styled.div`
  padding: 18px 36px;
  border-right: 1px solid ${p => p.theme.border};
  text-align: center;
  transition: border-color 0.4s;

  &:last-child { border-right: none; }
`;

const StatNum = styled.div`
  font-size: 1.6rem;
  font-weight: 800;
  color: ${p => p.theme.accent};
  line-height: 1;
  margin-bottom: 4px;
`;

const StatLabel = styled.div`
  font-size: 12px;
  font-weight: 500;
  color: ${p => p.theme.textDim};
  white-space: nowrap;
`;

/* ══════════════════════════════════════════
   SHARED
══════════════════════════════════════════ */
const SectionWrap = styled.section`
  padding: 80px 28px;
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
`;

const Divider = styled.div`
  height: 1px;
  background: ${p => p.theme.border};
  opacity: 0.6;
  transition: background 0.4s;
`;

const SectionTop = styled.div`
  margin-bottom: 48px;
  text-align: center;
`;

const Eyebrow = styled.p`
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: ${p => p.color || p.theme.accent};
  margin-bottom: 10px;
`;

const SectionTitle = styled.h2`
  font-size: clamp(1.6rem, 3vw, 2.1rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: ${p => p.theme.textPri};
  margin-bottom: 14px;
  line-height: 1.2;
`;

const SectionDesc = styled.p`
  font-size: 1rem;
  color: ${p => p.theme.textSec};
  line-height: 1.7;
  max-width: 500px;
  margin: 0 auto;
`;

/* ══════════════════════════════════════════
   FRAMEWORK CARDS
══════════════════════════════════════════ */
const FrameworkGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 18px;
`;

const FwCard = styled(Link)`
  background: ${p => p.theme.bgCard};
  border: 1.5px solid ${p => p.theme.border};
  border-radius: 16px;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  transition: transform 0.25s, border-color 0.25s, box-shadow 0.25s, background 0.4s;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    background: ${p => p.accent || p.theme.accent};
    border-radius: 16px 16px 0 0;
    opacity: 0;
    transition: opacity 0.25s;
  }

  &:hover {
    transform: translateY(-5px);
    border-color: ${p => p.accent || p.theme.accent}55;
    box-shadow: 0 12px 36px ${p => p.theme.mode === 'dark'
      ? 'rgba(0,0,0,0.4)' : 'rgba(15,23,42,0.1)'};
    background: ${p => p.theme.bgCardHov};
    &::before { opacity: 1; }
  }
`;

const FwIconWrap = styled.div`
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: ${p => p.bg};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 18px;
  animation: ${bobble} 4s ease-in-out infinite;
  animation-delay: ${p => p.delay || '0s'};
`;

const FwTitle = styled.h3`
  font-size: 1rem;
  font-weight: 700;
  color: ${p => p.theme.textPri};
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const FwBadge = styled.span`
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: ${p => p.bg};
  color: ${p => p.color};
`;

const FwDesc = styled.p`
  font-size: 0.875rem;
  color: ${p => p.theme.textSec};
  line-height: 1.65;
  flex: 1;
`;

const FwFooter = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 20px;
  font-size: 13px;
  font-weight: 600;
  color: ${p => p.color || p.theme.accent};
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s, transform 0.2s;

  ${FwCard}:hover & { opacity: 1; transform: translateY(0); }
`;

/* ══════════════════════════════════════════
   HOW IT WORKS
══════════════════════════════════════════ */
const StepsWrap = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  background: ${p => p.theme.bgCard};
  border: 1.5px solid ${p => p.theme.border};
  border-radius: 16px;
  overflow: hidden;
  transition: background 0.4s, border-color 0.4s;
`;

const Step = styled.div`
  padding: 36px 28px;
  border-right: 1px solid ${p => p.theme.border};
  transition: background 0.2s, border-color 0.4s;

  &:last-child { border-right: none; }
  &:hover { background: ${p => p.theme.bgCardHov}; }
`;

const StepNum = styled.div`
  font-size: 11px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.1em;
  color: ${p => p.theme.accent};
  margin-bottom: 16px;
  opacity: 0.7;
`;

const StepIcon = styled.div`
  font-size: 28px;
  margin-bottom: 14px;
`;

const StepTitle = styled.h3`
  font-size: 0.95rem;
  font-weight: 700;
  color: ${p => p.theme.textPri};
  margin-bottom: 8px;
`;

const StepDesc = styled.p`
  font-size: 0.83rem;
  color: ${p => p.theme.textSec};
  line-height: 1.65;
`;

/* ══════════════════════════════════════════
   FEATURES
══════════════════════════════════════════ */
const FeatGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
`;

const FeatCard = styled.div`
  padding: 24px;
  background: ${p => p.theme.bgCard};
  border: 1.5px solid ${p => p.theme.border};
  border-radius: 12px;
  transition: background 0.4s, border-color 0.2s, transform 0.2s;

  &:hover {
    border-color: ${p => p.theme.accent}44;
    transform: translateY(-3px);
    background: ${p => p.theme.bgCardHov};
  }
`;

const FeatIcon = styled.div` font-size: 22px; margin-bottom: 12px; `;
const FeatTitle = styled.h3`
  font-size: 0.92rem; font-weight: 700;
  color: ${p => p.theme.textPri}; margin-bottom: 6px;
`;
const FeatDesc = styled.p`
  font-size: 0.82rem; color: ${p => p.theme.textSec}; line-height: 1.65;
`;

/* ══════════════════════════════════════════
   CTA
══════════════════════════════════════════ */
const CtaWrap = styled.section`
  padding: 64px 28px 80px;
  text-align: center;
`;

const CtaBox = styled.div`
  max-width: 580px;
  margin: 0 auto;
  background: ${p => p.theme.mode === 'dark'
    ? 'linear-gradient(135deg, #172033 0%, #1a2a44 100%)'
    : 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)'};
  border: 1.5px solid ${p => p.theme.border};
  border-radius: 20px;
  padding: 56px 48px;
  transition: background 0.4s, border-color 0.4s;
`;

const CtaTitle = styled.h2`
  font-size: clamp(1.5rem, 3vw, 1.9rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: ${p => p.theme.textPri};
  margin-bottom: 14px;
`;

const CtaDesc = styled.p`
  font-size: 0.98rem;
  color: ${p => p.theme.textSec};
  line-height: 1.7;
  max-width: 400px;
  margin: 0 auto 32px;
`;

/* ══════════════════════════════════════════
   FOOTER
══════════════════════════════════════════ */
const Footer = styled.footer`
  border-top: 1px solid ${p => p.theme.border};
  padding: 24px 28px;
  transition: border-color 0.4s;
`;

const FooterInner = styled.div`
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
`;

const FooterText = styled.p`
  font-size: 13px;
  color: ${p => p.theme.textDim};
`;

const FooterLinks = styled.div` display: flex; gap: 20px; `;
const FooterLink = styled(Link)`
  font-size: 13px; color: ${p => p.theme.textDim};
  transition: color 0.2s;
  &:hover { color: ${p => p.theme.accent}; }
`;

/* ══════════════════════════════════════════
   DADOS
══════════════════════════════════════════ */
const frameworks = [
  {
    id: 1, title: 'OpenMP', icon: '⚡', badge: 'Threads',
    badgeBg: 'rgba(245,158,11,0.12)', badgeColor: '#f59e0b',
    accent: '#f59e0b', iconBg: 'rgba(245,158,11,0.12)',
    link: '/login', delay: '0s',
    desc: 'Aprenda paralelismo com threads para sistemas de memória compartilhada. Ideal para CPUs multicore.',
  },
  {
    id: 2, title: 'OpenMPI', icon: '🌐', badge: 'Distribuído',
    badgeBg: 'rgba(6,182,212,0.12)', badgeColor: '#06b6d4',
    accent: '#06b6d4', iconBg: 'rgba(6,182,212,0.12)',
    link: '/login', delay: '0.3s',
    desc: 'Explore computação distribuída em clusters. Entenda troca de mensagens entre processos independentes.',
  },
  {
    id: 3, title: 'OpenACC', icon: '🚀', badge: 'GPU',
    badgeBg: 'rgba(239,68,68,0.12)', badgeColor: '#ef4444',
    accent: '#ef4444', iconBg: 'rgba(239,68,68,0.12)',
    link: '/login', delay: '0.6s',
    desc: 'Acelere seus programas com GPUs. Entenda como transferir cargas computacionais para hardware dedicado.',
  },
  {
    id: 4, title: 'Java Threads', icon: '☕', badge: 'Novo',
    badgeBg: 'rgba(59,130,246,0.15)', badgeColor: '#3b82f6',
    accent: '#3b82f6', iconBg: 'rgba(59,130,246,0.12)',
    link: '/editor-java-threads', delay: '0.9s',
    desc: 'Escreva código Java e receba feedback educacional com IA. Detecta race conditions e deadlocks com explicações didáticas.',
  },
];

const steps = [
  { num: '01', icon: '📝', title: 'Escolha um exercício', desc: 'Selecione o framework e o nível que deseja praticar.' },
  { num: '02', icon: '💻', title: 'Escreva seu código', desc: 'Use o editor integrado com suporte a sintaxe para desenvolver sua solução.' },
  { num: '03', icon: '🔍', title: 'Analise os resultados', desc: 'Veja o feedback detalhado, identifique erros e entenda as causas.' },
  { num: '04', icon: '📈', title: 'Evolua na prática', desc: 'Acompanhe seu progresso e avance para desafios mais complexos.' },
];

const features = [
  { id: 1, icon: '✏️', title: 'Editor de Código', desc: 'Editor com realce de sintaxe e execução integrada para prática imediata.' },
  { id: 2, icon: '🤖', title: 'Feedback com IA', desc: 'Explicações didáticas geradas por inteligência artificial sobre seus erros.' },
  { id: 3, icon: '💾', title: 'Projetos Salvos', desc: 'Guarde e retome seus projetos a qualquer momento.' },
  { id: 4, icon: '📖', title: 'Tutoriais Guiados', desc: 'Conteúdo estruturado do básico ao avançado em cada framework.' },
];

/* ══════════════════════════════════════════
   COMPONENTE
══════════════════════════════════════════ */
function PaginaPrincipal() {
  const [isDark, setIsDark] = useState(true);
  const theme = isDark ? darkTheme : lightTheme;

  return (
    <ThemeProvider theme={theme}>
      <GlobalStyle />
      <Page>

        {/* ── Header ── */}
        <Header>
          <HeaderInner>
            <LogoArea>
              <LogoImg src={Logo} alt="SUMAWMA" />
              <div>
                <BrandName>SUMAWMA</BrandName>
                <BrandSub> · Plataforma de Aprendizado</BrandSub>
              </div>
            </LogoArea>
            <HeaderRight>
              <ToggleBtn onClick={() => setIsDark(v => !v)}>
                {isDark ? '☀️' : '🌙'}
                {isDark ? 'Modo claro' : 'Modo escuro'}
              </ToggleBtn>
              <NavLink to="/login">Entrar</NavLink>
              <NavBtn to="/registro">Criar conta grátis</NavBtn>
            </HeaderRight>
          </HeaderInner>
        </Header>

        {/* ── Hero ── */}
        <HeroWrap>
          <HeroBlob />
          <HeroInner>
            <HeroPill>
              <PillBadge>Novo</PillBadge>
              Análise de Java Threads com IA disponível agora
            </HeroPill>

            <HeroTitle>
              Aprenda computação paralela <mark>na prática</mark>
            </HeroTitle>

            <HeroDesc>
              Uma plataforma educacional para estudantes de computação dominarem
              OpenMP, OpenMPI, OpenACC e Java Threads — com exercícios interativos
              e feedback inteligente.
            </HeroDesc>

            <HeroActions>
              <BtnPrimary to="/registro">
                Começar gratuitamente
                <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
                  <path d="M3 7.5h9M9 4l3 3.5-3 3.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </BtnPrimary>
              <BtnGhost to="/login">
                Já tenho conta
              </BtnGhost>
            </HeroActions>

            <HeroStats>
              <StatItem>
                <StatNum>4</StatNum>
                <StatLabel>Frameworks</StatLabel>
              </StatItem>
              <StatItem>
                <StatNum>IA</StatNum>
                <StatLabel>Feedback inteligente</StatLabel>
              </StatItem>
              <StatItem>
                <StatNum>100%</StatNum>
                <StatLabel>Gratuito</StatLabel>
              </StatItem>
            </HeroStats>
          </HeroInner>
        </HeroWrap>

        <Divider />

        {/* ── Frameworks ── */}
        <SectionWrap>
          <SectionTop>
            <Eyebrow>Ferramentas disponíveis</Eyebrow>
            <SectionTitle>Escolha seu framework</SectionTitle>
            <SectionDesc>
              Pratique com os principais paradigmas da computação paralela,
              cada um com exercícios e materiais próprios.
            </SectionDesc>
          </SectionTop>
          <FrameworkGrid>
            {frameworks.map(fw => (
              <FwCard to={fw.link} key={fw.id} accent={fw.accent}>
                <FwIconWrap bg={fw.iconBg} delay={fw.delay}>{fw.icon}</FwIconWrap>
                <FwTitle>
                  {fw.title}
                  <FwBadge bg={fw.badgeBg} color={fw.badgeColor}>{fw.badge}</FwBadge>
                </FwTitle>
                <FwDesc>{fw.desc}</FwDesc>
                <FwFooter color={fw.accent}>
                  Acessar
                  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                    <path d="M2.5 6.5h8M7 3l3.5 3.5L7 10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                </FwFooter>
              </FwCard>
            ))}
          </FrameworkGrid>
        </SectionWrap>

        <Divider />

        {/* ── Como funciona ── */}
        <SectionWrap>
          <SectionTop>
            <Eyebrow color="#06b6d4">Como funciona</Eyebrow>
            <SectionTitle>Do zero ao paralelo em 4 passos</SectionTitle>
            <SectionDesc>
              Um fluxo simples pensado para estudantes que estão começando
              ou aperfeiçoando seus conhecimentos.
            </SectionDesc>
          </SectionTop>
          <StepsWrap>
            {steps.map(s => (
              <Step key={s.num}>
                <StepNum>{s.num}</StepNum>
                <StepIcon>{s.icon}</StepIcon>
                <StepTitle>{s.title}</StepTitle>
                <StepDesc>{s.desc}</StepDesc>
              </Step>
            ))}
          </StepsWrap>
        </SectionWrap>

        <Divider />

        {/* ── Recursos ── */}
        <SectionWrap>
          <SectionTop>
            <Eyebrow>Recursos</Eyebrow>
            <SectionTitle>Tudo para o seu aprendizado</SectionTitle>
            <SectionDesc>
              Ferramentas integradas para que você foque no que importa: aprender e evoluir.
            </SectionDesc>
          </SectionTop>
          <FeatGrid>
            {features.map(f => (
              <FeatCard key={f.id}>
                <FeatIcon>{f.icon}</FeatIcon>
                <FeatTitle>{f.title}</FeatTitle>
                <FeatDesc>{f.desc}</FeatDesc>
              </FeatCard>
            ))}
          </FeatGrid>
        </SectionWrap>

        {/* ── CTA ── */}
        <CtaWrap>
          <CtaBox>
            <CtaTitle>Pronto para aprender?</CtaTitle>
            <CtaDesc>
              Crie sua conta gratuitamente e comece a praticar programação paralela
              com exercícios reais e feedback imediato.
            </CtaDesc>
            <BtnPrimary to="/registro">
              Criar minha conta grátis
              <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
                <path d="M3 7.5h9M9 4l3 3.5-3 3.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </BtnPrimary>
          </CtaBox>
        </CtaWrap>

        {/* ── Footer ── */}
        <Footer>
          <FooterInner>
            <FooterText>© {new Date().getFullYear()} SUMAWMA · Todos os direitos reservados</FooterText>
            <FooterLinks>
              <FooterLink to="/login">Entrar</FooterLink>
              <FooterLink to="/registro">Registrar</FooterLink>
            </FooterLinks>
          </FooterInner>
        </Footer>

      </Page>
    </ThemeProvider>
  );
}

export default PaginaPrincipal;