<html>
<head>
    <title>[@ui.header pageKey='Coverage' object='${build.name} ${buildResults.buildNumber}' title=true /]</title>
    <meta name="decorator" content="result">
    <meta name="tab" content="sauce"/>
</head>
<body>
[@cp.resultsSubMenu selectedTab='sauce' /]

[#if jobId?has_content ]

<script type="text/javascript" src="http://saucelabs.com/job-embed/${jobId}.js?auth=${hmac}"></script>

[#--<script type="text/javascript" src="http://saucelabs.com/video-embed/${jobId}.js"></script>--]

[#else]

<p>
Unable to find a Sauce Job result for ${buildKey}.
</p>

<p>Please verify that your Sauce tests are applying the value of the SAUCE_CUSTOM_DATA environment variable to the
selenium context, eg.
</p>
<pre>
String bambooData = System.getProperty("SAUCE_CUSTOM_DATA");
this.selenium.setContext(bambooData);
</pre>



[/#if]
</body>
</html>