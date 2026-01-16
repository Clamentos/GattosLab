function logout(role, redirect) {

    fetch(`/api/session?role=${role}`, { method: "DELETE" }).then(response => {

        if(response.status === 200) {

            const role = localStorage.getItem("GattosLabRole");

            localStorage.clear("GattosLabRole");
            localStorage.clear(`GattosLabSessionExpire${role}`);

            if(redirect !== undefined && redirect !== null && response.status === 200) {

                globalThis.location = redirect;
            }
        }
    });
}
