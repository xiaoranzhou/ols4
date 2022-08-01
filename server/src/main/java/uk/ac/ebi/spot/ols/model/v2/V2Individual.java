
package uk.ac.ebi.spot.ols.model.v2;

import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.spot.ols.service.GenericLocalizer;
import uk.ac.ebi.spot.ols.service.OntologyEntity;

@Relation(collectionRelation = "individuals")
public class V2Individual extends DynamicJsonObject {

    public V2Individual(OntologyEntity node, String lang) {

        if(!node.hasType("individual")) {
            throw new IllegalArgumentException("Node has wrong type");
        }

        put("lang", lang);

        for(String k : node.asMap().keySet()) {
            Object v = node.asMap().get(k);
            put(k, GenericLocalizer.localize(v, lang));
        }

    }

}


