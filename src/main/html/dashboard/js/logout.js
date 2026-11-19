updateCounter();
setInterval(updateCounter, 60000);

function logout(redirect) {

    fetch("/api/authentication/logout", { method: "DELETE" }).then(response => {

        if(response.status === 200) {

            localStorage.clear("GattosLabSessionExpire");

            if(redirect !== undefined && redirect !== null && response.status === 200) {

                globalThis.location = redirect;
            }
        }
    });
}

function updateCounter() {

    const expiration = localStorage.getItem("GattosLabSessionExpire");

    if(expiration) {

        const timestamp = Number.parseInt(expiration) - Date.now();

        let hours = 0;
        let minutes = 0;

        if(timestamp >= 0) {

            hours = Math.floor((timestamp % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            minutes = Math.floor((timestamp % (1000 * 60 * 60)) / (1000 * 60));
        }

        document.getElementById("logout-text").innerText = `Logout ${hours}:${minutes}`;
    }
}
