function onSubmitEvent(event) {

    event.preventDefault();

    document.getElementById("API-error").innerText = "";
    document.getElementById("submit-loader").style = "display: inline-block";

    fetch("/admin/api/session", {

        method: "POST",
        headers: { "Authorization": event.target.apiKey.value }
    })
    .then(response => {

        switch(response.status) {

            case 200: globalThis.location = "./index.html"; break;
            case 401: document.getElementById("API-error").innerText = "Unauthorized"; break;

            default: document.getElementById("API-error").innerText = `Error: ${response.status}`; break;
        }
    })
    .catch(error_ => document.getElementById("API-error").innerText = `Error: ${error_}`)
    .finally(() => document.getElementById("submit-loader").style = "display: none");
}
