# Wings configuration documentation

The wings configuration file is located at `/etc/wings/portal.properties`. It contains the following sections:

- **main**: modify main properties
- **storage**: modify the paths where the data and database is stored
- **ontology**: reference to the WINGS ontology
- **execution**: available engines to run the workflow
- **publisher**: describes how the wings system shares data, execution, and provenance.

Be sure to modify the configuration file carefully, as changes may affect the functionality of the Wings system.

To inspect the configuration file, open a browser and go to [http://localhost:8080/wings-portal/config](http://localhost:8080/wings-portal/config). The sensitive information is hidden.

### Main

The **main** section of the configuration file allows you to modify the main properties of the Wings system.

| Name           | Description                                                                                                                                                                                                                                                                                                          | Default                                                      |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| server         | The URL of the Wings instance is used to generate the accessible URI for resources. For example, if the value is set to the default, the component library of the domain CaesarCypher and user admin will be available at http://localhost:8080/wings-portal/export/users/admin/CaesarCypher/components/library.owl. | Obtained by HTTP request. For example, http://localhost:8080 |
| graphviz       | Path where the graphviz software is installed                                                                                                                                                                                                                                                                        | /usr/bin/dot                                                 |
| light-reasoner | Enable or disable validation in the planning of workflow                                                                                                                                                                                                                                                             | false                                                        |

### Storage

The **storage** section of the configuration file allows you to specify the location of the data and database directories. Make sure to set the paths to directories with sufficient storage space and appropriate permissions for the following properties:

| name          | description                                        | default value        |
| ------------- | -------------------------------------------------- | -------------------- |
| storage.local | Directory where the data and components are stored | $HOME/.wings/storage |
| storage.local | RDF store database (Apache JENA)                   | $HOME/.wings/TDB     |
| storage.local | Directory where the log files are stored           | $HOME/.wings/logs    |

### Ontology

The following text describes the ontology section of the Wings configuration file. The properties within this section should not be modified unless you are an advanced user with knowledge of the WINGS ontology

The ontology section of the configuration file specifies the location and URIs of the WINGS ontology files. The following properties defin

| Name               | Description                                           | Default Value                                         |
| ------------------ | ----------------------------------------------------- | ----------------------------------------------------- |
| ontology.component | Location and URI of the WINGS ontology component ont  | http://www.wings-workflows.org/ontology/component.owl |
| ontology.data      | Location and URI of the WINGS ontology data file      | http://www.wings-workflows.org/ontology/data.owl      |
| ontology.execution | Location and URI of the WINGS ontology execution file | http://www.wings-workflows.org/ontology/execution.owl |
| ontology.workflow  | Location and URI of the WINGS ontology workflow file  | http://www.wings-workflows.org/ontology/workflow.owl  |

Again, these properties should not be modified unless you are an advanced user with knowledge of the WINGS ontology.

### Execution and engine

The "Execution" section lists the available engines for running your workflows.

| Name           | Description                                                |
| -------------- | ---------------------------------------------------------- |
| name           | A short name to describe the engine                        |
| implementation | Java path where the implementation is                      |
| type           | Modify if the engine is used in the planning, step or both |

WINGS supports the following engines:

1. `local` runs the workflow as a UNIX process.
2. `distributed` runs the workflows using round robin, connecting to the server using the SSH protocol.

By default, the local engine is activated.

```json
execution =
    {
        engine =
        {
            name = Local;
            implementation = edu.isi.wings.execution.engine.api.impl.local.LocalExecutionEngine;
            type = BOTH;
        }

        engine =
        {
            name = Distributed;
            implementation = edu.isi.wings.execution.engine.api.impl.distributed.DistributedExecutionEngine;
            type = BOTH;
        }
    }
```

## Publisher

The publisher section of the configuration file describes how the Wings system shares data, execution, and provenance. The following properties are available:

```
    publisher =
    {
        file-store=
        {
            url = "https://publisher.mint.isi.edu";
            type = "FILE_SYSTEM";
        }
        triple-store = {
            export-name = "exportTest";
            export-url = "https://opmw.org/";
            publish = https://endpoint.mint.isi.edu/provenance/data;
            query = https://endpoint.mint.isi.edu/provenance/query;
            domains-directory = /opt/wings/storage/default;
        }
    }
```

### File store

The file store section of the configuration file describes how the Wings system shares data. The following properties are available:

| Name                      | Description                       | Default Value |
| ------------------------- | --------------------------------- | ------------- |
| publisher.file-store.url  | URI where the files are published |               |
| publisher.file-store.type | Type of file store (FILE_SYSTEM)  | FILE_SYSTEM   |

### Triple store

The triple store section of the configuration file describes how the Wings system shares execution and provenance. The following properties are available:

| Name                                     | Description                            | Default Value                                  |
| ---------------------------------------- | -------------------------------------- | ---------------------------------------------- |
| publisher.triple-store.export-name       | Name of the export (exportTest)        | exportTest                                     |
| publisher.triple-store.export-url        | URI where the files are published      | https://opmw.org/                              |
| publisher.triple-store.publish           | URI where the provenance is published  | https://endpoint.mint.isi.edu/provenance/data  |
| publisher.triple-store.query             | URI where the provenance is queried    | https://endpoint.mint.isi.edu/provenance/query |
| publisher.triple-store.domains-directory | Directory where the domains are stored | /opt/wings/storage/default                     |
