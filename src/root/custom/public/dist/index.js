import { html, render } from '/assets/dist/lit-html.min.js';

let Strophe, $iq, $msg, $pres, _ , __, dayjs, converse_html, _converse, gitea;
	
var converse_api = (function(api)
{
    window.addEventListener("unload", function()
    {
        console.debug("converse_api addListener unload");
    });

    window.addEventListener("load", function()
    {
        gitea = {follow: [], repo: ['lobby@conference.' + location.hostname]};

        const repo = document.querySelectorAll(".repository .repo-title a");
        const follow = document.querySelector(".user.profile .follow button");
        const signIn = document.querySelector("form button");
        const signOut = document.querySelector("a[data-url='/user/logout']");

        console.debug("converse_api addListener load", repo, follow, signIn, signOut);

        if (signOut && signOut.innerHTML.indexOf("Sign Out") > -1)
        {
            signOut.addEventListener("click", function(evt)
            {
                console.debug("converse_api gitea logout");

                sessionStorage.removeItem("gitea_password");
                sessionStorage.removeItem("gitea_username");
                if (_converse) _converse.connection.disconnect();
            });
        }

        if (signIn && signIn.innerHTML.indexOf("Sign In") > -1)
        {
            signIn.addEventListener("click", function(evt)
            {
                const username = document.querySelector("form #user_name").value;
                const password = document.querySelector("form #password").value;

                if (username && password)
                {
                    console.debug("converse_api gitea login", username);

                    sessionStorage.setItem("gitea_password", password);
                    sessionStorage.setItem("gitea_username", username);
                }
            });

            return; // dont load converse
        }

        if (follow && follow.innerHTML.indexOf("Unfollow") > -1)
        {
            gitea.follow = [location.pathname.substring(1) + '@' + location.hostname];
            console.debug("converse_api gitea follow", gitea.follow);
        }

        if (repo && repo.length == 2 && repo[1].innerHTML.indexOf("/") == -1 && repo[1].innerHTML.indexOf("\t") == -1 && repo[1].innerHTML.indexOf(" ") == -1)
        {
            gitea.repo.push(repo[1].innerHTML + '@conference.' + location.hostname);
            console.debug("converse_api gitea repo", gitea.repo);

            const jbake_properties = document.querySelector("a[title='jbake.properties']");
            const jbake = document.querySelector(".repository .jbake-tab");
            const active = document.querySelector(".active.item");

            if (jbake)
            {
                jbake.style.display = 'none';
                jbake.setAttribute("data-repo", location.pathname);

                if (jbake_properties) {
                    jbake.style.display = '';

                    jbake.addEventListener("click", function(evt)
                    {
                        const repo = evt.target.getAttribute("data-repo");
                        console.debug("converse_api jbake", repo);
                        active.classList.remove("active");
                        jbake.classList.add("active");
                        setupjBake(repo);
                    });
                }
            }
			
            const config_toml = document.querySelector("a[title='config.toml']");
            const hugo = document.querySelector(".repository .hugo-tab");

            if (hugo)
            {
                hugo.style.display = 'none';
                hugo.setAttribute("data-repo", location.pathname);

                if (config_toml) {
                    hugo.style.display = '';

                    hugo.addEventListener("click", function(evt)
                    {
                        const repo = evt.target.getAttribute("data-repo");
                        console.debug("converse_api hugo", repo);
                        active.classList.remove("active");
                        hugo.classList.add("active");
                        setupHugo(repo);
                    });
                }
            }			
        }

        gitea.password = sessionStorage.getItem("gitea_password");
        gitea.username = sessionStorage.getItem("gitea_username");

        console.debug("converse_api addListener load", gitea.username);

        if (gitea.username && gitea.password)
        {
            loadCSS('/dist/converse.min.css');
            loadJS('/dist/libsignal-protocol.min.js');
            loadJS('/dist/converse.js');
			
            loadJS('/packages/galene/galene.js');			

            setupConverse();
        }
    });
	
    function setupHugo(repository)
    {
        gitea.container = document.querySelector("div.repository > div.ui.container");
        console.debug("converse_api setupHugo", gitea);

        gitea.template = (repo, status) => html
        `
            <div class="navbar">
                <div class="ui left">
                    <a data-url="/assets/www${repo}/index.html" class="ui blue button" @click=${doView}>View</a>
                </div>
                <div class="ui right">
                    <a data-repo="${repo}" class="jbake-proceed ui green button" @click=${doHugo}>Proceed</a>
                </div>
            </div>
            <div class="ui center">${status}</div>
            <div class="ui center segment"><img src="/assets/jbake_process.png" /></div>
        `;

        render(gitea.template(repository, ""), gitea.container);
    }
	
    function doHugo(ev)
    {
        const repo = ev.target.getAttribute("data-repo");
        console.debug("converse_api doHugo", repo, gitea.username);

        if (repo && repo != "null")
        {
            render(gitea.template(repo, "baking..."), gitea.container);
            const options = {method: "GET", headers: {"authorization": "Basic " + btoa(gitea.username + ":" + gitea.password), "accept": "application/json"}};

            fetch(location.protocol + "//" + location.host + "/jsp/hugo_repo.jsp?repo=" + repo, options).then(function(response){ return response.json()}).then(function(response)
            {
                console.log("converse_api doHugo response", response);
                render(gitea.template(repo, response.status), gitea.container);

            }).catch(function (err) {
                console.error("converse_api doHugo error", err);
                render(gitea.template(repo, err), gitea.container);
            });
        }
    }	
	
    function setupjBake(repository)
    {
        gitea.container = document.querySelector("div.repository > div.ui.container");
        console.debug("converse_api setupjBake", gitea);

        gitea.template = (repo, status) => html
        `
            <div class="navbar">
                <div class="ui left">
                    <a data-url="/assets/www${repo}/index.html" class="ui blue button" @click=${doView}>View</a>
                </div>
                <div class="ui right">
                    <a data-repo="${repo}" class="jbake-proceed ui green button" @click=${dojBake}>Proceed</a>
                </div>
            </div>
            <div class="ui center">${status}</div>
            <div class="ui center segment"><img src="/assets/jbake_process.png" /></div>
        `;

        render(gitea.template(repository, ""), gitea.container);
    }

    function dojBake(ev)
    {
        const repo = ev.target.getAttribute("data-repo");
        console.debug("converse_api dojBake", repo, gitea.username);

        if (repo && repo != "null")
        {
            render(gitea.template(repo, "baking..."), gitea.container);
            const options = {method: "GET", headers: {"authorization": "Basic " + btoa(gitea.username + ":" + gitea.password), "accept": "application/json"}};

            fetch(location.protocol + "//" + location.host + "/jsp/bake_repo.jsp?repo=" + repo, options).then(function(response){ return response.json()}).then(function(response)
            {
                console.log("converse_api dojBake response", response);
                render(gitea.template(repo, response.status), gitea.container);

            }).catch(function (err) {
                console.error("converse_api dojBake error", err);
                render(gitea.template(repo, err), gitea.container);
            });
        }
    }

    function doView(ev)
    {
        const url = ev.target.getAttribute("data-url");
        console.debug("converse_api doView", url);
        if (url && url != "null") window.open(url, "website_view");
    }
	
    function setupConverse()
    {
        if (!window.converse)
        {
            setTimeout(setupConverse, 500);
            return;
        }

        var config =
        {
            theme: 'concord',
			assets_path: "/assets/dist/",			
            allow_non_roster_messaging: true,
            loglevel: 'info',
            authentication: 'login',
            auto_login: true,
            password: gitea.password,
		    discover_connection_methods: false,					
            jid: gitea.username + '@' + location.hostname,
            default_domain: location.hostname,
            domain_placeholder: location.hostname,
            locked_domain: location.hostname,
            auto_away: 300,
            nickname: gitea.username,
            auto_reconnect: true,
            bosh_service_url: '/http-bind/',
            auto_join_rooms: gitea.repo,
            auto_join_private_chats: gitea.follow,
            message_archiving: 'always',
			websocket_url: (location.host == "localhost:7070" || location.protocol == "http:" ? "ws://" : "wss://") + location.host + '/ws/',
            whitelisted_plugins: ['gitea', 'galene']
        }

        console.debug("converse_api setupConverse", config, gitea);

        converse.plugins.add("gitea", {
            dependencies: [],

            initialize: function () {
                _converse = this._converse;

                Strophe = converse.env.Strophe;
                $iq = converse.env.$iq;
                $msg = converse.env.$msg;
                $pres = converse.env.$pres;
                _ = converse.env._;
                __ = _converse.__;
                dayjs = converse.env.dayjs;
                converse_html = converse.env.html;

                Strophe.addConnectionPlugin('giteasasl',
                {
                    init: function (connection)
                    {
                        Strophe.SASLGitea = function () { };
                        Strophe.SASLGitea.prototype = new Strophe.SASLMechanism("GITEA", true, 2000);

                        Strophe.SASLGitea.test = function (connection)
                        {
                            return true;
                        };

                        Strophe.SASLGitea.prototype.onChallenge = function (connection)
                        {
                            return btoa(Strophe.getNodeFromJid(connection.jid) + ":" + connection.pass);
                        };

                        connection.mechanisms[Strophe.SASLGitea.prototype.name] = Strophe.SASLGitea;
                        console.debug("converse_api SASL authentication enabled");
                    }
                });

                _converse.api.listen.on('getToolbarButtons', function(toolbar_el, buttons)
                {
                    //console.debug("getToolbarButtons", toolbar_el);

                    buttons.push(converse_html`
                        <button class="gitea-exit" title="${__('Minimize chat')}" @click=${exitConversation} .chatview=${this.chatview}/>
                            <converse-icon class="fa fa-minus" size="1em"></converse-icon>
                        </button>
                    `);
                    return buttons;
                });

            }

        });

        converse.initialize( config );
    };

    function exitConversation(ev)
    {
        ev.stopPropagation();
        ev.preventDefault();

        const toolbar_el = converse.env.utils.ancestor(ev.target, 'converse-chat-toolbar');
        console.debug("exitConversation", toolbar_el.model);
        const view = _converse.chatboxviews.get(toolbar_el.model.get("jid"));
        if (view) view.minimize(ev);
    }

    function loadJS(name)
    {
        var s1 = document.createElement('script');
        s1.src ="/assets" + name;
        s1.async = false;
        document.body.appendChild(s1);
    }

    function loadCSS(name)
    {
        var head  = document.getElementsByTagName('head')[0];
        var link  = document.createElement('link');
        link.rel  = 'stylesheet';
        link.type = 'text/css';
        link.href = "/assets" + name;
        head.appendChild(link);
    }

    //-------------------------------------------------------
    //
    //  Startup
    //
    //-------------------------------------------------------

    const div = document.createElement('div');
    const container = "#conversejs .converse-chatboxes {bottom: 45px;}\n";
    const control = "#conversejs.converse-overlayed .toggle-controlbox {display: none;}\n";
    const chatroom = "#conversejs .chat-head-chatroom, #conversejs.converse-embedded .chat-head-chatroom { background-color: #eee; }\n";
    const chatbox = "#conversejs.converse-overlayed #minimized-chats .minimized-chats-flyout .chat-head { background-color: #eee;}";

    div.innerHTML = '<style>' + control + chatroom + chatbox + '</style><div id="conversejs" class="theme-concord"></div>';
    document.body.appendChild(div);

    return api;

}(converse_api || {}));