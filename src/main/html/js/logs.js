const today = new Date();
today.setUTCHours(0, 0, 0, 0);

const todayTime = today.getTime();
const nextDayTime = todayTime + 86400000;

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(nextDayTime).toISOString().slice(0, 16);

fetchAndRenderLogs(todayTime, nextDayTime, "", "", "", "", "");

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formSeverities = event.target.severities.value;
    const formThreadPattern = event.target.threadPattern.value;
    const formLoggerPattern = event.target.loggerPattern.value;
    const formMessagePattern = event.target.messagePattern.value;
    const formExceptionClassPattern = event.target.exceptionClassPattern.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderLogs(

        range.start,
        range.end,
        isOk(formSeverities) ? formSeverities.split(",") : "",
        isOk(formThreadPattern) ? formThreadPattern : "",
        isOk(formLoggerPattern) ? formLoggerPattern : "",
        isOk(formMessagePattern) ? formMessagePattern : "",
        isOk(formExceptionClassPattern) ? formExceptionClassPattern : "",
    );
}

function fetchAndRenderLogs(startTimestamp, endTimestamp, severities, threadPattern, loggerPattern, messagePattern, exceptionClassPattern) {

    document.getElementById("submit-loader").style = "display: inline-block";
    document.getElementById("logs-count").innerText = "Logs count: -";

    const tableBody = document.getElementById("table-data-hook");
    tableBody.replaceChildren();

    const filter = `${startTimestamp}|${endTimestamp}|${severities}|${threadPattern}|${loggerPattern}|${messagePattern}|${exceptionClassPattern}`;

    fetch("/api/observability/logs", {

        method: "GET",
        headers: { "Filter": filter }

    }).then((response) => {

        if(response.status === 200) {

            response.text().then(text => {

                const lines = text.split('\n');
                let numLines = 0;

                for(const log of lines) {

                    if(isOk(log)) {

                        appendRow(log, tableBody);
                        numLines++;
                    }
                }

                document.getElementById("logs-count").innerText = `Logs count: ${numLines}`;
            });
        }

        else {

            response.text().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}

function appendRow(log, table) {

    const splits = log.split('|');

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    if(splits[2] === "ERROR") tr.style = "color: #ff5555;";
    if(splits[2] === "WARNING") tr.style = "color: #ffb86c;";

    const timestamp = document.createElement("div");
    const severity = document.createElement("div");
    const message = document.createElement("div");
    const logger = document.createElement("div");
    const thread = document.createElement("div");
    const exception = document.createElement("div");

    timestamp.className = "table-data-elem";
    timestamp.style = "text-align: center; width: 7%";
    timestamp.innerText = formatDate(new Date(Number.parseInt(splits[1])));

    severity.className = "table-data-elem";
    severity.style = "text-align: center; width: 4%";
    severity.innerText = splits[2];

    message.className = "table-data-elem";
    message.style = "width: 44%";
    message.innerText = splits[5];

    logger.className = "table-data-elem";
    logger.style = "width: 15%";
    logger.innerText = splits[4];

    thread.className = "table-data-elem";
    thread.style = "width: 15%";
    thread.innerText = splits[3];

    exception.className = "table-data-elem";
    exception.style = "width: 15%";
    exception.innerText = splits[6].replaceAll('\u0002', '\n');

    tr.appendChild(timestamp);
    tr.appendChild(severity);
    tr.appendChild(message);
    tr.appendChild(logger);
    tr.appendChild(thread);
    tr.appendChild(exception);

    table.appendChild(tr);
}
