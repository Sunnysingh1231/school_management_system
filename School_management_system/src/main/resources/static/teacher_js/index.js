
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

let overly = document.getElementById('asoverlay')
let detail = document.getElementById('modal-box')
let btn = document.getElementById('btn-view')

function viewAssignmentDetail(button){
	overly.classList.add("active")
	detail.classList.add("active")
	
	detail.innerHTML = "";
	
	let h3v = document.createElement('h3');
	h3v.textContent ="Title: "+button.dataset.title;
	
	let p1 = document.createElement('p');
	p1.textContent ="Title: "+button.dataset.message;
	
	let span3 = document.createElement('span');
	span3.textContent ="Due Date: "+button.dataset.dueDate;
	
	let div2 = document.createElement('div');
	div2.classList.add('as');
	div2.style.display = "flex";
	div2.style.justifyContent = "center";
	div2.style.marginTop = "40px";
				
	let btnn = document.createElement('button');
	btnn.classList.add('btn-primary');
	btnn.textContent = 'Ok';
		
	div2.appendChild(btnn);
	detail.append(h3v,p1,span3, div2);
		
	btnn.addEventListener('click', (e) => {
		overly.classList.remove('active')
		detail.classList.remove("active")
	});
				
}
overly.addEventListener('click', (e) => {
	overly.classList.remove('active')
	detail.classList.remove("active")
});

//	< !--delete notice js-- >
function deleteNotice(event) {

    event.preventDefault();

    Swal.fire({
        title: "Delete Notification",
        text: "Do you want to delete this Notice Permanentally ?",
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "Yes, Delete",
        cancelButtonText: "Cancel"
    }).then((result) => {

        if (result.isConfirmed) {
            document.getElementById("deleteNotice").submit();
        }

    });

    return false;

}

// ================= time table =================  -->

const days=["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"];

function generate(){

   // const classes=parseInt(document.getElementById("classCount").value);

    const periods=parseInt(document.getElementById("periodCount").value);
	

    const totalDays=parseInt(document.getElementById("dayCount").value);

    let html="";

    for(let c=1;c<=1;c++){

        html+=`
        <div class="glass-card timetable-card">
		<input type = "hidden" value = ${periods} name = "periods">
            <h3>Class ${c}</h3>

            <div class="table-container">

            <table class="timetable">

            <thead>

            <tr>

                <th>Day</th>

                ${Array.from({length:periods},(_,i)=>`<th>P${i+1}</th>`).join("")}

            </tr>

            </thead>

            <tbody>
        `;
		console.log(periods)
        for(let d=0;d<totalDays;d++){

            html+=`<tr><td>${days[d]}</td>
			<input type = "hidden" value = ${days[d]} name = "days">
			`;
			
            for(let p=1;p<=periods;p++){

                html+=`

                <td>

                    <input class="time-input" type = "time" placeholder="09:00" name = "st">
					<input class="time-input" type = "time" placeholder="09:00" name = "et">
					<br>

                    <input placeholder="Subject" name = "subject">
                </td>

                `;

            }

            html+="</tr>";

        }

        html+=`
            </tbody>

            </table>

            </div>

        </div>
        `;
    }

    document.getElementById("result").innerHTML=html;
}

//	DELETE TIMETABLE PERMANENTALLY----------------------------------------------------------------------------
function deleteTimetable(event) {

	event.preventDefault();

	    Swal.fire({
	        title: "Delete Timetable?",
	        text: "Are you Sure to want Delete this Timetable Permanentally ?",
	        icon: "question",
	        showCancelButton: true,
	        confirmButtonText: "Yes, Delete",
	        cancelButtonText: "Cancel"
	    }).then((result) => {

	        if (result.isConfirmed) {
	            document.getElementById("deleteTimetable").submit();
	        }

	    });

	    return false;
}