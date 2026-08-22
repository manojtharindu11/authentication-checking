import { useState } from "react";
import { useSearchParams } from "react-router-dom";

function ResetPassword() {
  const [searchParams] = useSearchParams();

  const token = searchParams.get("token");

  const [password, setPassword] = useState("manoj456");
  const [message, setMessage] = useState("...");

  const resetPassword = async () => {
    const response = await fetch("http://localhost:8080/auth/reset-password", {
      method: "POST",

      headers: {
        "Content-Type": "application/json",
      },

      body: JSON.stringify({
        token,
        newPassword: password,
      }),
    });

    const text = await response.text();

    setMessage(text);
  };

  return (
    <div className="out">
      <h2>Reset Password</h2>

      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="New password"
      />

      <br/>

      <button type="button" onClick={resetPassword}>Reset Password</button>

      <p>{message}</p>
    </div>
  );
}

export default ResetPassword;
