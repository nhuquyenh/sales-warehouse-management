const passwordInput = document.querySelector("#password");
const passwordToggle = document.querySelector("#password-toggle");

if (passwordInput && passwordToggle) {
    passwordToggle.addEventListener("click", () => {
        const shouldShow = passwordInput.type === "password";
        passwordInput.type = shouldShow ? "text" : "password";
        passwordToggle.textContent = shouldShow ? "Ẩn" : "Hiện";
        passwordToggle.setAttribute("aria-pressed", String(shouldShow));
        passwordInput.focus();
    });
}
