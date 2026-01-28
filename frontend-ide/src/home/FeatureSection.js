import React from "react";
import img1 from "../img/img1.jpeg";
import { Link } from "react-router-dom";
const FeatureSection = () => {

  return (
    <div>
    <div style={{ justifyContent: 'center', fontSize: '60px', marginLeft: "20%", marginTop: "2%", color: '#ffffff', }}> 
      Utilize Frameworks Paralelos
     </div> 
    <div style={{ display: "flex", marginLeft: "5%", marginRight: "5%", justifyContent: 'center'}}>
      <button
        style={{
          justifyContent: "center",
          color: "#fff",
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
            marginTop: '-55px',
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
              fontWeight: "normal",
              fontSize: '18px',
              textAlign: 'justify',
              margin: '15px',
            }}
          >
            <Link to="/criar-projeto" style={{ textDecoration: 'none', color: '#efefef'}}>
            Mussum Ipsum, cacilds vidis litro abertis. Posuere libero varius.
            Nullam a nisl ut ante blandit hendrerit. </Link>
          </div>
        </div>
      </button>


      {/* <div
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
      </div> */}

    </div>
  </div>
  );
};

export default FeatureSection;