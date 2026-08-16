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
    let token = localStorage.getItem("accessToken");

    let response = await fetch("http://localhost:8080/api/profile", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.status === 401) {
      token = await refreshAccessToken();

      if (!token) {
        setMessage("Please login again");
        return;
      }

      response = await fetch("http://localhost:8080/api/profile", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
    }

    if (!response.ok) {
      setMessage(`Request failed: ${response.status}`);
      return;
    }

    const data = await response.text();

    setMessage(data);
  };

  const callAdminApi = async () => {
    setMessage("Fetching data...");

    const token = localStorage.getItem("accessToken");

    const response = await fetch("http://localhost:8080/api/admin", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      setMessage(`Request failed: ${response.status}`);
      return;
    }

    const data = response.text();

    setMessage(data);
  };

  const getCsrfToken = async () => {
    const response = await fetch("http://localhost:8080/auth/csrf", {
      credentials: "include",
    });

    const data = await response.json();

    return data.token;
  };

  const register = async () => {
    setMessage("Fetching data...");

    // const csrfToken = await getCsrfToken();

    const response = await fetch("http://localhost:8080/auth/register", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        // "X-XSRF-TOKEN": csrfToken,
      },
      // credentials: "include",
      body: JSON.stringify({
        username: username,
        password: password,
      }),
    });
    console.log(response);
    const data = await response.text();

    setMessage(data);
  };

  const login = async () => {
    setMessage("Fetching data...");

    // const csrfToken = await getCsrfToken();

    const response = await fetch("http://localhost:8080/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        // "X-XSRF-TOKEN": csrfToken,
      },
      credentials: "include",
      body: JSON.stringify({
        username: username,
        password: password,
      }),
    });

    if (!response.ok) {
      setMessage("Login failed");
      return;
    }
    const data = await response.json();
    console.log(data);

    localStorage.setItem("accessToken", data.accessToken);

    setMessage("Login successful");
  };

  const logout = async () => {
    setMessage("Fetching data...");

    // const csrfToken = await getCsrfToken();

    const response = await fetch("http://localhost:8080/auth/logout", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        // "X-XSRF-TOKEN": csrfToken,
      },
    });

    if (response.ok) {
      localStorage.removeItem("accessToken");
      setMessage("Logged out");
    } else {
      setMessage(`Logout failed: ${response.status}`);
    }
  };

  const updateProfile = async () => {
    setMessage("Fetching data...");

    const csrfToken = await getCsrfToken();

    const response = await fetch("http://localhost:8080/api/profile/update", {
      method: "POST",
      credentials: "include",
      headers: {
        "X-XSRF-Token": csrfToken,
      },
    });

    console.log("Logout status:", response.status);

    if (response.ok) {
      setMessage("Logged out");
    } else {
      setMessage(`Logout failed: ${response.status}`);
    }
  };

  const refreshAccessToken = async () => {
    const response = await fetch("http://localhost:8080/auth/refresh", {
      method: "POST",
      credentials: "include",
    });

    if (!response.ok) {
      localStorage.removeItem("accessToken");
      return null;
    }

    const data = await response.json();

    localStorage.setItem("accessToken", data.accessToken);

    return data.accessToken;
  };

  const loginWithGoogle = () => {
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
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
      <button type="button" onClick={register}>
        Register
      </button>
      <span> | </span>
      <button type="button" onClick={loginWithGoogle}>
        Continue with Google
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
      <button type="button" onClick={updateProfile}>
        Update profile API
      </button>
      <span> | </span>
      <button type="button" onClick={callAdminApi}>
        Call Admin API
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
