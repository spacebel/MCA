# Metrics Client Application (MCA)

## Introduction

The Metrics Client application provides a Web-based user interface allowing to browse catalogue access metrics.  The metrics visualised comprise the following groups defined by the CEOS WGISS System Level Team:

*	Global Core Metrics
*	Data Provider Core Metrics
*	Collection Core Metrics


## External interfaces

The Metrics Client Application retrieves raw catalogue requests from the Elasticsearch engine.  In the ESE-ERGO deployment, this search engine is part of the Kubernetes Cluster installation.  The Elasticsearch database is populated with raw access information extracted from the NGINX load-balancer access.log file by a data collector (Fluentd).  Information is captured about incoming catalogue requests (either requests for an OSDD or actual granule search requests).

In addition, it interfaces with an OpenSearch (collection) catalogue to obtain the list of collections available in the system and their mapping on “organisations”.
Finally, it published daily summaries of metrics as CSV files on an FTP server for future consumption by a corporate dashboard (e.g. ESA TellUs).  
 


## User interface

The following screen captures give some examples of functionality available to the users through the Web-based user interface.  

![Global metrics](/images/global-metrics.png)

TBD.

![Provider metrics](/images/provider-metrics.png)

TBD.

![Collection metrics](/images/collection-metrics.png)


## Resources

* References
  * [ESE-ERGO Project Page](https://wiki.services.eoportal.org/tiki-index.php?page=ESE-ERGO)
  * [OGC 13-026r9, OGC OpenSearch Extension for Earth Observation](https://docs.opengeospatial.org/is/13-026r9/13-026r9.html)
  * [CEOS OpenSearch Best Practice v1.3](https://ceos.org/document_management/Working_Groups/WGISS/Documents/WGISS%20Best%20Practices/CEOS%20OpenSearch%20Best%20Practice.pdf) 
  
* Presentations
  * [ESE-ERGO Final Presentation, 28/05/2021](./documentation/20210528-ESE-ERGO-FP-Achievements-2-metadataeditor.pdf)  
  * [CEOS WGISS-50, fedEO Metadata Editor, 22/09/2020](http://ceos.org/document_management/Working_Groups/WGISS/Meetings/WGISS-50/1.%20Tuesday%20Sept%2022/2020.09.22_fedeo_metadata_editor.pptx)

## Credits

This project has received funding from the [European Space Agency](https://esa.int) under the [General Support Technology Programme](http://www.esa.int/Enabling_Support/Space_Engineering_Technology/Shaping_the_Future/About_the_General_Support_Technology_Programme_GSTP) and was supported by the [Belgian Science Policy Office](https://www.belspo.be/belspo/index_en.stm).
