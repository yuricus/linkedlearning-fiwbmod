package ru.ifmo.ailab;

import com.fluidops.ajax.FClientUpdate;
import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.util.Rand;

/**
 * Created with IntelliJ IDEA.
 * User: yuemelyanov
 * Date: 13.11.13
 * Time: 11:01
 */
public class OntoViewer extends FComponent {

    String path;

    public OntoViewer(String id, String startPath) {
        super(id);
        this.path = startPath;
    }

    @Override
    public String render() {
        String contID = "ontograph"+Rand.getIncrementalFluidUUID();
        addClientUpdate( new FClientUpdate( FClientUpdate.Prio.VERYEND, "startIt('" + contID + "','"+path+"')" ) );
        StringBuilder html = new StringBuilder();
        html.append(
                "<input style=\"visibility: collapse;\" type=\"text\" id='prefixList' value=\"http://www.owl-ontologies.com/Ontology1372834575.owl\"/>"+
                "<div id='middle'>\n" +
                "    <div id='container'>\n" +
                "        <div id='" + contID + "'></div>\n" +
                "    </div>\n" +
                "</div>");
//        for ( String head : jsURLs() )
//            html.append( "<script type='text/javascript' src='" ).append( head ).append( "'></script>\n" );
//        for ( String head : cssURLs() )
//            html.append( "<link rel='stylesheet' type='text/css' href='" ).append( head ).append( "'/>\n" );
        return html.toString();
    }

    /**
     * This is not being called from IWB!!!!!!!!!!!!!
     * @return
     */
    @Override
    public String[] jsURLs() {
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] {
                cp+"/oeditor/js/libs/d3.v3.js",
//                cp+"/oeditor/js/libs/jquery-2.0.2.js",
//                cp+"/oeditor/js/libs/jquery.treeview.js",
//                cp+"/oeditor/js/libs/jquery.cookie.js",
                cp+"/oeditor/js/libs/primitives.js?" + Rand.getIncrementalFluidUUID(),
                cp+"/oeditor/js/libs/klib.js?" + Rand.getIncrementalFluidUUID(),
                cp+"/oeditor/js/sparql.js?" + Rand.getIncrementalFluidUUID(),
                cp+"/oeditor/js/mainProcessorSimple.js?" + Rand.getIncrementalFluidUUID()
        };
    }

    /**
     * This is not being called from IWB!!!!!!!!!!!!!
     * @return
     */
    @Override
    public String[] cssURLs() {
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return new String[] {
                cp+"/oeditor/css/libs/jquery.treeview.css",
                cp+"/oeditor/css/general.css"
        };
    }
}
