
document.addEventListener("click", function (event) {
    const logoutBtn = event.target.closest("#logoutBtn");

    if (!logoutBtn) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();

    const logoutForm = document.getElementById("logoutForm");

    if (!logoutForm) {
        console.error("Logout form not found!");
        return;
    }

    Swal.fire({
        title: "Are you sure?",
        text: "Do you really want to logout?",
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#d33",
        cancelButtonColor: "#3085d6",
        confirmButtonText: "Yes, Logout",
        cancelButtonText: "Cancel"
    }).then(function (result) {
        if (result.isConfirmed) {
            logoutForm.submit();
        }
    });
});


