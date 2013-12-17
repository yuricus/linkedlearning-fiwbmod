/**
 * Created with IntelliJ IDEA.
 * User: Kivan
 * Date: 16.09.13
 * Time: 17:40
 * To change this template use File | Settings | File Templates.
 */

var pathTo;
var Oclasses = null;
var arrayOfClassVals = null;
var arrayOfClassKeys = null;
var objectProperties = null;
var arrayOfObjectPropVals = null;
var arrayOfObjectPropKeys = null;
var dataTypeProperties = null;
var arrayOfDataPropsVals = null;
function startIt(divId, pathToX) {
    pathTo = pathToX;
    var tooltiper = kiv.tooltip("tooltip");
    var ontologyViewerTree = kiv.graphStuff.ontologyViewerTree({containerid:divId});
    var allClassesQuery = getRequestToAllClasses();
    var allObjectPropsQuery = getRequestToAllObjProps();
    var allDataPropsQuery = getRequestToAllDataProps();
    var queries = [allClassesQuery, allObjectPropsQuery, allDataPropsQuery];
    processAllQueriesAndGetResult(queries, endpoint, processRequest);

    function processRequest(listOfObjects) {
        Oclasses = sparqlJSONToObject(listOfObjects[0], "class");
        arrayOfClassVals = objToArrayValues(Oclasses);
        arrayOfClassKeys = objToArrayKeys(Oclasses);
        objectProperties = sparqlJSONToObject(listOfObjects[1], "objectProperty");
        arrayOfObjectPropVals = objToArrayValues(objectProperties);
        arrayOfObjectPropKeys = objToArrayKeys(objectProperties);
        dataTypeProperties = sparqlJSONToObject(listOfObjects[2], "dataProperty");
        arrayOfDataPropsVals = objToArrayValues(dataTypeProperties);
        renderEditor(pathTo);
    }

    function renderEditor(objectId) {
        ontologyViewerTree.render({idOfInstance: objectId, requestString: getRequestToInstance(objectId)});
    }
}

function getSomeObjectColor(objId, listWhereItExists) {
    var index = listWhereItExists.indexOf(objId);
    if (index == -1) return 'black';
    var size = listWhereItExists.length;
    var hslVal = index / size;
    var v = function (d, min, max, numoftimes) {
        var mult = (max - min) / (numoftimes);
        return min + mult * (d % numoftimes);
    };

    return d3.hsl(Math.round(hslVal * 360), v(index * index, 0.2, 0.8, 7), v(index, 0.3, 0.6, 3)).toString();
}






