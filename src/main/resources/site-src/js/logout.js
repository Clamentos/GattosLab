function logout(role, redirect) {

    fetch(`/api/session?role=${role}`, { method: "DELETE" }).then(response => {

        if(redirect !== undefined && redirect !== null && response.status === 200) {

            globalThis.location = redirect;
        }
    });
}
