const bubbleSizeScale = 4;

function formatDate(date) {

    return date.toLocaleString("sv-SE");
}

function toggleVisibility(toHideId) {

    const toHide = document.getElementById(toHideId);
    const direction = toHide.className.includes("hidden-element");

    toHide.className = direction ? toHide.className.replace("hidden-element", "") : `${toHide.className} hidden-element`;
    document.getElementById(`${toHideId}-icon-up`).className = direction ? "hidden-element" : "";
    document.getElementById(`${toHideId}-icon-down`).className = direction ? "" : "hidden-element";
}

function normalizeTimeRange(startStr, endStr, todayStartDate) {

    const todayStartMillis = todayStartDate.getTime();
    if(!isOk(startStr) && !isOk(endStr)) return {start: todayStartMillis, end: todayStartMillis + 86400000};

    if(isOk(startStr) && !isOk(endStr)) {

        const startMillis = Date.parse(startStr);
        return {start: startMillis, end: startMillis + 86400000};
    }

    if(!isOk(startStr) && isOk(endStr)) {

        const endMillis = Date.parse(startStr);
        return {start: endMillis - 86400000, end: endMillis};
    }

    if(isOk(startStr) && isOk(endStr)) return {start: Date.parse(startStr), end: Date.parse(endStr)};
}

function isOk(value) {

    return value !== "" && value !== null && value !== undefined;
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

function renderLineChart(chartList, hook, title, chartData) {

    chartList.push(new Chart(document.getElementById(hook), {

        type: "line",

        data: {

            labels: chartData.labels.map(e => formatDate(new Date(e))),
            datasets: chartData.datasets
        },

        options: getChartOptions(title)
    }));
}

function renderBubbleChart(chartList, hook, title, chartData, latencyBuckets) {

    chartList.push(new Chart(document.getElementById(hook), {

        type: "bubble",

        data: {

            datasets: chartData.datasets
        },

        options: getChartOptions(

            title,
            (value, _, __) => formatDate(new Date(value)),

            (value, _, __) => {

                const val = Number.parseInt(value);

                if(val % 1 === 0) return latencyBuckets[val];
                else return "-";
            },

            (context) => `${formatDate(new Date(context[0].raw.x))}`,
            (context) => `${context.dataset.label}: ${Math.round(Math.exp(context.raw.r / bubbleSizeScale))}`
        )
    }));
}
