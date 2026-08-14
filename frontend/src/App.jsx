import React, { useState } from "react";

function App() {
  const [message, setMessage] = useState("...");

  const callPublicApi = async () => {
    setMessage("Fetching data...")
    const response = await fetch("http://localhost:8080/api/public");

    const data = await response.text();
    setMessage(data);
  };

  return (
    <div>
      <h1>Authentication Checking</h1>
      <button type="button" onClick={callPublicApi}>
        Call public API
      </button>
      <p>{message}</p>
    </div>
  );
}

export default App;
