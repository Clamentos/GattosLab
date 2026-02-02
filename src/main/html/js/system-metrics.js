Chart.defaults.color = "#FFFFFF";
Chart.defaults.datasets.line.fill = true;
Chart.defaults.datasets.bubble.fill = true;
Chart.defaults.elements.line.borderWidth = 1;
Chart.defaults.elements.point.pointRadius = 1;

const today = new Date();
const timestampMax = 999999999999999;
const defaultTimeResolution = 600000;

let activeCharts = [];

today.setUTCHours(0, 0, 0, 0);

document.getElementById("start-timestamp").value = today.toISOString().slice(0, 16);
document.getElementById("submit-loader").style = "display: inline-block";

fetchAndRenderSystemMetrics(today.getTime(), timestampMax, defaultTimeResolution);

function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    for(const oldChart of activeCharts) oldChart.destroy();
    activeCharts = [];

    const formStartTimestamp = event.target.startTimestamp.value;
    const formEndTimestamp = event.target.endTimestamp.value;
    const resolution = event.target.resolution.value;

    fetchAndRenderSystemMetrics(

        formStartTimestamp === "" ? 0 : Date.parse(formStartTimestamp),
        formEndTimestamp === "" ? timestampMax : Date.parse(formEndTimestamp),
        resolution === "" ? defaultTimeResolution : Number(resolution) * 1000
    );
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

                const timeline = prepareTimeline(startTimestamp, endTimestamp, resolution, json);

                const memoryDatasets = [

                    { label: "Heap used (bytes)", data: [] },
                    { label: "Non heap used (bytes)", data: [] }
                ];

                const cpuDatasets = [{ label: "CPU load %", data: [] }];

                const threadDatasets = [

                    { label: "Daemon", data: [] },
                    { label: "Non daemon", data: [] }
                ];

                const classDatasets = [

                    { label: "Loaded classes", data: [] },
                    { label: "Unloaded classes", data: [] }
                ];

                const gcDatasets = [

                    { label: "Cumulative GC time (ms)", data: [] },
                    { label: "Cumulative GC count", data: [] }
                ];

                for(const timestamp of timeline.xAxis) {

                    const metrics = timeline.systemDataMap.get(timestamp);

                    let heapAvg = 0;
                    let nonHeapAvg = 0;
                    let cpuAvg = 0;
                    let daemonAvg = 0;
                    let nonDaemonAvg = 0;
                    let loadedClassAvg = 0;
                    let unloadedClassAvg = 0;

                    let totalGcTimeMax = 0;
                    let totalGcCountMax = 0;

                    if(metrics !== undefined && metrics !== null) {

                        for(const metric of metrics) {

                            heapAvg += metric.heap;
                            nonHeapAvg += metric.nonHeap;
                            cpuAvg += metric.cpuLoadAvg;
                            daemonAvg += metric.daemons;
                            nonDaemonAvg += metric.threads;
                            loadedClassAvg += metric.loadedClassCount;
                            unloadedClassAvg += metric.unloadedClassCount;

                            if(metric.totalGcTime > totalGcTimeMax) totalGcTimeMax = metric.totalGcTime;
                            if(metric.totalGcCount > totalGcCountMax) totalGcCountMax = metric.totalGcCount;
                        }

                        heapAvg = heapAvg / metrics.length;
                        nonHeapAvg = nonHeapAvg / metrics.length;
                        cpuAvg = cpuAvg / metrics.length;
                        daemonAvg = daemonAvg / metrics.length;
                        nonDaemonAvg = nonDaemonAvg / metrics.length;
                        loadedClassAvg = loadedClassAvg / metrics.length;
                        unloadedClassAvg = unloadedClassAvg / metrics.length;
                    }

                    memoryDatasets[0].data.push(heapAvg);
                    memoryDatasets[1].data.push(nonHeapAvg);
                    cpuDatasets[0].data.push(cpuAvg);
                    threadDatasets[0].data.push(daemonAvg);
                    threadDatasets[1].data.push(nonDaemonAvg);
                    classDatasets[0].data.push(loadedClassAvg);
                    classDatasets[1].data.push(unloadedClassAvg);
                    gcDatasets[0].data.push(totalGcTimeMax);
                    gcDatasets[1].data.push(totalGcCountMax);
                }

                const chartLabels = timeline.xAxis.map(e => formatDate(new Date(e)));

                activeCharts.push(

                    new Chart(document.getElementById("SystemMemoryChart"), {

                        type: "line",

                        data: {

                            labels: chartLabels,
                            datasets: memoryDatasets
                        },

                        options: getChartOptions("Average JVM memory usage (bytes)")
                    }),

                    new Chart(document.getElementById("SystemCpuChart"), {

                        type: "line",

                        data: {

                            labels: chartLabels,
                            datasets: cpuDatasets
                        },

                        options: getChartOptions("Average JVM CPU load %")
                    }),

                    new Chart(document.getElementById("SystemThreadChart"), {

                        type: "line",

                        data: {

                            labels: chartLabels,
                            datasets: threadDatasets
                        },

                        options: getChartOptions("JVM thread count")
                    }),

                    new Chart(document.getElementById("SystemClassChart"), {

                        type: "line",

                        data: {

                            labels: chartLabels,
                            datasets: classDatasets
                        },

                        options: getChartOptions("JVM class count")
                    }),

                    new Chart(document.getElementById("SystemGcChart"), {

                        type: "line",

                        data: {

                            labels: chartLabels,
                            datasets: gcDatasets
                        },

                        options: getChartOptions("Cumulative JVM GC usage")
                    })
                )
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}

function prepareTimeline(startTimestamp, endTimestamp, resolution, json) {

    const systemDataMap = new Map(); /* timestamp -> metrics[] */
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

    return { systemDataMap: systemDataMap, xAxis: xAxis };
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
