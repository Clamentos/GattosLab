function getChartOptions(title) {

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

                    color: "rgba(255, 255, 255, 0.10)"
                }
            },

            y: {

                grid: {

                    color: "rgba(255, 255, 255, 0.10)"
                }
            }
        }
    };

    return options;
}

function renderLineChart(chartList, hook, title, chartData) {

    const palette = [

        "#FF5555",
        "#FFB86C",
        "#F1FA8C",
        "#50FA7B",
        "#8BE9FD",
        "#BD93F9",
        "#FF79C6"
    ];

    for(let i = palette.length - 1; i > 0; i--) {

        const j = Math.floor(Math.random() * (i + 1));
        [palette[i], palette[j]] = [palette[j], palette[i]];
    }

    chartList.push(new Chart(document.getElementById(hook), {

        type: "line",

        data: {

            labels: chartData.labels.map(e => formatDate(new Date(e))),

            datasets: chartData.datasets.map((dataset, i) => ({
                ...dataset,
                borderColor: palette[i % palette.length],
                backgroundColor: palette[i % palette.length]
            }))
        },

        options: getChartOptions(title)
    }));
}
