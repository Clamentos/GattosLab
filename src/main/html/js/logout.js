function logout(role, redirect) {

    fetch(`/api/session?role=${role}`, { method: "DELETE" }).then(response => {

        if(response.status === 200) {

            localStorage.clear("GattosLabRole");
            localStorage.clear(`GattosLabSessionExpire${localStorage.getItem("GattosLabRole")}`);

            if(redirect !== undefined && redirect !== null && response.status === 200) {

                globalThis.location = redirect;
            }
        }
    });
}
