import React from "react";
import Grid from "@mui/material/Grid";
import Box from "@mui/material/Box";
import SearchAppBar from "./SearchAppBar";
import Cadastro from "./components/Cadastro";
import Login from "./components/Login";
import Logo from "./img/logo111.png";
import img1 from "./img/img1.jpeg";

const Rodape = () => {
  return (
    <div>
      <footer style={{ backgroundColor: '#5e9188', padding: '20px', textAlign: 'center', color: '#ffffff'}}>
      <p>&copy; {new Date().getFullYear()} All Right Reserved.</p>
    </footer>
    </div>
  )
}

const Home = ({ onLogin }) => {
  
  return (
    <div style={{ background: '#253342'}}>
      <div>
      <div style={{ margin: '0px 30px 30px 30px', position: 'relative', background: '#5e9188', color: '#ffffff', textAlign: 'center', borderRadius: '50px', padding: '50px 0'}} >
      <img style={{ maxWidth: '10%', height: 'auto' }}
        src={Logo}
        alt="Programação Paralela"
        className="banner-image"
      />
      <h1 style={{ fontSize: '24px', marginTop: '20px' }}>Aprenda Programação Paralela de Forma Eficiente</h1>
    </div>
      </div>
      <Grid container spacing={2} style={{ background: "#253342", }}>
        <Grid item xs={12} md={6}>
          <Box
            sx={{
              // backgroundColor: "#aed6dc",
              // padding: 2,
              // height: "50vh",
              // width: "100%",
              // justifyContent: "center",
              // borderRadius: '50px',
              // alignItems: "center",
              backgroundColor: "#aed6dc",
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              height: "100%",
              borderRadius: '50px',
              marginLeft: '35px'
            }}
          >
            <Login onLogin={onLogin} />
          </Box>
        </Grid>
        <Grid item xs={12} md={6}>
          <Box
            sx={{
              backgroundColor: "#aed6dc",
              display: "flex",
              justifyContent: "left",
              // alignItems: "center",
              height: "100%",
              borderRadius: '50px',
              marginRight: '30px'
            }}
          >
            <div>
              <div style={{ fontSize: '20px', margin: '80px', fontWeight: "bold", fontFamily: 'sans-serif'}}>
                <h1 style={{ color: '#3f5954', marginTop: '-30px' }}>Sumawma</h1>
              </div>

              <div style={{ fontSize: '50px', margin: '80px', fontWeight: "normal",}}>
                <h1 style={{ color: '#3f5954', marginTop: '-30px'}}>“Desbloqueie o potencial da computação paralela.”</h1>
              </div>
            </div>
          </Box>
        </Grid>
      </Grid>
      <div>
      <div style={{ justifyContent: 'center', fontSize: '60px', marginLeft: "20%", marginTop: "2%", color: '#ffffff', }}> 
      Posuere libero varius
     </div> 
    <div style={{ display: "flex", marginLeft: "5%", marginRight: "5%", justifyContent: 'center', marginBottom: '5%'}}>
      
      {/* Caixa 1 */}
      <div
        style={{
          justifyContent: "center",
          color: "#fff",
          marginLeft: "2%",
          background: "#232226",
          marginTop: "30px",
          borderRadius: "50px",
          width: "350px",
        }}
      >
        <img
          style={{
            width: "100%",
            height: "60%",
            float: "left",
            backgroundColor: "#ff1",
            display: "initial",
            margin: "auto",
            overflow: "hidden",
            borderRadius: "50px",
          }}
          src={img1}
          alt="Programação Paralela"
          className="banner-image"
        />
        <div
          style={{
            justifyContent: "center",
            textAlign: "center",
            color: "#ffffff",
            fontWeight: "bold",
            fontSize: '28px',
          }}
        >
          Aprenda de forma fácil
          <div
            style={{
              color: "#efefef",
              fontWeight: "normal",
              fontSize: '18px',
              textAlign: 'justify',
              margin: '15px',
            }}
          >
            Mussum Ipsum, cacilds vidis litro abertis. Posuere libero varius.
            Nullam a nisl ut ante blandit hendrerit. 
          </div>
        </div>
      </div>
      
      {/* Caixa 2 */}


      <div
        style={{
          justifyContent: "center",
          color: "#fff",
          marginLeft: "2%",
          background: "#232226",
          marginTop: "30px",
          borderRadius: "50px",
          width: "350px",
        }}
      >
        <img
          style={{
            width: "100%",
            height: "60%",
            float: "left",
            backgroundColor: "#ff1",
            display: "initial",
            margin: "auto",
            overflow: "hidden",
            borderRadius: "50px",
          }}
          src={img1}
          alt="Programação Paralela"
          className="banner-image"
        />
        <div
          style={{
            justifyContent: "center",
            textAlign: "center",
            color: "#ffffff",
            fontWeight: "bold",
            fontSize: '28px',
          }}
        >
          Aprenda de forma fácil
          <div
            style={{
              color: "#efefef",
              fontWeight: "normal",
              fontSize: '18px',
              textAlign: 'justify',
              margin: '15px',
            }}
          >
            Mussum Ipsum, cacilds vidis litro abertis. Posuere libero varius.
            Nullam a nisl ut ante blandit hendrerit. 
          </div>
        </div>
      </div>
      {/* Caixa 3 */}
      <div
        style={{
          justifyContent: "center",
          color: "#fff",
          marginLeft: "2%",
          background: "#232226",
          marginTop: "30px",
          borderRadius: "50px",
          width: "350px",
        }}
      >
        <img
          style={{
            width: "100%",
            height: "60%",
            float: "left",
            backgroundColor: "#ff1",
            display: "initial",
            margin: "auto",
            overflow: "hidden",
            borderRadius: "50px",
          }}
          src={img1}
          alt="Programação Paralela"
          className="banner-image"
        />
        <div
          style={{
            justifyContent: "center",
            textAlign: "center",
            color: "#ffffff",
            fontWeight: "bold",
            fontSize: '28px',
          }}
        >
          Aprenda de forma fácil
          <div
            style={{
              color: "#efefef",
              fontWeight: "normal",
              fontSize: '18px',
              textAlign: 'justify',
              margin: '15px',
            }}
          >
            Mussum Ipsum, cacilds vidis litro abertis. Posuere libero varius.
            Nullam a nisl ut ante blandit hendrerit. 
          </div>
        </div>
      </div>

    </div>
      <Rodape/>
      </div>
    </div>
  );
};

export default Home;
