function onSubmitEvent(event) {

    event.preventDefault();

    document.getElementById("API-error").innerText = "";
    document.getElementById("submit-loader").style = "display: inline-block";

    fetch(`/api/session?role=${event.target.loginArea.value}`, {

        method: "POST",
        headers: { "Authorization": event.target.password.value }
    })
    .then(response => {

        switch(response.status) {

            case 200: globalThis.location = "./admin/index.html"; break;
            case 401: document.getElementById("API-error").innerText = "Unauthorized"; break;

            default: document.getElementById("API-error").innerText = `Error: ${response.status}`; break;
        }
    })
    .catch(error_ => {

        let text;

        if(error_.title === "about:custom_error") text = error_.title;
        else text = `Error: ${error_}`;

        document.getElementById("API-error").innerText = text;
    })
    .finally(() => document.getElementById("submit-loader").style = "display: none");
}
