package edu.isi.wings.catalog.provenance;

import java.util.Properties;

import edu.isi.wings.catalog.provenance.api.ProvenanceAPI;
import edu.isi.wings.catalog.provenance.api.impl.kb.ProvenanceKB;

public class ProvenanceFactory {
  public static ProvenanceAPI getAPI(Properties props) {
    return new ProvenanceKB(props);
  }
}
