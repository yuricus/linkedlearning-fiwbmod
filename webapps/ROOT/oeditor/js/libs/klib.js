/**
 * Created with IntelliJ IDEA.
 * User: Kivan
 * Date: 18.06.13
 * Time: 14:13
 * To change this template use File | Settings | File Templates.
 */
kiv = {};
kiv.graphStuff = {};
//-----------------------------------------------------------------------------------------
//-----------------------------------------GRAPH
//-----------------------------------------------------------------------------------------
kiv.graphStuff.graph = function (outer, linesFirst) {
    //Линии рисуются первыми
    linesFirst = (arguments.length == 2) ? linesFirst : 0;
    //Внешний элемент
    var outerElement = outer,
    //Функция вызывается в начале перерисовки
        rebuildSVGStart = function () {
        },
    //Функция вызывается в конце перерисовки
        rebuildSVGEnd = function () {
        },
    //Функции создания, обновления и удаления линков и нодов
        linkCreator = function (link) {
            link.append('path').attr('stroke', 'black').attr('fill', 'none').attr('stroke-width', function (d, i) {
                return '2px';
            })
        },
        linkUpdater = function (link) {
            link.select('path').attr("d", function (d, i) {
                return 'M' + d.source.x + ',' + d.source.y + 'L' + d.target.x + ',' + d.target.y
            })
        },
        linkRemover = function (link) {
            link.remove()
        },
        nodeCreator = function (node) {
            node.append('circle').attr("r", 5)
        },
        nodeUpdater = function (node) {
            node.attr('transform', function (d, i) {
                return 'translate(' + d.x + ',' + d.y + ')';
            })
        },
        nodeRemover = function (node) {
            node.remove()
        },
    //Это контейнер с линками. Линки задаются пафами
        linksOnSVG = false,
    //Это контейнер с нодами. Ноды задаются группами элементов
        nodesOnSVG = false,
    //Это реальные ноды (в примере обязательные поля, которые будут использоваться)
        nodes = [
            {x: 0, y: 0},
            {x: 5, y: 5}
        ],
    //Это реальные линки (в примере обязательные поля, которые будут использоваться)
        links = [
            {source: nodes[0], target: nodes[1]}
        ],
    //Функции мапинга для линков и нодов
        keyFuncLinks,
        keyFuncNodes,
    //Функции для удаления линков и нодов
        deleteLink = function (link) {
            links.splice(links.indexOf(link), 1);
        },
        deleteNode = function (node) {
            spliceNodes(node);
        },
        updatePositions = function (updateInfo) {
            defaultTick(updateInfo)
        }
        ;

    function graph() {
    }

    graph.rebuildSVG = function () {

        rebuildSVGStart();
        if (!linksOnSVG) {
            if (linesFirst) {
                linksOnSVG = outerElement.append('g').selectAll('g');
                nodesOnSVG = outerElement.append('g').selectAll('g');
            } else {
                linksOnSVG = outerElement.append('g').selectAll('g');
                nodesOnSVG = outerElement.append('g').selectAll('g');
            }
        }

        if (!keyFuncLinks) linksOnSVG = linksOnSVG.data(links);
        else linksOnSVG = linksOnSVG.data(links, keyFuncLinks);
        if (!linksOnSVG.enter().empty()) linkCreator(linksOnSVG.enter().append("g"));
        linksOnSVG.each(function (d) {
            linkUpdater(d3.select(this))
        });
        linkRemover(linksOnSVG.exit());

        if (!keyFuncNodes) nodesOnSVG = nodesOnSVG.data(nodes);
        else nodesOnSVG = nodesOnSVG.data(nodes, keyFuncNodes);
        var entered = nodesOnSVG.enter();
        if (!entered.empty()) nodeCreator(nodesOnSVG.enter().append("g"));
        nodesOnSVG.each(function (d) {
            nodeUpdater(d3.select(this))
        });
        nodeRemover(nodesOnSVG.exit());

        rebuildSVGEnd();

        return graph;
    };

    graph.nodeCreator = function (elementCreator) {
        if (typeof (elementCreator) == "function") nodeCreator = elementCreator;
        return graph;
    };

    graph.nodeUpdater = function (elementUpdater) {
        if (typeof (elementUpdater) == "function") nodeUpdater = elementUpdater;
        return graph;
    };

    graph.nodeRemover = function (elementRemover) {
        if (typeof (elementRemover) == "function") nodeRemover = elementRemover;
        return graph;
    };

    graph.linkCreator = function (elementCreator) {
        if (typeof (elementCreator) == "function") linkCreator = elementCreator;
        return graph;
    };

    graph.linkUpdater = function (elementUpdater) {
        if (typeof (elementUpdater) == "function") linkUpdater = elementUpdater;
        return graph;
    };

    graph.linkRemover = function (elementRemover) {
        if (typeof (elementRemover) == "function") linkRemover = elementRemover;
        return graph;
    };

    graph.keyFuncLinks = function (keyFunc) {
        if (typeof (keyFunc) == "function") keyFuncLinks = keyFunc;
        return graph;
    };

    graph.keyFuncNodes = function (keyFunc) {
        if (typeof (keyFunc) == "function") keyFuncNodes = keyFunc;
        return graph;
    };

    graph.links = function (linkz) {
        if (!arguments.length) return links;
        if (Array.isArray(linkz))links = linkz;
        return graph;
    };

    graph.nodes = function (nodez) {
        if (!arguments.length) return nodes;
        if (Array.isArray(nodez)) {
            nodes = nodez;
            graph.nodeDurationsShouldBeUpdated();
        }
        return graph;
    };

    graph.deleteLink = function (link) {
        deleteLink(link);
        return graph;
    };

    graph.deleteNode = function (node) {
        deleteNode(node);
        return graph;
    };

    graph.nodesOnSVG = function () {
        return nodesOnSVG;
    };

    graph.linksOnSVG = function () {
        return linksOnSVG;
    };

    graph.setUpdatePositions = function (update) {
        if (typeof update == "function") updatePositions = update;
        return graph;
    };

    graph.updatePositions = function (update) {
        return updatePositions(update);
    };

    graph.rebuildSVGStart = function (rebuildSvgStrt) {
        if (typeof rebuildSvgStrt == "function") rebuildSVGStart = rebuildSvgStrt;
        return graph;
    };

    graph.rebuildSVGEnd = function (rebuildSvgEnd) {
        if (typeof rebuildSvgEnd == "function") rebuildSVGEnd = rebuildSvgEnd;
        return graph;
    };

    graph.nodeDurationsShouldBeUpdated = function () {
        each(nodes, function (d) {
            d['update_duration'] = true;
        });
    };
    graph.linkDurationsShouldBeUpdated = function () {
        each(links, function (d) {
            d['update_duration'] = true;
        });
    };

    function spliceNodes(node) {
        nodes.splice(nodes.indexOf(node), 1);
        var toSplice = links.filter(
            function (l) {
                return (l.source === node) || (l.target === node);
            });
        toSplice.map(
            function (l) {
                links.splice(links.indexOf(l), 1);
            });
    }

    function defaultTick(updateInfo) {
        /*linksOnSVG.select('path').attr('d', function (d) {
         return 'M' + d.source.x + ',' + d.source.y + 'L' + d.target.x + ',' + d.target.y
         });
         nodesOnSVG.attr('transform', function (d, i) {
         return 'translate(' + d.x + ',' + d.y + ')';
         })*/
        graph.rebuildSVG();
    }

    return graph;
};
//-----------------------------------------------------------------------------------------
//-----------------------------------------/GRAPH
//-----------------------------------------------------------------------------------------

