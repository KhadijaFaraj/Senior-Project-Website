// mode.js
fetch("navbar.html")
  .then(r => r.text())
  .then(d => {
    document.getElementById("navbar").innerHTML = d;

    // Now the navbar exists
    const toggle = document.getElementById("darkModeToggle");
    const html = document.documentElement;

    // Apply saved theme if it exists
    const savedTheme = localStorage.getItem("theme");
    if (savedTheme === "dark") {
      html.setAttribute("data-theme", "dark");
      toggle.textContent = "☀️ Light Mode";
    } else {
      toggle.textContent = "🌙 Dark Mode";
    }

    // Attach click listener
    toggle.addEventListener("click", () => {
      if (html.getAttribute("data-theme") === "dark") {
        html.removeAttribute("data-theme");
        localStorage.setItem("theme", "light");
        toggle.textContent = "🌙 Dark Mode";
      } else {
        html.setAttribute("data-theme", "dark");
        localStorage.setItem("theme", "dark");
        toggle.textContent = "☀️ Light Mode";
      }
    });
  });