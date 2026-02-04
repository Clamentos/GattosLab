Chart.defaults.color = "#FFFFFF";
Chart.defaults.datasets.line.fill = true;
Chart.defaults.datasets.bubble.fill = true;
Chart.defaults.elements.line.borderWidth = 1;
Chart.defaults.elements.point.pointRadius = 1;

const today = new Date();
const timestampMax = 999999999999999;
const defaultTimeResolution = 600000;
const bubbleSizeScale = 4;

const latencyBuckets = [

    {start: 0, end: 1, text: "0-1 ms"},
    {start: 2, end: 4, text: "2-4 ms"},
    {start: 5, end: 8, text: "4-8 ms"},
    {start: 9, end: 16, text: "9-16 ms"},
    {start: 17, end: 32, text: "17-32 ms"},
    {start: 33, end: 64, text: "33-64 ms"},
    {start: 65, end: 128, text: "65-128 ms"},
    {start: 129, end: 256, text: "129-256 ms"},
    {start: 257, end: 512, text: "257-512 ms"},
    {start: 513, end: 999999, text: "513+ ms"}
];

let activeCharts = [];

today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("submit-loader").style = "display: inline-block";

fetchAndRenderPerformanceMetrics(today.getTime(), timestampMax, null, null, null, defaultTimeResolution);

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

    fetchAndRenderPerformanceMetrics(

        formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp),
        formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp),
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
                fieldsToExclude: ["userAgent"]
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

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}

function prepareTimeline(startTimestamp, endTimestamp, json, resolution) {

    let actualStart = startTimestamp === 0 ? timestampMax : startTimestamp;
    let actualEnd = endTimestamp === timestampMax ? 0 : endTimestamp;

    const performanceDataMap = new Map(); /* status+path -> timestamp -> metrics[] */

    for(const performanceMetric of json) {

        if(performanceMetric.isOthers) performanceMetric.path = "<others>";

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
            r: Math.log(latencyData[i] + 1) * bubbleSizeScale
        });
    }
}

function renderCharts(preparedData, datasets) {

    activeCharts.push(

        new Chart(document.getElementById("RequestsRateChart"), {

            type: "line",

            data: {

                labels: preparedData.rpsXAxis.map(e => formatDate(new Date(e))),
                datasets: datasets.rpsDatasets
            },

            options: getChartOptions("Request rates")
        }),

        new Chart(document.getElementById("RequestLatencyChart"), {

            type: "bubble",

            data: {

                datasets: datasets.latencyDatasets
            },

            options: getChartOptions(

                "Request latencies",
                (value, _, __) => formatDate(new Date(value)),

                (value, _, __) => {

                    const val = Number.parseInt(value);

                    if(val % 1 === 0) return latencyBuckets[val].text;
                    else return "-";
                },

                (context) => `${formatDate(new Date(context[0].raw.x))}`,
                (context) => `${context.dataset.label}: ${Math.round(Math.exp(context.raw.r / bubbleSizeScale))}`
            )
        })
    );
}

function getChartOptions(title, xCallback, yCallback, tooltipTitleCallback, tooltipCallback) {

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
            },

            tooltip: {}
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

    if(tooltipTitleCallback !== undefined && tooltipCallback !== undefined) {

        options.plugins["tooltip"]["callbacks"] = { title: tooltipTitleCallback, label: tooltipCallback };
    }

    return options;
}
