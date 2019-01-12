package edu.isi.wings.opmm;

/**
 * Auxiliary class for defining the queries used by hash Utils and encoding
 * @author dgarijo
 */
public class QueriesHashUtils {
    /**
     * Query designed to return the component bindings of a template. 
     * Assumption: only one template has been loaded
     * @return the query
     */
    public static String getComponentsURIsOfTemplate(){
        return "select distinct ?compURI where {?anyComponent <"+Constants.WINGS_PROP_HAS_COMPONENT_BINDING+"> ?compURI}";
    }
}
