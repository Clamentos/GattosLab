function formatDate(date) {

    return date.toLocaleString("sv-SE");
}

function toggleVisibility(toHideId) {

    const toHide = document.getElementById(toHideId);

    if(toHide.className.includes("hidden-element")) {

        toHide.className = toHide.className.replace("hidden-element", "");
        document.getElementById(`${toHideId}-icon-up`).className = "hidden-element";
        document.getElementById(`${toHideId}-icon-down`).className = "";
    }

    else {

        toHide.className = `${toHide.className} hidden-element`;
        document.getElementById(`${toHideId}-icon-up`).className = "";
        document.getElementById(`${toHideId}-icon-down`).className = "hidden-element";
    }
}
