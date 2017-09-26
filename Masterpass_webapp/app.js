const express = require('express')
const app = express()

app.get('/pay', function (req, res) {
  console.log('GET / HEAD request received')
  res.set('Link', '<payment-method-manifest.json>; rel="payment-method-manifest"');
  res.send('Success!')
})

app.use(express.static('public'))

app.listen(process.env.PORT || 3000, function () {
  console.log('Example app listening!')
})
 
/*'use strict';
 
console.log('Loading function');
 
exports.handler = (event, context, callback) => {
    //console.log('Received event:', JSON.stringify(event, null, 2));
console.log("success");
context.succeed({"statusCode": 200,
"body": "success","headers": {'Link':'<payment-method-manifest.json>; rel="payment-method-manifest"'}});
};*/