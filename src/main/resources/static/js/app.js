"use strict";
// Progressive enhancement: form tetap berfungsi tanpa JavaScript.
const passwordToggle = document.getElementById("password-toggle");
const passwordInput = document.getElementById("password");
if (passwordToggle && passwordInput) {
  passwordToggle.hidden = false;
  passwordToggle.addEventListener("click", () => {
    const showPassword = passwordInput.type === "password";
    passwordInput.type = showPassword ? "text" : "password";
    passwordToggle.textContent = showPassword ? "Sembunyikan" : "Tampilkan";
    passwordToggle.setAttribute("aria-pressed", String(showPassword));
  });
}
