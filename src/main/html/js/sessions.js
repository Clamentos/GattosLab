fetchAndRenderSessionMetadata();

function fetchAndRenderSessionMetadata() {

    document.getElementById("loader").style = "display: inline-block; width: 85%";
    document.getElementById("session-count").innerText = `Sessions count: -`;

    const tableBody = document.getElementById("session-table-hook");
    tableBody.replaceChildren();

    fetch("/admin/api/observability/sessions-metadata", { method: "GET" }).then((response) => {

        if(response.status === 200) {

            response.json().then(json => {

                document.getElementById("session-count").innerText = `Sessions count: ${json.length}`;
                for(const entry of json) appendRow(entry, tableBody);
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("loader").style = "");
}

function appendRow(entry, table) {

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const role = document.createElement("div");
    const fingerprint = document.createElement("div");
    const createdAt = document.createElement("div");
    const expiresAt = document.createElement("div");

    role.className = "table-data-elem";
    role.style = "width: 25%";
    role.innerText = entry.role;

    fingerprint.className = "table-data-elem";
    fingerprint.style = "width: 25%";
    fingerprint.innerText = entry.fingerprint;

    createdAt.className = "table-data-elem";
    createdAt.style = "width: 25%";
    createdAt.innerText = formatDate(new Date(entry.createdAt));

    expiresAt.className = "table-data-elem";
    expiresAt.style = "width: 25%";
    expiresAt.innerText = formatDate(new Date(entry.expiresAt));

    tr.appendChild(role);
    tr.appendChild(fingerprint);
    tr.appendChild(createdAt);
    tr.appendChild(expiresAt);

    table.appendChild(tr);
}
