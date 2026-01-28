// Importando os componentes necessários do React, Material UI, seu componente Cadastro e Link do react-router-dom
import * as React from "react";
import Button from "@mui/material/Button";
import { Link } from "react-router-dom";

// Definindo o componente MenuBar
export default function LoginBotton() {
  // Retornando o JSX para renderizar o componente
  return (
    <div>
      {/* Renderizando o botão que leva para a rota cadastro */}
      <Link to="/login">
        <Button variant="contained" disableElevation>
          Entrar
        </Button>
      </Link>
    </div>
  );
}
