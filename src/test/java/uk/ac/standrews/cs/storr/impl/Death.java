package uk.ac.standrews.cs.storr.impl;

import uk.ac.standrews.cs.storr.types.LXPBaseType;
import uk.ac.standrews.cs.storr.types.LXP_SCALAR;

public class Death extends StaticLXP {

    private static LXPMetadata static_metadata;

    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int FORENAME;
    @LXP_SCALAR(type = LXPBaseType.STRING)
    public static int SURNAME;

    @Override
    public LXPMetadata getMetaData() {
            return static_metadata;
    }

    static {
        try {
            static_metadata = new LXPMetadata(Death.class, "Death");
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }
}