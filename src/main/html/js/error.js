let toastCounter = 0;

function pushError(error) {

    const errorContainer = document.getElementById("error-container");
    const errorDiv = document.createElement("div");

    errorDiv.id = `error-toast-${toastCounter++}`;
    errorDiv.className = "error-toast";
    errorDiv.innerText = error.type === "about:custom_error" ? `${error.status} ${error.title}` : `Error: ${error}`;

    errorContainer.insertBefore(errorDiv, errorContainer.firstChild);
    setTimeout(function() { errorDiv.remove(); }, 5000);
}
