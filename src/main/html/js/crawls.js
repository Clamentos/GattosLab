const today = new Date();
today.setUTCHours(0, 0, 0, 0);

const todayTime = today.getTime();
const nextDayTime = todayTime + 86400000;

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(nextDayTime).toISOString().slice(0, 16);

fetchAndRenderCrawls(todayTime, nextDayTime, "", "", "", "", "");

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formIsUnknown = event.target.isUnknown.value;
    const formIsUserAgentBlocked = event.target.isUserAgentBlocked.value;
    const formUserAgentPattern = event.target.userAgentPattern.value;
    const formPathPattern = event.target.pathPattern.value;
    const formRemoteIpAddressPattern = event.target.remoteIpAddressPattern.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderCrawls(

        range.start,
        range.end,
        isOk(formIsUnknown) ? formIsUnknown : "",
        isOk(formIsUserAgentBlocked) ? formIsUserAgentBlocked : "",
        isOk(formUserAgentPattern) ? formUserAgentPattern : "",
        isOk(formPathPattern) ? formPathPattern : "",
        isOk(formRemoteIpAddressPattern) ? formRemoteIpAddressPattern : ""
    );
}

function fetchAndRenderCrawls(startTimestamp, endTimestamp, isUnknown, isUserAgentBlocked, userAgentPattern, pathPattern, remoteIpAddressPattern) {

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

    const filter =

        `${startTimestamp}|${endTimestamp}|${isUnknown}|${isUserAgentBlocked}|${userAgentPattern}|${pathPattern}|${remoteIpAddressPattern}`
    ;

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

    /*
        uri            |numberOfCalls|lastCalled|isUnknown|statuses
        userAgent      |numberOfCalls|lastSeen  |isBlocked
        remoteIpAddress|numberOfCalls|lastSeen
    */

    const splits = entry.split('|');

    const tr = document.createElement("div");
    const key = document.createElement("div");
    const count = document.createElement("div");
    const lastInvocation = document.createElement("div");

    tr.appendChild(key);
    tr.appendChild(count);
    tr.appendChild(lastInvocation);

    tr.className = "table-data-row";

    if(hook !== "remote-ips-table-hook") tr.style = `color: ${splits[3] === "true" ? "#f1fa8c" : "#ffffff"}`;
    else tr.style = "color: #ffffff";

    key.className = "table-data-elem";
    key.innerText = splits[0];

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
        httpStatuses.innerText = splits[4];

        tr.appendChild(httpStatuses);
    }

    else {

        key.style = "width: 84%";
    }

    table.appendChild(tr);
}
