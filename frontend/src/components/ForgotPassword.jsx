import React, { useState } from "react";

function ForgotPassword() {
  const [email, setEmail] = useState("manojtharindu11@gmail.com");
  const [message, setMessage] = useState("...");

  const submit = async () => {
    setMessage("Sending...");
    const response = await fetch("http://localhost:8080/auth/forgot-password", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email: email }),
    });

    const text = response.text();
    setMessage(text);
  };

  return (
    <div className="out">
      <h1>Forgot Password</h1>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email"
      />

      <br/>

      <button type="button" onClick={submit}>Send reset link</button>

      <p>{message}</p>
    </div>
  );
}

export default ForgotPassword;
