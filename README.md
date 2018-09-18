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
    jQuery(window).click(function(e){
        if($(e.target).parent("button.clipboard")){
            setTimeout(clipboardAdapterProcessor, 500)
        }
    });
});

function clipboardAdapterProcessor() {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', "/polopoly/clipboardAdapter", true);
    xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xhr.send();
}


