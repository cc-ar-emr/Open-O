/*
********************************************************
Copyright @ WebOnWebOff.com, by D. Miller
You may reuse this script, on condition that: 
	- this copyright text is kept
	- report improvements/changes to contact(at)WebOnWebOff.com
www.WebOnWebOff.com
********************************************************
*/
ylib.namespace('ylib.util');

ylib.util.isTextChar = function(keyCode){
	if(isNaN(keyCode)) return false;
	switch(keyCode){
		case 10,13: //carriage return
			return true;
		case 127:	//DEL
			return false;
		default:
			if(keyCode>=0&&keyCode<=47) return false;
			if(keyCode>=91&&keyCode<=95) return false;
			if(keyCode>=112&&keyCode<=187) return false;
	}
	return true;
}

ylib.util.FindValueInList = function (sValue, sList) {
    var tValue;
    sValue = sValue.replace(/^\s+|\s+$/g, '');
    
    // Check if sList is a valid array format
    if (sList.indexOf("[") === -1 || sList.indexOf("]") === -1) {
        return sList.replace(/^\s+|\s+$/g, '') === sValue;
    }

    var sArr;
    try {
        sArr = JSON.parse("[" + sList + "]");
    } catch (e) {
        console.error("Invalid input for sList", e);
        return false;
    }

    // Find in list
    for (var i = 0; i < sArr.length; i++) {
        tValue = sArr[i].toString().replace(/^\s+|\s+$/g, '');
        if (tValue === sValue) return true;
    }
    
    return false;
};

