package uk.ac.standrews.cs.neoStorr.impl;

import uk.ac.standrews.cs.neoStorr.impl.exceptions.PersistentObjectException;

public class DynamicLXPWithRef extends DynamicLXP {

    public DynamicLXPWithRef(Birth b) throws PersistentObjectException {
        this.put("A_REF", b.getThisRef() );
    }

}