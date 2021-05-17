package uk.ac.standrews.cs.neoStorr.impl;

import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.types.LXPBaseType;
import uk.ac.standrews.cs.neoStorr.types.LXP_SCALAR;

import java.util.Map;

public class Birth extends StaticLXP {

    private static LXPMetadata static_metadata;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int FORENAME;
    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int SURNAME;

    public Birth() {}

    public Birth(long persistent_object_id, Map properties, IBucket bucket ) throws PersistentObjectException {
        super( persistent_object_id, properties, bucket );
    }

    public Birth(String forename, String surname ) {
        this.put( Birth.FORENAME, forename );
        this.put( Birth.SURNAME, surname );
    }

    public static LXPReference<Birth> makeRef( String serialized ) {
        return new LXPReference<>(serialized);
    }

    public static Birth getRef( LXPReference<Birth> ref ) throws BucketException, RepositoryException {
        return (Birth) ref.getReferend(Birth.class);
    }

    @Override
    public LXPMetadata getMetaData() {
        return static_metadata;
    }

    static {
        try {
            static_metadata = new LXPMetadata(Birth.class, "Birth");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }
}