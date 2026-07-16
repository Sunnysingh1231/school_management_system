
const toggleBtn = document.getElementById("menuToggle");
const sidebar = document.querySelector(".sidebar");
const overlay = document.getElementById("overlay");
const navLinks = document.querySelectorAll(".nav-item");
const contentArea = document.getElementById("contentArea");

// Sidebar toggle
toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("active");
    overlay.classList.toggle("active");
});

// Overlay click
overlay.addEventListener("click", () => {
    sidebar.classList.remove("active");
    overlay.classList.remove("active");
});

// Active menu + close sidebar
navLinks.forEach(link => {
    link.addEventListener("click", () => {
        navLinks.forEach(item => item.classList.remove("active"));
        link.classList.add("active");

        sidebar.classList.remove("active");
        overlay.classList.remove("active");
    });
});



// ================= LOAD PAGE =================  -->
function loadPage(event, url) {
    event.preventDefault();

    contentArea.innerHTML = "Loading...";

    fetch(url, {
        headers: { "X-Requested-With": "XMLHttpRequest" }
    })
        .then(res => res.text())
        .then(html => {
            contentArea.innerHTML = html;
            history.pushState(null, "", url);
        });
}

// ================= BACK BUTTON =================  -->
window.onpopstate = function() {
    contentArea.innerHTML = "Loading...";

    fetch(location.pathname, {
        headers: { "X-Requested-With": "XMLHttpRequest" }
    })
        .then(res => res.text())
        .then(html => {
            contentArea.innerHTML = html;
        });
};