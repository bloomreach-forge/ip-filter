<%--
  ~ Copyright 2017 Hippo B.V. (http://www.onehippo.com)
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~  http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%@ page isErrorPage="true" %>
<% response.setStatus(404); %>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <title>404 error</title>
</head>
<body>
<h2>Welcome to Hippo</h2>
<p>
  It appears that you just created an empty Hippo project from the archetype. There is nothing to show on the site yet.
  We recommend you use
  <a href="http://<%=request.getServerName() + ':' + request.getServerPort() + "/essentials"%>" target="_blank">Hippo's setup application</a>
  to start developing your project.
</p>
</body>
</html>