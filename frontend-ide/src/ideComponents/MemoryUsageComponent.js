import React, { useState, useEffect } from 'react';
import axios from 'axios';

function MemoryUsageComponent() {
  const [memoryUsage, setMemoryUsage] = useState(null);

  useEffect(() => {
    const intervalId = setInterval(() => {
      axios.get('http://localhost:5000/api/system/memory-usage')
        .then(response => {
          const { total, used } = response.data;
          const usedPercentage = ((used / total) * 100).toFixed(2);
          setMemoryUsage(usedPercentage);
        })
        .catch(error => {
          console.error(`Erro ao buscar o uso de memória: ${error}`);
        });
    }, 10000); // Intervalo de 10 segundos

    return () => clearInterval(intervalId);
  }, []);

  return (
    <div style={{ color: '#fff', fontFamily: 'sans-serif'}}>
      {memoryUsage !== null ? (
        <p> | &nbsp;Memória {memoryUsage}%</p>
      ) : (
        <p>Carregando...</p>
      )}
    </div>
  );
}

export default MemoryUsageComponent;