//-----------------------------------------------------------------------------------------
//-----------------------------------------Zooming area
//-----------------------------------------------------------------------------------------
/**
 * Создает зумер
 * @param width ширина зумабельного пространства
 * @param height высота зумабельного пространства
 * @param baseElementForOuter элемент (обычно SVG) выбранный D3, в который будет добавлена область с зумом
 * @param borderfill цвет заливки границы. Можно оставить белым.
 * @param scaleExtent это массив-пара, задающая границы зуминга, например [0.5,2]
 */
kiv.zoomingArea = function (width, height, baseElementForOuter, borderfill, scaleExtent) {
    var zoomingGroup, outerGroup, zoom, x, y, trans = [0, 0], scale = 1;

    x = d3.scale.linear().domain([-width / 2, width / 2]).range([0, width]);
    y = d3.scale.linear().domain([-height / 2, height / 2]).range([height, 0]);

    zoom = d3.behavior.zoom().x(x).y(y).scaleExtent(scaleExtent).on("zoom", rescale);
    //zoom.center();
    //На эту группу должны вешаться все обработчики
    outerGroup = baseElementForOuter
        .append('svg:g')
        .call(zoom);

    addBorderRect(outerGroup, width, height, 1, borderfill);

    //В этой группе внутри все будет перемещаться
    zoomingGroup = outerGroup.append('svg:g');

    function zoomingArea() {
    }

    /**
     * @returns Возвращает внешнюю группу. На эту группу должны вешаться все обработчики. Важно, что
     * необходимо рескейлить координаты мыши в этих обработчиках методом rescaleCoords
     */
    zoomingArea.getOuterGroup = function () {
        return outerGroup;
    };

    /**
     * @returns Возвращает внутреннюю группу. Добавленное в эту группу будет соотственно зумиться и перемещаться.
     * Добавлять в эту группу обработчики достаточно бесполезно
     */
    zoomingArea.getZoomingGroup = function () {
        return zoomingGroup;
    };

    /**
     * Отменяет зумабельное поведение
     */
    zoomingArea.disableZoom = function () {
        zoomingGroup.call(d3.behavior.zoom().on("zoom", null));
    };

    /**
     * Включает зумабельное поведение
     */
    zoomingArea.enableZoom = function () {
        zoomingGroup.call(d3.behavior.zoom().on("zoom", rescale));
    };

    /**
     * Рескейлит координаты на картинке и координаты в координатной плоскости.
     * @param coords на картинке в браузере
     * @returns {Array} координаты в координатной плоскости
     */
    zoomingArea.rescaleCoords = function (coords) {
        return [(coords[0] - trans[0]) / scale, (coords[1] - trans[1]) / scale];
    };

    /**
     * Выполняет перемещение к исходным координатам. Должно быть плавно, но не получилось пока
     */
    zoomingArea.returnBack = function () {
        zoomingArea.translate(100, 100);
    };

    zoomingArea.translate = function (toX, toY) {
        d3.transition().duration(500).tween("zoom", function () {
            var trans = d3.interpolate(zoom.translate(), [toX, toY]);
            //var scale = d3.interpolate(zoom.scale(), 1);
            return function (yyy) {
                zoom.translate(trans(yyy));
                //zoom.scale(scale(yyy));
                rescale();
            }
        });
    };

    /**
     * Выполняет преобразование координат в координатной плоскости
     */
    function rescale() {
        trans = zoom.translate();
        scale = zoom.scale();
        zoomingGroup.attr("transform",
            "translate(" + trans + ")"
                + " scale(" + scale + ")");
    }

    return zoomingArea;
};
//-----------------------------------------------------------------------------------------
//-----------------------------------------/Zooming area
//-----------------------------------------------------------------------------------------

//-----------------------------------------------------------------------------------------
//-----------------------------------------TOOLTIPER
//-----------------------------------------------------------------------------------------
var tooltipHelperId = 0;
/**
 * Создает тултип контейнер
 * @param style название класса стиля
 * стиль в вакууме:
 * tooltip {
            position: absolute;
            text-align: center;
            width: 60px;
            padding: 2px;
            font: 12px sans-serif;
            background: lightsteelblue;
            border: 0px;
            border-radius: 8px;
            pointer-events: none;
        }
 * @returns {Function}
 */
