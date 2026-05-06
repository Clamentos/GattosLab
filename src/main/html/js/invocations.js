const today = new Date();
today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(today.getTime() + 86400000).toISOString().slice(0, 16);

fetchAndRenderInvocations(today.getTime(), today.getTime() + 86400000);

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formOnlyOthers = event.target.onlyOthers.value;
    const formPathPattern = event.target.pathPattern.value;
    const formHttpStatuses = event.target.httpStatuses.value;
    const formUserAgentPattern = event.target.userAgentPattern.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderInvocations(

        range.start,
        range.end,
        formOnlyOthers === "" ? null : formOnlyOthers === "true",
        formPathPattern === "" ? null : formPathPattern,
        formHttpStatuses === "" ? null : String(formHttpStatuses).split(",").map(s => Number.parseInt(s)),
        formUserAgentPattern === "" ? null : formUserAgentPattern
    );
}

function fetchAndRenderInvocations(startTimestamp, endTimestamp, onlyOthers, pathPattern, httpStatuses, userAgentPattern) {

    document.getElementById("submit-loader").style = "display: inline-block";
    document.getElementById("invocations-count").innerText = "Distinct paths: -";
    document.getElementById("user-agents-count").innerText = "Distinct user agents: -";

    const invocationsTableBody = document.getElementById("invocations-table-hook");
    const userAgentsTableBody = document.getElementById("user-agents-table-hook");

    invocationsTableBody.replaceChildren();
    userAgentsTableBody.replaceChildren();

    fetch("/admin/api/observability/invocation-metrics",

        {
            method: "POST",
            headers: new Headers({"content-type": "application/json"}),

            body: JSON.stringify({

                startTimestamp: startTimestamp,
                endTimestamp: endTimestamp,
                onlyOthers: onlyOthers,
                pathPattern: pathPattern,
                httpStatuses: httpStatuses,
                userAgentPattern: userAgentPattern
            })
        }
    )
    .then((response) => {

        if(response.status === 200) {

            response.json().then(json => {

                const invocations = json.paths;
                const userAgents = json.userAgents;

                document.getElementById("invocations-count").innerText = `Distinct paths: ${invocations.length}`;
                document.getElementById("user-agents-count").innerText = `Distinct user agents: ${userAgents.length}`;

                for(const invocation of invocations) appendRow(invocation, invocationsTableBody, "invocations-table-hook");
                for(const userAgent of userAgents) appendRow(userAgent, userAgentsTableBody, "user-agents-table-hook");
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}

function appendRow(entry, table, hook) {

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const key = document.createElement("div");
    const count = document.createElement("div");
    const firstInvocation = document.createElement("div");
    const lastInvocation = document.createElement("div");

    tr.appendChild(key);
    tr.appendChild(count);
    tr.appendChild(firstInvocation);
    tr.appendChild(lastInvocation);

    key.className = "table-data-elem";
    key.innerText = hook === "invocations-table-hook" ? entry.path : entry.userAgent;

    count.className = "table-data-elem";
    count.style = "width: 5%; text-align: end";
    count.innerText = entry.count;

    firstInvocation.className = "table-data-elem";
    firstInvocation.style = "width: 10%; text-align: center";
    firstInvocation.innerText = formatDate(new Date(entry.firstInvocation));

    lastInvocation.className = "table-data-elem";
    lastInvocation.style = "width: 10%; text-align: center";
    lastInvocation.innerText = formatDate(new Date(entry.lastInvocation));

    if(hook === "invocations-table-hook") {

        if(entry.isOthers !== true) tr.style = "color: #00FF00";
        const httpStatuses = document.createElement("div");

        key.style = "width: 60%";

        httpStatuses.className = "table-data-elem";
        httpStatuses.style = "width: 15%";
        httpStatuses.innerText = Array.from(entry.httpStatuses).join(", ");

        tr.appendChild(httpStatuses);
    }

    else {

        key.style = "width: 75%";
    }

    table.appendChild(tr);
}
