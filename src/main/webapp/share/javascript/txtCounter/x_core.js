/* x_core.js compiled from X 4.0 with XC 0.27b. Distributed by GNU LGPL. For copyrights, license, documentation and more visit Cross-Browser.com */
var xOp7Up, xOp6Dn, xIE4Up, xIE4, xIE5, xNN4, xUA = navigator.userAgent.toLowerCase();
if (window.opera) {
    var i = xUA.indexOf('opera');
    if (i != -1) {
        var v = parseInt(xUA.charAt(i + 6));
        xOp7Up = v >= 7;
        xOp6Dn = v < 7;
    }
} else if (navigator.vendor != 'KDE' && document.all && xUA.indexOf('msie') != -1) {
    xIE4Up = parseFloat(navigator.appVersion) >= 4;
    xIE4 = xUA.indexOf('msie 4') != -1;
    xIE5 = xUA.indexOf('msie 5') != -1;
} else if (document.layers) {
    xNN4 = true;
}
xMac = xUA.indexOf('mac') != -1;

function xBackground(e, c, i) {
    if (!(e = xGetElementById(e))) return '';
    var bg = '';
    if (e.style) {
        if (xStr(c)) {
            if (!xOp6Dn) e.style.backgroundColor = c; else e.style.background = c;
        }
        if (xStr(i)) e.style.backgroundImage = (i != '') ? 'url(' + i + ')' : null;
        if (!xOp6Dn) bg = e.style.backgroundColor; else bg = e.style.background;
    }
    return bg;
}

function xClientHeight() {
    var h = 0;
    if (xOp6Dn) h = window.innerHeight; else if (document.compatMode == 'CSS1Compat' && !window.opera && document.documentElement && document.documentElement.clientHeight) h = document.documentElement.clientHeight; else if (document.body && document.body.clientHeight) h = document.body.clientHeight; else if (xDef(window.innerWidth, window.innerHeight, document.width)) {
        h = window.innerHeight;
        if (document.width > window.innerWidth) h -= 16;
    }
    return h;
}

function xClientWidth() {
    var w = 0;
    if (xOp6Dn) w = window.innerWidth; else if (document.compatMode == 'CSS1Compat' && !window.opera && document.documentElement && document.documentElement.clientWidth) w = document.documentElement.clientWidth; else if (document.body && document.body.clientWidth) w = document.body.clientWidth; else if (xDef(window.innerWidth, window.innerHeight, document.height)) {
        w = window.innerWidth;
        if (document.height > window.innerHeight) w -= 16;
    }
    return w;
}

function xClip(e, t, r, b, l) {
    if (!(e = xGetElementById(e))) return;
    if (e.style) {
        if (xNum(l)) e.style.clip = 'rect(' + t + 'px ' + r + 'px ' + b + 'px ' + l + 'px)'; else e.style.clip = 'rect(0 ' + parseInt(e.style.width) + 'px ' + parseInt(e.style.height) + 'px 0)';
    }
}

function xColor(e, s) {
    if (!(e = xGetElementById(e))) return '';
    var c = '';
    if (e.style && xDef(e.style.color)) {
        if (xStr(s)) e.style.color = s;
        c = e.style.color;
    }
    return c;
}

function xDef() {
    for (var i = 0; i < arguments.length; ++i) {
        if (typeof (arguments[i]) == 'undefined') return false;
    }
    return true;
}

function xDisplay(e, s) {
    if (!(e = xGetElementById(e))) return null;
    if (e.style && xDef(e.style.display)) {
        if (xStr(s)) e.style.display = s;
        return e.style.display;
    }
    return null;
}

function xGetComputedStyle(oEle, sProp, bInt) {
    var s, p = 'undefined';
    var dv = document.defaultView;
    if (dv && dv.getComputedStyle) {
        s = dv.getComputedStyle(oEle, '');
        if (s) p = s.getPropertyValue(sProp);
    } else if (oEle.currentStyle) {
        var a = sProp.split('-');
        sProp = a[0];
        for (var i = 1; i < a.length; ++i) {
            c = a[i].charAt(0);
            sProp += a[i].replace(c, c.toUpperCase());
        }
        p = oEle.currentStyle[sProp];
    } else return null;
    return bInt ? (parseInt(p) || 0) : p;
}

