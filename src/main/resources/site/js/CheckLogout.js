///
function checkLogout(redirect) {

    const isloggedIn = localStorage.getItem("GattosLabIsLoggedIn");

    if(isloggedIn === "true") {

        const elements = document.getElementsByClassName("nav-container");

        if(elements.length === 1) {

            const navContainerDiv = elements.item(0);

            const logoutButtonElement = document.createElement("div");
            logoutButtonElement.className = "nav-item";

            const logoutButton = document.createElement("button");
            logoutButton.className = "logout-button";
            logoutButton.type = "button";
            logoutButton.innerText = "Logout";
            logoutButton.onclick = () => handleLogout(redirect);

            logoutButtonElement.appendChild(logoutButton);
            navContainerDiv.appendChild(logoutButtonElement);
        }

        else {

            console.error(`${navContainerDiv.length} nav containers found...`);
        }
    }
}

///..
function handleLogout(redirect) {

    fetch("/admin/api/session", {method: "DELETE"}).then(response => {

        if(response.status === 200) {

            localStorage.removeItem("GattosLabIsLoggedIn");

            if(redirect === null || redirect === undefined) {

                // Remove button, loop will only do 0 or 1 iterations.
                const button = document.getElementsByClassName("logout-button");

                for(let i = 0; i < button.length; i++) {

                    const item = button.item(i);
                    if(item !== null) item.remove();
                }
            }

            else {

                globalThis.location = redirect;
            }
        }
    });
}

///
