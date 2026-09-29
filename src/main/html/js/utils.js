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

    return value !== null && value !== undefined && value !== "";
}
