const timestampMax = 999999999999999;
const today = new Date();

today.setUTCHours(0, 0, 0, 0);
document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);

fetchAndRenderInvocations(today.getTime(), timestampMax);

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formOnlyOthers = event.target.onlyOthers.value;
    const formPathPattern = event.target.pathPattern.value;
    const formHttpStatuses = event.target.httpStatuses.value;
    const formUserAgentPattern = event.target.userAgentPattern.value;

    fetchAndRenderInvocations(

        formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp),
        formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp),
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

    fetch("/admin/api/observability/request-metrics",

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

                const processedInvocations = processPaths(json);
                const processedUserAgents = processUserAgents(json);

                document.getElementById("invocations-count").innerText = `Distinct paths: ${processedInvocations.length}`;
                document.getElementById("user-agents-count").innerText = `Distinct user agents: ${processedUserAgents.length}`;

                for(const invocation of processedInvocations) appendRow(invocation, invocationsTableBody, "invocations-table-hook");
                for(const userAgent of processedUserAgents) appendRow(userAgent, userAgentsTableBody, "user-agents-table-hook");
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}

function processPaths(json) {

    /* path -> processed invocation obj */
    const processedInvocations = new Map();

    for(const metric of json) {

        if(processedInvocations.has(metric.path)) {

            const elem = processedInvocations.get(metric.path);

            elem.count++;
            elem.httpStatuses.add(elem.httpStatus);

            if(elem.timestamp < metric.timestamp) elem.timestamp = metric.timestamp;
        }

        else {

            processedInvocations.set(metric.path, {

                key: metric.path,
                isOthers: metric.isOthers,
                count: 1,
                timestamp: metric.timestamp,
                httpStatuses: new Set([metric.httpStatus])
            });
        }
    }

    let sortedInvocations = [];
    for(const elem of processedInvocations.values()) sortedInvocations.push(elem);

    return sortedInvocations.sort((a, b) => a.count < b.count);
}

function processUserAgents(json) {

    /* user agent -> processed user agent obj */
    const processedUserAgents = new Map();

    for(const metric of json) {

        if(processedUserAgents.has(metric.userAgent)) {

            const elem = processedUserAgents.get(metric.userAgent);

            elem.count++;
            if(elem.timestamp < metric.timestamp) elem.timestamp = metric.timestamp;
        }

        else {

            processedUserAgents.set(metric.userAgent, {

                key: metric.userAgent,
                count: 1,
                timestamp: metric.timestamp
            });
        }
    }

    let sortedUserAgents = [];
    for(const elem of processedUserAgents.values()) sortedUserAgents.push(elem);

    return sortedUserAgents.sort((a, b) => a.count < b.count);
}

function appendRow(entry, table, hook) {

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const key = document.createElement("div");
    const count = document.createElement("div");
    const timestamp = document.createElement("div");

    tr.appendChild(key);
    tr.appendChild(count);
    tr.appendChild(timestamp);

    key.className = "table-data-elem";
    key.innerText = (entry.key === null || entry.key === undefined) ? "" : entry.key;

    count.className = "table-data-elem";
    count.style = "width: 10%";
    count.innerText = entry.count;

    timestamp.className = "table-data-elem";
    timestamp.style = "width: 15%";
    timestamp.innerText = formatDate(new Date(entry.timestamp));

    if(hook === "invocations-table-hook") {

        if(!entry.isOthers) tr.style = "color: #00FF00";
        const httpStatuses = document.createElement("div");

        key.style = "width: 55%";

        httpStatuses.className = "table-data-elem";
        httpStatuses.style = "width: 20%";
        httpStatuses.innerText = Array.from(entry.httpStatuses).join(", ");

        tr.appendChild(httpStatuses);
    }

    else {

        key.style = "width: 75%";
    }

    table.appendChild(tr);
}
