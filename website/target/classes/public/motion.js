document.addEventListener("DOMContentLoaded", () => {
  const elements = document.querySelectorAll(".section, .card, .download-card, .stats, .form-section");

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
      }
    });
  }, {
    threshold: 0.12
  });

  elements.forEach(el => {
    el.classList.add("reveal");
    observer.observe(el);
  });
});