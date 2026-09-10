const today = new Date();
today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(today.getTime() + 86400000).toISOString().slice(0, 16);

fetchAndRenderInvocations(today.getTime(), today.getTime() + 86400000, "", "");

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formisUnknown = event.target.isUnknown.value;
    const formUserAgentPattern = event.target.userAgentPattern.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderInvocations(

        range.start,
        range.end,
        isOk(formisUnknown) ? formisUnknown : "",
        isOk(formUserAgentPattern) ? formUserAgentPattern : ""
    );
}

function fetchAndRenderInvocations(startTimestamp, endTimestamp, isUnknown, userAgentPattern) {

    document.getElementById("submit-loader").style = "display: inline-block";
    document.getElementById("invocations-count").innerText = "Distinct paths: -";
    document.getElementById("user-agents-count").innerText = "Distinct user agents: -";

    const invocationsTableBody = document.getElementById("invocations-table-hook");
    const userAgentsTableBody = document.getElementById("user-agents-table-hook");

    invocationsTableBody.replaceChildren();
    userAgentsTableBody.replaceChildren();

    const filter = `${startTimestamp}|${endTimestamp}|${isUnknown}|${userAgentPattern}`;

    fetch(`/api/observability/crawl-metrics?filter=${encodeURI(filter)}`,

        {
            method: "GET",
            headers: new Headers({"content-type": "application/json"})
        }
    )
    .then((response) => {

        if(response.status === 200) {

            response.text().then(text => {

                const lines = text.split('\n');
                const invocations = [];
                const userAgents = [];
                let flag = true;

                for(const line of lines) {

                    if(line !== "") {

                        if(flag) invocations.push(line);
                        else userAgents.push(line);
                    }

                    else {

                        flag = false;
                    }
                }

                document.getElementById("invocations-count").innerText = `Distinct paths: ${invocations.length}`;
                document.getElementById("user-agents-count").innerText = `Distinct user agents: ${userAgents.length}`;

                for(const invocation of invocations) appendRow(invocation, invocationsTableBody, "invocations-table-hook");
                for(const userAgent of userAgents) appendRow(userAgent, userAgentsTableBody, "user-agents-table-hook");
            });
        }

        else {

            response.text().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}

function appendRow(entry, table, hook) {

    /*path|isUnknown|lastCalled|numberOfCalls|statuses*/
    /*userAgent|lastSeen|numberOfCalls*/
    const splits = entry.split('|');

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    const key = document.createElement("div");
    const count = document.createElement("div");
    const lastInvocation = document.createElement("div");

    tr.appendChild(key);
    tr.appendChild(count);
    tr.appendChild(lastInvocation);

    key.className = "table-data-elem";
    key.innerText = splits[0];

    count.className = "table-data-elem";
    count.style = "width: 5%; text-align: end";
    count.innerText = hook === "invocations-table-hook" ? splits[3] : splits[2];

    lastInvocation.className = "table-data-elem";
    lastInvocation.style = "width: 10%; text-align: center";
    lastInvocation.innerText = formatDate(new Date(Number.parseInt(hook === "invocations-table-hook" ? splits[2] : splits[1])));

    if(hook === "invocations-table-hook") {

        if(splits[1] === "true") tr.style = "color: #FFFF00";
        const httpStatuses = document.createElement("div");

        key.style = "width: 70%";

        httpStatuses.className = "table-data-elem";
        httpStatuses.style = "width: 15%";
        httpStatuses.innerText = splits[4];

        tr.appendChild(httpStatuses);
    }

    else {

        key.style = "width: 85%";
    }

    table.appendChild(tr);
}
