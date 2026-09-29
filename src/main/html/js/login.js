function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    fetch("/api/authentication/login", {

        method: "POST",
        headers: { "Authorization": event.target.password.value }
    })
    .then(response => {

        if(response.status === 200) {

            response.text().then(expire => {

                localStorage.setItem("GattosLabSessionExpire", String(expire));
                globalThis.location = "./admin/index.html";
            });
        }

        else {

            response.text().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}
