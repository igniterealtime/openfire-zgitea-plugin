function lS(src) {
return new Promise(function (resolve, reject) {
  let s = document.createElement("script");
  s.src = src;
  s.addEventListener("load", () => {
	resolve();
  });
  document.body.appendChild(s);
});
}

if ($('a.ui.mini.basic.button[href$=".adaptivecard"]').length) {
$("body").append(
  '<link href="/assets/adaptive-cards/adaptivecards.css" rel="stylesheet">'
);
Promise.all([
	lS("/assets/adaptive-cards/adaptivecards.min.js"),
	lS("/assets/adaptive-cards/markdown-it.min.js"),
]).then(function () {
	$(".view-raw").attr("id", "view-raw").attr("style", "padding: 0;margin-bottom: -10px;");
  
	const url = $('a.ui.mini.basic.button[href$=".adaptivecard" i]').attr("href");
    const options = {method: "GET", headers: {"accept": "application/json"}};	

	console.debug("adapative fetching json", url, options);		
	
	fetch(url, options).then(function(response){ return response.json()}).then(function(response)
	{
		console.debug("adapative fetch response", response);		
		const adaptiveCard = new AdaptiveCards.AdaptiveCard();
		
		adaptiveCard.onExecuteAction = function(action) { 
			console.debug("adaptiveCard.onExecuteAction", action) 
			
			if (action._propertyBag?.title) {
				let data = {};
				
				if (action._processedData && Object.getOwnPropertyNames(action._processedData).length > 0) {
					data = action._processedData;
					console.debug("action data", data);
				}
				
				//sendCardResult(action._propertyBag?.title, JSON.stringify(data)).then(resp => resultsSaved(resp, action._propertyBag?.title)).catch(err => resultsError(err, action._propertyBag?.title));
			}
		}
			
		adaptiveCard.parse(response);
		const renderedCard = adaptiveCard.render();

		const viewer = document.querySelector(".file-view.code-view");
		viewer.innerHTML = "";
		document.querySelector(".file-view.code-view").appendChild(renderedCard);		

	}).catch(function (err) {
		console.error("adapative fetch error", err);
	});	

  //$('.view-raw>a[href$=".adaptivecard"]').remove();
});
}