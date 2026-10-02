const today = new Date();
today.setUTCHours(0, 0, 0, 0);

const todayTime = today.getTime();
const nextDayTime = todayTime + 86400000;

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(nextDayTime).toISOString().slice(0, 16);

fetchAndRenderCrawls(todayTime, nextDayTime, "", "");

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formIsUnknown = event.target.isUnknown.value;
    const formUserAgentPattern = event.target.userAgentPattern.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderCrawls(

        range.start,
        range.end,
        isOk(formIsUnknown) ? formIsUnknown : "",
        isOk(formUserAgentPattern) ? formUserAgentPattern : ""
    );
}

function fetchAndRenderCrawls(startTimestamp, endTimestamp, isUnknown, userAgentPattern) {

    document.getElementById("submit-loader").style = "display: inline-block";
    document.getElementById("invocations-count").innerText = "Distinct paths: -";
    document.getElementById("user-agents-count").innerText = "Distinct user agents: -";

    const invocationsTableBody = document.getElementById("invocations-table-hook");
    const userAgentsTableBody = document.getElementById("user-agents-table-hook");

    invocationsTableBody.replaceChildren();
    userAgentsTableBody.replaceChildren();

    fetch("/api/observability/crawl-metrics", {
        
        method: "GET",
        headers: { "Filter": `${startTimestamp}|${endTimestamp}|${isUnknown}|${userAgentPattern}` }

    }).then((response) => {

        if(response.status === 200) {

            response.text().then(text => {

                const lines = text.split('\n');
                const invocations = [];
                const userAgents = [];

                let flag = true;

                for(const line of lines) {

                    if(isOk(line)) {

                        if(flag) invocations.push(line);
                        else userAgents.push(line);
                    }

                    else {

                        flag = false;
                    }
                }

                document.getElementById("invocations-count").innerText = `Distinct paths: ${invocations.length}`;
                document.getElementById("user-agents-count").innerText = `Distinct user agents: ${userAgents.length}`;

                for(const invocation of invocations) {

                    appendRow(invocation, invocationsTableBody, "invocations-table-hook");
                }

                for(const userAgent of userAgents) {

                    appendRow(userAgent, userAgentsTableBody, "user-agents-table-hook");
                }
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

    const isInvocations = hook === "invocations-table-hook";

    count.className = "table-data-elem";
    count.style = "width: 5%; text-align: end";
    count.innerText = isInvocations ? splits[3] : splits[2];

    lastInvocation.className = "table-data-elem";
    lastInvocation.style = "width: 11%; text-align: center";
    lastInvocation.innerText = formatDate(new Date(Number.parseInt(isInvocations ? splits[2] : splits[1])));

    if(isInvocations) {

        if(splits[1] === "true") tr.style = "color: #f1fa8c";
        const httpStatuses = document.createElement("div");

        key.style = "width: 70%";

        httpStatuses.className = "table-data-elem";
        httpStatuses.style = "width: 14%";
        httpStatuses.innerText = splits[4];

        tr.appendChild(httpStatuses);
    }

    else {

        key.style = "width: 84%";
    }

    table.appendChild(tr);
}