function xGetElementById(e) {
    if (typeof (e) != 'string') return e;
    if (document.getElementById) e = document.getElementById(e); else if (document.all) e = document.all[e]; else e = null;
    return e;
}

function xHasPoint(e, x, y, t, r, b, l) {
    if (!xNum(t)) {
        t = r = b = l = 0;
    } else if (!xNum(r)) {
        r = b = l = t;
    } else if (!xNum(b)) {
        l = r;
        b = t;
    }
    var eX = xPageX(e), eY = xPageY(e);
    return (x >= eX + l && x <= eX + xWidth(e) - r && y >= eY + t && y <= eY + xHeight(e) - b);
}

function xHeight(e, h) {
    if (!(e = xGetElementById(e))) return 0;
    if (xNum(h)) {
        if (h < 0) h = 0; else h = Math.round(h);
    } else h = -1;
    var css = xDef(e.style);
    if (e == document || e.tagName.toLowerCase() == 'html' || e.tagName.toLowerCase() == 'body') {
        h = xClientHeight();
    } else if (css && xDef(e.offsetHeight) && xStr(e.style.height)) {
        if (h >= 0) {
            var pt = 0, pb = 0, bt = 0, bb = 0;
            if (document.compatMode == 'CSS1Compat') {
                var gcs = xGetComputedStyle;
                pt = gcs(e, 'padding-top', 1);
                if (pt !== null) {
                    pb = gcs(e, 'padding-bottom', 1);
                    bt = gcs(e, 'border-top-width', 1);
                    bb = gcs(e, 'border-bottom-width', 1);
                } else if (xDef(e.offsetHeight, e.style.height)) {
                    e.style.height = h + 'px';
                    pt = e.offsetHeight - h;
                }
            }
            h -= (pt + pb + bt + bb);
            if (isNaN(h) || h < 0) return; else e.style.height = h + 'px';
        }
        h = e.offsetHeight;
    } else if (css && xDef(e.style.pixelHeight)) {
        if (h >= 0) e.style.pixelHeight = h;
        h = e.style.pixelHeight;
    }
    return h;
}

function xHide(e) {
    return xVisibility(e, 0);
}

function xLeft(e, iX) {
    if (!(e = xGetElementById(e))) return 0;
    var css = xDef(e.style);
    if (css && xStr(e.style.left)) {
        if (xNum(iX)) e.style.left = iX + 'px'; else {
            iX = parseInt(e.style.left);
            if (isNaN(iX)) iX = 0;
        }
    } else if (css && xDef(e.style.pixelLeft)) {
        if (xNum(iX)) e.style.pixelLeft = iX; else iX = e.style.pixelLeft;
    }
    return iX;
}

function xMoveTo(e, x, y) {
    xLeft(e, x);
    xTop(e, y);
}

function xNum() {
    for (var i = 0; i < arguments.length; ++i) {
        if (isNaN(arguments[i]) || typeof (arguments[i]) != 'number') return false;
    }
    return true;
}

function xOffsetLeft(e) {
    if (!(e = xGetElementById(e))) return 0;
    if (xDef(e.offsetLeft)) return e.offsetLeft; else return 0;
}

function xOffsetTop(e) {
    if (!(e = xGetElementById(e))) return 0;
    if (xDef(e.offsetTop)) return e.offsetTop; else return 0;
}

function xPageX(e) {
    if (!(e = xGetElementById(e))) return 0;
    var x = 0;
    while (e) {
        if (xDef(e.offsetLeft)) x += e.offsetLeft;
        e = xDef(e.offsetParent) ? e.offsetParent : null;
    }
    return x;
}

function xPageY(e) {
    if (!(e = xGetElementById(e))) return 0;
    var y = 0;
    while (e) {
        if (xDef(e.offsetTop)) y += e.offsetTop;
        e = xDef(e.offsetParent) ? e.offsetParent : null;
    }
    return y;
}

function xParent(e, bNode) {
    if (!(e = xGetElementById(e))) return null;
    var p = null;
    if (!bNode && xDef(e.offsetParent)) p = e.offsetParent; else if (xDef(e.parentNode)) p = e.parentNode; else if (xDef(e.parentElement)) p = e.parentElement;
    return p;
}

