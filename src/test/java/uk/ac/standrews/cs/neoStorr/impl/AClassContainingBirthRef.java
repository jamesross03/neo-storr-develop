package uk.ac.standrews.cs.neoStorr.impl;

import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.types.LXP_REF;

import java.util.Map;

public class AClassContainingBirthRef extends StaticLXP {

    private static LXPMetadata static_metadata;

    @LXP_REF(type = "Birth")
    public static int MY_FIELD;

    public AClassContainingBirthRef() {}

    public AClassContainingBirthRef(Birth b) throws PersistentObjectException {
        this.put(AClassContainingBirthRef.MY_FIELD, b.getThisRef() );
    }

    public AClassContainingBirthRef(long persistent_object_id, Map properties, IBucket bucket ) throws PersistentObjectException {
        super( persistent_object_id, properties, bucket );
    }

    @Override
    public LXPMetadata getMetaData() {
        return static_metadata;
    }

    static {
        try {
            static_metadata = new LXPMetadata(AClassContainingBirthRef.class, "AClassContainingBirthRef");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

}