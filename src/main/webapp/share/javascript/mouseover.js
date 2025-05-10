  
//    This script and many more are available free online at
//    The JavaScript Source!! http://javascript.internet.com
//    Original:  Patrick Lewis (gtrwiz@aol.com)
//    Web Site:  http://www.patricklewis.net


let mouseOverX;
let mouseOverY;
var overdiv = "0";

//  #########  CREATES POP UP BOXES
function popLayer(content) {
    let pad = "1";
    let bord = "0";

    // Get the popup container
    let popup = document.getElementById("object1");
    popup.style.left = (mouseOverX + 15) + "px";
    popup.style.top = (mouseOverY - 5) + "px";

    // Clear existing content
    popup.textContent = "";

    // Create outer table
    let outerTable = document.createElement("table");
    outerTable.cellSpacing = "0";
    outerTable.cellPadding = pad;
    outerTable.border = bord;
    outerTable.style.backgroundColor = "#000000";

    let outerRow = document.createElement("tr");
    let outerCell = document.createElement("td");

    // Create inner table
    let innerTable = document.createElement("table");
    innerTable.cellSpacing = "0";
    innerTable.cellPadding = "3";
    innerTable.border = "0";
    innerTable.width = "100%";

    let innerRow = document.createElement("tr");
    let innerCell = document.createElement("td");
    innerCell.style.backgroundColor = "#ffffdd";
    innerCell.style.textAlign = "center";

    // Add content safely
    let textNode = document.createTextNode(content);
    innerCell.appendChild(textNode);

    // Build tables
    innerRow.appendChild(innerCell);
    innerTable.appendChild(innerRow);
    outerCell.appendChild(innerTable);
    outerRow.appendChild(outerCell);
    outerTable.appendChild(outerRow);

    // Append to popup container
    popup.appendChild(outerTable);
}

function hideLayer() {
    if (overdiv == "0") {
        document.getElementById("object1").style.top = "-500px";
    }
}

//  ########  TRACKS MOUSE POSITION FOR POPUP PLACEMENT
function handlerMM(e) {
    mouseOverX = event.clientX + document.body.scrollLeft;
    mouseOverY = event.clientY + document.body.scrollTop;
}

document.captureEvents(Event.MOUSEMOVE);
document.onmousemove = handlerMM;

