"use strict";

document.addEventListener("DOMContentLoaded", () => {
    document.documentElement.dataset.js = "enabled";

    const form = document.querySelector("#registration-form");
    const message = document.querySelector("#registration-message");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        message.textContent = "Registering...";

        try {
            const response = await fetch("/api/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams(new FormData(form))
            });

            const responseMessage = await response.text();
            message.textContent = responseMessage;

            if (response.ok) {
                form.reset();
            }
        } catch (error) {
            message.textContent = "The registration request could not be sent.";
        }
    });
});