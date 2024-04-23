//获取内容高度
var height1 = document.documentElement.scrollHeight;
var height2 = window.innerHeight;
var height3 = document.documentElement.clientHeight;
var height4 = document.body.clientHeight;
var allHeight = height1+ " "+ height2+ " "+height3+ " "+height4
//let height = "hh";
browser.runtime.sendNativeMessage("browser", height1);
//let manifest = document.querySelector("head > link[rel=manifest]");
//if (manifest) {
//     fetch(manifest.href)
//        .then(response => response.json())
//        .then(json => {
//             let message = {type: "WPAManifest", manifest: json};
//             browser.runtime.sendNativeMessage("browser", message);
//        });
//}