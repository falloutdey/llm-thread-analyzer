import React, { useState, useEffect } from "react";
import "./PaginaEstudante.css";
import { useNavigate, Navigate, Link } from "react-router-dom";
import Logo from "../img/logo111.png";
import AppBar from "./barComponent/AppBar";

function Footer() {
 
  return (
    <div className="pagina-estudante">
        {/* Rodapé */}
        <footer className="plataforma-footer">
          <div className="container">
            <div className="footer-links">
              <div className="footer-section">
                <h3>Sobre</h3>
                <ul>
                  <li><Link to="/sobre">Sobre o sistema</Link></li>
                  <li><Link to="/termos">Termos de uso</Link></li>
                  <li><Link to="/privacidade">Política de privacidade</Link></li>
                </ul>
              </div>
              <div className="footer-section">
                <h3>Ajuda</h3>
                <ul>
                  <li><Link to="/ajuda">Central de ajuda</Link></li>
                  <li><Link to="/contato">Fale conosco</Link></li>
                  <li><Link to="/tutoriais">Tutoriais</Link></li>
                </ul>
              </div>
              <div className="footer-section">
                <h3>Redes Sociais</h3>
                <ul>
                  <li><a href="https://www.instagram.com/labiocad.ufpa/">Instagram</a></li>
                  
                  {/* <li><Link to="/twitter">Twitter</Link></li>

                  <li><Link to="/youtube">YouTube</Link></li> */}
                </ul>
              </div>
            </div>
            <div className="footer-bottom">
              <p>© 2025 SUMAWMA - Universidade Federal do Pará. Todos os direitos reservados.</p>
            </div>
          </div>
        </footer>
    </div>
  );
}

export default Footer;
