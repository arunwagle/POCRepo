# Consumer Financial Protection Bureau(ccpb) - Consumer Complaint Database (ccd)
<h2>Source code for the project demonstrating data visualization using d3.js, dc.js, node.js and mongodb<h2><br/>

For the full post please visit: <br/>
<h2></h2>

Required Components:<br/>
D3.js<br/>
Dc.js<br/>
Node.js<br/>
Crossfilter.js<br/>
Jquery<br/>
MongoDB<br/>

Steps for successful execution:<br/>
1. Create Database in Compose Mongo DB
  a. Import Consumer_Complaints.csv into Compose mongo DB(cfpb database)
  mongoimport --host=aws-us-east-1-portal.19.dblayer.com:11490 -d CFPB -c consumer_complaints -u cfpbAdmin -p cfpbAdmin  --type csv --headerline --file /Users/arunwagle/Bluemix/Projects/Personal/NodeJs-Samples/nodejs-cfpb-ccd/sample-dataset/Consumer_Complaints.csv

2.NodeJS setup
  npm init <br/>
  npm adduser <br/>
  npm publish (optional) <br/>
  npm	install express --save <br/>
  npm install body-parser --save <br/>
  npm install cookie-parser --save <br/>
  npm install multer --save <br/>
  npm install mongoose --save <br/>
  npm install method-override --save  </br>


3. Deploy to Bluemix
    Connect to Bluemix:
		cf api https://api.ng.bluemix.net
    <br/>

    Log into Bluemix and set your target org when prompted:
		cf login
    </br>

    Deploy your app. For example:
		cf push nodejs-cfpb-ccd

4. Run the application
