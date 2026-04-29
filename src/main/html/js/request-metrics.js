Chart.defaults.color = "#FFFFFF";
Chart.defaults.datasets.line.fill = true;
Chart.defaults.datasets.bubble.fill = true;
Chart.defaults.elements.line.borderWidth = 1;
Chart.defaults.elements.point.pointRadius = 1;

const today = new Date();
const defaultTimeResolution = 600000;

const latencyBuckets = [

    "0-1 ms",
    "2-4 ms",
    "5-9 ms",
    "10-19 ms",
    "20-49 ms",
    "50-99 ms",
    "100-199 ms",
    "200-499 ms",
    "500-999 ms",
    "1000+ ms"
];

let activeCharts = [];

today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(today.getTime() + 86400000).toISOString().slice(0, 16);
document.getElementById("submit-loader").style = "display: inline-block";

fetchAndRenderPerformanceMetrics(today.getTime(), today.getTime() + 86400000, null, null, null, defaultTimeResolution);

function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    for(const oldChart of activeCharts) oldChart.destroy();
    activeCharts = [];

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const formOnlyOthers = event.target.onlyOthers.value;
    const formPathPattern = event.target.pathPattern.value;
    const httpStatuses = event.target.httpStatuses.value;
    const resolution = event.target.resolution.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderPerformanceMetrics(

        range.start,
        range.end,
        formOnlyOthers === "" ? null : formOnlyOthers === "true",
        formPathPattern === "" ? null : formPathPattern,
        httpStatuses === "" ? null : String(httpStatuses).split(",").map(s => Number.parseInt(s)),
        resolution === "" ? defaultTimeResolution : Number(resolution) * 1000
    );
}

function fetchAndRenderPerformanceMetrics(startTimestamp, endTimestamp, onlyOthers, pathPattern, httpStatuses, resolution) {

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
                bucketSize: resolution
            })
        }
    )
    .then(response => {

        if(response.status === 200) {

            response.json().then(json => {

                renderLineChart(activeCharts, "RequestsRateChart", "Request rates", json.rate);
                renderBubbleChart(activeCharts, "RequestLatencyChart", "Request latencies", json.latency, latencyBuckets);
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}
