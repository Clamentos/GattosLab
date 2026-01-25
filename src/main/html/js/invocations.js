const pathMeta = {id: "invocations-count", text: "Distinct paths:", hook: "invocations-table-hook"};
const userAgentMeta = {id: "user-agents-count", text: "Distinct user agents:", hook: "user-agents-table-hook"};
const timestampMax = 999999999999999;
const today = new Date();

let inFlightCounter = 0;

today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("submit-loader").style = "display: inline-block";

fetchAndRenderInvocations(today.getTime(), timestampMax, "path-invocations", pathMeta);
fetchAndRenderInvocations(today.getTime(), timestampMax, "user-agents-count", userAgentMeta);

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;

    const filterStartTimestamp = formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp);
    const filterEndTimestamp = formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp);

    document.getElementById("submit-loader").style = "display: inline-block";

    fetchAndRenderInvocations(filterStartTimestamp, filterEndTimestamp, "path-invocations", pathMeta);
    fetchAndRenderInvocations(filterStartTimestamp, filterEndTimestamp, "user-agents-count", userAgentMeta);
}

function fetchAndRenderInvocations(startTimestamp, endTimestamp, path, meta) {

    inFlightCounter++;
    document.getElementById(meta.id).innerText = `${meta.text} -`;

    const tableBody = document.getElementById(meta.hook);
    tableBody.replaceChildren();

    fetch(`/admin/api/observability/${path}`,

        {
            method: "POST",
            headers: new Headers({"content-type": "application/json"}),

            body: JSON.stringify({

                startTimestamp: startTimestamp,
                endTimestamp: endTimestamp
            })
        }
    )
    .then((response) => {

        if(response.status === 200) {

            response.json().then(json => {

                document.getElementById(meta.id).innerText = `${meta.text} ${json.length}`;

                const processedData = json.sort((a, b) => a.count < b.count);
                for(const entry of processedData) appendRow(entry, tableBody, meta.hook);
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => {

        inFlightCounter--;
        if(inFlightCounter === 0) document.getElementById("submit-loader").style = "";
    });
}

function appendRow(entry, table, hook) {

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const key = document.createElement("div");
    const count = document.createElement("div");
    const timestamp = document.createElement("div");

    count.className = "table-data-elem";
    count.style = "width: 10%";
    count.innerText = entry.count;

    timestamp.className = "table-data-elem";
    timestamp.style = "width: 15%";
    timestamp.innerText = formatDate(new Date(entry.timestamp));

    if(hook === "invocations-table-hook") {

        const httpStatuses = document.createElement("div");

        key.className = "table-data-elem";
        key.style = "width: 55%";
        key.innerText = entry.key;

        httpStatuses.className = "table-data-elem";
        httpStatuses.style = "width: 20%";
        httpStatuses.innerText = entry.httpStatuses.join(", ");

        tr.appendChild(key);
        tr.appendChild(count);
        tr.appendChild(timestamp);
        tr.appendChild(httpStatuses);
    }

    else {

        key.className = "table-data-elem";
        key.style = "width: 75%";
        key.innerText = entry.key;

        tr.appendChild(key);
        tr.appendChild(count);
        tr.appendChild(timestamp);
    }

    table.appendChild(tr);
}
