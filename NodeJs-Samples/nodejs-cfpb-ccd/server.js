// modules =================================================
var express        = require('express');
var app            = express();
var mongoose       = require('mongoose');
var bodyParser     = require('body-parser');
var methodOverride = require('method-override');



// Database connection parameters
// var db = require('./config/mongo-db');

// connect to our mongoDB database (commented out after you enter in your own credentials)
// console.log("##### db.urlSubjectViews ######" + db.urlSubjectViews);
// mongoose.connect(db.urlSubjectViews);
//
// mongoose.connection.on('connected', function () {
//   connectionsubject = mongoose.connection;
//   console.log("##### connectionsubject readyState ######" + connectionsubject.readyState + " connectionsubject:" + connectionsubject);
// });


// get all data/stuff of the body (POST) parameters
app.use(bodyParser.json()); // parse application/json
app.use(bodyParser.json({ type: 'application/vnd.api+json' })); // parse application/vnd.api+json as json
app.use(bodyParser.urlencoded({ extended: true })); // parse application/x-www-form-urlencoded

app.use(methodOverride('X-HTTP-Method-Override')); // override with the X-HTTP-Method-Override header in the request. simulate DELETE/PUT

// Load static data: js, css, img, htmls
app.use(express.static(__dirname + '/public')); // set the static files location /public/img will be /img for users


// routes ==================================================
require('./app/routes')(app); // pass our application into our routes

// Start the app using some of the values from process.env, i.e. the IP address // and port of the Cloud Foundry DEA (Droplet Execution Agent) that hosts this
// application
var host = (process.env.VCAP_APP_HOST || 'localhost');
var port = (process.env.VCAP_APP_PORT || 3000);
app.listen(port, host);
console.log('Magic happens on port ' + port + " host=" + host); 			// shoutout to the user
exports = module.exports = app; 						// expose app
