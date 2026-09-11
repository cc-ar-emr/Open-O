<%--

    Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
    This software is published under the GPL GNU General Public License.
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    This software was written for the
    Department of Family Medicine
    McMaster University
    Hamilton
    Ontario, Canada

--%>
<%--
    Landing page after a ONE ID sign-out completes at the identity provider. The End Session redirect
    targets this page only for users with EHR service access, so it shows the residual-PHI reminder to
    exactly those users without needing a session. Styles are inline because static assets are not
    served to logged-out users.

    @since 2026-07-02
--%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Signed out</title>
    <style media="all">
        body {
            margin: 0;
            font-family: 'Roboto', Helvetica, Arial, sans-serif;
            font-size: 16px;
            color: #333333;
            background-color: #ffffff;
        }

        .wrap {
            max-width: 560px;
            margin: 60px auto;
            padding: 0 20px;
        }

        h1 {
            font-size: 24px;
            font-weight: 300;
            margin: 0 0 20px;
        }

        .notice {
            background-color: #fff3cd;
            border: 1px solid #ffe69c;
            color: #664d03;
            border-radius: 6px;
            padding: 16px;
            margin-bottom: 24px;
        }

        a.btn {
            display: inline-block;
            background-color: #0d6efd;
            color: #ffffff;
            text-decoration: none;
            padding: 8px 18px;
            border-radius: 6px;
        }
    </style>
</head>
<body>
<div class="wrap">
    <h1>You have signed out of OpenO</h1>
    <div class="notice">
        Any browser windows you opened during your session may still be open. To protect patient
        privacy, close every browser window containing personal health information (PHI) now.
    </div>
    <a class="btn" href="${pageContext.request.contextPath}/index.jsp">Return to login</a>
</div>
</body>
</html>
