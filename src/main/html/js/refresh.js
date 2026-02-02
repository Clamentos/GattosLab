doRefresh();

function doRefresh() {

    setTimeout(function() {

        const role = localStorage.getItem("GattosLabRole");

        fetch(`/api/session?role=${role}`, { method: "PUT" }).then(response => {

            if(response.status === 200) {

                response.text().then(expire => {

                    localStorage.setItem(`GattosLabSessionExpire${role}`, String(expire));
                    doRefresh();
                });
            }
        });

    }, Number(localStorage.getItem(`GattosLabSessionExpire${localStorage.getItem("GattosLabRole")}`)) - Date.now() - 3000);
}
