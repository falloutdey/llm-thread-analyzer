import React, { useState } from "react";
import axios from "axios";
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";

const CadastroAdm = ({ onCadastro }) => {
  const [formData, setFormData] = useState({
    user: {
      first_name: "",
      last_name: "",
      username: "",
      email: "",
      password1: "",
      password2: "",
      user_type: "professor",
    },
  });

  const [showPassword1, setShowPassword1] = useState(false);
  const [showPassword2, setShowPassword2] = useState(false);

  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setFormData({
      user: {
        ...formData.user,
        [e.target.name]: e.target.value,
      },
    });
  };

  const handleCadastro = async () => {
    try {
      // Verifica se os campos obrigatórios estão preenchidos
      if (
        !formData.user.username ||
        !formData.user.email ||
        !formData.user.password1 ||
        !formData.user.password2
      ) {
        setError("Preencha todos os campos obrigatórios.");
        return;
      }

      // Faz a requisição para a API de cadastro
      await axios.post("http://127.0.0.1:5000/api/admin/", formData);

      // Cadastro bem-sucedido
      console.log("Cadastro bem-sucedido!");
      onCadastro();
      setError(null);
    } catch (error) {
      // Trata os erros durante o cadastro
      if (error.response) {
        console.error("Erro no cadastro:", error.response.data);

        if (error.response.data.errors) {
          console.error("Detalhes do erro:", error.response.data.errors);
        }
      }
      setError(
        // "Ocorreu um erro durante o cadastro. Tente novamente mais tarde."
      );
    }
  };

  return (
    <div>
      <h1 style={{ textAlign: "center", marginBottom: "20px" }}>
        Cadastro do Administrador da Plataforma
      </h1>
      <Box
        sx={{
          width: 500,
          maxWidth: "100%",
          margin: "auto",
        }}
      >
        <TextField
          fullWidth
          label="First Name"
          name="first_name"
          value={formData.user.first_name}
          onChange={handleChange}
          margin="normal"
        />
        <TextField
          fullWidth
          label="Last Name"
          name="last_name"
          value={formData.user.last_name}
          onChange={handleChange}
          margin="normal"
        />
        <TextField
          fullWidth
          label="Username"
          name="username"
          value={formData.user.username}
          onChange={handleChange}
          margin="normal"
        />
        <TextField
          fullWidth
          label="Email"
          name="email"
          type="email"
          value={formData.user.email}
          onChange={handleChange}
          margin="normal"
        />
        <TextField
          fullWidth
          label="Password"
          name="password1"
          type={showPassword1 ? "text" : "password"}
          value={formData.user.password1}
          onChange={handleChange}
          margin="normal"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={() => setShowPassword1(!showPassword1)}>
                  {showPassword1 ? <Visibility /> : <VisibilityOff />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
        <TextField
          fullWidth
          label="Confirm Password"
          name="password2"
          type={showPassword2 ? "text" : "password"}
          value={formData.user.password2}
          onChange={handleChange}
          margin="normal"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={() => setShowPassword2(!showPassword2)}>
                  {showPassword2 ? <Visibility /> : <VisibilityOff />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
        <Button variant="contained" onClick={handleCadastro}>
          Cadastrar
        </Button>
        {error && <p>{error}</p>}
      </Box>
    </div>
  );
};

export default CadastroAdm;
