async function addRecord() {
  const name = document.getElementById("name").value;
  const age = document.getElementById("age").value;
  const gender = document.getElementById("gender").value;
  const disease = document.getElementById("disease").value;

  await fetch("http://localhost:8080/add", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `name=${name}&age=${age}&gender=${gender}&disease=${disease}`
  });

  alert("Record added!");
  loadRecords();
}

async function loadRecords() {
  const res = await fetch("http://localhost:8080/records");
  const data = await res.json();
  const div = document.getElementById("records");
  div.innerHTML = "";
  if (!Array.isArray(data) || data.length === 0) {
    div.innerHTML = "<p>No Records Found.</p>";
    return;
  }
  data.forEach(r => {
    div.innerHTML += `
      <div class='record'>
        <b>${r.name}</b> (${r.age} yrs, ${r.gender}) - ${r.disease}
        <button class='delete-btn' onclick='deleteRecord(${r.id})'>Delete</button>
      </div>`;
  });
}

async function deleteRecord(id) {
  await fetch("http://localhost:8080/delete", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `id=${id}`
  });
  alert("Record deleted!");
  loadRecords();
}

// Optionally load records on page open
document.addEventListener('DOMContentLoaded', () => {
  // nothing automatic for now; user can click "Load Records"
});
