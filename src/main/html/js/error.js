let counter = 0;

function pushError(error) {

    const id = `error-toast-${counter++}`;
    const errorContainer = document.getElementById("error-container");

    const errorDiv = document.createElement("div");
    errorDiv.className = "error-toast";
    errorDiv.innerText = formatError(error);
    errorDiv.id = id;

    errorContainer.insertBefore(errorDiv, errorContainer.firstChild);
    setTimeout(function() { errorDiv.remove(); }, 5000);
}

function formatError(error) {

    let text;

    if(error.type === "about:custom_error") text = `${error.status} ${error.title}`;
    else text = `Error: ${error}`;

    return text;
}
