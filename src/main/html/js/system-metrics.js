Chart.defaults.color = "#FFFFFF";
Chart.defaults.datasets.line.fill = false;
Chart.defaults.elements.line.borderWidth = 1;
Chart.defaults.elements.point.pointRadius = 0;

const today = new Date();
const defaultTimeResolution = 600000;

let activeCharts = [];

today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("end-timestamp").value = new Date(today.getTime() + 86400000).toISOString().slice(0, 16);
document.getElementById("submit-loader").style = "display: inline-block";

fetchAndRenderSystemMetrics(today.getTime(), today.getTime() + 86400000, defaultTimeResolution);

function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    for(const oldChart of activeCharts) oldChart.destroy();
    activeCharts = [];

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const resolution = event.target.resolution.value;

    const range = normalizeTimeRange(formStartTimestamp, formEndTimestamp, today);
    fetchAndRenderSystemMetrics(range.start, range.end, resolution === "" ? defaultTimeResolution : Number(resolution) * 1000);
}

function fetchAndRenderSystemMetrics(startTimestamp, endTimestamp, resolution) {

    const filter = `${startTimestamp}|${endTimestamp}|${resolution}`;

    fetch(`/api/observability/system-metrics?filter=${encodeURI(filter)}`, {method: "GET"})
    .then(response => {

        if(response.status === 200) {

            response.json().then(json => {

                renderLineChart(activeCharts, "SystemThreadChart", "JVM threads", json.threads);
                renderLineChart(activeCharts, "SystemClassChart", "Loaded JVM classes", json.classes);
                renderLineChart(activeCharts, "IoChart", "JVM IO resources", json.ioResources);
                renderLineChart(activeCharts, "SystemGcChart", "JVM GC", json.garbageCollection);
                renderLineChart(activeCharts, "SystemCpuChart", "CPU utilization %", json.cpuUtilization);
                renderLineChart(activeCharts, "SystemMemoryChart", "Memory utilization", json.memoryUtilization);
                renderLineChart(activeCharts, "StorageChart", "Storage utilization", json.storageUtilization);
                renderLineChart(activeCharts, "RequestMetricsEquilibriumChart", "Request metrics equilibrium", json.requestMetricsEquilibrium);
            });
        }

        else {

            response.text().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}
