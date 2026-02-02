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

function mapComputeIfAbsent(map, key, func) {

    const current = map.get(key);

    if(current == null) {

        const newValue = func();
        map.set(key, newValue);

        return newValue;
    }

    return current;
}
