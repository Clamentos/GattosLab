function onSubmitEvent(event) {

    event.preventDefault();
    document.getElementById("submit-loader").style = "display: inline-block";

    const role = event.target.loginArea.value;

    fetch(`/api/session?role=${role}`, {

        method: "POST",
        headers: { "Authorization": event.target.password.value }
    })
    .then(response => {

        if(response.status === 200) {

            response.text().then(expire => {

                localStorage.setItem("GattosLabRole", role);
                localStorage.setItem(`GattosLabSessionExpire${role}`, String(expire));

                globalThis.location = "./admin/index.html";
            });
        }

        else {

            response.json().then(errorBody => pushError(errorBody));
        }
    })
    .catch(error_ => pushError(error_))
    .finally(() => document.getElementById("submit-loader").style = "");
}