kiv.tooltip = function (style) {

    var div = d3.select("body").append("div").attr('id', "__tooltipHelper_" + tooltipHelperId++).attr("class", style).style("opacity", "0");

    function tooltip() {
    }

    /**
     * Привязывет тултип к элементу
     * @param element элемент. Работает так: если наводят мышь - тултип появляется, если отводят - пропадает
     * @param textFormer - функция, позволяющая сформировать штпль, для его рендеринга в тултипе
     * @param params - параметры, на данный момент доступны следующие параметры:
     *      x - сдвиг по х от событиия
     *      y - сдвиг по y от события
     *      sdelay - задержка при начале показа
     *      edelay - задержка при конце показа
     *      sduration - время начала показа
     *      eduration - время конца показа
     * defaultParams = {x:5,y:5,sdelay:0, edelay:0, sduration:200,eduration:500 }
     * @constructor
     */
    tooltip.AddToolTip = function (element, textFormer, params) {
        var defaultParams = {x: 5, y: 5, sdelay: 0, edelay: 0, sduration: 200, eduration: 500 };
        params = (arguments.length == 3) ? mergeProperties(params, defaultParams) : defaultParams;
        element.on("mouseover.tooltip", function (d) {
            div.transition()
                .delay(params.sdelay)
                .duration(params.sduration)
                .style("opacity", "0.95");
            div.html(textFormer(d))
                .style("left", (d3.event.pageX + params.x) + "px")
                .style("top", (d3.event.pageY + params.y) + "px");
        })
            .on("mousemove.tooltip", function (d) {
                div.style("left", (d3.event.pageX + params.x) + "px")
                    .style("top", (d3.event.pageY + params.y) + "px");
            })
            .on("mouseout.tooltip", function (d) {
                div.transition()
                    .delay(params.edelay)
                    .duration(params.eduration)
                    .style("opacity", "0");
            });
    };


    return tooltip;
};
//-----------------------------------------------------------------------------------------
//-----------------------------------------/TOOLTIPER
//-----------------------------------------------------------------------------------------

kiv.krand = function () {

    function krand() {
    }

    /**
     * Рандомный int, включая последний в интервале
     * @param from
     * @param to
     */
    krand.rInt = function (min, max) {
        if (min == max) return min;
        var ax = Math.floor(min + Math.random() * (max - min + 1));
        return ax;
    };

    return krand;
};

//-----------------------------------------------------------------------------------------
//-----------------------------------------ONTOLOGY VIEWER
//-----------------------------------------------------------------------------------------
/**
 *
 * @param params параметры, см defaultParams
 */
