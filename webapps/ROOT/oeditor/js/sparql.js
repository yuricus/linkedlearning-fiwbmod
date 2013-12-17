/**
 * Created with IntelliJ IDEA.
 * User: Kivan
 * Date: 04.07.13
 * Time: 13:34
 * To change this template use File | Settings | File Templates.
 *
 * Запросы и вспомогательные функции для формирования запросов к спарклу
 */


var endpoint = window.location.protocol + "//" + window.location.host + "/sparql";

/**
 * Берет параметры из формы и присваивает переменной objPaths, разбивая по запятым
 * @returns {Array} - список префиксов
 */
function getObjPathsToCurrent() {
    var objPaths = [];
    var text = $("#prefixList").val();
    var prefixes = text.split(",");
    for (var index in prefixes) {
        var a = prefixes[index].trim();
        if (a != '')objPaths.push(a);
    }
    return objPaths;
}
/**
 * Формирует запрос на взятие всех классов соответствующих префиксам неймспейса
 */
function getRequestToAllClasses() {
    var objName = 'class';
    var objPaths = getObjPathsToCurrent();
    return "select distinct ?class ?subclass ?label ?objIN ?objOUT ?dataProp (COUNT(distinct ?s) AS ?count ) where {\
        ?class rdf:type ?clstype.\
        {?s a ?class} \
        UNION {?class rdfs:subClassOf ?subclass.}\
        UNION { ?class rdfs:label ?label } \
        UNION { ?objIN a owl:ObjectProperty. ?objIN rdfs:range ?class.}\
        UNION { ?objIN a owl:ObjectProperty. ?objIN rdfs:range _:bb. _:bb owl:unionOf _:b. _:b rdf:rest* /rdf:first ?class.}\
        UNION { ?objOUT a owl:ObjectProperty. ?objOUT rdfs:domain ?class. }\
        UNION { ?objOUT a owl:ObjectProperty. ?objOUT rdfs:domain  _:cc. _:cc owl:unionOf _:c. _:c rdf:rest* /rdf:first ?class.}\
        UNION { ?dataProp a owl:DatatypeProperty. ?dataProp rdfs:domain ?class. }\
        UNION { ?dataProp a owl:DatatypeProperty. ?dataProp rdfs:domain _:dddmn. _:dddmn owl:unionOf _:d. _:d rdf:rest* /rdf:first ?class.}\
        " + createFilterString([ {objPaths: objPaths, objName: objName},{objPaths: [".Class"], objName: "clstype"}]) + " \
    } group by ?class ?subclass ?label ?objIN ?objOUT ?dataProp";
}

/**
 * Формирует запрос на взятие всех object properties соответствующих префиксам неймспейса
 */
function getRequestToAllObjProps() {
    var objName = 'objectProperty';
    var objPaths = getObjPathsToCurrent();
    return "select distinct ?objectProperty ?label ?domain ?range where {\
        ?objectProperty rdf:type owl:ObjectProperty.\
        { ?objectProperty rdfs:label ?label }\
        UNION { ?objectProperty rdfs:domain ?domain. FILTER(NOT EXISTS{?domain owl:unionOf _:x})}\
        UNION { ?objectProperty rdfs:domain _:a. _:a owl:unionOf _:b. _:b rdf:rest* /rdf:first ?domain. }\
        UNION { ?objectProperty  rdfs:range ?range. FILTER(NOT EXISTS{?range owl:unionOf _:y}) }\
        UNION { ?objectProperty  rdfs:range _:d. _:d owl:unionOf _:c. _:c rdf:rest* /rdf:first ?range. }\
        " + createFilterString([
        {objPaths: objPaths, objName: objName}
    ]) + "\
    }";
}
/**
 * Формирует запрос на взятие всех data properties соответствующих префиксам неймспейса
 */
function getRequestToAllDataProps() {
    var objName = 'dataProperty';
    var objPaths = getObjPathsToCurrent();
    return "select distinct ?dataProperty ?label ?domain ?range where {\
        ?dataProperty rdf:type owl:DatatypeProperty.\
        { ?dataProperty rdfs:label ?label }\
        UNION { ?dataProperty rdfs:domain ?domain. FILTER(NOT EXISTS{?domain owl:unionOf _:x}) }\
        UNION { ?dataProperty rdfs:domain _:a. _:a owl:unionOf _:b. _:b rdf:rest* /rdf:first ?domain.}\
        UNION { ?dataProperty  rdfs:range ?range. FILTER(NOT EXISTS{?range owl:unionOf _:y}) }\
        UNION { ?dataProperty  rdfs:range _:d. _:d owl:unionOf _:c. _:c rdf:rest* /rdf:first ?range. }\
        " + createFilterString([
        {objPaths: objPaths, objName: objName}
    ]) + "\
    }";
}

/**
 * Формирует запрос на взятие всех инстансов некоторого класса.
 * @param classid идентификатор класса
 * @returns {string}
 */
