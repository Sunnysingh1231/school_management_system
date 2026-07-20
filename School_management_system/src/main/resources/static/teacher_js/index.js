
const toggleBtn = document.getElementById("menuToggle");
const sidebar = document.querySelector(".sidebar");
const overlay = document.getElementById("overlay");
const navLinks = document.querySelectorAll(".nav-item");
const contentArea = document.getElementById("contentArea");

console.log(toggleBtn)
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



//	< !--pay - student js-- >




function updateTotal() {

    let total = 0;

    document.querySelectorAll('.fee-check:checked').forEach(function(checkbox) {

        total += parseFloat(checkbox.dataset.amount);

    });

    document.getElementById('total').textContent = total.toFixed(2);

}

function selectAll() {

    // Get only enabled checkboxes
    const checkboxes = document.querySelectorAll('.fee-check:not(:disabled)');

    // Check if all enabled checkboxes are already selected
    const allSelected = Array.from(checkboxes).every(function(checkbox) {
        return checkbox.checked;
    });

    // Toggle only enabled checkboxes
    checkboxes.forEach(function(checkbox) {
        checkbox.checked = !allSelected;
    });

    updateTotal();

}


//	< !--Fee - student js-- >
function validateFeeForm(event) {

    event.preventDefault();

    const checked = document.querySelectorAll(".fee-check:checked");

    if (checked.length === 0) {

        Swal.fire({
            icon: "warning",
            title: "No Month Selected",
            text: "Please select at least one month."
        });

        return false;
    }
    Swal.fire({
        title: "Confirm Payment",
        text: "Do you want to pay the selected fee?",
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "Yes, Pay",
        cancelButtonText: "Cancel"
    }).then((result) => {

        if (result.isConfirmed) {
            document.getElementById("feeForm").submit();
        }

    });

    return false;

}

//	< !--Fee - student js-- >
function publishNewAssignment(event) {

    event.preventDefault();

    

        
    Swal.fire({
        title: "Publish Assignment",
        text: "Do you want to publish this assignment?",
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "Yes, Publish",
        cancelButtonText: "Cancel"
    }).then((result) => {

        if (result.isConfirmed) {
            document.getElementById("publish").submit();
        }

    });

    return false;

}


//    < !-- ================= PROFILE UPDATE SUCCESS ================= -->

function initForms(event) {

    event.preventDefault();
    const form = document.getElementById("updateStudentForm");

    Swal.fire({
        title: "Student profile updated successfully.",
        icon: "success",
        draggable: true
    }).then(() => {

        form.submit();

    });
}


//        < !-- ================= ATTENDENCE UPDATE SUCCESS ================= -->

function attendenceForm(event) {

    event.preventDefault();

    const form = document.getElementById("updateAttendence");

    Swal.fire({
        title: "Attendance updated successfully.",
        icon: "success",
        draggable: true
    }).then(() => {

        form.submit();

    });
}