Chart.defaults.color = "#FFFFFF";
Chart.defaults.datasets.line.fill = true;
Chart.defaults.datasets.bubble.fill = true;
Chart.defaults.elements.line.borderWidth = 1;
Chart.defaults.elements.point.pointRadius = 1;

const timestampMax = 999999999999999;
const defaultResolution = 600000; // 10 minutes.
const bubbleScale = 4;
let charts = [];

const latencyBuckets = [

    {start: 0, end: 1, text: "0-1 ms"},
    {start: 2, end: 4, text: "2-4 ms"},
    {start: 5, end: 8, text: "4-8 ms"},
    {start: 9, end: 16, text: "9-16 ms"},
    {start: 17, end: 32, text: "17-32 ms"},
    {start: 33, end: 64, text: "33-64 ms"},
    {start: 65, end: 128, text: "65-128 ms"},
    {start: 129, end: 256, text: "129-256 ms"},
    {start: 257, end: 999999, text: "257+ ms"}
];

const today = new Date();
today.setUTCHours(0, 0, 0, 0);
document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("API-error").innerHTML = "";

fetchAndRenderPerformanceMetrics(today.getTime(), timestampMax, null, null, defaultResolution);
fetchAndRenderSystemMetrics(today.getTime(), timestampMax, defaultResolution);

function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("API-error").innerHTML = "";
    clearOldCharts();

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const paths = event.target.paths.value;
    const httpStatuses = event.target.httpStatuses.value;
    const resolution = event.target.resolution.value;

    const requestStartTimestamp = formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp);
    const requestEndTimestamp = formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp);
    const requestResolution = resolution === "" ? defaultResolution : Number(resolution) * 1000;

    fetchAndRenderPerformanceMetrics(

        requestStartTimestamp,
        requestEndTimestamp,
        paths === "" ? null : String(paths).split(","),
        httpStatuses === "" ? null : String(httpStatuses).split(",").map(s => Number.parseInt(s)),
        requestResolution
    );

    fetchAndRenderSystemMetrics(requestStartTimestamp, requestEndTimestamp, requestResolution);
}

function clearOldCharts() {

    for(const oldChart of charts) oldChart.destroy();
    charts = [];
}

function fetchAndRenderPerformanceMetrics(startTimestamp, endTimestamp, paths, httpStatuses, resolution) {

    fetch("/admin/api/observability/request-metrics",

        {
            method: "POST",
            headers: new Headers({"content-type": "application/json"}),

            body: JSON.stringify({

                startTimestamp: startTimestamp,
                endTimestamp: endTimestamp,
                paths: paths,
                httpStatuses: httpStatuses
            })
        }
    )
    .then(response => {

        if(response.status === 200) {

            response.json().then(json => {

                const preparedData = prepareTimeline(startTimestamp, endTimestamp, json, resolution);
                const datasets = prepareDatasets(preparedData);

                renderCharts(preparedData, datasets);
            });
        }

        else {

            showError(response);
        }
    })
    .catch(error_ => showError(error_));
}

function fetchAndRenderSystemMetrics(startTimestamp, endTimestamp, resolution) {

    fetch("/admin/api/observability/system-metrics",

        {
            method: "POST",
            headers: new Headers({"content-type": "application/json"}),

            body: JSON.stringify({

                startTimestamp: startTimestamp,
                endTimestamp: endTimestamp
            })
        }
    )
    .then(response => {

        if(response.status === 200) {

            response.json().then(json => {

                const systemDataMap = new Map(); // timestamp -> metrics[]
                let actualStart = startTimestamp === 0 ? timestampMax : startTimestamp;
                let actualEnd = endTimestamp === timestampMax ? 0 : endTimestamp;

                for(const systemMetric of json) {

                    const metricTimestamp = systemMetric.timestamp;

                    if(startTimestamp === 0 && metricTimestamp < actualStart) actualStart = metricTimestamp;
                    if(endTimestamp === timestampMax && metricTimestamp > actualEnd) actualEnd = metricTimestamp;

                    const key = Math.floor(metricTimestamp / resolution) * resolution;
                    mapComputeIfAbsent(systemDataMap, key, () => []).push(systemMetric);
                }

                const xAxis = [];
                for(let i = actualStart; i < actualEnd; i += resolution) xAxis.push(i);

                const memoryDatasets = [

                    { label: "Heap used (bytes)", data: [] },
                    { label: "Non heap used (bytes)", data: [] }
                ];

                const cpuDatasets = [{ label: "CPU load", data: [] }];

                const threadDatasets = [

                    { label: "Daemon", data: [] },
                    { label: "Non daemon", data: [] }
                ];

                for(const timestamp of xAxis) {

                    const metrics = systemDataMap.get(timestamp);

                    let heapAvg = 0;
                    let nonHeapAvg = 0;
                    let cpuAvg = 0;
                    let daemonAvg = 0;
                    let nonDaemonAvg = 0;

                    if(metrics !== undefined && metrics !== null) {

                        for(const metric of metrics) {

                            heapAvg += metric.heap;
                            nonHeapAvg += metric.nonHeap;
                            cpuAvg += metric.cpuLoadAvg;
                            daemonAvg += metric.daemons;
                            nonDaemonAvg += metric.threads;
                        }

                        heapAvg = heapAvg / metrics.length;
                        nonHeapAvg = nonHeapAvg / metrics.length;
                        cpuAvg = cpuAvg / metrics.length;
                        daemonAvg = daemonAvg / metrics.length;
                        nonDaemonAvg = nonDaemonAvg / metrics.length;
                    }

                    memoryDatasets[0].data.push(heapAvg);
                    memoryDatasets[1].data.push(nonHeapAvg);
                    cpuDatasets[0].data.push(cpuAvg);
                    threadDatasets[0].data.push(daemonAvg);
                    threadDatasets[1].data.push(nonDaemonAvg);
                }

                charts.push(

                    new Chart(document.getElementById("SystemMemoryChart"), {

                        type: "line",

                        data: {

                            labels: xAxis.map(e => new Date(e).toLocaleString()),
                            datasets: memoryDatasets
                        },

                        options: getChartOptions("Average JVM memory usage")
                    }),

                    new Chart(document.getElementById("SystemCpuChart"), {

                        type: "line",

                        data: {

                            labels: xAxis.map(e => new Date(e).toLocaleString()),
                            datasets: cpuDatasets
                        },

                        options: getChartOptions("Average JVM CPU load")
                    }),

                    new Chart(document.getElementById("SystemThreadChart"), {

                        type: "line",

                        data: {

                            labels: xAxis.map(e => new Date(e).toLocaleString()),
                            datasets: threadDatasets
                        },

                        options: getChartOptions("Average JVM thread count")
                    })
                )
            });
        }

        else {

            showError(response);
        }
    })
    .catch(error_ => showError(error_));
}

