const colorTable = new Map([

    ["599", 1],
    ["999", 1],

    ["400", 2],
    ["401", 2],
    ["403", 2],
    ["404", 2],
    ["405", 2],
    ["505", 2],

    ["503", 3],
    ["413", 3],
    ["429", 3],

    ["422", 4],
    ["500", 4]
]);

const today = new Date();
today.setUTCHours(0, 0, 0, 0);

const todayTime = today.getTime();
const nextDayTime = todayTime + 86400000;

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(nextDayTime).toISOString().slice(0, 16);

fetchAndRenderCrawls(todayTime, nextDayTime, "", "", "", "");

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formIsUnknown = event.target.isUnknown.value;
    const formUserAgentPattern = event.target.userAgentPattern.value;
    const formPathPattern = event.target.pathPattern.value;
    const formRemoteIpAddressPattern = event.target.remoteIpAddressPattern.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderCrawls(

        range.start,
        range.end,
        isOk(formIsUnknown) ? formIsUnknown : "",
        isOk(formUserAgentPattern) ? formUserAgentPattern : "",
        isOk(formPathPattern) ? formPathPattern : "",
        isOk(formRemoteIpAddressPattern) ? formRemoteIpAddressPattern : ""
    );
}

function fetchAndRenderCrawls(startTimestamp, endTimestamp, isUnknown, userAgentPattern, pathPattern, remoteIpAddressPattern) {

    document.getElementById("submit-loader").style = "display: inline-block";
    document.getElementById("invocations-count").innerText = "Distinct paths: -";
    document.getElementById("user-agents-count").innerText = "Distinct user agents: -";
    document.getElementById("remote-ips-count").innerText = "Distinct remote IP addresses: -";

    const invocationsTableBody = document.getElementById("invocations-table-hook");
    const userAgentsTableBody = document.getElementById("user-agents-table-hook");
    const remoteIpAddressesTableBody = document.getElementById("remote-ips-table-hook");

    invocationsTableBody.replaceChildren();
    userAgentsTableBody.replaceChildren();
    remoteIpAddressesTableBody.replaceChildren();

    const filter =`${startTimestamp}|${endTimestamp}|${isUnknown}|${userAgentPattern}|${pathPattern}|${remoteIpAddressPattern}`;

    fetch("/api/observability/crawl-metrics", {

        method: "GET",
        headers: { "Filter": filter }

    }).then((response) => {

        if(response.status === 200) {

            response.text().then(text => {

                const lines = text.split('\n');
                const invocations = [];
                const userAgents = [];
                const remoteIpAddresses = [];

                let section = 0;

                for(const line of lines) {

                    if(isOk(line)) {

                        switch(section) {

                            case 0: invocations.push(line); break;
                            case 1: userAgents.push(line); break;
                            case 2: remoteIpAddresses.push(line); break;
                        }
                    }

                    else {

                        section++;
                    }
                }

                document.getElementById("invocations-count").innerText = `Distinct paths: ${invocations.length}`;
                document.getElementById("user-agents-count").innerText = `Distinct user agents: ${userAgents.length}`;
                document.getElementById("remote-ips-count").innerText = `Distinct remote IP addresses: ${remoteIpAddresses.length}`;

                for(const invocation of invocations) {

                    appendRow(invocation, invocationsTableBody, "invocations-table-hook");
                }

                for(const userAgent of userAgents) {

                    appendRow(userAgent, userAgentsTableBody, "user-agents-table-hook");
                }

                for(const remoteIpAddress of remoteIpAddresses) {

                    appendRow(remoteIpAddress, remoteIpAddressesTableBody, "remote-ips-table-hook");
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
    const key = document.createElement("div");
    const count = document.createElement("div");
    const lastInvocation = document.createElement("div");

    tr.appendChild(key);
    tr.appendChild(count);
    tr.appendChild(lastInvocation);

    tr.className = "table-data-row";

    key.className = "table-data-elem";
    key.innerText = cleanObservabilityString(splits[0]);

    count.className = "table-data-elem";
    count.style = "width: 5%; text-align: end";
    count.innerText = splits[1];

    lastInvocation.className = "table-data-elem";
    lastInvocation.style = "width: 11%; text-align: center";
    lastInvocation.innerText = formatDate(new Date(Number.parseInt(splits[2])));

    if(hook === "invocations-table-hook") {

        key.style = "width: 70%";

        const httpStatuses = document.createElement("div");
        httpStatuses.className = "table-data-elem";
        httpStatuses.style = "width: 14%";
        httpStatuses.innerText = splits[3];

        tr.appendChild(httpStatuses);

        const httpStatusesArray = splits[3].split(',');
        let worst = 0;

        for(const httpStatus of httpStatusesArray) {

            const color = colorTable.get(httpStatus);

            if(color !== null && color !== undefined && color > worst) {

                worst = color;
            }
        }

        switch(worst) {

            case 1: tr.style = "color: #8be9fd"; break;
            case 2: tr.style = "color: #f1fa8c"; break;
            case 3: tr.style = "color: #ffb86c"; break;
            case 4: tr.style = "color: #ff5555"; break;

            default: tr.style = "color: #ffffff"; break;
        }
    }

    else {

        key.style = "width: 84%; color: #ffffff";
    }

    table.appendChild(tr);
}