kiv.graphStuff.ontologyViewer = function (params) {
    var defaultParams = {
        width: 1000, height: 600, containerid: "chart",
        tooltiper: kiv.tooltip("tooltip"), borderfill: 'black',
        scaleextent: [0.01, 100],
        force_linkdist: 100, force_gravity: 0.1, force_charge: -50000,
        elem_width: 200, elem_vertmargin: 5, elem_hormargin: 5
    };
    params = (arguments.length == 1) ? mergeProperties(params, defaultParams) : defaultParams;

    var width = params.width, height = params.height;
    var outer = d3.select("#" + params.containerid).append("svg:svg")
        .attr("width", width).attr("height", height)
        .attr("pointer-events", "all");

    var tooltiper = params.tooltiper;
    var zoomer = kiv.zoomingArea(width, height, outer, params.borderfill, params.scaleextent);
    zoomer.getOuterGroup()
        .on("dblclick.zoom", null);
    var nodes = [];
    var links = [];
    var randomizer = kiv.krand();

    var graph = kiv.graphStuff.graph(zoomer.getZoomingGroup(), false)
            .nodeCreator(function (d, i) {
                //d.call(force.drag);
                d.each(function (dd, i) {
                    var g = d3.select(this);
                    var width = params.elem_width;
                    var vertMargin = params.elem_vertmargin;
                    var horMargin = params.elem_hormargin;
                    var rects = [];
                    rects.push(addRectWithText(dd, width, vertMargin, horMargin, dd.headcolor, "blue", dd.label, 'headersInGraph',
                        null, 'basicTextInGraph', tooltiper));
                    rects.push(addRectWithText(dd, width, vertMargin, horMargin, "lightsteelblue", "blue", "Classes:", 'headersInGraph',
                        unWrapNameValArray(dd.classes), 'basicTextInGraph', tooltiper));
                    rects.push(addRectWithText(dd, width, vertMargin, horMargin, "lightsteelblue", "blue", "Data properties:", 'headersInGraph',
                        unWrapNameValArray(dd.dataProps), 'basicTextInGraph', tooltiper));
                    rects.push(addRectWithText(dd, width, vertMargin, horMargin, "lightsteelblue", "blue", "Annotation properties:", 'headersInGraph',
                        unWrapNameValArray(dd.annoProps), 'basicTextInGraph', tooltiper));
                    var allHeight = d3.sum(rects, function (d) {
                        return d.height()
                    });
                    var currentY = 0 - allHeight / 2;
                    for (var rectI in rects) {
                        rects[rectI].render(g, 0, currentY + rects[rectI].height() / 2);
                        currentY += rects[rectI].height();
                    }

                    g.on("dblclick.element", function (d) {
                        if (!d.main && !anonym(d.id)) ontologyViewer.render(d.id, getRequestToInstance(d.id));
                    });
                    dd['_$_height_$_'] = allHeight;
                    dd['_$_width_$_'] = width;
                });
            })
            .nodeUpdater(function (d, i) {
            })
            .linkCreator(function (d, i) {
                d.each(
                    function (dd, i) {
                        var forceStarted = force.alpha() != 0;
                        var g = d3.select(this);
                        var id = dd.source.id + "_" + dd.target.id;
                        textArrowLine(g, "_linkId_" + id, dd.text, 'basicTextInGraph', dd.lineType,
                            dd.source.x, dd.source.y, dd.target.x, dd.target.y, 2, dd.lineColor);
                        tooltiper.AddToolTip(g, function (d) {
                            if (d.lineType == 0) return d.text;
                            if (d.lineType == 1) return d.source.label + "<br/>" + d.text + "<br/>" + d.target.label;
                            if (d.lineType == 2) return d.source.label + "<br/><br/>" + d.text + "<br/><br/>" + d.target.label + "<br/><br/>и наоборот";
                            return "";
                        });
                        g.on('mouseover', function (d, i) {
                            forceStarted = force.alpha() != 0;
                            force.stop()
                        });
                        g.on('mouseout', function (d, i) {
                            /*if(forceStarted) {
                             }*/
                        })
                    }
                );
            })
            .linkUpdater(function (d, i) {
            })
            .nodes(nodes)
            .links(links)
            .keyFuncLinks(function (d) {
                return [d.target.id, d.source.id]
            })
            .keyFuncNodes(function (d) {
                return [d.id, d.id]
            })
            .setUpdatePositions(function (d, i) {
                var coords = {};
                for (var index in graph.nodes()) {
                    /*var y = (graph.nodes()[index].inOrOut==='in')
                     ?-500:(graph.nodes()[index].inOrOut==='out')?+500:0;
                     graph.nodes()[index].y = y;*/

                    var y = graph.nodes()[index].y;

                    coords[graph.nodes()[index].id] = y;
                }

                graph.linksOnSVG().each(
                    function (dd, i) {
                        /**
                         * {text:text, linetype: lt, source:node, target:node}
                         * linetype - 0 - 1 - 2
                         * @constructor
                         */
                        var g = d3.select(this);
                        var id = dd.source.id + "_" + dd.target.id;

                        var firstIntersection = lineRectIntersection(
                            dd.source.x, coords[dd.source.id], dd.source['_$_width_$_'], dd.source['_$_height_$_'], dd.target.x + dd.target['_$_width_$_'] / 2, coords[dd.target.id] + dd.target['_$_height_$_'] / 2
                        );
                        var secondIntersection = lineRectIntersection(
                            dd.target.x, coords[dd.target.id], dd.target['_$_width_$_'], dd.target['_$_height_$_'], dd.source.x + dd.source['_$_width_$_'] / 2, coords[dd.source.id] + dd.source['_$_height_$_'] / 2
                        );

                        if (firstIntersection && secondIntersection)
                            textArrowLine(g, "_linkId_" + id, dd.text, 'basicTextInGraph', dd.lineType,
                                firstIntersection.x, firstIntersection.y, secondIntersection.x, secondIntersection.y, 2, dd.lineColor);

                    }
                );
                graph.nodesOnSVG().attr('transform', function (d, i) {
                    var a = 'translate(' + d.x + ',' + coords[d.id] + ')';
                    return a;
                });
                graph.rebuildSVG();
            })
            .rebuildSVGEnd(function () {
                if (d3.event) {
                    d3.event.preventDefault();
                }
            })
        ;


    var force = d3.layout.force()
        .size([width, height])
        .nodes(nodes)
        .links(links)
        .linkDistance(params.force_linkdist)
        .gravity(params.force_gravity)
        .charge(params.force_charge)
        .on("tick", graph.updatePositions);

    function ontologyViewer() {
    }

    /**
     * Нарисовать инстанс. Посылает запрос в базу и отображает инстанс
     * @param idOfInstance
     */
    ontologyViewer.render = function (idOfInstance, requestString) {

        links.length = 0;
        nodes.length = 0;
        graph.rebuildSVG();
        processAllQueriesAndGetResult([requestString], endpoint, function (d) {
            //addLogCountEntry("done");
            var data = sparqlJSONToObject(d[0], "object");
            var key = objToArrayKeys(data[0]);
            var mainElement = function () {
                for (var index in data) if (isMain(index)) return index;
                return "";
            }();
            var id = 0;
            nodemap = {};
            for (var index in data) {
                var obj = data[index];
                var node = ((index === mainElement)) ? {id: index, x: 0, y: 0, main: true} : {id: index, main: false, x: (-500 + randomizer.rInt(0, 1000)), y: (-500 + randomizer.rInt(0, 1000))};
                node.headcolor = function () {
                    for (var index in obj.class)
                        if (arrayOfClassKeys.indexOf(obj.class[index]) != -1) return getSomeObjectColor(obj.class[index], arrayOfClassKeys);
                }();
                node['label'] = getName(index, data);
                if (obj['class'].length > 0) {
                    var classes = [];
                    for (var i = 0; i < obj.class.length; i++) {
                        classes.push({'name': getName(obj.class[i], Oclasses)})
                    }
                    node['classes'] = classes;
                }
                if (obj.dataProperty.length > 0) {
                    var dataProps = [];
                    for (var i = 0; i < obj.dataProperty.length; i++) {
                        var dataPropId = obj.dataProperty[i];
                        var dataPropValue = obj.dpropertyValue[i];
                        var dataPropName = getName(dataPropId, dataTypeProperties);
                        dataProps.push({name: dataPropName, value: dataPropValue});
                    }
                    node['dataProps'] = dataProps;
                }
                nodes.push(node);
                nodemap[index] = node;
            }

            var outLinks = data[mainElement].objpout;
            for (var indexLink in outLinks) {
                var link = {lineType: 1};
                link.source = nodemap[mainElement];
                link.target = nodemap[data[mainElement].objpoutval[indexLink]];
                link.target.inOrOut = 'out';
                link.text = getName(outLinks[indexLink], objectProperties);
                link.lineColor = getSomeObjectColor(outLinks[indexLink], arrayOfObjectPropKeys);
                links.push(link);
            }

            var inLinks = data[mainElement].objpin;
            for (var indexLink in inLinks) {
                var link = {lineType: 1};
                link.target = nodemap[mainElement];
                link.source = nodemap[data[mainElement].objpinval[indexLink]];
                link.source.inOrOut = 'in';
                link.text = getName(inLinks[indexLink], objectProperties);
                link.lineColor = getSomeObjectColor(inLinks[indexLink], arrayOfObjectPropKeys);

                links.push(link);
            }

            /*
             NODE:
             {id:http://www.semanticweb.org/k0shk/ontologies/2013/5/learning#Introduction,
             x:100,y:100, main:true,  label:"РљСЂР°СЃРЅС‹Р№ РєСЂРµСЃС‚СЊСЏРЅСЃРєРёР№ РєР°Р»РµРЅРґР°СЂСЊ",
             headcolor:blue, inOrOut='in'/'out'/'center'
             classes:[{name:"РЇР±Р»РѕРєРѕ"},{name:"Strange thing"}],
             dataProps:[{name:"С†РІРµС‚", value:"РљСЂР°СЃРЅС‹Р№"},{name:"РІРєСѓСЃ", value:"Р·Р°Р±Р°РІРЅС‹Р№"}],
             annoProps:[{name:"comment", value:"Р¦РІРµС‚Р°СЃС‚Рѕ РєСЂР°СЃРЅРѕРµ, Р·Р°Р±Р°РІРЅРѕ РІРєСѓСЃРЅРѕРµ"},{name:"Р°Р±РѕРЅРµР�?РµРЅС‚", value:"РћС‚СЃСѓС‚СЃС‚РІСѓРµС‚"}]}
             LINK:
             {source:nodes[0],target:nodes[1], text:"РќРµ РїСЂРѕС‚РёРІРѕСЂРµС‡РёС‚", lineType:2, lineColor:"blue"},
             */
            graph.rebuildSVG();
            graph.updatePositions();
            force.start().alpha(0.015);
            function isMain(objId) {
                return data[objId]['aclass'].length > 0;
            }
        });
    };
    ontologyViewer.zoomer = function () {
        return zoomer;
    };
    ontologyViewer.graph = function () {
        return graph;
    };
    ontologyViewer.force = function () {
        return force;
    };
    return ontologyViewer;
};
kiv.graphStuff.ontologyViewerTree = function (params) {
    var defaultParams = {
        width: 1200, height: 600, nodewidth: 300, nodeDif: 80, heightPerTextLine: 1,
        heightBetweenNodesOfOneParent: 10, heightBetweenNodesOfDifferentParent: 40,
        animdur: 500, containerid: 'chart', buttonWidth: 20, textInLine: 35,
        tooltiper: kiv.tooltip("tooltip")
    };
    params = (arguments.length == 1) ? mergeProperties(params, defaultParams) : defaultParams;

    var w = params.width;
    var h = params.height;
    var nodeWidth = params.nodewidth;
    var nodeDif = params.nodeDif;
    var buttonWidth = params.buttonWidth;
    var heightPerTextLine = params.heightPerTextLine;
    var heightBetweenNodesOfOneParent = params.heightBetweenNodesOfOneParent;
    var heightBetweenNodesOfDifferentParent = params.heightBetweenNodesOfDifferentParent;
    var animationDuration = params.animdur;
    var tooltiper = params.tooltiper;
    var textInLine = params.textInLine;
    var histowidth = nodeWidth - nodeDif + 20;

    var history = [];

    var tree = d3.layout.tree()
        .nodeSize([heightPerTextLine, nodeWidth])
        .separation(function (a, b) {
            var height1 = addRectWithFullText({}, nodeWidth - nodeDif, 5, 5, 'black', a.name, 'headersInGraph', null, 0, 0, 5, 5).height();
            if (containsInObj(a, "expanded") && a.expanded) {
                getExpandedlementList(a);
                height1 = a['_$_height_$_'];
            }
            var height2 = addRectWithFullText({}, nodeWidth - nodeDif, 5, 5, 'black', b.name, 'headersInGraph', null, 0, 0, 5, 5).height();
            if (containsInObj(b, "expanded") && b.expanded) {
                getExpandedlementList(b);
                height2 = b['_$_height_$_']
            }
            var dif = height1 / 2 + height2 / 2 + (a.parent == b.parent ? heightBetweenNodesOfOneParent : heightBetweenNodesOfDifferentParent);

            return dif;
        });

    var diagonal = d3.svg.diagonal().projection(function (d) {
        return [d.y, d.x];
    });

    var svg = d3.select("#" + params.containerid).append("svg:svg")
        .attr("width", w).attr("height", h)
        .attr("pointer-events", "all");


    var zoomPart = formD3ChainCalls(svg, "g#zoom_part|id'zoom_part");
    var historypart = formD3ChainCalls(svg, "g#hist_part|id'hist_part");
    addRect(historypart, 30, 50, histowidth, h - 100, 5, 5)
        .attr("fill", "lightgray")
        .attr("stroke", 'blue')
        .attr("stroke-width", 2);

    var zoomer = kiv.zoomingArea(w, h, zoomPart, 'black', [0.01, 100]);
    zoomer.getOuterGroup().on("dblclick.zoom", null);
    zoomer.translate(nodeWidth / 2, h / 2);


    svg = zoomer.getZoomingGroup();

    function ontologyViewerTree() {
    }

    var KID = 0;

    ontologyViewerTree.render = function (ip) {
        var defaultParams = {
            idOfInstance: null, requestString: "", mainRoot: null, currentRoot: null, usedElements: {}
        };
        ip = (arguments.length == 1) ? mergeProperties(ip, defaultParams) : defaultParams;

        processAllQueriesAndGetResult([ip.requestString], endpoint, function (d) {
                var allData = sparqlJSONToObject(d[0], "object");
                var mainElement = getMain(ip.idOfInstance, allData);
                var objProperties = {parent: mainElement};
                var nodemap = formNodeMap(allData, mainElement, objProperties);
                //var objPropElements = formObjProperties(nodemap);
                var usedElements = ip.usedElements;

                //формируем дерево с классами.
                var root = (ip.currentRoot == null) ? nodemap[mainElement] : ip.currentRoot;
                var objPropNodes = {};
                //Формируем дерево
                each(nodemap, function (d, i) {
                    if (d != root)
                        each(objProperties, function (jj, dd) {
                            if (!(typeof objProperties[dd] === "string") && containsInObj(objProperties[dd], d.id) && (dd in objectProperties)) {
                                var children = [];
                                if (!(dd in objPropNodes)) {
                                    objPropNodes[dd] = {name: objectProperties[dd].o_name, children: children, isRoot: true, headcolor: getSomeObjectColor(d.id, arrayOfClassKeys)};
                                    if (!containsInObj(usedElements, dd))  usedElements[dd] = 1;
                                    else objPropNodes[dd].cloned = '_' + (KID++);
                                }
                                children = objPropNodes[dd].children;
                                if (!(i in usedElements)) {
                                    children.push(nodemap[i]);
                                    usedElements[i] = 1;
                                } else {
                                    var newObj = jQuery.extend(true, {}, nodemap[i]);
                                    newObj.cloned = '_' + (KID++);
                                    children.push(newObj);
                                }
                            }
                        });
                    else {
                        if (!(i in usedElements)) {
                            usedElements[i] = 1;
                        } else {
                            var newObj = jQuery.extend(true, {}, nodemap[i]);
                            newObj.cloned = '_' + (KID++);
                            root = newObj;
                        }
                    }
                });

                //Назначаем последователей
                root.children = objToArrayValues(objPropNodes);
                //root.isRoot = true;
                root.querry = ip.requestString;
                root.usedElements = usedElements;
                if (ip.mainRoot == null) ip.mainRoot = root;

                paintAll(svg, ip.mainRoot, function () {
                        return function (j) {
                            window.location.replace("/resource/?uri="+ encodeURIComponent(j.id));
                           /* if (!j.main && !anonym(j.id)) {
                                var p = j.parent.parent;
                                var ind = goodIndexOf(history, p.id, function (d, i) {
                                    return d.id == i;
                                });
                                if (ind != -1) history.splice(ind, 1);
                                history.push({id: p.id, headcolor: p.headcolor, name: p.name, height: p['_$_uirect_$_'][0].height() });
                                ontologyViewerTree.render({idOfInstance: j.id, requestString: getRequestToInstance(j.id)});
                                zoomer.translate(nodeWidth / 2, h / 2);
                            }*/
                        }
                    }, function (a, bbb) {
                        return function (j) {
                            bbb.expanded = !bbb.expanded;
                        }
                    },
                    function (d) {
                        return function (k) {
                            ontologyViewerTree.render({idOfInstance: k.id, requestString: getRequestToInstance(k.id), mainRoot: ip.mainRoot, currentRoot: k, usedElements: ip.usedElements});
                        }
                    }
                );


            }
        );

        function getMain(objId, allData) {
            for (var index in allData) if (allData[objId]['aclass'].length > 0) return index;
            return "";
        }

        function formNodeMap(allData, mainElement, objProperties) {
            var nodemap = {};
            for (var index in allData) {
                var obj = allData[index];
                var node = ((index === mainElement)) ? {id: index, /*x: 0, y: 0,*/ main: true} : {id: index, main: false/*, x: 0, y: 0*/};
                node.headcolor = function () {
                    for (var index in obj.class)
                        if (arrayOfClassKeys.indexOf(obj.class[index]) != -1) return getSomeObjectColor(obj.class[index], arrayOfClassKeys);
                    return "darkgray";
                }();
                node['label'] = getName(index, allData);
                node['name'] = getName(index, allData);
                if (obj['class'].length > 0) {
                    var classes = [];
                    for (var i = 0; i < obj.class.length; i++) {
                        classes.push({'name': getName(obj.class[i], Oclasses), id: obj.class[i]})
                    }
                    node['classes'] = classes;
                }

                if (obj.dataProperty.length > 0) {
                    var dataProps = [];
                    for (var i = 0; i < obj.dataProperty.length; i++) {
                        var dataPropId = obj.dataProperty[i];
                        var dataPropValue = obj.dpropertyValue[i];
                        var dataPropName = getName(dataPropId, dataTypeProperties);
                        dataProps.push({name: dataPropName, value: dataPropValue});
                    }
                    node['dataProps'] = dataProps;
                }

                node['inProperties'] = addObjPropsToNode(obj.objpin, obj.objpinval, objProperties, true);
                node['outProperties'] = addObjPropsToNode(obj.objpout, obj.objpoutval, objProperties, false);
                node['expanded'] = false;
                nodemap[index] = node;
            }
            return nodemap;
        }

        function addObjPropsToNode(objProps, objPropVals, objProperties, inOrOut) {
            if (objProps.length > 0) {
                var props = [];
                for (var i = 0; i < objProps.length; i++) {
                    var opID = objProps[i];
                    var opVal = objPropVals[i];
                    var opName = getName(opID, objectProperties);
                    props.push({id: opID, name: opName, value: opVal});
                    if (!containsInObj(objProperties, opID)) {
                        objProperties[opID] = {};
                    }
                    objProperties[opID]['IN'] = inOrOut;
                    objProperties[opID][opVal] = 1;
                }
                return props;
            }
            return [];
        }
    };

    return ontologyViewerTree;

    function getExpandedlementList(dd) {
        var width = nodeWidth - nodeDif;
        var vertMargin = 5;
        var horMargin = 5;
        var rects = [];
        rects.push(addRectWithText(dd, width, vertMargin, horMargin, dd.headcolor, "blue", dd.label, 'headersInGraph',
            null, 'basicTextInGraph', tooltiper));
        rects.push(addRectWithText(dd, width, vertMargin, horMargin, "lightsteelblue", "blue", "Классы:", 'headersInGraph',
            removeNamedIndividual(unWrapNameValArray(dd.classes)), 'basicTextInGraph', tooltiper));
        rects.push(addRectWithText(dd, width, vertMargin, horMargin, "lightsteelblue", "blue", "Свойства:", 'headersInGraph',
            convertBoolean(unWrapNameValArray(dd.dataProps)), 'basicTextInGraph', tooltiper));
        /*rects.push(addRectWithText(dd, width, vertMargin, horMargin, "lightsteelblue", "blue", "Annotation properties:", 'headersInGraph',
         unWrapNameValArray(dd.annoProps), 'basicTextInGraph', tooltiper));*/
        var allHeight = d3.sum(rects, function (d) {
            return d.height()
        });
        var currentY = 0 - allHeight / 2;

        dd['_$_height_$_'] = allHeight;
        dd['_$_width_$_'] = width;

        return rects;

        function removeNamedIndividual(arr) {
            var toRet = [];
            each(arr, function (d) {
                if (d != 'owl#NamedIndividual') toRet.push(d);
            });
            return toRet;
        }

        function convertBoolean(arr) {
            var toRet = {};
            each(arr, function (d, i) {
                toRet[i] = (d == 'true') ? "Да" : (d == 'false') ? "Нет" : d;
            });
            return toRet;
        }
    }

    function paintAll(svgParent, root, leftActionGenerator, centerActionGenerator, rightActionForLeafs) {
        var nodes = tree.nodes(root);
        var links = tree.links(nodes);

        setParent(nodes);

        var historyElement = formD3ChainCalls(historypart, "g#histo|id'histo");

        var hist = historyElement.selectAll('g#histeria').data(history, function (d) {
            if (typeof d === 'object')return d.id;
            return 0;
        });
        var historyEnter = hist.enter();
        var historyUpdate = hist;
        var historyExit = hist.exit();
        historyEnter.append("g")
            .attr('id', 'histeria')
            .style('opacity', 0);
        var curHeight = 70;
        historyUpdate.transition().duration(animationDuration)
            .style('opacity', 1)
            .each(function (d) {
                d3.select(this).text('');
                var a = addRectWithFullText(d, nodeWidth - nodeDif, 5, 5, ('headcolor' in d) ? (d.headcolor != null) ? d.headcolor : 'gray' : 'lightgray', d.name, 'headersInGraph', d3.select(this), 0, 0, 5, 5);
                d3.select(this).attr('transform', "translate(" + nodeWidth / 2 + "," + (curHeight + a.height() / 2) + ")");
                curHeight += a.height() + heightBetweenNodesOfOneParent;
                a.render();
                a.setAction("click.histo", function (aa) {
                    var p = root;
                    var dd = d;
                    var ind = goodIndexOf(history, p.id, function (d, i) {
                        return d.id == i;
                    });
                    if (ind != -1) history.splice(ind, 1);
                    history.push({id: p.id, headcolor: p.headcolor, name: p.name, height: p['_$_uirect_$_'][0].height() });
                    ontologyViewerTree.render({idOfInstance: dd.id, requestString: getRequestToInstance(dd.id)});
                    //{idOfInstance: j.id, requestString: getRequestToInstance(j.id)}
                });
            });
        historyExit.transition().duration(animationDuration).style('opacity', 0).remove();

        var imbaelement = formD3ChainCalls(svgParent, "g#imbah|id'imbah");
        imbaelement.text('');
        var forLinks = formD3ChainCalls(svgParent, "g#linkers|id'linkers");
        forLinks.attr('transform', 'translate(' + nodeWidth + ',' + 0 + ')');
        var link = forLinks.selectAll(".link")
            .data(links, function (d) {
                return d.source.name + (containsInObj(d.source, 'cloned') ? d.source.cloned : "") + "_" + d.target.name + (containsInObj(d.target, 'cloned') ? d.target.cloned : "");
            });
        var linkEnter = link
            .enter().append("path")
            .attr("class", "link")
            .attr('stroke', function (d) {
                return d.target.headcolor;
            })
            .attr("d", function (d) {
                var o = {x: 0, y: 0};
                return diagonal({source: o, target: o});
            });

        var linkUpdate = link;
        linkUpdate.transition().duration(animationDuration)
            .attr("d", function (d) {
                var m = (d.source.y + d.target.y) / 2;
                var p = [
                    {x: d.source.x, y: d.source.y + nodeWidth / 2 - buttonWidth},
                    {x: d.source.x, y: m},
                    {x: d.target.x, y: m},
                    {x: d.target.x, y: d.target.y - nodeWidth / 2 + ((d.target.isRoot) ? buttonWidth * 2 : buttonWidth)}
                ];

                p = p.map(function (d) {
                    return [ d.y, d.x ]
                });
                return "M" + p[0] + "C" + p[1] + " " + p[2] + " " + p[3];
            });
        //return "M" + p[0] + "C" + p[1] + " " + p[2] + " " + p[3];
        //M0,0C150,0 150,-146.25 300,-146.25

        var linkExit = link.exit().transition().duration(animationDuration)
            .attr("d", function (d) {
                var o = {x: d.target.x, y: d.target.y};
                return diagonal({source: o, target: o});
            })
            .style('opacity', 0).remove();

        var forNodes = formD3ChainCalls(svgParent, "g#noderz|id'noderz");
        forNodes.attr('transform', 'translate(' + nodeWidth + ',' + 0 + ')');
        var node = forNodes.selectAll(".node").data(nodes, function (d) {
            var name = d.name + ((('children' in d) || ('_children' in d)) ? "_children" : "_nochildren");
            if ('cloned' in d) name += d.cloned;
            name += d.expanded;
            return name;
        });
        var nodeEnter = node.enter().append("g")
            .attr("class", "node")
            .attr('transform', function (d) {
                if (typeof d === 'object' && 'parent' in d) return "translate(" + d.parent.y + "," + d.parent.x + ")";
                else return "translate(0,0)"
            })
            .style('opacity', 0);

        var nodeUpdate = node;
        nodeUpdate.transition().duration(animationDuration)
            .attr("transform", function (d) {
                return "translate(" + d.y + "," + d.x + ")";
            })
            .style('opacity', 1)
            .each(function (d) {
                d3.select(this).text('');
                var a = 0;
                if (!d.expanded) {
                    a = addRectWithFullText(d, nodeWidth - nodeDif, 5, 5, ('headcolor' in d) ? (d.headcolor != null) ? d.headcolor : 'gray' : 'lightgray', d.name, 'headersInGraph', d3.select(this), 0, 0, 5, 5);
                    a.render();
                } else {
                    var rects = getExpandedlementList(d);
                    var currentY = 0;
                    var allHeight = d['_$_height_$_'];
                    for (var rectI in rects) {
                        var rect = rects[rectI].render(d3.select(this), 0, -allHeight / 2 + currentY + rects[rectI].height() / 2);
                        a = (a == 0) ? rect : a;
                        currentY += rects[rectI].height();
                    }
                    var x = 0;
                }
                if (containsInObj(d, 'classes'))
                    a.setAction("click.open", function (yyy) {
                        centerActionGenerator(imbaelement, d)();
                        paintAll(svg, root, leftActionGenerator, centerActionGenerator, rightActionForLeafs);
                    });
                var leftRightheight = (d.expanded) ? d['_$_height_$_'] : d['_$_uirect_$_'][0].height();
                if (!('isRoot' in d)) {
                    var left = addRect(d3.select(this), -(nodeWidth - nodeDif) / 2 - buttonWidth, -(leftRightheight / 2), buttonWidth, leftRightheight, 5, 5);
                    left.attr('fill', 'black');
                    left.on("mousedown.left", leftActionGenerator());
                    left.on('mouseover.left', function (d, i) {
                        left.transition().duration(animationDuration / 2).attr('fill', 'blue')
                    });
                    left.on('mouseout.left', function (d, i) {
                        left.transition().duration(animationDuration / 2).attr('fill', 'black')
                    });
                }
                var right = addRect(d3.select(this), (nodeWidth - nodeDif) / 2, -(leftRightheight / 2), buttonWidth, leftRightheight, 5, 5);
                if (('children' in d) || containsInObj(d, '_children')) {
                    right.attr('fill', 'black');
                    right.on("mousedown.close", function (dd) {
                        var cur = d;
                        if (!('_children' in cur))cur._children = false;
                        if (cur._children) {
                            cur.children = cur._children;
                            cur._children = false;
                        } else {
                            cur._children = cur.children;
                            cur.children = false;
                        }
                        paintAll(svg, root, leftActionGenerator, centerActionGenerator, rightActionForLeafs);
                    });
                    right.on('mouseover.left', function (d, i) {
                        right.transition().duration(animationDuration / 2).attr('fill', 'blue')
                    });
                    right.on('mouseout.left', function (d, i) {
                        right.transition().duration(animationDuration / 2).attr('fill', 'black')
                    });
                } else {
                    right.attr('fill', 'black');
                    right.on("mousedown.close", rightActionForLeafs(d));
                    right.on('mouseover.left', function (d, i) {
                        right.transition().duration(animationDuration / 2).attr('fill', 'blue')
                    });
                    right.on('mouseout.left', function (d, i) {
                        right.transition().duration(animationDuration / 2).attr('fill', 'black')
                    });
                }
            });

        var nodeExit = node.exit().transition().duration(animationDuration)
            .style('opacity', 0)
            .attr('transform', function (d) {
                if (typeof d === 'object' && 'parent' in d) return "translate(" + d.parent.y + "," + d.parent.x + ")";
                else return "translate(0,0)"
            })
            .remove();
    }

    function setParent(node) {
        if ('children' in node) {
            for (var index in node.children) {
                node.children[index].parent = node;
                setParent(node.children[index]);
            }
        }
    }
}
;

