package trip.planner

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import trip.planner.osm.api.POIApi
import trip.planner.osm.model.Node
import trip.planner.osm.model.POIRequest
import trip.planner.osm.model.Tag

import java.util.concurrent.TimeoutException

class POIParser {

    public static
    final String FALSE_QUERY_MESSAGE = "The value of attribute \"n\" of the element \"bbox-query\" must " +
            "always be greater or equal than the value of attribute \"s\"."
    public static final long TEN_SECONDS = 10000L
    private static Log log = LogFactory.getLog(POIParser.class)

    static List<Node> parse(POIApi api) {
        log.info "Begin parsing of poi-request with bbox: $api.BBox"

        for (int i = 0; i < 4; i++) {
            if (api.doRequest()) {
                String term = api.xmlContent
                if (term.contains(FALSE_QUERY_MESSAGE)) {
                    throw new IllegalArgumentException("False query input parameter: $FALSE_QUERY_MESSAGE")
                }

                try {
                    POIRequest request = parsePOIRequest(term)

                    if (request.remark != null) {
                        throw new TimeoutException(request.remark)
                    }

                    if (request.nodes == null) {
                        log.info "No nodes found in given bbox: $api.BBox"
                        return Collections.emptyList()
                    }

                    return request.nodes
                } catch (CannotResolveClassException crce) {
                    throw new IllegalArgumentException("Cannot parse: $term. Cause from parser: " + crce.message)
                }
            } else {
                if (i == 3) {
                    return Collections.emptyList()
                }
                log.info "Wait 10 s then try again."
                Thread.sleep(TEN_SECONDS)
            }
        }
        return Collections.emptyList()
    }

    private static POIRequest parsePOIRequest(String term) {
        XStream xstream = new XStream();
        xstream.ignoreUnknownElements()
        xstream.processAnnotations(POIRequest.class);
        xstream.processAnnotations(Node.class);
        xstream.processAnnotations(Tag.class);
        POIRequest request = (POIRequest) xstream.fromXML(term);
        request
    }
}
