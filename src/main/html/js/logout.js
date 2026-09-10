function logout(role, redirect) {

    fetch("/api/authentication/logout", { method: "DELETE" }).then(response => {

        if(response.status === 200) {

            localStorage.clear("GattosLabSessionExpire");

            if(redirect !== undefined && redirect !== null && response.status === 200) {

                globalThis.location = redirect;
            }
        }
    });
}