function xResizeTo(e, w, h) {
    xWidth(e, w);
    xHeight(e, h);
}

function xScrollLeft(e, bWin) {
    var offset = 0;
    if (!xDef(e) || bWin || e == document || e.tagName.toLowerCase() == 'html' || e.tagName.toLowerCase() == 'body') {
        var w = window;
        if (bWin && e) w = e;
        if (w.document.documentElement && w.document.documentElement.scrollLeft) offset = w.document.documentElement.scrollLeft; else if (w.document.body && xDef(w.document.body.scrollLeft)) offset = w.document.body.scrollLeft;
    } else {
        e = xGetElementById(e);
        if (e && xNum(e.scrollLeft)) offset = e.scrollLeft;
    }
    return offset;
}

function xScrollTop(e, bWin) {
    var offset = 0;
    if (!xDef(e) || bWin || e == document || e.tagName.toLowerCase() == 'html' || e.tagName.toLowerCase() == 'body') {
        var w = window;
        if (bWin && e) w = e;
        if (w.document.documentElement && w.document.documentElement.scrollTop) offset = w.document.documentElement.scrollTop; else if (w.document.body && xDef(w.document.body.scrollTop)) offset = w.document.body.scrollTop;
    } else {
        e = xGetElementById(e);
        if (e && xNum(e.scrollTop)) offset = e.scrollTop;
    }
    return offset;
}

function xShow(e) {
    return xVisibility(e, 1);
}

function xStr(s) {
    for (var i = 0; i < arguments.length; ++i) {
        if (typeof (arguments[i]) != 'string') return false;
    }
    return true;
}

function xTop(e, iY) {
    if (!(e = xGetElementById(e))) return 0;
    var css = xDef(e.style);
    if (css && xStr(e.style.top)) {
        if (xNum(iY)) e.style.top = iY + 'px'; else {
            iY = parseInt(e.style.top);
            if (isNaN(iY)) iY = 0;
        }
    } else if (css && xDef(e.style.pixelTop)) {
        if (xNum(iY)) e.style.pixelTop = iY; else iY = e.style.pixelTop;
    }
    return iY;
}

function xVisibility(e, bShow) {
    if (!(e = xGetElementById(e))) return null;
    if (e.style && xDef(e.style.visibility)) {
        if (xDef(bShow)) e.style.visibility = bShow ? 'visible' : 'hidden';
        return e.style.visibility;
    }
    return null;
}

function xWidth(e, w) {
    if (!(e = xGetElementById(e))) return 0;
    if (xNum(w)) {
        if (w < 0) w = 0; else w = Math.round(w);
    } else w = -1;
    var css = xDef(e.style);
    if (e == document || e.tagName.toLowerCase() == 'html' || e.tagName.toLowerCase() == 'body') {
        w = xClientWidth();
    } else if (css && xDef(e.offsetWidth) && xStr(e.style.width)) {
        if (w >= 0) {
            var pl = 0, pr = 0, bl = 0, br = 0;
            if (document.compatMode == 'CSS1Compat') {
                var gcs = xGetComputedStyle;
                pl = gcs(e, 'padding-left', 1);
                if (pl !== null) {
                    pr = gcs(e, 'padding-right', 1);
                    bl = gcs(e, 'border-left-width', 1);
                    br = gcs(e, 'border-right-width', 1);
                } else if (xDef(e.offsetWidth, e.style.width)) {
                    e.style.width = w + 'px';
                    pl = e.offsetWidth - w;
                }
            }
            w -= (pl + pr + bl + br);
            if (isNaN(w) || w < 0) return; else e.style.width = w + 'px';
        }
        w = e.offsetWidth;
    } else if (css && xDef(e.style.pixelWidth)) {
        if (w >= 0) e.style.pixelWidth = w;
        w = e.style.pixelWidth;
    }
    return w;
}

function xZIndex(e, uZ) {
    if (!(e = xGetElementById(e))) return 0;
    if (e.style && xDef(e.style.zIndex)) {
        if (xNum(uZ)) e.style.zIndex = uZ;
        uZ = parseInt(e.style.zIndex);
    }
    return uZ;
}
