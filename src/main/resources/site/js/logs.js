const timestampMax = 999999999999999;
const today = new Date();
today.setUTCHours(0, 0, 0, 0);
document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);

fetchAndRenderLogs(today.getTime(), timestampMax, null, null, null);

function onSubmitEvent(event) {

    event.preventDefault();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formSeverities = event.target.severities.value;
    const formThreadPattern = event.target.threadPattern.value;
    const formLoggerPattern = event.target.loggerPattern.value;
    const formMessagePattern = event.target.messagePattern.value;
    const formExceptionClassPattern = event.target.exceptionClassPattern.value;

    fetchAndRenderLogs(

        formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp),
        formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp),
        formSeverities === "" ? null : formSeverities.split(","),
        formThreadPattern === "" ? null : formThreadPattern,
        formLoggerPattern === "" ? null : formLoggerPattern,
        formMessagePattern === "" ? null : formMessagePattern,
        formExceptionClassPattern === "" ? null : formExceptionClassPattern,
    );
}

function fetchAndRenderLogs(startTimestamp, endTimestamp, severities, threadPattern, loggerPattern, messagePattern, exceptionClassPattern) {

    document.getElementById("API-error").innerHTML = "";
    document.getElementById("logs-count").innerText = "Logs count: -";

    const tableBody = document.getElementById("table-data-hook");
    tableBody.replaceChildren();

    fetch("/admin/api/observability/logs",

        {
            method: "POST",
            headers: new Headers({"content-type": "application/json"}),

            body: JSON.stringify({

                startTimestamp: startTimestamp,
                endTimestamp: endTimestamp,
                severities: severities,
                threadPattern: threadPattern,
                loggerPattern: loggerPattern,
                messagePattern: messagePattern,
                exceptionClassPattern: exceptionClassPattern
            })
        }
    )
    .then((response) => {

        if(response.status === 200) {

            response.json().then(json => {

                document.getElementById("logs-count").innerText = `Logs count: ${json.length}`;
                for(const log of json) appendRow(log, tableBody);
            });
        }

        else {

            showError(response);
        }
    })
    .catch(error_ => showError(error_));
}

function appendRow(log, table) {

    const tr = document.createElement("div");
    tr.className = "table-data-row";

    if(log.severity === "ERROR") tr.style = "color: red";
    if(log.severity === "WARN") tr.style = "color: orange";

    const timestamp = document.createElement("div");
    const severity = document.createElement("div");
    const message = document.createElement("div");
    const logger = document.createElement("div");
    const thread = document.createElement("div");
    const exception = document.createElement("div");

    timestamp.className = "table-data-elem";
    timestamp.style = "width: 5%";
    timestamp.innerText = new Date(log.timestamp).toLocaleString();

    severity.className = "table-data-elem";
    severity.style = "text-align: center; width: 5%";
    severity.innerText = log.severity;

    message.className = "table-data-elem";
    message.style = "width: 45%";
    message.innerText = log.message;

    logger.className = "table-data-elem";
    logger.style = "width: 15%";
    logger.innerText = log.logger;

    thread.className = "table-data-elem";
    thread.style = "width: 15%";
    thread.innerText = log.thread;

    exception.className = "table-data-elem";
    exception.style = "width: 15%";
    if(log.exception !== null && log.exception !== undefined) exception.innerText = formatException(log.exception);
    else exception.innerText = "";

    tr.appendChild(timestamp);
    tr.appendChild(severity);
    tr.appendChild(message);
    tr.appendChild(logger);
    tr.appendChild(thread);
    tr.appendChild(exception);

    table.appendChild(tr);
}

function formatException(exception) {

    const message = (exception.message !== null && exception.message !== undefined) ? exception.message : "";
    let trace = "";

    if(exception.stacktrace !== null && exception.stacktrace !== undefined) {

        for(const entry of exception.stacktrace) trace += `${entry}\n`;
    }

    return `${exception.className}: ${message} trace: ${trace}`;
}

function showError(error) {

    document.getElementById("API-error").innerHTML = `Error: ${error}`;
}