function prepareTimeline(startTimestamp, endTimestamp, json, resolution) {

    let actualStart = startTimestamp === 0 ? timestampMax : startTimestamp;
    let actualEnd = endTimestamp === timestampMax ? 0 : endTimestamp;
    const performanceDataMap = new Map(); // status+path -> timestamp -> metrics[]

    for(const performanceMetric of json) {

        const metricTimestamp = performanceMetric.timestamp;

        if(startTimestamp === 0 && metricTimestamp < actualStart) actualStart = metricTimestamp;
        if(endTimestamp === timestampMax && metricTimestamp > actualEnd) actualEnd = metricTimestamp;

        const key = `${performanceMetric.httpStatus} ${performanceMetric.path}`;
        const innerMapKey = Math.floor(metricTimestamp / resolution) * resolution;

        const innerMap = mapComputeIfAbsent(performanceDataMap, key, () => new Map());
        mapComputeIfAbsent(innerMap, innerMapKey, () => []).push(performanceMetric);
    }

    const rpsXAxis = [];
    for(let i = actualStart; i < actualEnd; i += resolution) rpsXAxis.push(i);

    return { performanceDataMap: performanceDataMap, rpsXAxis: rpsXAxis };
}

function prepareDatasets(preparedData) {

    const rpsDatasets = [];
    const latencyDatasets = [];

    for(const [label, innerMap] of preparedData.performanceDataMap) {

        const rpsDataset = { label: label, data: [] };
        const latencyDataset = { label: label, data: [] };

        for(const timestamp of preparedData.rpsXAxis) {

            const metrics = innerMap.get(timestamp);

            if(metrics !== undefined && metrics !== null) {

                rpsDataset.data.push(metrics.length);
                assignLatencies(metrics, timestamp, latencyDataset);
            }

            else {

                rpsDataset.data.push(0);
            }
        }

        rpsDatasets.push(rpsDataset);
        latencyDatasets.push(latencyDataset);
    }

    return { rpsDatasets: rpsDatasets, latencyDatasets: latencyDatasets };
}

function assignLatencies(metrics, timestamp, latencyDataset) {

    const latencyData = new Array(latencyBuckets.length).fill(0);

    for(const metric of metrics) {

        const idx = latencyBuckets.findIndex(b => metric.latency >= b.start && metric.latency <= b.end);
        latencyData[idx]++;
    }

    for(let i = 0; i < latencyData.length; i++) {

        latencyDataset.data.push({

            x: timestamp,
            y: i,
            r: latencyData[i] * bubbleScale
        });
    }
}

function renderCharts(preparedData, datasets) {

    charts.push(

        new Chart(document.getElementById("RequestsPerSecondChart"), {

            type: "line",

            data: {

                labels: preparedData.rpsXAxis.map(e => new Date(e).toLocaleString()),
                datasets: datasets.rpsDatasets
            },

            options: getChartOptions("Requests per second")
        }),

        new Chart(document.getElementById("LatencyChart"), {

            type: "bubble",

            data: {

                datasets: datasets.latencyDatasets
            },

            options: getChartOptions(

                "Request latencies",
                (value, index, ticks) => new Date(value).toLocaleString(),

                (value, index, ticks) => {

                    const val = Number.parseInt(value);

                    if(val % 1 === 0) return latencyBuckets[val].text;
                    else return "-";
                }
            )
        })
    );
}

function getChartOptions(title, xCallback, yCallback) {

    const options = {

        responsive: true,

        plugins: {

            legend: {

                position: "right"
            },

            title: {

                display: true,
                text: title
            },

            colors: {

                enabled: true
            },

            decimation: {

                enabled: true,
                algorithm: 'min-max',
            }
        },

        scales: {

            x: {

                grid: {

                    color: "rgba(255, 255, 255, 0.15)"
                }
            },

            y: {

                grid: {

                    color: "rgba(255, 255, 255, 0.15)"
                }
            }
        }
    };

    if(xCallback !== undefined) options.scales.x["ticks"] = { callback: xCallback };
    if(yCallback !== undefined) options.scales.y["ticks"] = { callback: yCallback };

    return options;
}

function mapComputeIfAbsent(map, key, func) {

    const current = map.get(key);

    if(current == null) {

        const newValue = func();
        map.set(key, newValue);

        return newValue;
    }

    return current;
}

function showError(error) {

    document.getElementById("API-error").innerHTML = `Error: ${error}`;
}
