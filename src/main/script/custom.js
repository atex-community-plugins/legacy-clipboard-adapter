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


