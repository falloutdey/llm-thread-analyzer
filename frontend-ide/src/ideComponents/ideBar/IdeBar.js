import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import CustomAvatar from "../../bar/CustomAvatar";
import HomeButton from "../../bar/HomeButton";
import UserProfile from "../../bar/UserProfile";
import BotaoIrParaProjeto from "./BotaoIrParaProjeto";

export default function Idebar() {
  const [user, setUser] = React.useState({});

  React.useEffect(() => {
    fetchUserDetails();
  }, []);

  const fetchUserDetails = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch("/protected", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error("Erro ao buscar detalhes do usuário");
      }

      const data = await response.json();
      setUser(data.user || {});
    } catch (error) {
      console.error("Erro ao buscar detalhes do usuário:", error.message);
    }
  };

  return (
    <AppBar position="static">
      <Toolbar>
        {" "}
        {/* Ajustando o padding */}
        <div style={{ display: "flex", flexGrow: 1 }}>
          <Typography
            variant="h4"
            style={{ paddingLeft: "200px" }}
          >
            Sumawma
          </Typography>
          <div style={{ paddingLeft: '10px' }}>
          <HomeButton />
          </div>
        </div>
        <div style={{ display: "flex", paddingRight: "200px" }}>
            <BotaoIrParaProjeto />
          <UserProfile />
          <CustomAvatar />
        </div>
      </Toolbar>
    </AppBar>
  );
}
