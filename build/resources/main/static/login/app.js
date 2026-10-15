"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#login-form");
    const message = document.querySelector("#login-message");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        message.textContent = "Signing in...";

        const formData = new FormData(form);

        try {
            const response = await fetch("/api/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    email: formData.get("email"),
                    password: formData.get("password")
                })
            });

            if (response.ok) {
                message.textContent = "Login successful";
                return;
            }

            const responseMessage = await response.text();
            message.textContent =
                responseMessage || "Invalid email or password";
        } catch (error) {
            message.textContent = "The login request could not be sent.";
        }
    });
});