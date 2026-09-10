Chart.defaults.color = "#FFFFFF";
Chart.defaults.datasets.line.fill = false;
Chart.defaults.datasets.bubble.fill = true;
Chart.defaults.elements.line.borderWidth = 1;
Chart.defaults.elements.point.pointRadius = 0;

const today = new Date();
const defaultTimeResolution = 600000;

let activeCharts = [];

today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(today.getTime() + 86400000).toISOString().slice(0, 16);
document.getElementById("submit-loader").style = "display: inline-block";

fetchAndRenderPerformanceMetrics(today.getTime(), today.getTime() + 86400000, defaultTimeResolution);

function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    for(const oldChart of activeCharts) oldChart.destroy();
    activeCharts = [];

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const resolution = event.target.resolution.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);

    fetchAndRenderPerformanceMetrics(

        range.start,
        range.end,
        resolution === "" ? defaultTimeResolution : Number(resolution) * 1000
    );
}

function fetchAndRenderPerformanceMetrics(startTimestamp, endTimestamp, resolution) {

    const filter = `${startTimestamp}|${endTimestamp}|${resolution}`;

    fetch(`/api/observability/request-metrics?filter=${encodeURI(filter)}`, {method: "GET"})
    .then(response => {

        if(response.status === 200) {

            response.json().then(json => {

                renderLineChart(activeCharts, "RequestsRateChart", "Request rates", json.rates);
                renderLineChart(activeCharts, "RequestLatencyChart", "Request latencies", json.latencies);
            });
        }

        else {

            response.text().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}
