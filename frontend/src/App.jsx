import React, { useState } from "react";

function App() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("...");

  const callPublicApi = async () => {
    setMessage("Fetching data...");
    const response = await fetch("http://localhost:8080/api/public");

    const data = await response.text();
    setMessage(data);
  };

  const callProfileApi = async () => {
    setMessage("Fetching data...");

    const credentials = btoa(`${username}:${password}`);

    const response = await fetch("http://localhost:8080/api/profile", {
      headers: {
        Authorization: `Basic ${credentials}`,
      },
      credentials: "include",
    });

    if (!response.ok) {
      setMessage(`Request failed: ${response.status}`);
      return;
    }

    const data = response.text();

    setMessage(data);
  };

  const login = async () => {
    setMessage("Fetching data...");

    const response = await fetch("http://localhost:8080/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({
        username: username,
        password: password,
      }),
    });

    const data = await response.text();

    setMessage(data);
  };

  const logout = async () => {
    setMessage("Fetching data...");
    const response = await fetch("http://localhost:8080/auth/logout", {
      method: "POST",
      credentials: "include",
    });

    const data = await response.text();

    setMessage(data);
  };

  return (
    <div className="out">
      <h1>Authentication Checking</h1>

      <div>
        <input
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <br />
        <input
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>

      <br />
      <br />

      <button type="button" onClick={callPublicApi}>
        Call public API
      </button>
      <span> | </span>
      <button type="button" onClick={login}>
        Login
      </button>
      <span> | </span>
      <button type="button" onClick={callProfileApi}>
        Call profile API
      </button>
      <span> | </span>
      <button type="button" onClick={logout}>
        Logout
      </button>

      <p>{message}</p>
    </div>
  );
}

export default App;
