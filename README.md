[![Build Status](https://travis-ci.org/grinfeld/redirect-to-stream.svg?branch=master)](https://travis-ci.org/grinfeld/redirect-to-stream)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/grinfeld/redirect-to-stream.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/grinfeld/redirect-to-stream/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/grinfeld/redirect-to-stream.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/grinfeld/redirect-to-stream/alerts)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.mikerusoft%3Aredirect-to-stream&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.mikerusoft%3Aredirect-to-stream)

Redirect-To-Stream
==========================

Small web server that receives http requests and redirect them to any (reactive) subscriber

The original purpose is - writing integration tests.

Tests subscribe to possible 4 end-points:
* /retrieve/all - redirects to subscriber any request received by server
* /retrieve/uri/{uri} - filters requests by URI
* /retrieve/method/{method} - filters requests by http method
* /retrieve/filter/{method}/{uri} - filters requests by uri and http method

Service (or some pipeline thta ends with sending HttpRequest) we want to test should send/redirect requests to:
* /receive/get/{any uri}
* /receive/post/{any uri}
* /receive/put/{any uri}

