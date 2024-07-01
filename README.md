# eIDAS TSP Certificates extractor

This repository contains the script for extracting CA certificates from XML files available through [eIDAS Trusted List](https://eidas.ec.europa.eu/efda/tl-browser/).

The script uses the API of the eIDAS Trusted List, which is described at https://eidas.ec.europa.eu/efda/swagger-ui/index.html.


## Prerequisites

- Java 17 or higher
- lxml library
- requests library

## Usage

```bash
java CAExtractor.java [-h] {QWAC,QSealC} country [--target_folder TARGET_FOLDER]
```

### Positional Arguments

- `{QWAC,QSealC}`: Type of service to retrieve certificate for. QWAC - Qualified certificate for website authentication; QSealC - Qualified certificate for electronic seal.
- `country`: ISO 3166-1 alpha-2 country code (only EEA countries are supported).

### Optional Arguments

- `--target_folder`: Target folder to save certificate files in.

### Example

To extract QWAC CA certificates for QTSPs based in Germany and save them in the certs folder, run:

```
java CAExtractor.java QWAC DE --target_folder certs
```