function getRequestToAllInstsIdsByClass(classid) {
    return "select distinct ?instance ?label where{\
        ?instance a <"+classid+">.\
        OPTIONAL {?instance rdfs:label ?label.}\
    }";
}
/**
 * Формирует запрос на взятие информации об инстансе
 * @param objid идентификатор инстанса
 * @returns {string}
 */
function getRequestToInstance2(objid){
    return "select distinct ?object ?class ?aclass ?label ?dataProperty ?dpropertyValue ?objpout ?objpoutval ?objpinval ?objpin where { \
    {\
        ?object rdfs:label ?aclass. \
        {<"+objid+"> rdf:type ?class. \
            OPTIONAL {<"+objid+"> rdfs:label ?label.} }\
        UNION { <"+objid+"> ?dataProperty ?dpropertyValue. ?dataProperty a owl:DatatypeProperty} \
        UNION { <"+objid+"> ?objpout ?objpoutval. ?objpout a owl:ObjectProperty} \
        UNION { ?objpinval ?objpin <"+objid+">. ?objpin a owl:ObjectProperty}\
        FILTER (regex(str(?object),'^"+objid+"$')) \
        } \
        UNION \
        {   <"+objid+"> ?temp1 ?object. \
             ?temp1 a owl:ObjectProperty. \
            { ?object rdf:type ?class.\
                OPTIONAL {?object rdfs:label ?label.}}\
            UNION { ?object ?dataProperty ?dpropertyValue.\
                ?dataProperty a owl:DatatypeProperty}\
                }\
         UNION {\
        ?object ?temp2 <"+objid+">.\
        ?temp2 a owl:ObjectProperty.\
        { ?object rdf:type ?class. OPTIONAL {?object rdfs:label ?label.}}\
        UNION { ?object ?dataProperty ?dpropertyValue. ?dataProperty a owl:DatatypeProperty} }\
}"
}

function getRequestToInstance(objid) {
    return "select distinct ?object ?class ?aclass ?label ?dataProperty ?dpropertyValue ?objpout ?objpoutval ?objpinval ?objpin where                      \n\
            	{                                                                                                                                          \n\
            		{select (<"+objid+"> as ?object) ?class ?aclass ?label ?dataProperty ?dpropertyValue ?objpout ?objpoutval ?objpinval ?objpin where     \n\
            		{ <"+objid+"> rdfs:label ?aclass.                                                                                                      \n\
            			{<"+objid+"> rdf:type ?class.                                                                                                      \n\
            			 OPTIONAL {<"+objid+"> rdfs:label ?label. }                                                                                        \n\
            			} UNION { <"+objid+"> ?dataProperty ?dpropertyValue. ?dataProperty a owl:DatatypeProperty}                                         \n\
            			  UNION { <"+objid+"> ?objpout ?objpoutval. ?objpout a owl:ObjectProperty}                                                         \n\
            			  UNION { ?objpinval ?objpin <"+objid+">. ?objpin a owl:ObjectProperty}                                                            \n\
            		}}                                                                                                                                     \n\
            			UNION                                                                                                                              \n\
            		{select ?object ?class ?aclass ?label ?dataProperty ?dpropertyValue ?objpout ?objpoutval ?objpinval ?objpin where {	                   \n\
                    {			<"+objid+"> ?temp1 ?object.                                                                                                \n\
                    				?temp1 a owl:ObjectProperty.                                                                                           \n\
                    				{ ?object rdf:type ?class. OPTIONAL {?object rdfs:label ?label.}}                                                      \n\
                    				UNION { ?object ?dataProperty ?dpropertyValue. ?dataProperty a owl:DatatypeProperty}                                   \n\
                    }				                                                                                                                       \n\
                    				UNION { ?object ?temp2 <"+objid+">.                                                                                    \n\
                    					?temp2 a owl:ObjectProperty.                                                                                       \n\
                    					{ ?object rdf:type ?class. OPTIONAL {?object rdfs:label ?label.}}                                                  \n\
                    			UNION { ?object ?dataProperty ?dpropertyValue. ?dataProperty a owl:DatatypeProperty}                                       \n\
                    			}                                                                                                                          \n\
                    }}                                                                                                                                     \n\
            	}"
}

/**
 * Создаем фильтры связанные по OR
 * @param filteringArray   [{objPaths:objPaths, objName:objName}, ....]
 * @returns {string}
 */
function createFilterString(filteringArray) {
    var toRet = "FILTER ( ";
    for (var index in filteringArray) {
        var filter = filteringArray[index];
        var part = createRegexSequence(filter['objPaths'], filter['objName']);
        if (part != "") {
            if (index != 0) toRet += " && ";
            toRet += part;
        }
    }
    return toRet + " ) ";
}

function createRegexSequence(paths, nameInRequest) {
    if (paths == null || paths.length == 0) return "";
    var part = " ( ";
    for (var index in paths) {
        part += " regex(str(?" + nameInRequest + "),'" + paths[index] + "') ";
        if (index != paths.length - 1) part += " || ";
    }
    part += " ) ";
    return part;
}

