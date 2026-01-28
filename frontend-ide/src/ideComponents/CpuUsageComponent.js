import React, { useState, useEffect } from 'react';
import axios from 'axios';

function CpuUsageComponent() {
  const [numCores, setNumCores] = useState(null);

  useEffect(() => {
    axios.get('http://localhost:5000/api/system/cpu-usage')
      .then(response => {
        const numberOfCores = response.data.length;
        setNumCores(numberOfCores);
      })
      .catch(error => {
        console.error(`Erro ao buscar o número de núcleos da CPU: ${error}`);
      });
  }, []);

  return (
    <div style={{ color: '#fff', fontFamily: 'sans-serif'  }}>
      {numCores !== null ? (
        <p>&nbsp;&nbsp;Núcleos: {numCores}&nbsp;</p>
      ) : (
        <p>Carregando...</p>
      )}
    </div>
  );
}

export default CpuUsageComponent;
