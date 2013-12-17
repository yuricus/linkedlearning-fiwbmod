package ru.ifmo.ailab;

import com.fluidops.ajax.components.FComponent;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.widget.AbstractWidget;
import com.fluidops.iwb.widget.config.WidgetBaseConfig;
import com.fluidops.util.Rand;
import com.google.common.collect.Lists;
import org.openrdf.model.URI;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: yuemelyanov
 * Date: 13.11.13
 * Time: 10:54
 */
public class OntoViewerWidget extends AbstractWidget<OntoViewerWidget.OntoViewerConfig> {

    public static class OntoViewerConfig extends WidgetBaseConfig {
    }

    @Override
    protected FComponent getComponent(String id) {
        OntoViewerConfig config = get();

        OntoViewer viewer = new OntoViewer(id, getDefaultCenter().toString());
        return viewer;
    }

    private URI getDefaultCenter() {
        if (pc.value instanceof URI)
            return (URI)pc.value;
        // create some URI from the BNode or literal that can serve as center
        return EndpointImpl.api().getNamespaceService().guessURI(pc.value.stringValue());
    }

    @Override
    public String getTitle() {
        return "Ontology Viewer";
    }

    @Override
    public Class<?> getConfigClass() {
        return OntoViewerConfig.class;
    }

    @Override
    public List<String> jsURLs() {
        String cp = EndpointImpl.api().getRequestMapper().getContextPath();
        return Lists.newArrayList (
                cp+"/oeditor/js/libs/d3.v3.js",
//                cp+"/oeditor/js/libs/jquery-2.0.2.js",
//                cp+"/oeditor/js/libs/jquery.treeview.js",
//                cp+"/oeditor/js/libs/jquery.cookie.js",
                cp+"/oeditor/js/libs/primitives.js?" + Rand.getIncrementalFluidUUID(),
                cp+"/oeditor/js/libs/klib.js?" + Rand.getIncrementalFluidUUID(),
                cp+"/oeditor/js/sparql.js?" + Rand.getIncrementalFluidUUID(),
                cp+"/oeditor/js/mainProcessorSimple.js?" + Rand.getIncrementalFluidUUID()
        );
    }
}
