fetchAndRenderSessionMetadata();

function fetchAndRenderSessionMetadata() {

    document.getElementById("loader").style = "display: inline-block; width: 85%";
    document.getElementById("session-count").innerText = `Sessions count: -`;

    const tableBody = document.getElementById("session-table-hook");
    tableBody.replaceChildren();

    fetch("/api/authentication/sessions", { method: "GET" }).then((response) => {

        if(response.status === 200) {

            response.text().then(text => {

                const lines = text.split('\n');

                document.getElementById("session-count").innerText = `Sessions count: ${lines.length}`;
                for(const entry of lines) appendRow(entry, tableBody);
            });
        }

        else {

            response.text().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("loader").style = "");
}

function appendRow(entry, table) {

    /*fingerprint|expiresAt*/
    const splits = entry.split('|');

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const fingerprint = document.createElement("div");
    const expiresAt = document.createElement("div");

    fingerprint.className = "table-data-elem";
    fingerprint.style = "width: 75%";
    fingerprint.innerText = splits[0];

    expiresAt.className = "table-data-elem";
    expiresAt.style = "width: 25%; text-align: center";
    expiresAt.innerText = formatDate(new Date(Number.parseInt(splits[1])));

    tr.appendChild(fingerprint);
    tr.appendChild(expiresAt);

    table.appendChild(tr);
}
