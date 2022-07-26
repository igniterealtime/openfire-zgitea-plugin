window.addEventListener("unload", function()
{

});

window.addEventListener("load", function()
{
	const repo = document.querySelectorAll(".repository .repo-title a");
	
	if (repo && repo.length == 2 && repo[1].innerHTML.indexOf("/") == -1 && repo[1].innerHTML.indexOf("\t") == -1 && repo[1].innerHTML.indexOf(" ") == -1)
	{
        const active = document.querySelector(".active.item");		
		const h5p_json = document.querySelector("a[title='h5p.json']");
		const h5p = document.querySelector(".repository .h5p-tab");
		
        const password = sessionStorage.getItem("gitea_password");
        const username = sessionStorage.getItem("gitea_username");		
		
		if (h5p && active && password && username) {
			h5p.style.display = 'none';
			h5p.setAttribute("data-repo", location.pathname);	
				
			if (h5p_json) {
				h5p.style.display = '';

				h5p.addEventListener("click", function(evt)
				{
					const repoName = evt.target.getAttribute("data-repo");
					console.debug("h5p.addEventListener", repoName);
					active.classList.remove("active");
					h5p.classList.add("active");
					setupH5p(repoName, username, password);
				});			
			}
		}
	}

	function setupH5p(repo, username, password)
	{		
        if (repo && repo != "null") {	
			loadCSS('/assets/h5p/styles/h5p.css');
			loadJS('/assets/h5p/main.bundle.js');	
					
            const options = {method: "GET", headers: {"authorization": "Basic " + btoa(username + ":" + password), "accept": "application/json"}};

            fetch(location.protocol + "//" + location.host + "/jsp/h5p_repo.jsp?repo=" + repo, options).then(function(response){ return response.json()}).then(function(response)
            {
				const container = document.querySelector("div.repository > div.ui.container");				
                console.log("setupH5p response", repo, response, container);

				if (container) {
					createElement(container, 'div', 'h5p-container');		
					
					const {H5P} = H5PStandalone;	
					
					new H5P(container,  {
						h5pJsonPath: '/assets/www' + repo,	  
						frameJs: '/assets/h5p/frame.bundle.js',  
						frameCss: '/assets/h5p/styles/h5p.css'
					});	
				}
				
            }).catch(function (err) {
                console.error("setupH5p error", err);
            });	
		}
	}	
	
	function createElement(parent, eleName, id, className)
	{
		parent.innerHTML = "";
		const ele = document.createElement(eleName);
		if (id) ele.id = id;
		if (className) ele.classList.add(className);
		parent.appendChild(ele);		
	}
		
    function loadJS(name)
    {
        var s1 = document.createElement('script');
        s1.src = name;
        s1.async = false;
        document.body.appendChild(s1);
    }

    function loadCSS(name)
    {
        var head  = document.getElementsByTagName('head')[0];
        var link  = document.createElement('link');
        link.rel  = 'stylesheet';
        link.type = 'text/css';
        link.href = name;
        head.appendChild(link);
    }	
});