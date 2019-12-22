[![Build Status](https://travis-ci.org/grinfeld/redirect-to-stream.svg?branch=master)](https://travis-ci.org/grinfeld/redirect-to-stream)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/grinfeld/redirect-to-stream.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/grinfeld/redirect-to-stream/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/grinfeld/redirect-to-stream.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/grinfeld/redirect-to-stream/alerts)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.mikerusoft%3Aredirect-to-stream&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.mikerusoft%3Aredirect-to-stream)

Redirect-To-Stream
==========================

Small web server that receives http requests and redirect them to any (reactive) subscriber

and few other useful for tests use cases

The original purpose is - writing integration tests.

### Subscribers flow

Supported GET and POST

Tests subscribe to possible 4 end-points (supporting only: application/x-json-stream for streaming or single requests with application/json) :
* /subscribe/http/all - redirects to subscriber any request received by server
* /subscribe/http/uri/{uri} - filters requests by URI
* /subscribe/http/method/{method} - filters requests by http method
* /subscribe/http/filter/{method}/{uri} - filters requests by uri and http method

Service (or some pipeline that ends with sending HttpRequest) we want to test should send/redirect requests to:
* /receive/get/{any uri}
* /receive/post/{any uri}
* /receive/put/{any uri}

### Test different HttpStatuses

There are 2 options: 

1. global
2. per endpoint

#### Global

We defined 2 parameters: 
status (i.e. different from 200) and change frequency counter (a.k.k.a how often we return status)

Default values are: for status = 404 and freqCounter = 10 - means every 10th request server returns status 404

Initial values defined in application.yml file (but it could be changed on the fly - see later) 

Supported GET and POST

End Point to send requests to:  */status/global/{any uri suffix}*

End Point to change default global:  */global/change/status/{status}/freq/{freq}* - change of this values will affect anyone who uses end point above

#### Per Endpoint

Same logic, but you can define status and change frequency inside end point url

*/status/{status}/freq/{freq}/{/get:.*}* - self explained (if not open me bug :) )