package io.jawg.osmcontributor.ui.managers.loadPoi;

import java.util.List;

import io.jawg.osmcontributor.OsmTemplateApplication;
import io.jawg.osmcontributor.model.entities.relation.Relation;
import io.jawg.osmcontributor.rest.Backend;
import io.jawg.osmcontributor.rest.NetworkException;


/**
 * {@link RelationLoader} for retrieving {@link Relation} data.
 */
public class RelationLoader {
    private final Backend backend;

    public RelationLoader(Backend backend) {
        this.backend = backend;
    }

    /**
     * Download relations from backend
     */
    public List<Relation> remoteDownloadRelation(String id) {
        Boolean hasNetwork = OsmTemplateApplication.hasNetwork();
        if (hasNetwork != null && !hasNetwork) {
            throw new NetworkException();
        }
        return backend.getRelations(id);
    }

}
