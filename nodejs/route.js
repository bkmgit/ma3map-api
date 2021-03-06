var data;//this holds the data to be directly parsed to json
var complete;//this object holds the count to the number of routes done getting their data
var stops;

/**
* This module handles anything relating to a route.
* Functions include getting stops and shape points in a route
*/
function Route(d, s, c){
   //console.log("initializing route with id "+d.route_id);

   //start declaring attributes 
   data = d;
   complete = c;
   stops = s;

   this.setLines();//this will run asynchronously from the main thread
}

/**
* This method returns all data corresponding to a route, i.e:
*  - points that plot out the route
*  - long name
*  - short name
*  - desc
*  - color
*  - text color
*  - lines (list of points plotting out route)
*     - stops in the line
*/
Route.prototype.getData = function(){
   console.log("getting data from %s", data.route_id);
   return data;
};


/**
* This method extracts lines corresponding to this route from the 
* database, and constructs an array of these lines
*/
Route.prototype.setLines = function(){
   
   var Database = require('./database');
   var db = new Database();
   var context = {"data": data, "stops": stops, "complete": complete};

   db.runQuery("select direction_id, shape_id as line_id from \"gtfs_trips\" where route_id = '"+data.route_id+"'", context, function(context, dbData){
      var data = context.data;
      var stops = context.stops;
      var complete = context.complete;
      console.log("route %s has %s lines", data.route_id, dbData.length); 
      var routeComplete = {"count": 0, "size": dbData.length }

      data.lines = new Array();
      var Line = require('./line');
      var uLines = new Array();//store here ids for unique lines      

      for(var lIndex = 0; lIndex < dbData.length; lIndex++){
         var isDuplicate = false;
         if(lIndex != 0){//we get duplicate lines for most if not all lines, prune them outh
            for(var uIndex = 0; uIndex < uLines.length; uIndex++){
               if(uLines[uIndex] == dbData[lIndex].line_id){
                  console.log("duplicate line found");
                  routeComplete.size--;
                  isDuplicate = true;
                  continue;
               }
            }
         }
         
         if(isDuplicate == false){
            uLines.push(dbData[lIndex].line_id);
            var currLine = new Line(dbData[lIndex], stops, complete, routeComplete, data);
         }
      }

//      console.log("done getting data for %s routes", complete.count);
//      complete.count++;
//      complete.data.push(data);
   });
};

/**
* This method gets stops in a line 
*/
Route.prototype.setData = function(){
   var context = {
      "data": data,
      "complete": complete
   };
   
   var Database = require('./database');
   var db = new Database();
   db.runQuery("select * from \"gtfs_stops\"", context, function(context, dbData){
      var data = context.data;
      var complete = context.complete;
      console.log(route);
      route.setLines(data, dbData, complete);//get lines corresponding to this route from db
   });
};

module.exports = Route;
