// src/pages/LoginForm.js

import React, { useState } from "react";
import { getDatabase, ref, get } from "firebase/database";
import { ToastContainer, toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import "react-toastify/dist/ReactToastify.css";

// Firebase initialization assumed already done in the main app.

const database = getDatabase();

const LoginForm = () => {
      const [email, setEmail] = useState("");
      const [password, setPassword] = useState("");
      const navigate = useNavigate();

      const handleLogin = async (e) => {
            e.preventDefault();
            try {
                  const usersRef = ref(database, "Users");
                  const snapshot = await get(usersRef);

                  if (!snapshot.exists()) {
                        toast.error("No users found.");
                        return;
                  }

                  const usersData = snapshot.val();
                  let foundUser = false;

                  for (const username in usersData) {
                        const user = usersData[username];
                        if (user.emailPeer === email) {
                              foundUser = true;
                              if (user.password === password) {
                                    localStorage.setItem("userKey", username);
                                    toast.success("Login successful!");

                                    setTimeout(() => {
                                          navigate("/dashboard");
                                          window.location.reload();
                                    }, 1000);

                              } else {
                                    toast.error("Incorrect password.");
                              }
                              break;
                        }
                  }

                  if (!foundUser) {
                        toast.error("Email not found.");
                  }
            } catch (error) {
                  console.error(error);
                  toast.error("Login failed.");
            }
      };

      return (
            <div style={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  minHeight: "100vh",
                  background: "linear-gradient(to bottom right, #93C5FD, #60A5FA)"
            }}>
                  <ToastContainer />
                  <div style={{
                        backgroundColor: "#FFFFFF",
                        padding: "32px",
                        borderRadius: "16px",
                        boxShadow: "0 4px 10px rgba(0, 0, 0, 0.1)",
                        width: "100%",
                        maxWidth: "400px"
                  }}>
                        <h1 style={{
                              fontSize: "28px",
                              fontWeight: "600",
                              color: "#3B82F6",
                              textAlign: "center",
                              marginBottom: "24px"
                        }}>
                              Elder Login
                        </h1>
                        <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                              <input
                                    type="email"
                                    placeholder="Email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    style={{
                                          padding: "12px",
                                          border: "1px solid #D1D5DB",
                                          borderRadius: "8px",
                                          outline: "none",
                                          fontSize: "16px",
                                          color: "#1F2937"
                                    }}
                              />
                              <input
                                    type="password"
                                    placeholder="Password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    style={{
                                          padding: "12px",
                                          border: "1px solid #D1D5DB",
                                          borderRadius: "8px",
                                          outline: "none",
                                          fontSize: "16px",
                                          color: "#1F2937"
                                    }}
                              />
                              <button
                                    type="submit"
                                    style={{
                                          padding: "12px",
                                          backgroundColor: "#3B82F6",
                                          color: "#FFFFFF",
                                          fontWeight: "600",
                                          borderRadius: "8px",
                                          cursor: "pointer",
                                          transition: "background-color 0.3s ease"
                                    }}
                                    onMouseOver={(e) => e.target.style.backgroundColor = "#2563EB"}
                                    onMouseOut={(e) => e.target.style.backgroundColor = "#3B82F6"}
                              >
                                    Login
                              </button>
                        </form>
                  </div>
            </div>
      );
};

export default LoginForm;
