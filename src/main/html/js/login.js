function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    fetch(`/api/session?role=${event.target.loginArea.value}`, {

        method: "POST",
        headers: { "Authorization": event.target.password.value }
    })
    .then(response => {

        if(response.status === 200) globalThis.location = "./admin/index.html";
        else response.json().then(errorBody => pushError(errorBody));
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}