//-----------------------------------------------------------------------------------------
//-----------------------------------------/ONTOLOGY VIEWER
//-----------------------------------------------------------------------------------------

//-----------------------------------------------------------------------------------------
//-----------------------------------------GOOD PLOTS
//-----------------------------------------------------------------------------------------
kiv.UI = function () {
    function UI() {
    }

    UI.NiceRoundRectangle = function (dataElement, params) {
        var defaultParams = {
            width:150, roundx: 10, roundy: 10, uText: "123", lText: "456",
            marginXtop:10,marginXbottom:10,marginY:10, classUpperText:"upper_element_text", classLowerText:"lower_element_text"
        };
        params = (arguments.length == 1) ? mergeProperties(params, defaultParams) : defaultParams;

        var tiUp = textInfo(params.uText, params.classUpperText);
        var tiDown = textInfo(params.lText, params.classLowerText);
        var listOFStrings = SmartText(params.width - params.marginXbottom * 2, params.lText, params.classLowerText);
        var gElement = null;
        var texts = [];

        function NiceRoundRectangle(){}

        NiceRoundRectangle.height = function () {
            return vertMargin + (listOFStrings.length * (ti.height - 2 * ti.baseLineHeight + vertMargin));
        }

        NiceRoundRectangle.Gelement = function () {
            return gElement;
        };

        NiceRoundRectangle.setAction = function (targetAction, functor) {
            rect.on(targetAction, functor);
            each(texts, function (d) {
                d.on(targetAction, functor);
            });
        } ;

        NiceRoundRectangle.render = function(){

        };

        if (!('_$_uirect_$_' in dataElement)) dataElement['_$_uirect_$_'] = [];
        dataElement['_$_uirect_$_'].push(a);
        return NiceRoundRectangle;
    };

    return UI;
};

//-----------------------------------------------------------------------------------------
//-----------------------------------------/GOOD PLOTS
//-----------------------------------------------------------------------------------------