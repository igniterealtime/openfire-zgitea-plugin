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

if ($('.view-raw>a[href$=".stl" i]').length) {
$("body").append(
  '<link href="/assets/madeleine.js/src/css/Madeleine.css" rel="stylesheet">'
);
Promise.all([
  lS("/assets/madeleine.js/src/lib/stats.js"),
  lS("/assets/madeleine.js/src/lib/detector.js"),
  lS("/assets/madeleine.js/src/lib/three.min.js"),
  lS("/assets/madeleine.js/src/madeleine.js"),
]).then(function () {
  $(".view-raw")
	.attr("id", "view-raw")
	.attr("style", "padding: 0;margin-bottom: -10px;");
  new Madeleine({
	target: "view-raw",
	data: $('.view-raw>a[href$=".stl" i]').attr("href"),
	path: "/assets/madeleine.js/src",
  });
  $('.view-raw>a[href$=".stl"]').remove();
});
}