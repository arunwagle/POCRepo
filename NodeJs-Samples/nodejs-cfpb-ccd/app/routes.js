var mongoose   = require('mongoose');

var databaseURI = 'mongodb://cfpbAdmin:cfpbAdmin@aws-us-east-1-portal.19.dblayer.com:11490/CFPB';
mongoose.connect(databaseURI);
var db = mongoose.connection;

db.once("open", function(callback){
  console.log("Connected");
})

var Schema = mongoose.Schema;

var ConsumerComplaintsSchema = new Schema({
    company: String,
    product: String,
    sub_product: String,
    issue: String,
    state: String
});

// .select({'_id': 0, 'complaint_id': 1, 'year': 1, 'month': 1, 'day': 1, 'received_date': 1
// , 'product': 1, 'issue': 1, 'company': 1, 'state': 1});
Schema = mongoose.Schema;
var TSComplaintsSchema = new Schema({
  complaint_id: String
});

// var db = require('config/mongo-db');


// var Subjects = require('./models/SubjectViews');
// console.log("##### Subjects ######" + Subjects);

module.exports = function(app) {

	// server routes ===========================================================
	// handle things like api calls
	// authentication routes
	// sample api route
 app.get('/api/data', function(req, res) {

  console.log("##### Calling API Data ###### ");

  // Connect to the database
  console.log("##### Connection state: ###### " + mongoose.connection.readyState);


    if(mongoose.connection.readyState == 1) {
    console.log("##### CONNECTED TO MONGO ######"  );

    var Subjects =  mongoose.model('consumer_complaints', ConsumerComplaintsSchema, 'consumer_complaints');
    console.log("##### Subjects: ######" + Subjects);
    // use mongoose to get all data in the database
    Subjects.find({}, function(err, subjectDetails) {

      // console.log("##### subjectDetails ######" + subjectDetails);
       // if there is an error retrieving, send the error.
           // nothing after res.send(err) will execute
       if (err) res.send(err);
        res.json(subjectDetails);
    })
    .limit(20)
    .select({'_id': 0, 'company': 1, 'product': 1, 'sub_product': 1, 'issue': 1, 'state': 1});

  }

 });

 app.get('/api/ts/data', function(req, res) {
    console.log("##### Calling TS API Data ######");

    console.log("##### TS API Connection state: ###### " + mongoose.connection.readyState);

    if(mongoose.connection.readyState == 1) {
      console.log("##### TS API CONNECTED TO MONGO ######"  );

      var TSComplaintsQuery =  mongoose.model('monthly_consumer_complaints', TSComplaintsSchema);
      console.log("##### TSComplaintsQuery: ######" + TSComplaintsQuery);
      // use mongoose to get all data in the database
      TSComplaintsQuery.find({}, function(err, tsDetails) {

        // console.log("##### tsDetails ######" + tsDetails);
         // if there is an error retrieving, send the error.
             // nothing after res.send(err) will execute
        if (err) res.send(err);
        res.json(tsDetails);
      })
      .where('company').in(['Wells Fargo & Company', 'Santander Bank US', 'Bank of America', 'JPMorgan Chase & Co.', 'Capital One', 'Citibank', 'TD Bank US Holding Company'])
      .limit(50000)
      // .limit(100000)
      .select({'_id': 0, 'complaint_id': 1, 'year': 1, 'month': 1, 'day': 1, 'received_date': 1
      , 'product': 1, 'issue': 1, 'company': 1, 'state': 1});

    };
  });





//  // frontend routes =========================================================
//  app.get('*', function(req, res) {
//   res.sendfile('./public/login.html');
//  });
}
