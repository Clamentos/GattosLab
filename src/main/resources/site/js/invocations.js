const pathMeta = {id: "invocations-count", text: "Distinct paths:", hook: "invocations-table-hook"};
const userAgentMeta = {id: "user-agents-count", text: "Distinct user agents:", hook: "user-agents-table-hook"};
const timestampMax = 999999999999999;
const today = new Date();

today.setUTCHours(0, 0, 0, 0);
document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);

fetchAndRenderInvocations(today.getTime(), timestampMax, "paths-invocations", pathMeta);
fetchAndRenderInvocations(today.getTime(), timestampMax, "user-agents-count", userAgentMeta);

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;

    const filterStartTimestamp = formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp);
    const filterEndTimestamp = formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp);

    document.getElementById("API-error").innerHTML = "";
    fetchAndRenderInvocations(filterStartTimestamp, filterEndTimestamp, "paths-invocations", pathMeta);
    fetchAndRenderInvocations(filterStartTimestamp, filterEndTimestamp, "user-agents-count", userAgentMeta);
}

function fetchAndRenderInvocations(startTimestamp, endTimestamp, path, meta) {

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

                const keys = Object.keys(json);
                document.getElementById(meta.id).innerText = `${meta.text} ${keys.length}`;

                const processedData = [];

                for(const key of keys) {

                    processedData.push({

                        key: key,
                        count: json[key]
                    });
                }

                processedData.sort((a, b) => a.count < b.count);
                for(const entry of processedData) appendRow(entry, tableBody);
            });
        }

        else {

            showError(response);
        }
    })
    .catch(error_ => showError(error_));
}

function appendRow(entry, table) {

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const key = document.createElement("div");
    const count = document.createElement("div");

    key.className = "table-data-elem";
    key.style = "width: 80%";
    key.innerText = entry.key;

    count.className = "table-data-elem";
    count.style = "text-align: right; width: 20%";
    count.innerText = entry.count;

    tr.appendChild(key);
    tr.appendChild(count);

    table.appendChild(tr);
}

function showError(error) {

    let text;

    if(error.title === "about:custom_error") text = error.title;
    else text = `Error: ${error}`;

    document.getElementById("API-error").innerText = text;
}
