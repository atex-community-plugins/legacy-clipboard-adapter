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

