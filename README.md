# Legacy-Clipboard-Adapter
Enables copying legacy content into desk/editor clipboard.

# Setup
In order to get this plugin to work it is required to follow the following steps:
* Import the plugin from this repository and add it into your plugins directory.
* Add a dependency on the plugin to pom.xml in webapp-polopoly 
```xml
 <dependency>
    <groupId>com.atex.plugins</groupId>
    <artifactId>legacy-clipboard-adapter</artifactId>
    <version>0.1-SNAPSHOT</version>
  </dependency>
``` 

* Add plugin to project level pom.xml
```xml
<module>plugins/legacy-clipboard-adapter</module>
```
* Add a custom.js script to webapp-polopoly/src/main/webapp/script/custom.js
  
  (see below or copy it from legacy-clipboard-adapter/src/main/script/custom.js)

# Custom Script

```javascript
jQuery(document).ready(function() {

    updateSearchFrameEventHandlers();

    jQuery(window).on('click', 'button.clipboard > img', function(){
        setTimeout(clipboardAdapterProcessor, 500)
    });

    jQuery('div.searchbox input.searchInput').keypress(function (e) {
        if (e.which == 13) {
            setTimeout(updateSearchFrameEventHandlers,200);
        }
    });

    jQuery(document).on('click', '.toolbar-button > a.gridViewButton, .toolbar-button > a.listViewButton', function(){
        setTimeout(updateSearchFrameEventHandlers,200);
    });

});

function updateSearchFrameEventHandlers(){
    jQuery('div.copy_button > a > img').add(jQuery('td.copy > div > a > img')).on('click',function() {
        setTimeout(clipboardAdapterProcessor, 500)
    });
}

function clipboardAdapterProcessor() {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', "/polopoly/clipboardAdapter", true);
    xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xhr.send();
}


