let toastCounter = 0;

function pushError(error) {

    const errorContainer = document.getElementById("error-container");
    const errorDiv = document.createElement("div");
    const text = isOk(error.title) ? `${error.title}: ${error.details}` : error;

    errorDiv.id = `error-toast-${toastCounter++}`;
    errorDiv.className = "error-toast";
    errorDiv.innerText = text;

    errorContainer.insertBefore(errorDiv, errorContainer.firstChild);
    setTimeout(function() { errorDiv.remove(); }, 5000);
}
