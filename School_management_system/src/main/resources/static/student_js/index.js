


const hamburgerBtn = document.getElementById('hamburgerBtn');
const sidebar = document.getElementById('sidebar');
const overlay = document.getElementById('overlay');
hamburgerBtn.addEventListener('click', () => {
    sidebar.classList.toggle('active');
    overlay.classList.toggle('active');
    document.body.classList.add("no-scroll");
    overlay.style.display = "flex";
});

// Close sidebar when clicking outside of it (mobile)
document.addEventListener('click', (e) => {
    if (window.innerWidth <= 768) {
        if (!sidebar.contains(e.target) && !hamburgerBtn.contains(e.target)) {
            sidebar.classList.remove('active');
            overlay.classList.remove('active');
            document.body.classList.remove("no-scroll");
            overlay.style.display = "none";
        }
    }
});

const menuLinks = document.querySelectorAll('.sidebar-menu-link');
menuLinks.forEach(link => {
    link.addEventListener('click', function() {
        menuLinks.forEach(item => item.classList.remove('active'));
        this.classList.add('active');
    });
});